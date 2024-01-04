package org.pragmatica.db.postgres;

import com.github.pgasync.SqlError;
import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.SqlException;
import com.github.pgasync.net.netty.NettyConnectibleBuilder;
import org.pragmatica.db.postgres.Sql.Query;
import org.pragmatica.db.postgres.Sql.Script;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.AsyncCloseable;
import org.pragmatica.net.InetPort;

import java.util.Collection;

public interface DbEnv extends AsyncCloseable {
    Promise<ResultSet> execute(Query query);

    Promise<Collection<ResultSet>> execute(Script script);

    static DbEnv with(DbEnvConfig configuration) {
        record env(DbEnvConfig configuration, Connectible connectible) implements DbEnv {
            static final InetPort DEFAULT_PORT = InetPort.inetPort(5432);
            static final Cause DOMAIN_NAME_REQUIRED = new SqlError.ConfigurationError("Domain name is required");

            @Override
            public Promise<Unit> close() {
                return connectible().close()
                                    .asPromise();
            }

            @Override
            public Promise<ResultSet> execute(Query query) {
                return connectible().completeQuery(query.sql(), query.values())
                                    .asPromise();
            }

            @Override
            public Promise<Collection<ResultSet>> execute(Script script) {
                return connectible().completeScript(script.sql())
                                    .asPromise();
            }
        }

        return Result.all(configuration.url().domain()
                                       .toResult(env.DOMAIN_NAME_REQUIRED))
                     .map((domain) -> new NettyConnectibleBuilder()
                         .hostname(domain.name())
                         .port(configuration.url().port().or(env.DEFAULT_PORT).port())
                         .username(configuration.username())
                         .password(configuration.password())
                         .maxConnections(configuration.maxConnections())
                         .pool())
                     .fold(cause -> {throw new SqlException(cause.message());},
                           connectible -> new env(configuration, connectible));
    }
}
