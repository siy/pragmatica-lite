package com.github.pgasync;

import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.PreparedStatement;
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
    public static final DatabaseRule dbr = DatabaseRule.withMaxConnections(5);

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

    @Test
    public void shouldEvictedStatementBeReallyClosed() {
        Connection conn = pool.getConnection().await();
        try {
            PreparedStatement evictor = conn.prepareStatement(SELECT_52).await();
            try {
                PreparedStatement evicted = conn.prepareStatement(SELECT_32).await();
                evicted.close().await();
            } finally {
                evictor.close().await();
            }
        } finally {
            conn.close().await();
        }
    }

    @Test
    public void shouldDuplicatedStatementBeReallyClosed() {
        Connection conn = pool.getConnection().await();
        try {
            PreparedStatement stmt = conn.prepareStatement(SELECT_52).await();
            try {
                PreparedStatement duplicated = conn.prepareStatement(SELECT_52).await();
                duplicated.close().await();
            } finally {
                stmt.close().await();
            }
        } finally {
            conn.close().await();
        }
    }

    @Test
    public void shouldDuplicatedAndEvictedStatementsBeReallyClosed() {
        Connection conn = pool.getConnection().await();
        try {
            PreparedStatement stmt = conn.prepareStatement(SELECT_52).await();
            try {
                PreparedStatement duplicated = conn.prepareStatement(SELECT_52).await();
                try {
                    PreparedStatement evicted = conn.prepareStatement(SELECT_32).await();
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
