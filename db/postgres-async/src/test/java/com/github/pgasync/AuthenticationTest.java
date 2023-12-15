package com.github.pgasync;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Tag("Slow")
public class AuthenticationTest {

    @ClassRule
    public static DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @Test
    public void shouldReturnCorrespondingErrorOnInvalidCredentials() {
        var pool = dbr.builder
            .password("_invalid_")
            .pool();

        pool.completeQuery("SELECT 1").await()
            .onSuccess(Assert::fail)
            .onFailure(cause -> assertTrue(cause instanceof SqlError.InvalidCredentials));

        pool.close().await();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldGetResultOnValidCredentials() {
        var pool = dbr.builder
            .password(DatabaseRule.postgres.getPassword())
            .pool();
        try {
            var rs = pool.completeQuery("SELECT 1").await().unwrap();
            assertEquals(1L, (long) rs.index(0).getInt(0));
        } finally {
                pool.close().await();
        }
    }

}
