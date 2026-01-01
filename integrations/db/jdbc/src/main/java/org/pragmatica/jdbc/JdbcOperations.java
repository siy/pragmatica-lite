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
import java.sql.ResultSet;
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
    static JdbcOperations jdbcOperations(DataSource dataSource) {
        return new DataSourceJdbcOperations(dataSource);
    }
}
