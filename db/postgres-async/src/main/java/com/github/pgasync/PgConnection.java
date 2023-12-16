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

import com.github.pgasync.conversion.DataConverter;
import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.Authentication;
import com.github.pgasync.message.backend.DataRow;
import com.github.pgasync.message.frontend.*;
import com.github.pgasync.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.pgasync.message.backend.RowDescription.ColumnDescription;

/**
 * A connection to Postgres backend. The postmaster forks a backend process for each connection. A connection can process only a single query at a
 * time.
 *
 * @author Antti Laisi
 */
public class PgConnection implements Connection {
    private static final Logger log = LoggerFactory.getLogger(PgConnection.class);

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
        public CompletableFuture<ResultSet> query(Object... params) {
            var rows = new ArrayList<Row>();

            return fetch((_, _) -> {}, rows::add, params)
                .thenApply(_ -> new PgResultSet(columns.byName, columns.ordered, rows, 0));
        }

        //TODO: consider conversion to "reducer" style to eliminate externally stored state
        @Override
        public CompletableFuture<Integer> fetch(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns, Consumer<Row> processor, Object... params) {
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
        public CompletableFuture<Void> close() {
            return stream.send(Close.statement(sname))
                         .thenAccept(_ -> {});
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

    CompletableFuture<Connection> connect(String username, String password, String database) {
        return stream.connect(new StartupMessage(username, database))
                     .thenApply(authentication -> authenticate(username, password, authentication))
                     .thenCompose(Function.identity())
                     .thenApply(_ -> PgConnection.this);
    }

    private CompletableFuture<? extends Message> authenticate(String username, String password, Message message) {
        return message instanceof Authentication authentication && !authentication.authenticationOk()
               ? stream.authenticate(username, password, authentication)
               : CompletableFuture.completedFuture(message);
    }

    public boolean isConnected() {
        return stream.isConnected();
    }

    @Override
    public CompletableFuture<PreparedStatement> prepareStatement(String sql, Oid... parametersTypes) {
        return preparedStatementOf(sql, parametersTypes)
            .thenApply(Function.identity());
    }

    CompletableFuture<PgPreparedStatement> preparedStatementOf(String sql, Oid... parametersTypes) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("'sql' shouldn't be null or empty or blank string");
        }
        if (parametersTypes == null) {
            throw new IllegalArgumentException("'parametersTypes' shouldn't be null, atr least it should be empty");
        }

        var statementName = preparedStatementNames.next();

        return stream
            .send(new Parse(sql, statementName, parametersTypes))
            .thenApply(_ -> new PgPreparedStatement(statementName));
    }

    @Override
    public CompletableFuture<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
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
    public CompletableFuture<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                            Consumer<Row> onRow,
                                            String sql,
                                            Object... params) {
        return prepareStatement(sql, dataConverter.assumeTypes(params))
            .thenApply(ps -> ps.fetch(onColumns, onRow, params)
                               .handle((affected, th) -> ps.close()
                                                           .thenApply(_ -> {
                                                               if (th != null) {
                                                                   throw new RuntimeException(th);
                                                               } else {
                                                                   return affected;
                                                               }
                                                           })
                               )
                               .thenCompose(Function.identity())
            )
            .thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Transaction> begin() {
        return completeScript("BEGIN")
            .thenApply(_ -> new PgConnectionTransaction());
    }

    public CompletableFuture<Listening> subscribe(String channel, Consumer<String> onNotification) {
        // TODO: wait for commit before sending unlisten as otherwise it can be rolled back
        return completeScript(STR."LISTEN \{channel}")
            .thenApply(_ -> {
                var unsubscribe = stream.subscribe(channel, onNotification);

                return () -> completeScript(STR."UNLISTEN \{channel}")
                    .thenAccept(_ -> unsubscribe.run());
            });
    }

    @Override
    public CompletableFuture<Void> close() {
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
        public CompletableFuture<Transaction> begin() {
            return completeScript("SAVEPOINT sp_1")
                .thenApply(_ -> new PgConnectionNestedTransaction(1));
        }

        CompletableFuture<Void> sendCommit() {
            return PgConnection.this.completeScript("COMMIT")
                                    .thenAccept(_ -> {});
        }

        CompletableFuture<Void> sendRollback() {
            return PgConnection.this.completeScript("ROLLBACK")
                                    .thenAccept(_ -> {});
        }

        @Override
        public CompletableFuture<Void> commit() {
            return sendCommit().thenAccept(_ -> {});
        }

        @Override
        public CompletableFuture<Void> rollback() {
            return sendRollback();
        }

        @Override
        public CompletableFuture<Void> close() {
            return sendCommit()
                .handle((v, th) -> {
                    if (th != null) {
                        log.error("Error during closing of the connection", th);
                        return sendRollback();
                    } else {
                        return CompletableFuture.completedFuture(v);
                    }
                })
                .thenCompose(Function.identity());
        }

        @Override
        public Connection getConnection() {
            return PgConnection.this;
        }

        @Override
        public CompletableFuture<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                              Consumer<Row> onRow,
                                              Consumer<Integer> onAffected,
                                              String sql) {
            return PgConnection.this.script(onColumns, onRow, onAffected, sql)
                                    .handle((_, th) -> {
                                        if (th != null) {
                                            return rollback()
                                                .thenAccept(_ -> {
                                                    throw new RuntimeException(th);
                                                });
                                        } else {
                                            return CompletableFuture.<Void>completedFuture(null);
                                        }
                                    })
                                    .thenCompose(Function.identity());
        }

        public CompletableFuture<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                                                Consumer<Row> onRow,
                                                String sql,
                                                Object... params) {
            return PgConnection.this.query(onColumns, onRow, sql, params)
                                    .handle((affected, th) -> {
                                        if (th != null) {
                                            return rollback()
                                                .<Integer>thenApply(_ -> {
                                                    throw new RuntimeException(th);
                                                });
                                        } else {
                                            return CompletableFuture.completedFuture(affected);
                                        }
                                    })
                                    .thenCompose(Function.identity());
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
        public CompletableFuture<Transaction> begin() {
            return completeScript(STR."SAVEPOINT sp_\{depth + 1}")
                .thenApply(_ -> new PgConnectionNestedTransaction(depth + 1));
        }

        @Override
        public CompletableFuture<Void> commit() {
            return PgConnection.this.completeScript(STR."RELEASE SAVEPOINT sp_\{depth}")
                                    .thenAccept(_ -> {});
        }

        @Override
        public CompletableFuture<Void> rollback() {
            return PgConnection.this.completeScript(STR."ROLLBACK TO SAVEPOINT sp_\{depth}")
                                    .thenAccept(_ -> {});
        }
    }
}
