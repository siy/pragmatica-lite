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

    static ParameterBuilder handleOptions(String path) {
        return handleMethod(path, HttpMethod.OPTIONS);
    }

    static ParameterBuilder handleGet(String path) {
        return handleMethod(path, HttpMethod.GET);
    }

    static ParameterBuilder handleHead(String path) {
        return handleMethod(path, HttpMethod.HEAD);
    }

    static ParameterBuilder handlePost(String path) {
        return handleMethod(path, HttpMethod.POST);
    }

    static ParameterBuilder handlePut(String path) {
        return handleMethod(path, HttpMethod.PUT);
    }

    static ParameterBuilder handlePatch(String path) {
        return handleMethod(path, HttpMethod.PATCH);
    }

    static ParameterBuilder handleDelete(String path) {
        return handleMethod(path, HttpMethod.DELETE);
    }

    static ParameterBuilder handleTrace(String path) {
        return handleMethod(path, HttpMethod.TRACE);
    }

    static ParameterBuilder handleConnect(String path) {
        return handleMethod(path, HttpMethod.CONNECT);
    }

    static ParameterBuilder handleMethod(String path, HttpMethod method) {
        return () -> tuple(path, method);
    }

    interface ParameterBuilder {
        Tuple2<String, HttpMethod> pathAndMethod();

        default RawHandlerBuilder withoutParameters() {
            return this::pathAndMethod;
        }

        default <T> JsonBodyParameterBuilder<T> whereBodyIs(TypeToken<T> type) {
            return () -> tuple(pathAndMethod(), type);
        }
        //TODO: support for path and query parameters

        //Shortcuts for frequent case of no input parameters and popular content types
        default <T> ContentTypeBuilder<T> with(Handler<T> handler) {
            return withoutParameters().with(handler);
        }

        default <T> ContentTypeBuilder<T> withImmediate(Fn1<Result<T>, RequestContext> handler) {
            return withoutParameters().withImmediate(handler);
        }

        default <T> Route<T> withText(Handler<T> handler) {
            return with(handler).asText();
        }

        default <T> Route<T> withText(Supplier<T> handler) {
            return with(_ -> Promise.successful(handler.get())).asText();
        }

        default <T> Route<T> withJson(Handler<T> handler) {
            return with(handler).asJson();
        }

        default <T> Route<T> withJson(Supplier<T> handler) {
            return with(_ -> Promise.successful(handler.get())).asJson();
        }
    }

    // Short, but low level path
    interface RawHandlerBuilder {
        Tuple2<String, HttpMethod> pathAndMethod();

        default <T> ContentTypeBuilder<T> with(Handler<T> handler) {
            return contentType -> route(pathAndMethod().last(), pathAndMethod().first(), handler, contentType);
        }

        default <T> ContentTypeBuilder<T> withImmediate(Fn1<Result<T>, RequestContext> handler) {
            return contentType -> route(pathAndMethod().last(), pathAndMethod().first(), ctx -> Promise.resolved(handler.apply(ctx)), contentType);
        }
        // No shortcuts are provided. If we're here, we chose to be explicit and therefore need to specify content type
    }

    interface JsonBodyParameterBuilder<T> {
        Tuple2<Tuple2<String, HttpMethod>, TypeToken<T>> pathMethodAndBodyType();

        default <R> ContentTypeBuilder<R> with(Fn1<Promise<R>, T> handler) {
            var pathMethod = pathMethodAndBodyType().first();
            var bodyType = pathMethodAndBodyType().last();

            return contentType -> route(pathMethod.last(),
                                        pathMethod.first(),
                                        ctx -> ctx.fromJson(bodyType)
                                                  .toPromise()
                                                  .flatMap(handler),
                                        contentType);
        }
    }

    interface ContentTypeBuilder<T> {
        Route<T> as(ContentType contentType);

        default Route<T> asText() {
            return as(CommonContentTypes.TEXT_PLAIN);
        }

        default Route<T> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
}
