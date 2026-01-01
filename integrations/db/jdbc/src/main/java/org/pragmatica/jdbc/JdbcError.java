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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;

import static org.pragmatica.jdbc.JdbcError.ConnectionFailed.connectionFailed;
import static org.pragmatica.jdbc.JdbcError.ConstraintViolation.constraintViolation;
import static org.pragmatica.jdbc.JdbcError.DatabaseFailure.databaseFailure;

/// Typed error causes for JDBC operations.
/// Maps common SQL exceptions to domain-friendly error types.
public sealed interface JdbcError extends Cause {
    /// Connection to database failed.
    record ConnectionFailed(String message, Option<Throwable> cause) implements JdbcError {
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
    record QueryFailed(String sql, String message) implements JdbcError {
        @Override
        public String message() {
            return "Query failed: " + message + " [SQL: " + sql + "]";
        }
    }

    /// Database constraint violation (unique, foreign key, etc).
    record ConstraintViolation(String constraint, String message) implements JdbcError {
        public static ConstraintViolation constraintViolation(String message) {
            return new ConstraintViolation("unknown", message);
        }

        @Override
        public String message() {
            return "Constraint violation (" + constraint + "): " + message;
        }
    }

    /// Operation timeout exceeded.
    record Timeout(String operation) implements JdbcError {
        @Override
        public String message() {
            return "Timeout: " + operation;
        }
    }

    /// Transaction rollback (deadlock, serialization failure, etc).
    record TransactionRollback(String message) implements JdbcError {
        @Override
        public String message() {
            return "Transaction rolled back: " + message;
        }
    }

    /// Transaction not active when required.
    enum TransactionRequired implements JdbcError {
        INSTANCE;
        @Override
        public String message() {
            return "Transaction is required for this operation";
        }
    }

    /// General database failure (catch-all for unexpected errors).
    record DatabaseFailure(Throwable cause) implements JdbcError {
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

    /// Maps SQL exceptions to typed JdbcError causes.
    ///
    /// @param throwable Exception to map
    ///
    /// @return Corresponding JdbcError
    static JdbcError fromException(Throwable throwable) {
        return switch (throwable) {
            case SQLTimeoutException e -> new Timeout(e.getMessage());
            case SQLIntegrityConstraintViolationException e -> constraintViolation(e.getMessage());
            case SQLTransactionRollbackException e -> new TransactionRollback(e.getMessage());
            case SQLException e -> {
                var sqlState = e.getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    yield connectionFailed(e.getMessage(), e);
                }
                yield databaseFailure(e);
            }
            default -> databaseFailure(throwable);
        };
    }

    /// Maps SQL exceptions with SQL context to typed JdbcError causes.
    ///
    /// @param throwable Exception to map
    /// @param sql SQL that caused the error
    ///
    /// @return Corresponding JdbcError
    static JdbcError fromException(Throwable throwable, String sql) {
        return switch (throwable) {
            case SQLTimeoutException e -> new Timeout(e.getMessage());
            case SQLIntegrityConstraintViolationException e -> constraintViolation(e.getMessage());
            case SQLTransactionRollbackException e -> new TransactionRollback(e.getMessage());
            case SQLException e -> {
                var sqlState = e.getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    yield connectionFailed(e.getMessage(), e);
                }
                yield new QueryFailed(sql, e.getMessage());
            }
            default -> databaseFailure(throwable);
        };
    }
}
