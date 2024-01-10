package org.pragmatica.db.postgres;

import com.github.pgasync.PgResultSet;
import com.github.pgasync.SqlError;
import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.SqlException;
import com.github.pgasync.net.netty.NettyConnectibleBuilder;
import org.pragmatica.db.postgres.Sql.Query;
import org.pragmatica.db.postgres.Sql.Script;
import org.pragmatica.dns.DomainName;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.Result.Mapper3;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.AsyncCloseable;
import org.pragmatica.net.InetPort;
import org.pragmatica.uri.IRI;

import java.util.Collection;

import static org.pragmatica.lang.Result.all;
import static org.pragmatica.lang.Result.success;

public interface DbEnv extends AsyncCloseable {
    Promise<ResultAccessor> execute(Query query);

    Promise<Collection<ResultAccessor>> execute(Script script);

    static DbEnv with(DbEnvConfig configuration) {
        record env(DbEnvConfig configuration, Connectible connectible) implements DbEnv {
            static final InetPort DEFAULT_PORT = InetPort.inetPort(5432);
            static final Cause DOMAIN_NAME_REQUIRED = new SqlError.ConfigurationError("Domain name is required");
            static final Cause DATABASE_NAME_REQUIRED = new SqlError.ConfigurationError("Database name is required");

            @Override
            public Promise<Unit> close() {
                return connectible().close()
                                    .asPromise();
            }

            @Override
            public Promise<ResultAccessor> execute(Query query) {
                return connectible().completeQuery(query.sql(), query.values())
                                    .asPromise()
                                    .map(ResultAccessor::wrap);
            }

            @Override
            public Promise<Collection<ResultAccessor>> execute(Script script) {
                return connectible().completeScript(script.sql())
                                    .asPromise()
                                    .map(env::wrap);
            }

            private static Collection<ResultAccessor> wrap(Collection<PgResultSet> collection) {
                return collection.stream()
                                 .map(ResultAccessor::wrap)
                                 .toList();
            }

            static Mapper3<String, String, Integer> validate(IRI url) {
                return all(url.domain()
                              .map(DomainName::name)
                              .filter(domain -> !domain.isBlank())
                              .toResult(env.DOMAIN_NAME_REQUIRED),
                           url.path().map(path -> path.substring(1))
                              .filter(path -> !path.isBlank())
                              .toResult(env.DATABASE_NAME_REQUIRED),
                           success(url.port().or(env.DEFAULT_PORT).port()));
            }
        }

        var maxConnections = configuration.maxConnections() <= 0
                             ? Runtime.getRuntime().availableProcessors()
                             : configuration.maxConnections();
        var effectiveConfiguration = configuration.withMaxConnections(maxConnections);
        var url = configuration.url();

        return env.validate(url)
                  .map((domain, path, port) -> new NettyConnectibleBuilder().hostname(domain)
                                                                            .port(port)
                                                                            .username(configuration.username())
                                                                            .password(configuration.password())
                                                                            .database(path)
                                                                            .maxConnections(maxConnections)
                                                                            .maxStatements(configuration.maxStatements())
                                                                            .pool())
                  .fold(cause -> {throw new SqlException(cause.message());},
                        connectible -> new env(effectiveConfiguration, connectible));
    }
}
