package com.github.pgasync.net;

import com.github.pgasync.PgColumn;
import com.github.pgasync.PgResultSet;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

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
     * Sends parameter less query script. The script may be multi query. Queries are separated with semicolons. Accumulates fetched columns, rows and
     * affected rows counts into memory and transforms them into a ResultSet when each {@link ResultSet} is fetched. Completes returned
     * {@link CompletableFuture} when the whole process of multiple {@link ResultSet}s fetching ends.
     *
     * @param sql Sql Script text.
     *
     * @return CompletableFuture that is completed with a collection of fetched {@link ResultSet}s.
     */
    default Promise<Collection<ResultSet>> completeScript(String sql) {
        var results = new ArrayList<ResultSet>();
        var assembly = new ResultSetAssembly();

        return script(assembly::start,
                      assembly::add,
                      affected -> results.add(assembly.asResultSet(affected)),
                      sql)
            .map(_ -> results);
    }

    /**
     * Sends parameter less query script. The script may be multi query. Queries are separated with semicolons. Unlike {@link #completeScript(String)}
     * doesn't accumulate fetched columns, rows and affected rows counts into memory. Instead it calls passed in consumers, when columns, or
     * particular row or an affected rows count is fetched from Postgres. Completes returned {@link CompletableFuture} when the whole process of
     * multiple {@link ResultSet}s fetching ends.
     *
     * @param onColumns  Columns fetched callback consumer.
     * @param onRow      A row fetched callback consumer.
     * @param onAffected An affected rows callback consumer. It is called when a particular {@link ResultSet} is completely fetched with its affected
     *                   rows count. This callback should be used to create a {@link ResultSet} instance from already fetched columns, rows and
     *                   affected rows count.
     * @param sql        Sql Script text.
     *
     * @return CompletableFuture that is completed when the whole process of multiple {@link ResultSet}s fetching ends.
     */
    Promise<Unit> script(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                         Consumer<Row> onRow,
                         Consumer<Integer> onAffected,
                         String sql);

    /**
     * Sends single query with parameters. Uses extended query protocol of Postgres. Accumulates fetched columns, rows and affected rows count into
     * memory and transforms them into a {@link ResultSet} when it is fetched. Completes returned {@link CompletableFuture} when the whole process of
     * {@link ResultSet} fetching ends.
     *
     * @param sql    Sql query text with parameters substituted with ?.
     * @param params Parameters of the query.
     *
     * @return CompletableFuture of {@link ResultSet}.
     */
    default Promise<ResultSet> completeQuery(String sql, Object... params) {
        var assembly = new ResultSetAssembly();

        return query(assembly::start, assembly::add, sql, params).map(assembly::asResultSet);
    }

    /**
     * Sends single query with parameters. Uses extended query protocol of Postgres. Unlike {@link #completeQuery(String, Object...)} doesn't
     * accumulate columns, rows and affected rows counts into memory. Instead it calls passed in consumers, when columns, or particular row is fetched
     * from Postgres. Completes returned {@link CompletableFuture} with affected rows count when the process of single {@link ResultSet}s fetching
     * ends.
     *
     * @param onColumns Columns fetched callback consumer.
     * @param onRow     A row fetched callback consumer.
     * @param sql       Sql query text with parameters substituted with ?.
     * @param params    Parameters of the query
     *
     * @return CompletableFuture of affected rows count. This future is used by implementation to create a {@link ResultSet} instance from already
     *     fetched columns, rows and affected rows count. Affected rows count is this future's completion value.
     */
    Promise<Integer> query(BiConsumer<Map<String, PgColumn>, PgColumn[]> onColumns,
                           Consumer<Row> onRow,
                           String sql,
                           Object... params);

    class ResultSetAssembly {
        private Map<String, PgColumn> columnsByName;
        private PgColumn[] orderedColumns;
        private List<Row> rows;

        private void reset() {
            columnsByName = null;
            orderedColumns = null;
            rows = null;
        }

        private PgResultSet asResultSet(int affectedRows) {
            var resultSet = new PgResultSet(
                columnsByName,
                orderedColumns,
                rows,
                affectedRows
            );
            reset();
            return resultSet;
        }

        private void start(Map<String, PgColumn> columnsByName, PgColumn[] orderedColumns) {
            this.columnsByName = columnsByName;
            this.orderedColumns = orderedColumns;
            this.rows = new ArrayList<>();
        }

        private void add(Row row) {
            rows.add(row);
        }
    }
}
