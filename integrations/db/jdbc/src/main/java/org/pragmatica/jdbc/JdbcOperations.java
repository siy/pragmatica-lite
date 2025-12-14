/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.jdbc;

import org.pragmatica.lang.Functions.ThrowingFn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/// Promise-based JDBC operations interface.
/// Provides a functional wrapper for common JDBC operations with typed error handling.
public interface JdbcOperations {

    /// Executes a query and maps the single result.
    ///
    /// @param sql SQL query
    /// @param mapper Function to map ResultSet to domain object (may throw)
    /// @param params Query parameters
    /// @param <T> Result type
    ///
    /// @return Promise containing the mapped result or failure
    <T> Promise<T> queryOne(String sql, ThrowingFn1<T, ResultSet> mapper, Object... params);

    /// Executes a query and returns optional result.
    ///
    /// @param sql SQL query
    /// @param mapper Function to map ResultSet to domain object (may throw)
    /// @param params Query parameters
    /// @param <T> Result type
    ///
    /// @return Promise containing Option with the mapped result
    <T> Promise<Option<T>> queryOptional(String sql, ThrowingFn1<T, ResultSet> mapper, Object... params);

    /// Executes a query and maps all results to a list.
    ///
    /// @param sql SQL query
    /// @param rowMapper Function to map each row to domain object (may throw)
    /// @param params Query parameters
    /// @param <T> Result element type
    ///
    /// @return Promise containing list of mapped results
    <T> Promise<List<T>> queryList(String sql, ThrowingFn1<T, ResultSet> rowMapper, Object... params);

    /// Executes an update statement (INSERT, UPDATE, DELETE).
    ///
    /// @param sql SQL statement
    /// @param params Statement parameters
    ///
    /// @return Promise containing number of affected rows
    Promise<Integer> update(String sql, Object... params);

    /// Executes a batch of update statements.
    ///
    /// @param sql SQL statement template
    /// @param paramsList List of parameter arrays for batch
    ///
    /// @return Promise containing array of update counts
    Promise<int[]> batch(String sql, List<Object[]> paramsList);

    /// Creates JdbcOperations from a DataSource.
    ///
    /// @param dataSource JDBC DataSource
    ///
    /// @return JdbcOperations instance
    static JdbcOperations create(DataSource dataSource) {
        return new DataSourceJdbcOperations(dataSource);
    }
}

/// DataSource-based implementation of JdbcOperations.
final class DataSourceJdbcOperations implements JdbcOperations {
    private final DataSource dataSource;

    DataSourceJdbcOperations(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> Promise<T> queryOne(String sql, ThrowingFn1<T, ResultSet> mapper, Object... params) {
        return Promise.lift(
            e -> JdbcError.fromException(e, sql),
            () -> executeQuery(sql, params, rs -> {
                if (rs.next()) {
                    var result = mapper.apply(rs);
                    if (rs.next()) {
                        throw new SQLException("Multiple results found");
                    }
                    return result;
                }
                throw new SQLException("No result found");
            })
        );
    }

    @Override
    public <T> Promise<Option<T>> queryOptional(String sql, ThrowingFn1<T, ResultSet> mapper, Object... params) {
        return Promise.lift(
            e -> JdbcError.fromException(e, sql),
            () -> executeQuery(sql, params, rs -> {
                if (rs.next()) {
                    return Option.option(mapper.apply(rs));
                }
                return Option.none();
            })
        );
    }

    @Override
    public <T> Promise<List<T>> queryList(String sql, ThrowingFn1<T, ResultSet> rowMapper, Object... params) {
        return Promise.lift(
            e -> JdbcError.fromException(e, sql),
            () -> executeQuery(sql, params, rs -> {
                var results = new ArrayList<T>();
                while (rs.next()) {
                    results.add(rowMapper.apply(rs));
                }
                return results;
            })
        );
    }

    @Override
    public Promise<Integer> update(String sql, Object... params) {
        return Promise.lift(
            e -> JdbcError.fromException(e, sql),
            () -> {
                try (var conn = dataSource.getConnection();
                     var stmt = prepareStatement(conn, sql, params)) {
                    return stmt.executeUpdate();
                }
            }
        );
    }

    @Override
    public Promise<int[]> batch(String sql, List<Object[]> paramsList) {
        return Promise.lift(
            e -> JdbcError.fromException(e, sql),
            () -> {
                try (var conn = dataSource.getConnection();
                     var stmt = conn.prepareStatement(sql)) {
                    for (var params : paramsList) {
                        setParameters(stmt, params);
                        stmt.addBatch();
                    }
                    return stmt.executeBatch();
                }
            }
        );
    }

    private <T> T executeQuery(String sql, Object[] params, ThrowingFn1<T, ResultSet> handler) throws Throwable {
        try (var conn = dataSource.getConnection();
             var stmt = prepareStatement(conn, sql, params);
             var rs = stmt.executeQuery()) {
            return handler.apply(rs);
        }
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, Object[] params) throws SQLException {
        var stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt;
    }

    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}
