package org.pragmatica.http.server.routing;

import java.util.stream.Stream;

public interface RouteSource {
    Stream<Route<?>> routes();

    default RouteSource withPrefix(String prefix) {
        return () -> routes()
            .map(route -> (Route<?>) route.withPrefix(prefix));
    }
}
