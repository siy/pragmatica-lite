package org.pragmatica.http.example.urlshortener;

import org.pragmatica.db.postgres.DbEnv;
import org.pragmatica.db.postgres.DbEnvConfig;
import org.pragmatica.db.postgres.DbEnvConfigTemplate;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerController;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerRequest;
import org.pragmatica.http.example.urlshortener.domain.service.UrlShortenerService;
import org.pragmatica.http.example.urlshortener.persistence.ShortenedUrlRepository;
import org.pragmatica.http.server.HttpServerConfiguration;
import org.pragmatica.http.server.HttpServerConfigurationTemplate;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.config.api.AppConfig.loadConfigs;
import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.HttpServerConfiguration.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.handlePost;

/**
 * This is the main class of the application. It is responsible for loading configuration, wiring all the dependencies together and starting the
 * server.
 */
public class UrlShortener {
    private static final Logger log = LoggerFactory.getLogger(UrlShortener.class);

    /**
     * Load configuration and start the server.
     *
     * @param args command line arguments
     */
    public static void main(String... args) {
        var configuration = loadConfigs(args);

        Result.all(configuration.load("database", DbEnvConfigTemplate.INSTANCE),
                   configuration.load("server", HttpServerConfigurationTemplate.INSTANCE))
              .map(UrlShortener::runApplication)
              .onFailure(cause -> log.error("Failed to load configuration {}", cause));
    }

    /**
     * Wire dependencies and run the application with the given configuration.
     *
     * @param dbEnvConfig             database configuration
     * @param httpServerConfiguration HTTP server configuration
     *
     * @return result of the application run
     */
    private static Result<Unit> runApplication(DbEnvConfig dbEnvConfig, HttpServerConfiguration httpServerConfiguration) {
        // In this application all dependencies form a chain:
        //
        // ShortenedUrlRepository (used by) -> UrlShortenerService (used by) -> UrlShortenerController
        //
        // Since all these dependencies are interfaces with single method, we can use lambdas to implement them.
        // The whole chain of dependencies can be wired in a single expression:
        //
        //    UrlShortenerController controller = () -> () -> () -> DbEnv.with(dbEnvConfig);
        //
        // While this would be very concise, unfortunately reasoning about this code would be hard, as significant
        // part of context is lost. A more verbose version with explicit types helps preserve the context.

        ShortenedUrlRepository repository = () -> DbEnv.with(dbEnvConfig);
        UrlShortenerService service = () -> repository;
        UrlShortenerController controller = () -> service;

        return httpServerWith(defaultConfiguration().withPort(3000))
            .serveNow(
                handlePost("/shorten")
                    .whereBodyIs(new TypeToken<UrlShortenerRequest>() {})
                    .with(controller::shortenUrl)
                    .asJson()
            );
    }
}