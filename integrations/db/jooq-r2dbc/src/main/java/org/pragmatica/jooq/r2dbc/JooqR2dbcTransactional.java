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
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn2;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.r2dbc.R2dbcError;
import org.pragmatica.r2dbc.ReactiveOperations;

/// Transaction aspect for JOOQ R2DBC operations.
/// Provides transactional boundaries with automatic commit/rollback.
public interface JooqR2dbcTransactional {

    /// Executes an operation within a transaction.
    /// Automatically commits on success and rolls back on failure.
    ///
    /// @param connectionFactory ConnectionFactory for connection acquisition
    /// @param dialect SQL dialect
    /// @param operation Operation to execute with DSLContext
    /// @param <R> Result type
    ///
    /// @return Promise with operation result
    static <R> Promise<R> withTransaction(ConnectionFactory connectionFactory,
                                          SQLDialect dialect,
                                          Fn2<Promise<R>, DSLContext, Connection> operation) {
        return withTransaction(connectionFactory, dialect, R2dbcError::fromException, operation);
    }

    /// Executes an operation within a transaction with custom error mapping.
    /// Automatically commits on success and rolls back on failure.
    ///
    /// @param connectionFactory ConnectionFactory for connection acquisition
    /// @param dialect SQL dialect
    /// @param errorMapper Function to map exceptions to errors
    /// @param operation Operation to execute with DSLContext
    /// @param <R> Result type
    ///
    /// @return Promise with operation result
    static <R> Promise<R> withTransaction(ConnectionFactory connectionFactory,
                                          SQLDialect dialect,
                                          Fn1<R2dbcError, Throwable> errorMapper,
                                          Fn2<Promise<R>, DSLContext, Connection> operation) {
        return ReactiveOperations.<Connection>fromPublisher(connectionFactory.create(), errorMapper)
            .flatMap(conn -> beginTransaction(conn, errorMapper)
                .flatMap(_ -> {
                    var dsl = DSL.using(conn, dialect);
                    return operation.apply(dsl, conn);
                })
                .flatMap(result -> commitTransaction(conn, errorMapper).map(_ -> result))
                .onFailure(_ -> rollbackTransaction(conn))
                .onResult(_ -> closeConnection(conn)));
    }

    private static Promise<Void> beginTransaction(Connection conn, Fn1<R2dbcError, Throwable> errorMapper) {
        return ReactiveOperations.fromPublisher(conn.beginTransaction(), errorMapper);
    }

    private static Promise<Void> commitTransaction(Connection conn, Fn1<R2dbcError, Throwable> errorMapper) {
        return ReactiveOperations.fromPublisher(conn.commitTransaction(), errorMapper);
    }

    private static void rollbackTransaction(Connection conn) {
        ReactiveOperations.fromPublisher(conn.rollbackTransaction())
            .await(); // Best effort, ignore result
    }

    private static void closeConnection(Connection conn) {
        ReactiveOperations.fromPublisher(conn.close())
            .await(); // Best effort, ignore result
    }
}
