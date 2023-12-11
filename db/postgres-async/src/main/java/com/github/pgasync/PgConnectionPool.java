/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync;

import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.Listening;
import com.github.pgasync.net.PreparedStatement;
import com.github.pgasync.net.Row;
import com.github.pgasync.net.SqlException;
import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.Transaction;
import org.pragmatica.lang.Promise;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Resource pool for backend connections.
 *
 * @author Antti Laisi
 */
public class PgConnectionPool extends PgConnectible {

    private class PooledPgConnection implements Connection {

        private class PooledPgTransaction implements Transaction {

            private final Transaction delegate;

            PooledPgTransaction(Transaction delegate) {
                this.delegate = delegate;
            }

            public CompletableFuture<Void> commit() {
                return delegate.commit();
            }

            public CompletableFuture<Void> rollback() {
                return delegate.rollback();
            }

            public CompletableFuture<Void> close() {
                return delegate.close();
            }

            public CompletableFuture<Transaction> begin() {
                return delegate.begin().thenApply(PooledPgTransaction::new);
            }

            @Override
            public CompletableFuture<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                    Consumer<Row> onRow,
                                                    String sql,
                                                    Object... params) {
                return delegate.query(onColumns, onRow, sql, params);
            }

            @Override
            public CompletableFuture<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                  Consumer<Row> onRow,
                                                  Consumer<Integer> onAffected,
                                                  String sql) {
                return delegate.script(onColumns, onRow, onAffected, sql);
            }

