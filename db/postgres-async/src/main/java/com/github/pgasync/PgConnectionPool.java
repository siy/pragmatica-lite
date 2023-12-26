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

import com.github.pgasync.async.ThrowableCause;
import com.github.pgasync.async.ThrowingPromise;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.Listening;
import com.github.pgasync.net.PreparedStatement;
import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.Row;
import com.github.pgasync.net.SqlException;
import com.github.pgasync.net.Transaction;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.lang.Functions.Fn1;

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

            public ThrowingPromise<Unit> commit() {
                return delegate.commit();
            }

            public ThrowingPromise<Unit> rollback() {
                return delegate.rollback();
            }

            public ThrowingPromise<Unit> close() {
                return delegate.close();
            }

            public ThrowingPromise<Transaction> begin() {
                return delegate.begin().map(PooledPgTransaction::new);
            }

            @Override
            public ThrowingPromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                  Consumer<Row> onRow,
                                                  String sql,
                                                  Object... params) {
                return delegate.query(onColumns, onRow, sql, params);
            }

            @Override
            public ThrowingPromise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
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

        ThrowingPromise<Connection> connect(String username, String password, String database) {
            return delegate.connect(username, password, database).map(_ -> PooledPgConnection.this);
        }

        public boolean isConnected() {
            return delegate.isConnected();
        }

        private void closeNextStatement(Iterator<PooledPgPreparedStatement> statementsSource, ThrowingPromise<Unit> onComplete) {
            if (statementsSource.hasNext()) {
                statementsSource.next().delegate.close()
                                                .onSuccess(_ -> {
                                                    statementsSource.remove();
                                                    closeNextStatement(statementsSource, onComplete);
                                                })
                                                .tryRecover(th -> {
                                                    onComplete.failAsync(() -> th);
                                                    return Unit.aUnit();
                                                });
            } else {
                onComplete.succeedAsync(Unit::aUnit);
            }
        }

        ThrowingPromise<Unit> shutdown() {
            var onComplete = ThrowingPromise.<Unit>create();

            closeNextStatement(statements.values().iterator(), onComplete);

            return onComplete
                .flatMap(_ -> {
                    if (!statements.isEmpty()) {
                        throw new IllegalStateException(STR."Stale prepared statements detected (\{statements.size()})");
                    }
                    return delegate.close();
                });
        }

        @Override
        public ThrowingPromise<Unit> close() {
            release(this);
            return ThrowingPromise.successful(Unit.aUnit());
        }

        @Override
        public ThrowingPromise<Listening> subscribe(String channel, Consumer<String> onNotification) {
            return delegate.subscribe(channel, onNotification);
        }

        @Override
        public ThrowingPromise<Transaction> begin() {
            return delegate.begin().map(PooledPgTransaction::new);
        }

        @Override
        public ThrowingPromise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                            Consumer<Row> onRow,
                                            Consumer<Integer> onAffected,
                                            String sql) {
            return delegate.script(onColumns, onRow, onAffected, sql);
        }

        @Override
        public ThrowingPromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<Row> onRow,
                                              String sql,
                                              Object... params) {
            return prepareStatement(sql, dataConverter.assumeTypes(params))
                .flatMap(stmt ->
                             stmt.fetch(onColumns, onRow, params)
                                 .fold(result ->
                                           stmt.close()
                                               .map(_ -> result.fold(
                                                   cause -> {throw new RuntimeException(((ThrowableCause) cause).throwable());},
                                                   Fn1.id()))));
        }

        @Override
        public ThrowingPromise<PreparedStatement> prepareStatement(String sql, Oid... parametersTypes) {
            PooledPgPreparedStatement statement = statements.remove(sql);
            if (statement != null) {
                return ThrowingPromise.successful(statement);
            } else {
                return delegate.preparedStatementOf(sql, parametersTypes)
                               .map(stmt -> new PooledPgPreparedStatement(sql, stmt));
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
            public ThrowingPromise<Unit> close() {
                PooledPgPreparedStatement already = statements.put(sql, this);
                if (evicted != null) {
                    try {
                        if (already != null && already != evicted) {
                            log.warn(DUPLICATED_PREPARED_STATEMENT_DETECTED, already.sql);
                            return evicted.delegate.close()
                                                   .flatMap(_ -> already.delegate.close());
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
                        return ThrowingPromise.successful(Unit.aUnit());
                    }
                }
            }

            @Override
            public ThrowingPromise<ResultSet> query(Object... params) {
                return delegate.query(params);
            }

            @Override
            public ThrowingPromise<Integer> fetch(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
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
    private final Queue<ThrowingPromise<? super Connection>> pending = new LinkedList<>();
    private final Queue<PooledPgConnection> connections = new LinkedList<>();
    private ThrowingPromise<Unit> closing;

    public PgConnectionPool(ConnectibleBuilder.ConnectibleConfiguration properties,
                            Supplier<ThrowingPromise<ProtocolStream>> obtainStream) {
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
                return () -> nextUser.succeed(connection);
            } else {
                connections.add(connection);
                return checkClosed();
            }
        });
        Promise.async(lucky);
    }

    @Override
    public ThrowingPromise<Connection> getConnection() {
        if (locked(() -> closing != null)) {
            return ThrowingPromise.failed(new SqlException("Connection pool is closed"));
        } else {
            var cached = locked(this::firstAliveConnection);

            if (cached != null) {
                return ThrowingPromise.successful(cached);
            } else {
                var deferred = ThrowingPromise.<Connection>create();
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
                                .flatMap(stream -> new PooledPgConnection(new PgConnection(stream, dataConverter))
                                    .connect(username, password, database))
                                .flatMap(pooledConnection -> {
                                    if (validationQuery != null && !validationQuery.isBlank()) {
                                        return pooledConnection.completeScript(validationQuery)
                                                               .fold(result ->
                                                                         result.fold(
                                                                             cause -> ((PooledPgConnection) pooledConnection).delegate
                                                                                 .close()
                                                                                 .flatMap(_ -> ThrowingPromise.failed(((ThrowableCause) cause).throwable())),
                                                                             _ -> ThrowingPromise.successful(pooledConnection)
                                                                         ));
                                    } else {
                                        return ThrowingPromise.successful(pooledConnection);
                                    }
                                })
                                .onResult(result -> result.fold(
                                    cause -> {
                                        var th = ((ThrowableCause) cause).throwable();
                                        var actions = locked(() -> {
                                            size--;
                                            var unlucky = Stream.concat(
                                                                    pending.stream()
                                                                           .map(item -> () -> item.fail(th)),
                                                                    Stream.of(checkClosed()))
                                                                .toList();
                                            pending.clear();
                                            return unlucky;
                                        });
                                        actions.forEach(Promise::async);
                                        return null;
                                    },
                                    connected -> {
                                        release((PooledPgConnection) connected);
                                        return null;
                                    }));
                }
                return deferred;
            }
        }
    }

    private record CloseTuple(ThrowingPromise<Unit> closing, Runnable immediate) {}

    @Override
    public ThrowingPromise<Unit> close() {
        var tuple = locked(() -> {
            if (closing == null) {
                closing = ThrowingPromise.create()
                                         .flatMap(_ ->
                                                          locked(() ->
                                                                     ThrowingPromise.allOf(connections.stream()
                                                                                                      .map(PooledPgConnection::shutdown))
                                                          ));
                return new CloseTuple(closing, checkClosed());
            } else {
                return new CloseTuple(ThrowingPromise.failed(new IllegalStateException("PG pool is already shutting down")), NO_OP);
            }
        });
        Promise.async(tuple.immediate);

        return tuple.closing;
    }

    private static final Runnable NO_OP = () -> {};

    private Runnable checkClosed() {
        if (closing != null && size <= connections.size()) {
            assert pending.isEmpty();
            return () -> closing.succeed(Unit.aUnit());
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
