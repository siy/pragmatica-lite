package org.pragmatica.http.example;


import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.HttpServerConfiguration.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.get;
import static org.pragmatica.lang.Promise.ok;

public class HelloWorld {
    public static void main(String[] args) {
        httpServerWith(defaultConfiguration().withPort(3000))
            .serveNow(
                get("/").with(_ -> ok("Hello world!")).asText()
            );
    }
}