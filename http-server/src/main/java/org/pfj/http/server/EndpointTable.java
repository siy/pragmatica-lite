package org.pfj.http.server;

import io.netty.handler.codec.http.HttpMethod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pfj.lang.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.pfj.lang.Option.option;

//WARNING: dynamic route configuration is not supported
public final class EndpointTable {
    private static final Logger log = LogManager.getLogger(EndpointTable.class);

    private final Map<HttpMethod, TreeMap<String, Route<?>>> routes = new HashMap<>();

    public static EndpointTable create() {
        return new EndpointTable();
    }

    public static EndpointTable with(RouteSource... routes) {
        var table = new EndpointTable();

        Stream.of(routes).flatMap(RouteSource::routes).forEach(table::add);

        return table;
    }

    public EndpointTable print() {
        routes.forEach((method, endpoints) ->
                           endpoints.forEach((path, route) -> log.info("{}", route)));
        return this;
    }

    public EndpointTable add(Route<?> route) {
        routes.compute(route.method(), (method, pathMap) -> {
            var map = pathMap == null ? new TreeMap<String, Route<?>>() : pathMap;
            map.put(route.path(), route);
            return map;
        });

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
