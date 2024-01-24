package org.pragmatica.http.example;


import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;

import static org.pragmatica.config.api.AppConfig.appConfig;
import static org.pragmatica.http.server.routing.Route.whenGet;

/**
 * Full-featured Hello World example, which loads server configuration from application.properties.
 */
public class HelloWorld {
    public static void main(String[] args) {
        appConfig("server", HttpServerConfig.template())
            .flatMap(configuration -> HttpServer.with(configuration,
                                                      whenGet("/")
                                                          .returnText(() -> "Hello world!")));
    }
}
