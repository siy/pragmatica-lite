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
import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.message.backend.DataRow;
import com.github.pgasync.message.frontend.*;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.Listening;
import com.github.pgasync.net.PreparedStatement;
import com.github.pgasync.net.Transaction;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.pgasync.message.backend.RowDescription.ColumnDescription;
import static org.pragmatica.lang.Functions.Fn1;

/**
 * A connection to Postgres backend. The postmaster forks a backend process for each connection. A connection can process only a single query at a
 * time.
 *
 * @author Antti Laisi
 */
public class PgConnection implements Connection {
    /**
     * Uses named server side prepared statement and named portal.
     */
    public class PgPreparedStatement implements PreparedStatement {
        private final String sname;
        private Columns columns;

        PgPreparedStatement(String sname) {
            this.sname = sname;
        }

        @Override
        public ThrowingPromise<PgResultSet> query(Object... params) {
            var rows = new ArrayList<PgRow>();

            return fetch((_, _) -> {}, rows::add, params)
                .map(_ -> new PgResultSet(columns.byName, columns.ordered, rows, 0));
        }

        //TODO: consider conversion to "reducer" style to eliminate externally stored state
        @Override
        public ThrowingPromise<Integer> fetch(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<PgRow> processor,
                                              Object... params) {
            var bind = new Bind(sname, dataConverter.fromParameters(params));
            Consumer<DataRow> rowProcessor = dataRow -> processor.accept(new PgRow(dataRow, columns.byName, columns.ordered, dataConverter));

            if (columns != null) {
                onColumns.accept(columns.byName, columns.ordered);
                return stream
                    .send(bind, rowProcessor);
            } else {
                return stream
                    .send(bind, Describe.portal(), columnDescriptions -> {
                        columns = calcColumns(columnDescriptions);
                        onColumns.accept(columns.byName, columns.ordered);
                    }, rowProcessor);
            }
        }

        @Override
        public ThrowingPromise<Unit> close() {
            return stream.send(Close.statement(sname))
                         .mapToUnit();
        }
    }

    public record Columns(Map<String, PgColumn> byName, PgColumn[] ordered) {}

    private static class NameSequence {
        private long counter;
        private String prefix;

        NameSequence(final String prefix) {
            this.prefix = prefix;
        }

        private String next() {
            if (counter == Long.MAX_VALUE) {
                counter = 0;
                prefix = "_" + prefix;
            }
            return prefix + ++counter;
        }
    }

    private final NameSequence preparedStatementNames = new NameSequence("s-");

    private final ProtocolStream stream;
    private final DataConverter dataConverter;

    private Columns currentColumns;

    PgConnection(ProtocolStream stream, DataConverter dataConverter) {
        this.stream = stream;
        this.dataConverter = dataConverter;
    }

    ThrowingPromise<Connection> connect(String username, String password, String database) {
        return stream.connect(new StartupMessage(username, database))
                     .flatMap(authentication -> authenticate(username, password, authentication))
                     .map(_ -> PgConnection.this);
    }

    private ThrowingPromise<? extends Message> authenticate(String username, String password, Message message) {
        return message instanceof Authentication authentication && !authentication.authenticationOk()
               ? stream.authenticate(username, password, authentication)
               : ThrowingPromise.successful(message);
    }

    public boolean isConnected() {
        return stream.isConnected();
    }

    @Override
    public ThrowingPromise<? extends PreparedStatement> prepareStatement(String sql, Oid... parametersTypes) {
        return preparedStatementOf(sql, parametersTypes);
    }

