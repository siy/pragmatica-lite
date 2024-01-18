package org.pragmatica.http.example;


import org.pragmatica.http.server.HttpServerConfigTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.config.api.AppConfig.defaultApplicationConfig;
import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.routing.Route.handleGet;

/**
 * Somewhat non-traditional "Hello world" example. Instead of trying to show minimal possible code it shows more practical use case, where
 * configuration is loaded from the file/environment and only then server is started. Unlike traditional "Hello world" examples, this one shows how
 * real application actually will look like.
 */
public class HelloWorld {
    private static final Logger log = LoggerFactory.getLogger(HelloWorld.class);

    public static void main(String[] args) {
        defaultApplicationConfig()
            .load("server", HttpServerConfigTemplate.INSTANCE)
            .onFailure(cause -> log.error("Failed to load configuration {}", cause))
            .onSuccess(config -> httpServerWith(config).serveNow(
                handleGet("/").withText(() -> "Hello world!")
            ));
    }
}