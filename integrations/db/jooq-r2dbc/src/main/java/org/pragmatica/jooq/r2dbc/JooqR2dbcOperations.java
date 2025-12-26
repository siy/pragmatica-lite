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

package org.pragmatica.jooq.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.r2dbc.R2dbcError;
import org.pragmatica.r2dbc.ReactiveOperations;

import java.util.List;

/// Promise-based JOOQ R2DBC operations interface.
/// Provides a thin wrapper over JOOQ with R2DBC connection management.
public interface JooqR2dbcOperations {

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

    /// Creates JooqR2dbcOperations from a ConnectionFactory.
    ///
    /// @param connectionFactory R2DBC ConnectionFactory
    /// @param dialect SQL dialect
    ///
    /// @return JooqR2dbcOperations instance
    static JooqR2dbcOperations jooqR2dbcOperations(ConnectionFactory connectionFactory, SQLDialect dialect) {
        return new ConnectionFactoryJooqR2dbcOperations(connectionFactory, dialect);
    }

    /// Creates JooqR2dbcOperations from a ConnectionFactory with default dialect.
    ///
    /// @param connectionFactory R2DBC ConnectionFactory
    ///
    /// @return JooqR2dbcOperations instance
    static JooqR2dbcOperations jooqR2dbcOperations(ConnectionFactory connectionFactory) {
        return jooqR2dbcOperations(connectionFactory, SQLDialect.DEFAULT);
    }
}

/// ConnectionFactory-based implementation of JooqR2dbcOperations.
/// Acquires R2DBC connections and executes JOOQ queries asynchronously.
final class ConnectionFactoryJooqR2dbcOperations implements JooqR2dbcOperations {
    private final ConnectionFactory connectionFactory;
    private final SQLDialect dialect;
    private final DSLContext dsl;

    ConnectionFactoryJooqR2dbcOperations(ConnectionFactory connectionFactory, SQLDialect dialect) {
        this.connectionFactory = connectionFactory;
        this.dialect = dialect;
        this.dsl = DSL.using(dialect);
    }

    @Override
    public <R extends Record> Promise<R> fetchOne(ResultQuery<R> query) {
        return withConnection(conn -> {
            var dslWithConn = DSL.using(conn, dialect);
            return Promise.lift(
                e -> mapException(e, query.getSQL()),
                () -> {
                    var result = dslWithConn.fetch(query);
                    if (result.isEmpty()) {
                        throw new R2dbcNoResultException("Query returned no results");
                    }
                    if (result.size() > 1) {
                        throw new R2dbcMultipleResultsException("Query returned " + result.size() + " results");
                    }
                    return result.get(0);
                }
            );
        });
    }

    @Override
    public <R extends Record> Promise<Option<R>> fetchOptional(ResultQuery<R> query) {
        return withConnection(conn -> {
            var dslWithConn = DSL.using(conn, dialect);
            return Promise.lift(
                e -> mapException(e, query.getSQL()),
                () -> {
                    var result = dslWithConn.fetch(query);
                    return result.isEmpty() ? Option.none() : Option.option(result.get(0));
                }
            );
        });
    }

    @Override
    public <R extends Record> Promise<List<R>> fetch(ResultQuery<R> query) {
        return withConnection(conn -> {
            var dslWithConn = DSL.using(conn, dialect);
            return Promise.lift(
                e -> mapException(e, query.getSQL()),
                () -> dslWithConn.fetch(query)
            );
        });
    }

    @Override
    public Promise<Integer> execute(Query query) {
        return withConnection(conn -> {
            var dslWithConn = DSL.using(conn, dialect);
            return Promise.lift(
                e -> mapException(e, query.getSQL()),
                () -> dslWithConn.execute(query)
            );
        });
    }

    @Override
    public DSLContext dsl() {
        return dsl;
    }

    private <T> Promise<T> withConnection(Fn1<Promise<T>, Connection> operation) {
        return ReactiveOperations.<Connection>fromPublisher(connectionFactory.create())
            .flatMap(conn -> operation.apply(conn)
                .onResult(_ -> ReactiveOperations.fromPublisher(conn.close())));
    }

    /// Maps exceptions to R2dbcError, with special handling for JOOQ-R2DBC specific exceptions.
    private static R2dbcError mapException(Throwable e, String sql) {
        return switch (e) {
            case R2dbcNoResultException _ -> new R2dbcError.NoResult(sql);
            case R2dbcMultipleResultsException ex -> new R2dbcError.MultipleResults(sql, parseCount(ex.getMessage()));
            default -> R2dbcError.fromException(e, sql);
        };
    }

    private static int parseCount(String message) {
        // Extract count from "Query returned N results"
        try {
            var parts = message.split(" ");
            return Integer.parseInt(parts[2]);
        } catch (Exception _) {
            return -1;
        }
    }

    /// Exception for no results case (used internally for error mapping).
    static final class R2dbcNoResultException extends RuntimeException {
        R2dbcNoResultException(String message) {
            super(message);
        }
    }

    /// Exception for multiple results case (used internally for error mapping).
    static final class R2dbcMultipleResultsException extends RuntimeException {
        R2dbcMultipleResultsException(String message) {
            super(message);
        }
    }
}