    ThrowingPromise<PgPreparedStatement> preparedStatementOf(String sql, Oid... parametersTypes) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("'sql' shouldn't be null or empty or blank string");
        }
        if (parametersTypes == null) {
            throw new IllegalArgumentException("'parametersTypes' shouldn't be null, atr least it should be empty");
        }

        var statementName = preparedStatementNames.next();

        return stream
            .send(new Parse(sql, statementName, parametersTypes))
            .map(_ -> new PgPreparedStatement(statementName));
    }

    @Override
    public ThrowingPromise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                        Consumer<PgRow> onRow,
                                        Consumer<Integer> onAffected,
                                        String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("'sql' shouldn't be null or empty or blank string");
        }

        return stream.send(
            new Query(sql),
            columnDescriptions -> {
                currentColumns = calcColumns(columnDescriptions);
                onColumns.accept(currentColumns.byName, currentColumns.ordered);
            },
            message -> onRow.accept(new PgRow(message, currentColumns.byName, currentColumns.ordered, dataConverter)),
            message -> {
                currentColumns = null;
                onAffected.accept(message.affectedRows());
            }
        );
    }

    @Override
    public ThrowingPromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                          Consumer<PgRow> onRow,
                                          String sql,
                                          Object... params) {
        return prepareStatement(sql, dataConverter.assumeTypes(params))
            .flatMap(ps -> ps.fetch(onColumns, onRow, params)
                             .fold(result -> closePreparedStatement(result, ps)));
    }

    private static ThrowingPromise<Integer> closePreparedStatement(Result<Integer> result, PreparedStatement ps) {
        return ps.close()
                 .map(_ -> result.fold(cause -> {
                                           throw new RuntimeException(((ThrowableCause) cause).throwable());
                                       },
                                       Fn1.id()));
    }

    @Override
    public ThrowingPromise<Transaction> begin() {
        return completeScript("BEGIN")
            .map(_ -> new PgConnectionTransaction());
    }

    public ThrowingPromise<Listening> subscribe(String channel, Consumer<String> onNotification) {
        // TODO: wait for commit before sending unlisten as otherwise it can be rolled back
        return completeScript("LISTEN " + channel)
            .map(_ -> {
                var unsubscribe = stream.subscribe(channel, onNotification);

                return () -> completeScript("UNLISTEN " + channel).onSuccess(_ -> unsubscribe.run())
                                                                  .mapToUnit();
            });
    }

    @Override
    public ThrowingPromise<Unit> close() {
        return stream.close();
    }

    private static Columns calcColumns(ColumnDescription[] descriptions) {
        var byName = new HashMap<String, PgColumn>();
        var ordered = new PgColumn[descriptions.length];

        for (int i = 0; i < descriptions.length; i++) {
            var column = new PgColumn(i, descriptions[i].getName(), descriptions[i].getType());

            byName.put(descriptions[i].getName(), column);
            ordered[i] = column;
        }
        return new Columns(Map.copyOf(byName), ordered);
    }

    /**
     * Transaction that rollbacks the tx on backend error and closes the connection on COMMIT/ROLLBACK failure.
     */
    class PgConnectionTransaction implements Transaction {
        @Override
        public ThrowingPromise<Transaction> begin() {
            return completeScript("SAVEPOINT sp_1")
                .map(_ -> new PgConnectionNestedTransaction(1));
        }

        @Override
        public ThrowingPromise<Unit> commit() {
            return PgConnection.this.completeScript("COMMIT")
                                    .map(Unit::toUnit);
        }

        @Override
        public ThrowingPromise<Unit> rollback() {
            return PgConnection.this.completeScript("ROLLBACK")
                                    .map(Unit::toUnit);
        }

        @Override
        public ThrowingPromise<Unit> close() {
            return commit()
                .fold(this::handleException);
        }

        @Override
        public Connection getConnection() {
            return PgConnection.this;
        }

        @Override
        public ThrowingPromise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                            Consumer<PgRow> onRow,
                                            Consumer<Integer> onAffected,
                                            String sql) {
            return PgConnection.this.script(onColumns, onRow, onAffected, sql)
                                    .fold(this::handleException);
        }

        public ThrowingPromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<PgRow> onRow,
                                              String sql,
                                              Object... params) {
            return PgConnection.this.query(onColumns, onRow, sql, params)
                                    .fold(this::handleException);
        }

        private <T> ThrowingPromise<T> handleException(Result<T> result) {
            return result.fold(cause -> rollback().fold(_ -> ThrowingPromise.failed((ThrowableCause) cause)),
                               ThrowingPromise::successful);
        }
    }

    /**
     * Nested transaction using savepoints.
     */
    class PgConnectionNestedTransaction extends PgConnectionTransaction {
        final int depth;

        PgConnectionNestedTransaction(int depth) {
            this.depth = depth;
        }

        @Override
        public ThrowingPromise<Transaction> begin() {
            return completeScript("SAVEPOINT sp_" + (depth + 1))
                .map(_ -> new PgConnectionNestedTransaction(depth + 1));
        }

        @Override
        public ThrowingPromise<Unit> commit() {
            return PgConnection.this.completeScript("RELEASE SAVEPOINT sp_" + depth)
                                    .map(Unit::toUnit);
        }

        @Override
        public ThrowingPromise<Unit> rollback() {
            return PgConnection.this.completeScript("ROLLBACK TO SAVEPOINT sp_" + depth)
                                    .map(Unit::toUnit);
        }
    }
}
