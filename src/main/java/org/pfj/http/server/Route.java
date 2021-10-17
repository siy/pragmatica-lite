package org.pfj.http.server;

import io.netty.handler.codec.http.HttpMethod;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pfj.http.server.Utils.normalize;

public record Route<T>(HttpMethod method, String path, Handler<T> handler,
                       ContentType contentType) implements RouteSource {
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

    public static RouteSource at(String basePath, RouteSource... routes) {
        return () -> Stream.of(routes).map(route -> route.withPrefix(basePath)).flatMap(RouteSource::routes);
    }

    public static RouteBuilder0 at(String path) {
        return new RouteBuilder0(path);
    }

    public record RouteBuilder0(String path) {
        public RouteBuilder1 options() {
            return new RouteBuilder1(path, HttpMethod.OPTIONS);
        }

        public RouteBuilder1 get() {
            return new RouteBuilder1(path, HttpMethod.GET);
        }

        public RouteBuilder1 head() {
            return new RouteBuilder1(path, HttpMethod.HEAD);
        }

        public RouteBuilder1 post() {
            return new RouteBuilder1(path, HttpMethod.POST);
        }

        public RouteBuilder1 put() {
            return new RouteBuilder1(path, HttpMethod.PUT);
        }

        public RouteBuilder1 patch() {
            return new RouteBuilder1(path, HttpMethod.PATCH);
        }

        public RouteBuilder1 delete() {
            return new RouteBuilder1(path, HttpMethod.DELETE);
        }

        public RouteBuilder1 trace() {
            return new RouteBuilder1(path, HttpMethod.TRACE);
        }

        public RouteBuilder1 connect() {
            return new RouteBuilder1(path, HttpMethod.CONNECT);
        }
    }

    public record RouteBuilder1(String path, HttpMethod method) {
        public RouteBuilder2 text() {
            return new RouteBuilder2(path, method, ContentType.TEXT_PLAIN);
        }

        public RouteBuilder2 json() {
            return new RouteBuilder2(path, method, ContentType.APPLICATION_JSON);
        }
    }

    public record RouteBuilder2(String path, HttpMethod method, ContentType contentType) {
        public <T> Route<T> from(Handler<T> handler) {
            return new Route<>(method, path, handler, contentType);
        }

        public <T> Route<T> from(String path, Supplier<Result<T>> supplier) {
            return new Route<>(method, path, __ -> Promise.promise(supplier.get()), contentType);
        }
    }
}
