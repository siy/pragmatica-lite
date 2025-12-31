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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;

/// Typed error causes for JOOQ operations.
public sealed interface JooqError extends Cause {
    /// Connection to database failed.
    record ConnectionFailed(String message, Option<Throwable> cause) implements JooqError {
        public static ConnectionFailed connectionFailed(String message) {
            return new ConnectionFailed(message, Option.none());
        }

        public static ConnectionFailed connectionFailed(String message, Throwable cause) {
            return new ConnectionFailed(message, Option.option(cause));
        }

        @Override
        public String message() {
            return "Connection failed: " + message;
        }
    }

    /// SQL query execution failed.
    record QueryFailed(String sql, String message) implements JooqError {
        @Override
        public String message() {
            return "Query failed: " + message + " [SQL: " + sql + "]";
        }
    }

    /// Query returned no results when exactly one was expected.
    record NoResult(String sql) implements JooqError {
        @Override
        public String message() {
            return "Query returned no results [SQL: " + sql + "]";
        }
    }

    /// Query returned multiple results when exactly one was expected.
    record MultipleResults(String sql, int count) implements JooqError {
        @Override
        public String message() {
            return "Query returned " + count + " results, expected 1 [SQL: " + sql + "]";
        }
    }

    /// Database constraint violation (unique, foreign key, etc).
    record ConstraintViolation(String constraint, String message) implements JooqError {
        public static ConstraintViolation constraintViolation(String message) {
            return new ConstraintViolation("unknown", message);
        }

        @Override
        public String message() {
            return "Constraint violation (" + constraint + "): " + message;
        }
    }

    /// Operation timeout exceeded.
    record Timeout(String operation) implements JooqError {
        @Override
        public String message() {
            return "Timeout: " + operation;
        }
    }

    /// Transaction rollback (deadlock, serialization failure, etc).
    record TransactionRollback(String message) implements JooqError {
        @Override
        public String message() {
            return "Transaction rolled back: " + message;
        }
    }

    /// Transaction not active when required.
    enum TransactionRequired implements JooqError {
        INSTANCE;
        @Override
        public String message() {
            return "Transaction is required for this operation";
        }
    }

    /// General database failure (catch-all for unexpected errors).
    record DatabaseFailure(Throwable cause) implements JooqError {
        public static DatabaseFailure databaseFailure(Throwable cause) {
            return new DatabaseFailure(cause);
        }

        @Override
        public String message() {
            var msg = cause.getMessage();
            return "Database operation failed: " + ( msg != null
                                                     ? msg
                                                     : cause.getClass()
                                                            .getName());
        }
    }

    /// Maps exceptions to typed JooqError causes.
    ///
    /// @param throwable Exception to map
    /// @param sql SQL that caused the error
    ///
    /// @return Corresponding JooqError
    static JooqError fromException(Throwable throwable, String sql) {
        return switch (throwable) {
            case JooqNoResultException _ -> new NoResult(sql);
            case JooqMultipleResultsException e -> new MultipleResults(sql, e.count());
            case SQLTimeoutException e -> new Timeout(e.getMessage());
            case SQLIntegrityConstraintViolationException e -> ConstraintViolation.constraintViolation(e.getMessage());
            case SQLTransactionRollbackException e -> new TransactionRollback(e.getMessage());
            case SQLException e -> {
                var sqlState = e.getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    yield ConnectionFailed.connectionFailed(e.getMessage(), e);
                }
                yield new QueryFailed(sql, e.getMessage());
            }
            default -> DatabaseFailure.databaseFailure(throwable);
        };
    }

    /// Maps exceptions to typed JooqError causes (without SQL context).
    ///
    /// @param throwable Exception to map
    ///
    /// @return Corresponding JooqError
    static JooqError fromException(Throwable throwable) {
        return fromException(throwable, "N/A");
    }
}
