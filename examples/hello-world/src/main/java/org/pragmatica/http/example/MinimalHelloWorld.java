import static org.pragmatica.http.server.HttpServer.with;
import static org.pragmatica.http.server.HttpServerConfig.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.whenGet;

/**
 * Minimal version of "Hello world" example.
 */
public static void main(String[] args) {
    with(defaultConfiguration(),
         whenGet("/")
             .returnText(() -> "Hello world!"));
}
