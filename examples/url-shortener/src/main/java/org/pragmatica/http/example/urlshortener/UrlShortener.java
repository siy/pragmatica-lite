package org.pragmatica.http.example.urlshortener;

import org.pragmatica.db.postgres.DbEnv;
import org.pragmatica.db.postgres.DbEnvConfig;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerController;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerRequest;
import org.pragmatica.http.example.urlshortener.api.UrlShortenerResponse;
import org.pragmatica.http.example.urlshortener.domain.service.UrlShortenerService;
import org.pragmatica.http.example.urlshortener.persistence.ShortenedUrlRepository;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import static org.pragmatica.config.api.AppConfig.appConfig;

/**
 * This is the main class of the application. It is responsible for loading configuration, wiring all the dependencies together and starting the
 * server.
 */
public class UrlShortener {

    /**
     * Load configuration and start the server.
     *
     * @param args command line arguments
     */
    public static void main(String... args) {
        appConfig("database", DbEnvConfig.template(),
                  "server", HttpServerConfig.template())
            .map(UrlShortener::runApplication);
    }

    /**
     * Wire dependencies and run the application with the given configuration. In this application all dependencies form a chain:
     * <pre>
     *    ShortenedUrlRepository (used by) -> UrlShortenerService (used by) -> UrlShortenerController
     * </pre>
     * Since all these dependencies are interfaces with single method, we can use lambdas to implement them. The whole chain of dependencies can be
     * wired in a single expression:
     * <pre>
     *    UrlShortenerController controller = () -> () -> () -> DbEnv.with(dbEnvConfig);
     * </pre>
     * While this would be very concise, unfortunately reasoning about this code would be hard, as significant part of context is lost. A more verbose
     * version with explicit types helps preserve the context.
     *
     * @param dbEnvConfig      database configuration
     * @param httpServerConfig HTTP server configuration
     *
     * @return result of the application run
     */
    private static Result<Unit> runApplication(DbEnvConfig dbEnvConfig, HttpServerConfig httpServerConfig) {
        var dbEnv = DbEnv.with(dbEnvConfig);

        ShortenedUrlRepository repository = () -> dbEnv;
        UrlShortenerService service = () -> repository;
        UrlShortenerController controller = () -> service;

        return HttpServer.with(httpServerConfig,
                               Route.<UrlShortenerResponse, UrlShortenerRequest>post("/shorten")
                                    .withBody(UrlShortenerRequest.class)
                                    .toJson(controller::shortenUrl));
    }
}