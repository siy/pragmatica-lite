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

import com.github.pgasync.net.*;
import org.pragmatica.lang.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resource pool for backend connections.
 *
 * @author Antti Laisi
 */
public class PgConnectionPool extends PgConnectible {
    private static final Logger log = LoggerFactory.getLogger(PgConnectionPool.class);

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
            return delegate.connect(username, password, database).thenApply(_ -> PooledPgConnection.this);
        }

        public boolean isConnected() {
            return delegate.isConnected();
        }

        private void closeNextStatement(Iterator<PooledPgPreparedStatement> statementsSource, CompletableFuture<Void> onComplete) {
            if (statementsSource.hasNext()) {
                statementsSource.next().delegate.close()
                                                .thenAccept(_ -> {
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
                .thenApply(_ -> {
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
                .thenCompose(stmt ->
                                 stmt.fetch(onColumns, onRow, params)
                                     .handle((affected, th) ->
                                                 stmt.close()
                                                     .thenApply(_ -> {
                                                         if (th == null) {
                                                             return affected;
                                                         } else {
                                                             throw new RuntimeException(th);
                                                         }
                                                     })
                                     ).thenCompose(Function.identity()));
//                ).thenCompose(Function.identity());
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
                "Duplicated prepared statement detected. Closing extra instance. \n{}";
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
                            log.warn(DUPLICATED_PREPARED_STATEMENT_DETECTED, already.sql);
                            return evicted.delegate.close()
                                                   .thenCompose(_ -> already.delegate.close());
//                                                   .thenCompose(Function.identity());
                        } else {
                            return evicted.delegate.close();
                        }
                    } finally {
                        evicted = null;
                    }
                } else {
                    if (already != null) {
                        log.warn(DUPLICATED_PREPARED_STATEMENT_DETECTED, already.sql);
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

    public PgConnectionPool(ConnectibleBuilder.ConnectibleConfiguration properties,
                            Supplier<CompletableFuture<ProtocolStream>> obtainStream) {
        super(properties, obtainStream);
        this.maxConnections = properties.maxConnections();
        this.maxStatements = properties.maxStatements();
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
            var nextUser = pending.poll();

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
                var deferred = new CompletableFuture<Connection>();
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
                                .thenCompose(stream -> new PooledPgConnection(new PgConnection(stream, dataConverter))
                                    .connect(username, password, database))
                                .thenCompose(pooledConnection -> {
                                    if (validationQuery != null && !validationQuery.isBlank()) {
                                        return pooledConnection.completeScript(validationQuery)
                                                               .handle((_, th) -> {
                                                                   if (th != null) {
                                                                       return ((PooledPgConnection) pooledConnection).delegate
                                                                           .close()
                                                                           .thenCompose(_ -> CompletableFuture.<Connection>failedFuture(th));
                                                                   } else {
                                                                       return CompletableFuture.completedFuture(pooledConnection);
                                                                   }
                                                               })
                                                               .thenCompose(Function.identity());
                                    } else {
                                        return CompletableFuture.completedFuture(pooledConnection);
                                    }
                                })
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
                    .thenCompose(_ -> locked(() ->
                                                 CompletableFuture.allOf(connections.stream()
                                                                                    .map(PooledPgConnection::shutdown)
                                                                                    .toArray(CompletableFuture[]::new))
                    ));
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
