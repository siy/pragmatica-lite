package com.github.pgasync;

import com.github.pgasync.net.*;
import com.github.pgasync.net.netty.NettyConnectibleBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Antti Laisi
 */
@Tag("Slow")
public class DatabaseRule extends ExternalResource {
    private static final int MAX_CAUSE_DEPTH = 100;
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:15-alpine"
    );


    final ConnectibleBuilder builder;
    Connectible pool;

    private static ConnectibleBuilder defaultBuilder() {
        return new NettyConnectibleBuilder().ssl(false).encoding("utf-8");
    }

    private DatabaseRule(final ConnectibleBuilder builder) {
        this.builder = builder;
    }

    public static DatabaseRule defaultConfiguration() {
        return withMaxConnections(1);
    }

    public static DatabaseRule withMaxConnections(int maxConnections) {
        return new DatabaseRule(defaultBuilder().maxConnections(maxConnections));
    }

    public static <T> DatabaseRule withConverter(Converter<T> converter) {
        return new DatabaseRule(defaultBuilder().maxConnections(1).converters(converter));
    }

    public static boolean ifCause(Throwable th, Consumer<SqlException> action, CheckedRunnable others) throws Exception {
        int depth = 1;
        while (depth++ < MAX_CAUSE_DEPTH && th != null && !(th instanceof SqlException)) {
            th = th.getCause();
        }
        if (th instanceof SqlException sqlException) {
            action.accept(sqlException);
            return true;
        } else {
            others.run();
            return false;
        }
    }

    @Override
    protected void before() {
        if (pool == null) {
            postgres.start();
            pool = builder
                .port(postgres.getMappedPort(5432))
                .hostname(postgres.getHost())
                .password(postgres.getPassword())
                .database(postgres.getDatabaseName())
                .username(postgres.getUsername())
                .pool()
            ;
        }
    }

    @Override
    protected void after() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                postgres.stop();
            }
        }
    }

    ResultSet query(String sql) {
        return block(pool().completeQuery(sql));
    }

    ResultSet query(String sql, List<?> params) {
        return block(pool().completeQuery(sql, params.toArray()));
    }

    Collection<ResultSet> script(String sql) {
        return block(pool().completeScript(sql));
    }

    private <T> T block(CompletableFuture<T> future) {
        try {
            return future.get(5_0000000, TimeUnit.SECONDS);
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    Connectible pool() {
        before();
        return pool;
    }

    @FunctionalInterface
    public interface CheckedRunnable {

        void run() throws Exception;
    }
}