            public Connection getConnection() {
                return PooledPgConnection.this;
            }
        }

        private final PgConnection delegate;
        private PooledPgPreparedStatement evicted;
        private final LinkedHashMap<String, PooledPgPreparedStatement> statements = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, PooledPgPreparedStatement> eldest) {
                if (size() > maxStatements) {
                    evicted = eldest.getValue();
                    return true;
                } else {
                    return false;
                }
            }
        };

        PooledPgConnection(PgConnection delegate) {
            this.delegate = delegate;
        }

        CompletableFuture<Connection> connect(String username, String password, String database) {
            return delegate.connect(username, password, database).thenApply(conn -> PooledPgConnection.this);
        }

        public boolean isConnected() {
            return delegate.isConnected();
        }

        private void closeNextStatement(Iterator<PooledPgPreparedStatement> statementsSource, CompletableFuture<Void> onComplete) {
            if (statementsSource.hasNext()) {
                statementsSource.next().delegate.close()
                                                .thenAccept(v -> {
                                                    statementsSource.remove();
                                                    closeNextStatement(statementsSource, onComplete);
                                                })
                                                .exceptionally(th -> {
                                                    Promise.runAsync(() -> onComplete.completeExceptionally(th));
                                                    return null;
                                                });
            } else {
                Promise.runAsync(() -> onComplete.complete(null));
            }
        }

        CompletableFuture<Void> shutdown() {
            CompletableFuture<Void> onComplete = new CompletableFuture<>();
            closeNextStatement(statements.values().iterator(), onComplete);
            return onComplete
                .thenApply(v -> {
                    if (!statements.isEmpty()) {
                        throw new IllegalStateException(STR."Stale prepared statements detected (\{statements.size()})");
                    }
                    return delegate.close();
                })
                .thenCompose(Function.identity());
        }

        @Override
        public CompletableFuture<Void> close() {
            release(this);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Listening> subscribe(String channel, Consumer<String> onNotification) {
            return delegate.subscribe(channel, onNotification);
        }

        @Override
        public CompletableFuture<Transaction> begin() {
            return delegate.begin().thenApply(PooledPgTransaction::new);
        }

        @Override
        public CompletableFuture<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<Row> onRow,
                                              Consumer<Integer> onAffected,
                                              String sql) {
            return delegate.script(onColumns, onRow, onAffected, sql);
        }

        @Override
        public CompletableFuture<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                Consumer<Row> onRow,
                                                String sql,
                                                Object... params) {
            return prepareStatement(sql, dataConverter.assumeTypes(params))
                .thenApply(stmt ->
                               stmt.fetch(onColumns, onRow, params)
                                   .handle((affected, th) ->
                                               stmt.close()
                                                   .thenApply(v -> {
                                                       if (th == null) {
                                                           return affected;
                                                       } else {
                                                           throw new RuntimeException(th);
                                                       }
                                                   })
                                   ).thenCompose(Function.identity())
                ).thenCompose(Function.identity());
        }

        @Override
        public CompletableFuture<PreparedStatement> prepareStatement(String sql, Oid... parametersTypes) {
            PooledPgPreparedStatement statement = statements.remove(sql);
            if (statement != null) {
                return CompletableFuture.completedFuture(statement);
            } else {
                return delegate.preparedStatementOf(sql, parametersTypes)
                               .thenApply(stmt -> new PooledPgPreparedStatement(sql, stmt));
            }
        }

        private class PooledPgPreparedStatement implements PreparedStatement {

            private static final String
                DUPLICATED_PREPARED_STATEMENT_DETECTED =
                "Duplicated prepared statement detected. Closing extra instance. \n{0}";
            private final String sql;
            private final PgConnection.PgPreparedStatement delegate;

            private PooledPgPreparedStatement(String sql, PgConnection.PgPreparedStatement delegate) {
                this.sql = sql;
                this.delegate = delegate;
            }

            @Override
            public CompletableFuture<Void> close() {
                PooledPgPreparedStatement already = statements.put(sql, this);
                if (evicted != null) {
                    try {
                        if (already != null && already != evicted) {
                            Logger.getLogger(PgConnectionPool.class.getName()).log(Level.WARNING,
                                                                                   DUPLICATED_PREPARED_STATEMENT_DETECTED,
                                                                                   already.sql);
                            return evicted.delegate.close()
                                                   .thenApply(v -> already.delegate.close())
                                                   .thenCompose(Function.identity());
                        } else {
                            return evicted.delegate.close();
                        }
                    } finally {
                        evicted = null;
                    }
                } else {
                    if (already != null) {
                        Logger.getLogger(PgConnectionPool.class.getName()).log(Level.WARNING, DUPLICATED_PREPARED_STATEMENT_DETECTED, already.sql);
                        return already.delegate.close();
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                }
            }

            @Override
            public CompletableFuture<ResultSet> query(Object... params) {
                return delegate.query(params);
            }

            @Override
            public CompletableFuture<Integer> fetch(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                    Consumer<Row> processor,
                                                    Object... params) {
                return delegate.fetch(onColumns, processor, params);
            }
        }
    }

    private final int maxConnections;
    private final int maxStatements;

    private final Lock guard = new ReentrantLock();
    private int size;
    private final Queue<CompletableFuture<? super Connection>> pending = new LinkedList<>();
    private final Queue<PooledPgConnection> connections = new LinkedList<>();
    private CompletableFuture<Void> closing;

    public PgConnectionPool(ConnectibleBuilder.ConnectibleProperties properties,
                            Supplier<CompletableFuture<ProtocolStream>> obtainStream) {
        super(properties, obtainStream);
        this.maxConnections = properties.getMaxConnections();
        this.maxStatements = properties.getMaxStatements();
    }

    private <T> T locked(Supplier<T> action) {
        guard.lock();
        try {
            return action.get();
        } finally {
            guard.unlock();
        }
    }

    private void release(PooledPgConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("'connection' should be not null");
        }
        Runnable lucky = locked(() -> {
            CompletableFuture<? super Connection> nextUser = pending.poll();
            if (nextUser != null) {
                return () -> nextUser.complete(connection);
            } else {
                connections.add(connection);
                return checkClosed();
            }
        });
        Promise.runAsync(lucky);
    }

    @Override
    public CompletableFuture<Connection> getConnection() {
        if (locked(() -> closing != null)) {
            return CompletableFuture.failedFuture(new SqlException("Connection pool is closed"));
        } else {
            Connection cached = locked(this::firstAliveConnection);
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            } else {
                CompletableFuture<Connection> deferred = new CompletableFuture<>();
                boolean makeNewConnection = locked(() -> {
                    pending.add(deferred);
                    if (size < maxConnections) {
                        size++;
                        return true;
                    } else {
                        return false;
                    }
                });
                if (makeNewConnection) {
                    obtainStream.get()
                                .thenApply(stream -> new PooledPgConnection(new PgConnection(stream, dataConverter))
                                    .connect(username, password, database))
                                .thenCompose(Function.identity())
                                .thenApply(pooledConnection -> {
                                    if (validationQuery != null && !validationQuery.isBlank()) {
                                        return pooledConnection.completeScript(validationQuery)
                                                               .handle((rss, th) -> {
                                                                   if (th != null) {
                                                                       return ((PooledPgConnection) pooledConnection).delegate.close()
                                                                                                                              .thenApply(v -> CompletableFuture.<Connection>failedFuture(
                                                                                                                                  th))
                                                                                                                              .thenCompose(Function.identity());
                                                                   } else {
                                                                       return CompletableFuture.completedFuture(pooledConnection);
                                                                   }
                                                               })
                                                               .thenCompose(Function.identity());
                                    } else {
                                        return CompletableFuture.completedFuture(pooledConnection);
                                    }
                                })
                                .thenCompose(Function.identity())
                                .whenComplete((connected, th) -> {
                                    if (th == null) {
                                        release((PooledPgConnection) connected);
                                    } else {
                                        Collection<Runnable> actions = locked(() -> {
                                            size--;
                                            var unlucky = pending.stream()
                                                                 .<Runnable>map(item -> () -> item.completeExceptionally(th))
                                                                 .collect(Collectors.toList());
                                            unlucky.add(checkClosed());
                                            pending.clear();
                                            return unlucky;
                                        });
                                        actions.forEach(Promise::runAsync);
                                    }
                                });
                }
                return deferred;
            }
        }
    }

    private record CloseTuple(CompletableFuture<Void> closing, Runnable immediate) {}

    @Override
    public CompletableFuture<Void> close() {
        var tuple = locked(() -> {
            if (closing == null) {
                closing = new CompletableFuture<>()
                    .thenApply(_ -> locked(() ->
                                               CompletableFuture.allOf(connections.stream()
                                                                                  .map(PooledPgConnection::shutdown)
                                                                                  .toArray(CompletableFuture[]::new))
                    ))
                    .thenCompose(Function.identity());
                return new CloseTuple(closing, checkClosed());
            } else {
                return new CloseTuple(CompletableFuture.failedFuture(new IllegalStateException("PG pool is already shutting down")), NO_OP);
            }
        });
        Promise.runAsync(tuple.immediate);

        return tuple.closing;
    }

    private static final Runnable NO_OP = () -> {
    };

    private Runnable checkClosed() {
        if (closing != null && size <= connections.size()) {
            assert pending.isEmpty();
            return () -> closing.complete(null);
        } else {
            return NO_OP;
        }
    }

    private Connection firstAliveConnection() {
        Connection connection = connections.poll();
        while (connection != null && !connection.isConnected()) {
            size--;
            connection = connections.poll();
        }
        return connection;
    }
}
