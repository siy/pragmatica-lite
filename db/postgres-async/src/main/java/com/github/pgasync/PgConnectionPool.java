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
import org.pragmatica.lang.Unit;
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
import java.util.logging.Level;

import static org.pragmatica.lang.Unit.unitResult;

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

            @Override
            public Promise<Unit> commit() {
                return delegate.commit();
            }

            @Override
            public Promise<Unit> rollback() {
                return delegate.rollback();
            }

            @Override
            public Promise<Unit> close() {
                return delegate.close();
            }

            @Override
            public Promise<Transaction> begin() {
                return delegate.begin().map(PooledPgTransaction::new);
            }

            @Override
            public Promise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                          Consumer<Row> onRow,
                                          String sql,
                                          Object... params) {
                return delegate.query(onColumns, onRow, sql, params);
            }

            @Override
            public Promise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
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

        Promise<Connection> connect(String username, String password, String database) {
            return delegate.connect(username, password, database)
                           .map(_ -> PooledPgConnection.this);
        }

        public boolean isConnected() {
            return delegate.isConnected();
        }

        Promise<Unit> shutdown() {
            return Promise.allOf(statements.values()
                                           .stream()
                                           .map(stmt -> stmt.delegate.close())
                                           .toList())
                          .flatMap(_ -> delegate.close());
        }

        @Override
        public Promise<Unit> close() {
            release(this);
            return Promise.resolved(unitResult());
        }

        @Override
        public Promise<Listening> subscribe(String channel, Consumer<String> onNotification) {
            return delegate.subscribe(channel, onNotification);
        }

        @Override
        public Promise<Transaction> begin() {
            return delegate.begin()
                           .map(PooledPgTransaction::new);
        }

        @Override
        public Promise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<Row> onRow,
                                              Consumer<Integer> onAffected,
                                              String sql) {
            return delegate.script(onColumns, onRow, onAffected, sql);
        }

        @Override
        public Promise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
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
        public Promise<PreparedStatement> prepareStatement(String sql, Oid... parametersTypes) {
            var statement = statements.remove(sql);
            if (statement != null) {
                return Promise.successful(statement);
            } else {
                return delegate.preparedStatementOf(sql, parametersTypes)
                               .map(stmt -> new PooledPgPreparedStatement(sql, stmt));
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
            public Promise<Unit> close() {
                var already = statements.put(sql, this);

                if (evicted != null) {
                    try {
                        if (already != null && already != evicted) {
                            Logger.getLogger(PgConnectionPool.class.getName()).log(Level.WARNING,
                                                                                   DUPLICATED_PREPARED_STATEMENT_DETECTED,
                                                                                   already.sql);
                            return evicted.delegate.close()
                                                   .onResultDo(already.delegate::close);
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
                        return Promise.resolved(unitResult());
                    }
                }
            }

            @Override
            public Promise<ResultSet> query(Object... params) {
                return delegate.query(params);
            }

            @Override
            public Promise<Integer> fetch(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
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
    private final Queue<Promise<? super Connection>> pending = new LinkedList<>();
    private final Queue<PooledPgConnection> connections = new LinkedList<>();
    private Promise<Unit> closing;

    public PgConnectionPool(ConnectibleBuilder.ConnectibleConfiguration properties,
                            Supplier<Promise<ProtocolStream>> obtainStream) {
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
    public Promise<Connection> connection() {
        if (locked(() -> closing != null)) {
            return Promise.failed(new SqlError.ConnectionPoolClosed("Connection pool is closed"));
        } else {
            var cached = locked(this::firstAliveConnection);

            if (cached != null) {
                return Promise.successful(cached);
            } else {
                var deferred = Promise.<Connection>promise();

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
                                .flatMap(stream -> new PooledPgConnection(new PgConnection(stream, dataConverter)).connect(username,
                                                                                                                           password,
                                                                                                                           database))
                                .flatMap(pooledConnection -> {
                                    if (validationQuery != null && !validationQuery.isBlank()) {
                                        return pooledConnection.completeScript(validationQuery)
                                                               .map(_ -> pooledConnection)
                                                               .onFailureDo(() -> ((PooledPgConnection) pooledConnection).delegate.close());
                                    } else {
                                        return Promise.successful(pooledConnection);
                                    }
                                }).onResult(result -> result.onSuccess(connection -> release((PooledPgConnection) connection))
                                                            .onFailureDo(() -> {
                                                                var promises = locked(() -> {
                                                                    List<Promise<? super Connection>> unlucky = new ArrayList<>(pending);
                                                                    pending.clear();
                                                                    return unlucky;
                                                                });
                                                                Promise.cancelAll(promises);
                                                            }));
                }
                return deferred;
            }
        }
    }

    @Override
    public Promise<Unit> close() {
        return locked(() -> {
            if (closing == null) {
                closing = Promise.allOf(connections.stream()
                                                   .map(PooledPgConnection::shutdown)
                                                   .toList())
                                 .map(Unit::unit);
            }
            return closing;
        });
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
