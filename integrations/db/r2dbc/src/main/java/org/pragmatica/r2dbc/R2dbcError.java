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

package org.pragmatica.r2dbc;

import org.pragmatica.lang.Cause;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientException;

import static org.pragmatica.r2dbc.R2dbcError.DatabaseFailure.databaseFailure;

/// Typed error causes for R2DBC operations.
/// Maps common R2DBC exceptions to domain-friendly error types.
public sealed interface R2dbcError extends Cause {
    /// Connection to database failed.
    record ConnectionFailed(String message) implements R2dbcError {
        @Override
        public String message() {
            return "Connection failed: " + message;
        }
    }

    /// SQL query execution failed.
    record QueryFailed(String sql, String message) implements R2dbcError {
        public static QueryFailed queryFailed(String message) {
            return new QueryFailed("unknown", message);
        }

        @Override
        public String message() {
            return "Query failed: " + message + " [SQL: " + sql + "]";
        }
    }

    /// Database constraint violation (unique, foreign key, etc).
    record ConstraintViolation(String constraint) implements R2dbcError {
        @Override
        public String message() {
            return "Constraint violation: " + constraint;
        }
    }

    /// Operation timeout exceeded.
    record Timeout(String operation) implements R2dbcError {
        @Override
        public String message() {
            return "Timeout: " + operation;
        }
    }

    /// Query returned no result when one was expected.
    record NoResult(String query) implements R2dbcError {
        @Override
        public String message() {
            return "No result for query: " + query;
        }
    }

    /// Query returned multiple results when single was expected.
    record MultipleResults(String query, int count) implements R2dbcError {
        @Override
        public String message() {
            return "Expected single result but got " + count + " for query: " + query;
        }
    }

    /// General database failure (catch-all for unexpected errors).
    record DatabaseFailure(Throwable cause) implements R2dbcError {
        public static DatabaseFailure databaseFailure(Throwable cause) {
            return new DatabaseFailure(cause);
        }

        @Override
        public String message() {
            var msg = cause.getMessage();
            return "Database operation failed: " + (msg != null
                                                    ? msg
                                                    : cause.getClass()
                                                           .getName());
        }
    }

    /// Maps R2DBC exceptions to typed R2dbcError causes.
    ///
    /// @param throwable Exception to map
    ///
    /// @return Corresponding R2dbcError
    static R2dbcError fromException(Throwable throwable) {
        return switch (throwable) {
            case R2dbcTimeoutException e -> new Timeout(e.getMessage());
            case R2dbcDataIntegrityViolationException e -> new ConstraintViolation(e.getMessage());
            case R2dbcTransientException e -> new ConnectionFailed(e.getMessage());
            case R2dbcException e -> {
                var sqlState = e.getSqlState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    yield new ConnectionFailed(e.getMessage());
                }
                yield databaseFailure(e);
            }
            default -> databaseFailure(throwable);
        };
    }

    /// Maps R2DBC exceptions with SQL context to typed R2dbcError causes.
    ///
    /// @param throwable Exception to map
    /// @param sql SQL that caused the error
    ///
    /// @return Corresponding R2dbcError
    static R2dbcError fromException(Throwable throwable, String sql) {
        return switch (throwable) {
            case R2dbcTimeoutException e -> new Timeout(e.getMessage());
            case R2dbcDataIntegrityViolationException e -> new ConstraintViolation(e.getMessage());
            case R2dbcTransientException e -> new ConnectionFailed(e.getMessage());
            case R2dbcException e -> {
                var sqlState = e.getSqlState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    yield new ConnectionFailed(e.getMessage());
                }
                yield new QueryFailed(sql, e.getMessage());
            }
            default -> databaseFailure(throwable);
        };
    }
}
