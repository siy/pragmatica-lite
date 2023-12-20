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

import com.github.pgasync.async.IntermediatePromise;
import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.message.backend.DataRow;
import com.github.pgasync.message.frontend.Bind;
import com.github.pgasync.message.frontend.Close;
import com.github.pgasync.message.frontend.Describe;
import com.github.pgasync.message.frontend.Parse;
import com.github.pgasync.message.frontend.Query;
import com.github.pgasync.message.frontend.StartupMessage;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.Listening;
import com.github.pgasync.net.PreparedStatement;
import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.Row;
import com.github.pgasync.net.Transaction;

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
        public IntermediatePromise<ResultSet> query(Object... params) {
            var rows = new ArrayList<Row>();

            return fetch((_, _) -> {}, rows::add, params)
                .map(_ -> new PgResultSet(columns.byName, columns.ordered, rows, 0));
        }

        //TODO: consider conversion to "reducer" style to eliminate externally stored state
        @Override
        public IntermediatePromise<Integer> fetch(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns, Consumer<Row> processor, Object... params) {
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
        public IntermediatePromise<Void> close() {
            return stream.send(Close.statement(sname))
                         .onSuccess(_ -> {});
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
                prefix = STR."_\{prefix}";
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

    IntermediatePromise<Connection> connect(String username, String password, String database) {
        return stream.connect(new StartupMessage(username, database))
                     .flatMap(authentication -> authenticate(username, password, authentication))
                     .map(_ -> PgConnection.this);
    }

    private IntermediatePromise<? extends Message> authenticate(String username, String password, Message message) {
        return message instanceof Authentication authentication && !authentication.authenticationOk()
               ? stream.authenticate(username, password, authentication)
               : IntermediatePromise.successful(message);
    }

    public boolean isConnected() {
        return stream.isConnected();
    }

    @Override
    public IntermediatePromise<PreparedStatement> prepareStatement(String sql, Oid... parametersTypes) {
        return preparedStatementOf(sql, parametersTypes)
            .map(Fn1.id());
    }

    IntermediatePromise<PgPreparedStatement> preparedStatementOf(String sql, Oid... parametersTypes) {
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
    public IntermediatePromise<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                            Consumer<Row> onRow,
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
    public IntermediatePromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<Row> onRow,
                                              String sql,
                                              Object... params) {
        return prepareStatement(sql, dataConverter.assumeTypes(params))
            .flatMap(ps -> ps.fetch(onColumns, onRow, params)
                             .fold((affected, th) -> closePreparedStatement(ps, affected, th)))
            .flatMap(Fn1.id()); //Avoid race conditions
    }

    private static IntermediatePromise<Integer> closePreparedStatement(PreparedStatement ps, Integer affected, Throwable th) {
        return ps.close()
                 .map(_ -> {
                     if (th != null) {
                         throw new RuntimeException(th);
                     } else {
                         return affected;
                     }
                 });
    }

    @Override
    public IntermediatePromise<Transaction> begin() {
        return completeScript("BEGIN")
            .map(_ -> new PgConnectionTransaction());
    }

    public IntermediatePromise<Listening> subscribe(String channel, Consumer<String> onNotification) {
        // TODO: wait for commit before sending unlisten as otherwise it can be rolled back
        return completeScript(STR."LISTEN \{channel}")
            .map(_ -> {
                var unsubscribe = stream.subscribe(channel, onNotification);

                return () -> completeScript(STR."UNLISTEN \{channel}")
                    .onSuccess(_ -> unsubscribe.run());
            });
    }

    @Override
    public IntermediatePromise<Void> close() {
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
        public IntermediatePromise<Transaction> begin() {
            return completeScript("SAVEPOINT sp_1")
                .map(_ -> new PgConnectionNestedTransaction(1));
        }

        @Override
        public IntermediatePromise<Void> commit() {
            return PgConnection.this.completeScript("COMMIT")
                                    .map(_ -> null);
        }

        @Override
        public IntermediatePromise<Void> rollback() {
            return PgConnection.this.completeScript("ROLLBACK")
                                    .map(_ -> null);
        }

        @Override
        public IntermediatePromise<Void> close() {
            return commit()
                .onResult(this::handleException);
        }

        @Override
        public Connection getConnection() {
            return PgConnection.this;
        }

        @Override
        public IntermediatePromise<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                Consumer<Row> onRow,
                                                Consumer<Integer> onAffected,
                                                String sql) {
            return PgConnection.this.script(onColumns, onRow, onAffected, sql)
                                    .fold(this::handleException)
                                    .flatMap(Fn1.id());
        }

        public IntermediatePromise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                  Consumer<Row> onRow,
                                                  String sql,
                                                  Object... params) {
            return PgConnection.this.query(onColumns, onRow, sql, params)
                                    .fold(this::handleException)
                                    .flatMap(Fn1.id());
        }

        private <T> IntermediatePromise<T> handleException(T unused, Throwable th) {
            if (th != null) {
                return rollback()
                    .map(_ -> {
                        throw new RuntimeException(th);
                    });
            } else {
                return IntermediatePromise.successful(unused);
            }
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
        public IntermediatePromise<Transaction> begin() {
            return completeScript(STR."SAVEPOINT sp_\{depth + 1}")
                .map(_ -> new PgConnectionNestedTransaction(depth + 1));
        }

        @Override
        public IntermediatePromise<Void> commit() {
            return PgConnection.this.completeScript(STR."RELEASE SAVEPOINT sp_\{depth}")
                                    .map(_ -> null);
        }

        @Override
        public IntermediatePromise<Void> rollback() {
            return PgConnection.this.completeScript(STR."ROLLBACK TO SAVEPOINT sp_\{depth}")
                                    .map(_ -> null);
        }
    }
}
