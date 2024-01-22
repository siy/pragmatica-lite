import static org.pragmatica.http.server.HttpServer.withConfig;
import static org.pragmatica.http.server.HttpServerConfig.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.handleGet;

/**
 * Minimal version of "Hello world" example.
 */
public static void main(String[] args) {
    withConfig(defaultConfiguration())
        .serveNow(
            handleGet("/").withText(() -> "Hello world!")
        );
}
