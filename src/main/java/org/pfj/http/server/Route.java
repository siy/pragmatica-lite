package org.pfj.http.server;

import io.netty.handler.codec.http.HttpMethod;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pfj.http.server.Utils.normalize;

public record Route<T>(HttpMethod method, String path, Handler<T> handler, ContentType contentType) implements RouteSource {
    public Route(HttpMethod method, String path, Handler<T> handler, ContentType contentType) {
        this.method = method;
        this.path = normalize(path);
        this.handler = handler;
        this.contentType = contentType;
    }

    @Override
    public Stream<Route<?>> routes() {
        return Stream.of(this);
    }

    @Override
    public RouteSource withPrefix(String prefix) {
        return new Route<T>(method, normalize(prefix + path), handler, contentType);
    }

    public static RouteSource from(String basePath, RouteSource ... routes) {
        return () -> Stream.of(routes).map(route -> route.withPrefix(basePath)).flatMap(RouteSource::routes);
    }

    public static <T> Route<T> getText(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.GET, path, handler, ContentType.TEXT_PLAIN);
    }

    public static <T> Route<T> getText(String path, Supplier<Result<T>> supplier) {
        return new Route<>(HttpMethod.GET, path, __ -> Promise.promise(supplier.get()), ContentType.TEXT_PLAIN);
    }

    public static <T> Route<T> postText(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.POST, path, handler, ContentType.TEXT_PLAIN);
    }

    public static <T> Route<T> postText(String path, Supplier<Result<T>> supplier) {
        return new Route<>(HttpMethod.POST, path, __ -> Promise.promise(supplier.get()), ContentType.TEXT_PLAIN);
    }

    public static <T> Route<T> getJson(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.GET, path, handler, ContentType.APPLICATION_JSON);
    }

    public static <T> Route<T> getJson(String path, Supplier<Result<T>> supplier) {
        return new Route<>(HttpMethod.GET, path, __ -> Promise.promise(supplier.get()), ContentType.APPLICATION_JSON);
    }

    public static <T> Route<T> postJson(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.POST, path, handler, ContentType.APPLICATION_JSON);
    }
}
