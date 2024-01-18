import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.HttpServerConfig.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.handleGet;

/**
 * Minimal version of "Hello world" example.
 */
public static void main(String[] args) {
    httpServerWith(defaultConfiguration())
        .serveNow(
            handleGet("/").withText(() -> "Hello world!")
        );
}
