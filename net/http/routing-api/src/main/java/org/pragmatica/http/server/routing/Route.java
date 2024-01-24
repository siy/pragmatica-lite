package org.pragmatica.http.server.routing;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.http.util.Utils;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.type.TypeToken;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.http.protocol.HttpMethod.*;
import static org.pragmatica.lang.Tuple.tuple;

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
                return STR."Route: \{method} \{path}, \{contentType}";
            }
        }

        return new route<>(method, Utils.normalize(path), handler, contentType);
    }

    static Subroutes in(String path) {
        return () -> path;
    }

    interface Subroutes {
        String path();

        default RouteSource serve(RouteSource... subroutes) {
            return () -> Stream.of(subroutes)
                               .map(route -> route.withPrefix(path()))
                               .flatMap(RouteSource::routes);
        }
    }

    static ParameterBuilder whenOptions(String path) {
        return when(OPTIONS, path);
    }

    static ParameterBuilder whenGet(String path) {
        return when(GET, path);
    }

    static ParameterBuilder whenHead(String path) {
        return when(HEAD, path);
    }

    static ParameterBuilder whenPost(String path) {
        return when(POST, path);
    }

    static ParameterBuilder whenPut(String path) {
        return when(PUT, path);
    }

    static ParameterBuilder whenPatch(String path) {
        return when(PATCH, path);
    }

    static ParameterBuilder whenDelete(String path) {
        return when(DELETE, path);
    }

    static ParameterBuilder whenTrace(String path) {
        return when(TRACE, path);
    }

    static ParameterBuilder whenConnect(String path) {
        return when(CONNECT, path);
    }

    static ParameterBuilder when(HttpMethod method, String path) {
        return () -> tuple(path, method);
    }

    interface ParameterBuilder {
        Tuple2<String, HttpMethod> pathAndMethod();

        default RawHandlerBuilder withoutParameters() {
            return this::pathAndMethod;
        }

        default <T> JsonBodyParameterBuilder<T> withBody(TypeToken<T> type) {
            return () -> tuple(pathAndMethod(), type);
        }

        default <T> JsonBodyParameterBuilder<T> withBody(Class<T> type) {
            return withBody(TypeToken.of(type));
        }

        //TODO: support for path and query parameters

        //Shortcuts for frequent case of no input parameters and popular content types
        default <T> ContentTypeBuilder<T> returnFrom(Handler<T> handler) {
            return withoutParameters().returnFrom(handler);
        }

        default <T> ContentTypeBuilder<T> returnNow(Fn1<Result<T>, RequestContext> handler) {
            return withoutParameters().returnNow(handler);
        }

        default <T> Route<T> returnText(Handler<T> handler) {
            return returnFrom(handler).text();
        }

        default <T> Route<T> returnText(Supplier<T> handler) {
            return returnFrom(_ -> Promise.successful(handler.get())).text();
        }

        default <T> Route<T> returnJson(Handler<T> handler) {
            return returnFrom(handler).json();
        }

        default <T> Route<T> returnJson(Supplier<T> handler) {
            return returnFrom(_ -> Promise.successful(handler.get())).json();
        }
    }

    // Short, but low level path
    interface RawHandlerBuilder {
        Tuple2<String, HttpMethod> pathAndMethod();

        default <T> ContentTypeBuilder<T> returnFrom(Handler<T> handler) {
            return contentType -> route(pathAndMethod().last(), pathAndMethod().first(), handler, contentType);
        }

        default <T> ContentTypeBuilder<T> returnNow(Fn1<Result<T>, RequestContext> handler) {
            return contentType -> route(pathAndMethod().last(), pathAndMethod().first(), ctx -> Promise.resolved(handler.apply(ctx)), contentType);
        }
        // No shortcuts are provided. If we're here, we chose to be explicit and therefore need to specify content type
    }

    interface JsonBodyParameterBuilder<T> {
        Tuple2<Tuple2<String, HttpMethod>, TypeToken<T>> pathMethodAndBodyType();

        default <R> ContentTypeBuilder<R> returnFrom(Fn1<Promise<R>, T> handler) {
            var pathMethod = pathMethodAndBodyType().first();
            var bodyType = pathMethodAndBodyType().last();

            return contentType -> route(pathMethod.last(),
                                        pathMethod.first(),
                                        ctx -> ctx.fromJson(bodyType)
                                                  .toPromise()
                                                  .flatMap(handler),
                                        contentType);
        }

        default <R> Route<R> returnJson(Fn1<Promise<R>, T> handler) {
            return returnFrom(handler).json();
        }
    }

    interface ContentTypeBuilder<T> {
        Route<T> a(ContentType contentType);

        default Route<T> text() {
            return a(CommonContentTypes.TEXT_PLAIN);
        }

        default Route<T> json() {
            return a(CommonContentTypes.APPLICATION_JSON);
        }
    }
}
