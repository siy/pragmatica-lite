package org.pragmatica.http.example.urlshortener;

import org.pragmatica.db.postgres.DbEnv;
import org.pragmatica.db.postgres.DbEnvConfig;
import org.pragmatica.db.postgres.DbEnvConfigTemplate;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerController;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerRequest;
import org.pragmatica.http.example.urlshortener.domain.service.UrlShortenerService;
import org.pragmatica.http.example.urlshortener.persistence.ShortenedUrlRepository;
import org.pragmatica.lang.type.TypeToken;

import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.HttpServerConfiguration.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.handlePost;
import static org.pragmatica.lang.Option.none;

/**
 * This is the main class of the application. It is responsible for wiring all the dependencies together and starting the server.
 */
public class UrlShortener {
    private static final DbEnvConfig dbEnvConfig =
        DbEnvConfigTemplate.builder()
                           .url("postgres://localhost:5432/urlshortener")
                           .username("urlshortener")
                           .password("urlshortener")
                           .maxConnections(-1)
                           .maxStatements(10)
                           .useSsl(false)
                           .validationQuery(none()).encoding(none());

    public static void main(String... args) {
        // Verbosity is intentional here to show the full dependency graph
        // Concise version would be:
        // UrlShortenerController controller = () -> () -> () -> DbEnv.with(dbEnvConfig);

        ShortenedUrlRepository repository = () -> DbEnv.with(dbEnvConfig);
        UrlShortenerService service = () -> repository;
        UrlShortenerController controller = () -> service;

        httpServerWith(defaultConfiguration().withPort(3000))
            .serveNow(
                handlePost("/shorten").whereBodyIs(new TypeToken<UrlShortenerRequest>() {})
                                      .with(controller::shortenUrl)
                                      .asJson()
            );
    }
}