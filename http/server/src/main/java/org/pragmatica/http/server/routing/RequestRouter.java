package org.pragmatica.http.server.routing;

import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.lang.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.pragmatica.lang.Option.option;

public final class RequestRouter {
    private static final Logger log = LoggerFactory.getLogger(RequestRouter.class);

    private final Map<HttpMethod, TreeMap<String, Route<?>>> routes;

    private RequestRouter(Map<HttpMethod, TreeMap<String, Route<?>>> routes) {
        this.routes = routes;
    }

    public static RequestRouter with(RouteSource... routes) {
        return with(Stream.of(routes));
    }

    public static RequestRouter with(Stream<RouteSource> routeStream) {
        var routes = new HashMap<HttpMethod, TreeMap<String, Route<?>>>();

        routeStream.flatMap(RouteSource::routes)
                   .forEach(route -> routes.compute(route.method(), (_, pathMap) -> collectRoutes(route, pathMap)));
        return new RequestRouter(routes);
    }

    private static TreeMap<String, Route<?>> collectRoutes(Route<?> route, TreeMap<String, Route<?>> pathMap) {
        var map = pathMap == null
                  ? new TreeMap<String, Route<?>>()
                  : pathMap;

        map.put(route.path(), route);

        return map;
    }

    public void print() {
        routes.forEach((_, endpoints) ->
                           endpoints.forEach((_, route) -> log.debug("{}", route)));
    }

    public Option<Route<?>> findRoute(HttpMethod method, String inputPath) {
        var path = inputPath + "/";

        return option(routes.get(method))
            .flatMap(map -> option(map.floorEntry(path)))
            .filter(routeEntry -> isSameOrStartOfPath(path, routeEntry.getKey()))
            .map(Map.Entry::getValue);
    }

    private boolean isSameOrStartOfPath(String inputPath, String routePath) {
        return (inputPath.length() == routePath.length() && inputPath.equals(routePath))
               || (inputPath.length() > routePath.length() && inputPath.charAt(routePath.length() - 1) == '/');
    }
}
