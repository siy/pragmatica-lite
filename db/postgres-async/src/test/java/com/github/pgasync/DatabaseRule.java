package com.github.pgasync;

import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.ConnectibleBuilder;
import com.github.pgasync.net.Converter;
import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.netty.NettyConnectibleBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.rules.ExternalResource;
import org.pragmatica.lang.Result;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collection;
import java.util.List;

/**
 * @author Antti Laisi
 */
@Tag("Slow")
public class DatabaseRule extends ExternalResource {
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

    Result<ResultSet> query(String sql) {
        return pool().completeQuery(sql).await();
    }

    Result<ResultSet> query(String sql, List<?> params) {
        return pool().completeQuery(sql, params.toArray()).await();
    }

    Result<Collection<ResultSet>> script(String sql) {
        return pool().completeScript(sql).await();
    }

    Connectible pool() {
        before();
        return pool;
    }
}
