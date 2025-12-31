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

package org.pragmatica.jooq;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/// Promise-based JOOQ operations interface.
/// Provides a thin wrapper over JOOQ with JDBC connection management.
public interface JooqOperations {
    /// Fetches a single record from the query.
    /// Fails if query returns zero or more than one record.
    ///
    /// @param query JOOQ ResultQuery
    /// @param <R> Record type
    ///
    /// @return Promise containing the single record or failure
    <R extends Record> Promise<R> fetchOne(ResultQuery<R> query);

    /// Fetches an optional record from the query.
    ///
    /// @param query JOOQ ResultQuery
    /// @param <R> Record type
    ///
    /// @return Promise containing Option with the record
    <R extends Record> Promise<Option<R>> fetchOptional(ResultQuery<R> query);

    /// Fetches all records from the query.
    ///
    /// @param query JOOQ ResultQuery
    /// @param <R> Record type
    ///
    /// @return Promise containing list of records
    <R extends Record> Promise<List<R>> fetch(ResultQuery<R> query);

    /// Executes a query (INSERT, UPDATE, DELETE).
    ///
    /// @param query JOOQ Query
    ///
    /// @return Promise containing number of affected rows
    Promise<Integer> execute(Query query);

    /// Returns the DSLContext for query building.
    ///
    /// @return DSLContext instance
    DSLContext dsl();

    /// Creates JooqOperations from a DataSource.
    ///
    /// @param dataSource JDBC DataSource
    /// @param dialect SQL dialect
    ///
    /// @return JooqOperations instance
    static JooqOperations jooqOperations(DataSource dataSource, SQLDialect dialect) {
        return new DataSourceJooqOperations(dataSource, dialect);
    }

    /// Creates JooqOperations from a DataSource with default dialect.
    ///
    /// @param dataSource JDBC DataSource
    ///
    /// @return JooqOperations instance
    static JooqOperations jooqOperations(DataSource dataSource) {
        return jooqOperations(dataSource, SQLDialect.DEFAULT);
    }
}

/// DataSource-based implementation of JooqOperations.
final class DataSourceJooqOperations implements JooqOperations {
    private final DataSource dataSource;
    private final SQLDialect dialect;
    private final DSLContext dsl;

    DataSourceJooqOperations(DataSource dataSource, SQLDialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.dsl = DSL.using(dialect);
    }

    @Override
    public <R extends Record> Promise<R> fetchOne(ResultQuery<R> query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> {
                                try (var conn = dataSource.getConnection()) {
                                    var result = DSL.using(conn, dialect)
                                                    .fetch(query);
                                    if (result.isEmpty()) {
                                        throw new JooqNoResultException("Query returned no results");
                                    }
                                    if (result.size() > 1) {
                                        throw new JooqMultipleResultsException("Query returned " + result.size()
                                                                               + " results",
                                                                               result.size());
                                    }
                                    return result.get(0);
                                }
                            });
    }

    @Override
    public <R extends Record> Promise<Option<R>> fetchOptional(ResultQuery<R> query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> {
                                try (var conn = dataSource.getConnection()) {
                                    var result = DSL.using(conn, dialect)
                                                    .fetch(query);
                                    return result.isEmpty()
                                           ? Option.none()
                                           : Option.option(result.get(0));
                                }
                            });
    }

    @Override
    public <R extends Record> Promise<List<R>> fetch(ResultQuery<R> query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> {
                                try (var conn = dataSource.getConnection()) {
                                    return DSL.using(conn, dialect)
                                              .fetch(query);
                                }
                            });
    }

    @Override
    public Promise<Integer> execute(Query query) {
        return Promise.lift(e -> JooqError.fromException(e, query.getSQL()),
                            () -> {
                                try (var conn = dataSource.getConnection()) {
                                    return DSL.using(conn, dialect)
                                              .execute(query);
                                }
                            });
    }

    @Override
    public DSLContext dsl() {
        return dsl;
    }
}

/// Exception for no results case (used internally for error mapping).
final class JooqNoResultException extends RuntimeException {
    JooqNoResultException(String message) {
        super(message);
    }
}

/// Exception for multiple results case (used internally for error mapping).
final class JooqMultipleResultsException extends RuntimeException {
    private final int count;

    JooqMultipleResultsException(String message, int count) {
        super(message);
        this.count = count;
    }

    int count() {
        return count;
    }
}
