package org.pragmatica.http.routing;

import org.pragmatica.lang.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.Option.option;

public final class RequestRouter {
    private static final Logger log = LoggerFactory.getLogger(RequestRouter.class);

    // Store multiple routes per base path to handle routes with different spacers
    private final Map<HttpMethod, TreeMap<String, List<Route<?>>>> routes;

    private RequestRouter(Map<HttpMethod, TreeMap<String, List<Route<?>>>> routes) {
        this.routes = routes;
    }

    public static RequestRouter with(RouteSource... routes) {
        return with(Stream.of(routes));
    }

    public static RequestRouter with(Stream<RouteSource> routeStream) {
        var routes = new HashMap<HttpMethod, TreeMap<String, List<Route<?>>>>();
        routeStream.flatMap(RouteSource::routes)
                   .forEach(route -> routes.compute(route.method(),
                                                    (_, pathMap) -> collectRoutes(route, pathMap)));
        return new RequestRouter(routes);
    }

    private static TreeMap<String, List<Route<?>>> collectRoutes(Route<?> route,
                                                                 TreeMap<String, List<Route<?>>> pathMap) {
        var map = option(pathMap).or(TreeMap::new);
        map.computeIfAbsent(route.path(),
                            _ -> new ArrayList<>())
           .add(route);
        return map;
    }

    public void print() {
        if (!log.isInfoEnabled()) {
            return;
        }
        routes.forEach((_, endpoints) -> endpoints.forEach((_, routeList) -> routeList.forEach(route -> log.info("{}",
                                                                                                                 route))));
    }

    public Option<Route<?>> findRoute(HttpMethod method, String inputPath) {
        var path = inputPath + "/";
        return option(routes.get(method)).flatMap(map -> option(map.floorEntry(path)))
                     .filter(routeEntry -> isSameOrStartOfPath(path,
                                                               routeEntry.getKey()))
                     .flatMap(routeEntry -> selectBestRoute(routeEntry.getValue(),
                                                            inputPath));
    }

    /**
     * Select the best matching route from a list of routes with the same base path.
     * This handles routes that differ only in their spacer parameters.
     */
    private Option<Route<?>> selectBestRoute(List<Route<?>> candidates, String inputPath) {
        if (candidates.size() == 1) {
            return Option.some(candidates.getFirst());
        }
        // Multiple routes - find the one whose spacers match the input path
        // First try routes with spacers (more specific), then fallback to routes without spacers
        Route<?> fallback = null;
        for (var route : candidates) {
            if (route.spacers()
                     .isEmpty()) {
                fallback = route;
            } else if (routeMatchesPath(route, inputPath)) {
                return Option.some(route);
            }
        }
        // No spacer match - return fallback (route without spacers) or first candidate
        return Option.some(fallback != null
                           ? fallback
                           : candidates.getFirst());
    }

    /**
     * Check if a route matches the input path by verifying all spacers are present.
     */
    private boolean routeMatchesPath(Route<?> route, String inputPath) {
        // Extract path elements after the route's base path
        var basePath = route.path();
        if (inputPath.length() <= basePath.length()) {
            return route.spacers()
                        .isEmpty();
        }
        var remainder = inputPath.substring(basePath.length());
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        var pathElements = remainder.split("/");
        // Check if all spacers are present in the path elements
        for (var spacer : route.spacers()) {
            boolean found = false;
            for (var element : pathElements) {
                if (element.equals(spacer)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean isSameOrStartOfPath(String inputPath, String routePath) {
        return ( inputPath.length() == routePath.length() && inputPath.equals(routePath)) || (inputPath.length() > routePath.length() && inputPath.charAt(routePath.length() - 1) == '/');
    }
}
