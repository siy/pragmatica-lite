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

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;

import static org.junit.jupiter.api.Assertions.*;

class JooqErrorTest {

    @Test
    void noResultErrorHasCorrectMessage() {
        var error = new JooqError.NoResult("SELECT * FROM users WHERE id = 1");

        assertTrue(error.message().contains("no results"));
        assertTrue(error.message().contains("SELECT * FROM users"));
    }

    @Test
    void multipleResultsErrorHasCorrectMessage() {
        var error = new JooqError.MultipleResults("SELECT * FROM users", 5);

        assertTrue(error.message().contains("5 results"));
        assertTrue(error.message().contains("expected 1"));
    }

    @Test
    void connectionFailedErrorHasCorrectMessage() {
        var error = JooqError.ConnectionFailed.connectionFailed("Connection refused");

        assertTrue(error.message().contains("Connection failed"));
        assertTrue(error.message().contains("Connection refused"));
    }

    @Test
    void queryFailedErrorIncludesSql() {
        var error = new JooqError.QueryFailed("SELECT * FROM missing_table", "Table not found");

        assertTrue(error.message().contains("Query failed"));
        assertTrue(error.message().contains("SELECT * FROM missing_table"));
    }

    @Test
    void constraintViolationErrorHasCorrectMessage() {
        var error = new JooqError.ConstraintViolation("users_email_unique", "Duplicate entry");

        assertTrue(error.message().contains("Constraint violation"));
        assertTrue(error.message().contains("users_email_unique"));
    }

    @Test
    void timeoutErrorHasCorrectMessage() {
        var error = new JooqError.Timeout("Query execution");

        assertTrue(error.message().contains("Timeout"));
        assertTrue(error.message().contains("Query execution"));
    }

    @Test
    void transactionRollbackErrorHasCorrectMessage() {
        var error = new JooqError.TransactionRollback("Deadlock detected");

        assertTrue(error.message().contains("Transaction rolled back"));
        assertTrue(error.message().contains("Deadlock detected"));
    }

    @Test
    void transactionRequiredErrorHasCorrectMessage() {
        var error = JooqError.TransactionRequired.INSTANCE;

        assertTrue(error.message().contains("Transaction is required"));
    }

    @Test
    void databaseFailureWrapsException() {
        var cause = new RuntimeException("Unexpected error");
        var error = JooqError.DatabaseFailure.databaseFailure(cause);

        assertTrue(error.message().contains("Database operation failed"));
        assertTrue(error.message().contains("Unexpected error"));
    }

    @Test
    void fromExceptionMapsTimeoutException() {
        var ex = new SQLTimeoutException("Query timeout");
        var error = JooqError.fromException(ex, "SELECT 1");

        assertInstanceOf(JooqError.Timeout.class, error);
    }

    @Test
    void fromExceptionMapsConstraintViolation() {
        var ex = new SQLIntegrityConstraintViolationException("Duplicate key");
        var error = JooqError.fromException(ex, "INSERT INTO users");

        assertInstanceOf(JooqError.ConstraintViolation.class, error);
    }

    @Test
    void fromExceptionMapsTransactionRollback() {
        var ex = new SQLTransactionRollbackException("Deadlock");
        var error = JooqError.fromException(ex, "UPDATE users");

        assertInstanceOf(JooqError.TransactionRollback.class, error);
    }

    @Test
    void fromExceptionMapsConnectionError() {
        var ex = new SQLException("Connection lost", "08001");
        var error = JooqError.fromException(ex, "SELECT 1");

        assertInstanceOf(JooqError.ConnectionFailed.class, error);
    }

    @Test
    void fromExceptionMapsGenericSqlException() {
        var ex = new SQLException("Syntax error", "42000");
        var error = JooqError.fromException(ex, "SELEC * FROM users");

        assertInstanceOf(JooqError.QueryFailed.class, error);
    }

    @Test
    void fromExceptionMapsUnknownException() {
        var ex = new RuntimeException("Unknown error");
        var error = JooqError.fromException(ex, "SELECT 1");

        assertInstanceOf(JooqError.DatabaseFailure.class, error);
    }
}
