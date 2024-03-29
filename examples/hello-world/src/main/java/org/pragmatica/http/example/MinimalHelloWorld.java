import org.pragmatica.http.server.routing.Route;

import static org.pragmatica.http.server.HttpServer.with;
import static org.pragmatica.http.server.HttpServerConfig.defaultConfiguration;

/**
 * Minimal version of "Hello world" example.
 */
public static void main(String[] args) {
    with(defaultConfiguration(),
         Route.get("/")
              .toText(() -> "Hello world!"));
}
