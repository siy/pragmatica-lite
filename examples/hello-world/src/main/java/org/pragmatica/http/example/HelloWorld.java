package org.pragmatica.http.example;


import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.http.server.HttpServerConfigTemplate;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import static org.pragmatica.config.api.AppConfig.appConfig;
import static org.pragmatica.http.server.routing.Route.handleGet;

/**
 * Somewhat non-traditional "Hello world" example. Instead of trying to show minimal possible code it shows more practical use case, where
 * configuration is loaded from the file/environment and only then server is started. Unlike traditional "Hello world" examples, this one shows
 * longest possible sequence how real application actually will look like.
 * <p>
 * Any errors are logged when they happen.
 */
public class HelloWorld {
    public static void main(String[] args) {
        appConfig("server", HttpServerConfigTemplate.INSTANCE)
            .flatMap(HelloWorld::runServer);
    }

    private static Result<Unit> runServer(HttpServerConfig configuration) {
        return HttpServer.with(configuration, route());
    }

    private static Route<String> route() {
        return handleGet("/")
            .withText(() -> "Hello world!");
    }
}
