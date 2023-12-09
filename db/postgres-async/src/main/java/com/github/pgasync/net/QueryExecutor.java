package com.github.pgasync.net;

import com.github.pgasync.PgColumn;
import com.github.pgasync.PgResultSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * QueryExecutor submits SQL for execution.
 *
 * @author Antti Laisi
 */
public interface QueryExecutor {

    /**
     * Sends parameter less query script. The script may be multi query. Queries are separated with semicolons.
     * Accumulates fetched columns, rows and affected rows counts into memory and transforms them into a ResultSet when each {@link ResultSet} is fetched.
     * Completes returned {@link CompletableFuture} when the whole process of multiple {@link ResultSet}s fetching ends.
     *
     * @param sql Sql Script text.
     * @return CompletableFuture that is completed with a collection of fetched {@link ResultSet}s.
     */
    default CompletableFuture<Collection<ResultSet>> completeScript(String sql) {
        List<ResultSet> results = new ArrayList<>();
        class ResultSetAssembly {
            private Map<String, PgColumn> columnsByName;
            private PgColumn[] orderedColumns;
            private List<Row> rows;

            private void reset() {
                columnsByName = null;
                orderedColumns = null;
                rows = null;
            }
        }
        ResultSetAssembly assembly = new ResultSetAssembly();
        return script(
                (columnsByName, orderedColumns) -> {
                    assembly.columnsByName = columnsByName;
                    assembly.orderedColumns = orderedColumns;
                    assembly.rows = new ArrayList<>();
                },
                row -> assembly.rows.add(row),
                affected -> {
                    results.add(new PgResultSet(
                            assembly.columnsByName != null ? assembly.columnsByName : Map.of(),
                            assembly.orderedColumns != null ? List.of(assembly.orderedColumns) : List.of(),
                            assembly.rows != null ? assembly.rows : List.of(),
                            affected
                    ));
                    assembly.reset();
                },
                sql
        )
                .thenApply(v -> results);

    }

    /**
     * Sends parameter less query script. The script may be multi query. Queries are separated with semicolons.
     * Unlike {@link #completeScript(String)} doesn't accumulate fetched columns, rows and affected rows counts into memory.
     * Instead it calls passed in consumers, when columns, or particular row or an affected rows count is fetched from Postgres.
     * Completes returned {@link CompletableFuture} when the whole process of multiple {@link ResultSet}s fetching ends.
     *
     * @param onColumns  Columns fetched callback consumer.
     * @param onRow      A row fetched callback consumer.
     * @param onAffected An affected rows callback consumer.
     *                   It is called when a particular {@link ResultSet} is completely fetched with its affected rows count.
     *                   This callback should be used to create a {@link ResultSet} instance from already fetched columns, rows and affected rows count.
     * @param sql        Sql Script text.
     * @return CompletableFuture that is completed when the whole process of multiple {@link ResultSet}s fetching ends.
     */
    CompletableFuture<Void> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns, Consumer<Row> onRow, Consumer<Integer> onAffected, String sql);

    /**
     * Sends single query with parameters. Uses extended query protocol of Postgres.
     * Accumulates fetched columns, rows and affected rows count into memory and transforms them into a {@link ResultSet} when it is fetched.
     * Completes returned {@link CompletableFuture} when the whole process of {@link ResultSet} fetching ends.
     *
     * @param sql    Sql query text with parameters substituted with ?.
     * @param params Parameters of the query.
     * @return CompletableFuture of {@link ResultSet}.
     */
    default CompletableFuture<ResultSet> completeQuery(String sql, Object... params) {
        class ResultSetAssembly {
            private Map<String, PgColumn> columnsByName;
            private PgColumn[] orderedColumns;
            private List<Row> rows;
        }
        ResultSetAssembly assembly = new ResultSetAssembly();
        return query(
                (columnsByName, orderedColumns) -> {
                    assembly.columnsByName = columnsByName;
                    assembly.orderedColumns = orderedColumns;
                    assembly.rows = new ArrayList<>();
                },
                row -> assembly.rows.add(row),
                sql,
                params
        )
                .thenApply(affected -> new PgResultSet(
                        assembly.columnsByName != null ? assembly.columnsByName : Map.of(),
                        assembly.orderedColumns != null ? List.of(assembly.orderedColumns) : List.of(),
                        assembly.rows != null ? assembly.rows : List.of(), affected
                ));

    }

    /**
     * Sends single query with parameters. Uses extended query protocol of Postgres.
     * Unlike {@link #completeQuery(String, Object...)} doesn't accumulate columns, rows and affected rows counts into memory.
     * Instead it calls passed in consumers, when columns, or particular row is fetched from Postgres.
     * Completes returned {@link CompletableFuture} with affected rows count when the process of single {@link ResultSet}s fetching ends.
     *
     * @param onColumns Columns fetched callback consumer.
     * @param onRow     A row fetched callback consumer.
     * @param sql       Sql query text with parameters substituted with ?.
     * @param params    Parameters of the query
     * @return CompletableFuture of affected rows count.
     * This future is used by implementation to create a {@link ResultSet} instance from already fetched columns, rows and affected rows count.
     * Affected rows count is this future's completion value.
     */
    CompletableFuture<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns, Consumer<Row> onRow, String sql, Object... params);
}
