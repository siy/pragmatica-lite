package org.pragmatica.http.example.urlshortener;

import org.pragmatica.config.api.AppConfig;
import org.pragmatica.db.postgres.DbEnv;
import org.pragmatica.db.postgres.DbEnvConfig;
import org.pragmatica.db.postgres.DbEnvConfigTemplate;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerController;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerRequest;
import org.pragmatica.http.example.urlshortener.domain.service.UrlShortenerService;
import org.pragmatica.http.example.urlshortener.persistence.ShortenedUrlRepository;
import org.pragmatica.lang.type.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.HttpServerConfiguration.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.handlePost;

/**
 * This is the main class of the application. It is responsible for wiring all the dependencies together and starting the server.
 */
public class UrlShortener {
    private static final Logger log = LoggerFactory.getLogger(UrlShortener.class);

    public static void main(String... args) {
        AppConfig.loadDefault(args)
                 .load("database", DbEnvConfigTemplate.INSTANCE)
                 .onFailure(cause -> log.error("Failed to load configuration {}", cause))
                 .onSuccess(UrlShortener::runApplication);
    }

    private static void runApplication(DbEnvConfig dbEnvConfig) {
        // Wire dependencies.
        // Since in this case all dependencies form a chain to assemble UrlShortenerController,
        // it can be written as follows:
        //    UrlShortenerController controller = () -> () -> () -> DbEnv.with(dbEnvConfig);
        // Explicit types help preserve context.

        ShortenedUrlRepository repository = () -> DbEnv.with(dbEnvConfig);
        UrlShortenerService service = () -> repository;
        UrlShortenerController controller = () -> service;

        httpServerWith(defaultConfiguration().withPort(3000))
            .serveNow(
                handlePost("/shorten")
                    .whereBodyIs(new TypeToken<UrlShortenerRequest>() {})
                    .with(controller::shortenUrl)
                    .asJson()
            );
    }
}