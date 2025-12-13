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

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JdbcErrorTest {

    @Test
    void connectionFailed_containsMessage() {
        var error = JdbcError.ConnectionFailed.of("Connection refused");

        assertThat(error.message()).contains("Connection refused");
        assertThat(error.cause().isEmpty()).isTrue();
    }

    @Test
    void connectionFailed_containsCause() {
        var cause = new RuntimeException("Network error");
        var error = JdbcError.ConnectionFailed.of("Failed", cause);

        assertThat(error.message()).contains("Failed");
        assertThat(error.cause().isPresent()).isTrue();
    }

    @Test
    void queryFailed_containsSqlAndMessage() {
        var error = new JdbcError.QueryFailed("SELECT * FROM users", "Table not found");

        assertThat(error.message()).contains("SELECT * FROM users");
        assertThat(error.message()).contains("Table not found");
    }

    @Test
    void constraintViolation_containsDetails() {
        var error = new JdbcError.ConstraintViolation("uk_email", "Duplicate email");

        assertThat(error.message()).contains("uk_email");
        assertThat(error.message()).contains("Duplicate email");
    }

    @Test
    void timeout_containsOperation() {
        var error = new JdbcError.Timeout("Query execution");

        assertThat(error.message()).contains("Query execution");
    }

    @Test
    void transactionRollback_containsMessage() {
        var error = new JdbcError.TransactionRollback("Deadlock detected");

        assertThat(error.message()).contains("Deadlock detected");
    }

    @Test
    void transactionRequired_hasMessage() {
        var error = JdbcError.TransactionRequired.INSTANCE;

        assertThat(error.message()).contains("Transaction is required");
    }

    @Test
    void databaseFailure_wrapsException() {
        var cause = new RuntimeException("Unexpected");
        var error = JdbcError.DatabaseFailure.of(cause);

        assertThat(error.message()).contains("Unexpected");
        assertThat(error.cause()).isSameAs(cause);
    }

    @Test
    void fromException_mapsTimeoutException() {
        var ex = new SQLTimeoutException("Query timeout");
        var error = JdbcError.fromException(ex);

        assertInstanceOf(JdbcError.Timeout.class, error);
    }

    @Test
    void fromException_mapsConstraintViolation() {
        var ex = new SQLIntegrityConstraintViolationException("Unique constraint");
        var error = JdbcError.fromException(ex);

        assertInstanceOf(JdbcError.ConstraintViolation.class, error);
    }

    @Test
    void fromException_mapsTransactionRollback() {
        var ex = new SQLTransactionRollbackException("Serialization failure");
        var error = JdbcError.fromException(ex);

        assertInstanceOf(JdbcError.TransactionRollback.class, error);
    }

    @Test
    void fromException_mapsConnectionError() {
        var ex = new SQLException("Connection closed", "08001");
        var error = JdbcError.fromException(ex);

        assertInstanceOf(JdbcError.ConnectionFailed.class, error);
    }

    @Test
    void fromException_mapsUnknownToDatabaseFailure() {
        var ex = new IllegalStateException("Unknown error");
        var error = JdbcError.fromException(ex);

        assertInstanceOf(JdbcError.DatabaseFailure.class, error);
    }

    @Test
    void fromExceptionWithSql_includesSqlInError() {
        var ex = new SQLException("Syntax error");
        var error = JdbcError.fromException(ex, "SELECT * FROM invalid");

        assertInstanceOf(JdbcError.QueryFailed.class, error);
        assertThat(error.message()).contains("SELECT * FROM invalid");
    }
}
