package org.pragmatica.http.server.routing;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.http.util.Utils;
import org.pragmatica.lang.Result;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.lang.Promise.resolved;
import static org.pragmatica.lang.Promise.successful;

//TODO: rework API
//TODO: better support for path parameter extraction
@SuppressWarnings("unused")
public interface Route<T> extends RouteSource {
    HttpMethod method();

    String path();

    Handler<T> handler();

    ContentType contentType();

    @Override
    default Stream<Route<?>> routes() {
        return Stream.of(this);
    }

    @Override
    RouteSource withPrefix(String prefix);

    static <T> Route<T> route(HttpMethod method, String path, Handler<T> handler, ContentType contentType) {
        record route<T>(HttpMethod method, String path, Handler<T> handler, ContentType contentType) implements Route<T> {
            @Override
            public RouteSource withPrefix(String prefix) {
                return new route<>(method, Utils.normalize(prefix + path), handler, contentType);
            }

            @Override
            public String toString() {
                return STR. "Route: \{ method } \{ path }, \{ contentType }" ;
            }
        }

        return new route<>(method, Utils.normalize(path), handler, contentType);
    }

    static RouteBuilder0 in(String path) {
        return RouteBuilder0.builder0(path);
    }

    interface RouteBuilder0 {
        String path();

        static RouteBuilder0 builder0(String path) {
            record routeBuilder0(String path) implements RouteBuilder0 {}

            return new routeBuilder0(path);
        }

        default RouteSource serve(RouteSource... subroutes) {
            return () -> Stream.of(subroutes)
                               .map(route -> route.withPrefix(path()))
                               .flatMap(RouteSource::routes);
        }
    }


    static RouteBuilder1 options(String path) {
        return method(path, HttpMethod.OPTIONS);
    }

    static RouteBuilder1 get(String path) {
        return method(path, HttpMethod.GET);
    }

    static RouteBuilder1 head(String path) {
        return method(path, HttpMethod.HEAD);
    }

    static RouteBuilder1 post(String path) {
        return method(path, HttpMethod.POST);
    }

    static RouteBuilder1 put(String path) {
        return method(path, HttpMethod.PUT);
    }

    static RouteBuilder1 patch(String path) {
        return method(path, HttpMethod.PATCH);
    }

    static RouteBuilder1 delete(String path) {
        return method(path, HttpMethod.DELETE);
    }

    static RouteBuilder1 trace(String path) {
        return method(path, HttpMethod.TRACE);
    }

    static RouteBuilder1 connect(String path) {
        return method(path, HttpMethod.CONNECT);
    }

    static RouteBuilder1 method(String path, HttpMethod method) {
        return RouteBuilder1.builder1(path, method);
    }

    interface RouteBuilder1 {
        String path();

        HttpMethod method();

        static RouteBuilder1 builder1(String path, HttpMethod method) {
            record routeBuilder1(String path, HttpMethod method) implements RouteBuilder1 {}

            return new routeBuilder1(path, method);
        }

        default <T> RouteBuilder2<T> with(Handler<T> handler) {
            return contentType -> route(method(), path(), handler, contentType);
        }

        default <T> RouteBuilder2<T> with(Supplier<Result<T>> supplier) {
            return with(_ -> resolved(supplier.get()));
        }

        default <T> Route<T> textWith(Handler<T> handler) {
            return with(handler).as(CommonContentTypes.TEXT_PLAIN);
        }

        default <T> Route<T> textWith(Supplier<T> supplier) {
            return textWith(_ -> successful(supplier.get()));
        }

        default <T> Route<T> jsonWith(Handler<T> handler) {
            return with(handler).as(CommonContentTypes.APPLICATION_JSON);
        }

        default <T> Route<T> jsonWith(Supplier<T> supplier) {
            return jsonWith(_ -> successful(supplier.get()));
        }
    }

    interface RouteBuilder2<T> {
        Route<T> as(ContentType contentType);
        default Route<T> asText() {
            return as(CommonContentTypes.TEXT_PLAIN);
        }
        default Route<T> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
}
