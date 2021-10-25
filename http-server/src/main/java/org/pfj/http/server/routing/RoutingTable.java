package org.pfj.http.server.routing;

import io.netty.handler.codec.http.HttpMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pfj.lang.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.pfj.lang.Option.option;

public final class RoutingTable {
    private static final Logger log = LogManager.getLogger(RoutingTable.class);

    private final Map<HttpMethod, TreeMap<String, Route<?>>> routes;

    private RoutingTable(Map<HttpMethod, TreeMap<String, Route<?>>> routes) {
        this.routes = routes;
    }

    public static RoutingTable with(RouteSource... routes) {
        return with(Stream.of(routes));
    }

    public static RoutingTable with(Stream<RouteSource> routeStream) {
        var routes = new HashMap<HttpMethod, TreeMap<String, Route<?>>>();

        routeStream.flatMap(RouteSource::routes)
            .forEach(route -> routes.compute(route.method(), (method, pathMap) -> collectRoutes(route, pathMap)));
        return new RoutingTable(routes);
    }

    private static TreeMap<String, Route<?>> collectRoutes(Route<?> route, TreeMap<String, Route<?>> pathMap) {
        var map = pathMap == null
            ? new TreeMap<String, Route<?>>()
            : pathMap;

        map.put(route.path(), route);

        return map;
    }

    public RoutingTable print() {
        routes.forEach((method, endpoints) ->
            endpoints.forEach((path, route) -> log.info("{}", route)));
        return this;
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
