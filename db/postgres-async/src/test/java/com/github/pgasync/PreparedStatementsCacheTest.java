package com.github.pgasync;

import com.github.pgasync.net.Connectible;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

/**
 * @author Marat Gainullin
 */
@Tag("Slow")
public class PreparedStatementsCacheTest {

    private static final String SELECT_52 = "select 52";
    private static final String SELECT_32 = "select 32";

    @ClassRule
    public static DatabaseRule dbr = DatabaseRule.withMaxConnections(5);

    private Connectible pool;

    @Before
    public void setup() {
        pool = dbr.builder
            .maxConnections(1)
            .maxStatements(1)
            .pool();
    }

    @After
    public void shutdown() {
        pool.close().await();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldEvictedStatementBeReallyClosed() { //TODO: what is this test for?
        var conn = pool.connection().await().unwrap();
        try {
            var evictor = conn.prepareStatement(SELECT_52).await().unwrap();
            try {
                var evicted = conn.prepareStatement(SELECT_32).await().unwrap();
                evicted.close().await();
            } finally {
                evictor.close().await();
            }
        } finally {
            conn.close().await();
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldDuplicatedStatementBeReallyClosed() {
        var conn = pool.connection().await().unwrap();
        try {
            var stmt = conn.prepareStatement(SELECT_52).await().unwrap();
            try {
                var duplicated = conn.prepareStatement(SELECT_52).await().unwrap();
                duplicated.close().await().unwrap();
            } finally {
                stmt.close().await();
            }
        } finally {
            conn.close().await();
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldDuplicatedAndEvictedStatementsBeReallyClosed() {
        var conn = pool.connection().await().unwrap();
        try {
            var stmt = conn.prepareStatement(SELECT_52).await().unwrap();
            try {
                var duplicated = conn.prepareStatement(SELECT_52).await().unwrap();
                try {
                    var evicted = conn.prepareStatement(SELECT_32).await().unwrap();
                    evicted.close().await();
                } finally {
                    duplicated.close().await();
                }
            } finally {
                stmt.close().await();
            }
        } finally {
            conn.close().await();
        }
    }
}
