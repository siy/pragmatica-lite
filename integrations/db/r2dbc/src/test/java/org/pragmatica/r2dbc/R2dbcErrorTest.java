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

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcTimeoutException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.pragmatica.r2dbc.R2dbcError.DatabaseFailure.databaseFailure;

class R2dbcErrorTest {

    @Test
    void connectionFailed_containsMessage() {
        var error = new R2dbcError.ConnectionFailed("Connection refused");

        assertThat(error.message()).contains("Connection refused");
    }

    @Test
    void queryFailed_containsSqlAndMessage() {
        var error = new R2dbcError.QueryFailed("SELECT * FROM users", "Table not found");

        assertThat(error.message()).contains("SELECT * FROM users");
        assertThat(error.message()).contains("Table not found");
    }

    @Test
    void constraintViolation_containsDetails() {
        var error = new R2dbcError.ConstraintViolation("uk_email");

        assertThat(error.message()).contains("uk_email");
    }

    @Test
    void timeout_containsOperation() {
        var error = new R2dbcError.Timeout("Query execution");

        assertThat(error.message()).contains("Query execution");
    }

    @Test
    void noResult_containsQuery() {
        var error = new R2dbcError.NoResult("SELECT * FROM users WHERE id = 999");

        assertThat(error.message()).contains("SELECT * FROM users WHERE id = 999");
    }

    @Test
    void multipleResults_containsQueryAndCount() {
        var error = new R2dbcError.MultipleResults("SELECT * FROM users", 5);

        assertThat(error.message()).contains("SELECT * FROM users");
        assertThat(error.message()).contains("5");
    }

    @Test
    void databaseFailure_wrapsException() {
        var cause = new RuntimeException("Unexpected");
        var error = databaseFailure(cause);

        assertThat(error.message()).contains("Unexpected");
        assertThat(error.cause()).isSameAs(cause);
    }

    @Test
    void fromException_mapsTimeoutException() {
        var ex = new R2dbcTimeoutException("Query timeout");
        var error = R2dbcError.fromException(ex);

        assertInstanceOf(R2dbcError.Timeout.class, error);
    }

    @Test
    void fromException_mapsConstraintViolation() {
        var ex = new R2dbcDataIntegrityViolationException("Unique constraint");
        var error = R2dbcError.fromException(ex);

        assertInstanceOf(R2dbcError.ConstraintViolation.class, error);
    }

    @Test
    void fromException_mapsUnknownToDatabaseFailure() {
        var ex = new IllegalStateException("Unknown error");
        var error = R2dbcError.fromException(ex);

        assertInstanceOf(R2dbcError.DatabaseFailure.class, error);
    }

    @Test
    void fromExceptionWithSql_includesSqlInError() {
        var ex = new RuntimeException("Syntax error");
        var error = R2dbcError.fromException(ex, "SELECT * FROM invalid");

        assertInstanceOf(R2dbcError.DatabaseFailure.class, error);
    }
}
