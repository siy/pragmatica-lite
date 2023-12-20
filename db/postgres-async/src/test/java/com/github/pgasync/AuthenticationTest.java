package com.github.pgasync;

import com.github.pgasync.net.SqlException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

import static com.github.pgasync.SqlError.ServerErrorInvalidAuthorizationSpecification;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("Slow")
public class AuthenticationTest {

    @ClassRule
    public static final DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @Test(expected = SqlException.class)
    public void shouldThrowExceptionOnInvalidCredentials() throws Exception {
        var pool = dbr.builder
            .password("_invalid_")
            .pool();
        try {
            pool.completeQuery("SELECT 1")
                .await();
        } catch (Exception ex) {
            DatabaseRule.ifCause(ex,
                                 sqlException -> {
                                     assertTrue(sqlException.error() instanceof ServerErrorInvalidAuthorizationSpecification);
                                     throw sqlException;
                                 },
                                 () -> {
                                     throw ex;
                                 });
        } finally {
            pool.close()
                .await();
        }
    }

    @Test
    public void shouldGetResultOnValidCredentials() {
        var pool = dbr.builder
            .password(DatabaseRule.postgres.getPassword())
            .pool();
        try {
            var rs = pool.completeQuery("SELECT 1").await();
            assertEquals(1L, (long) rs.index(0).getInt(0));
        } finally {
            pool.close()
                .await();
        }
    }

}
