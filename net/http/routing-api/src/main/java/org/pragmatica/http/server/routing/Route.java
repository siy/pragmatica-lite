package org.pragmatica.http.server.routing;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.http.util.Utils;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.http.protocol.HttpMethod.*;

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

    static <R, T> ParameterBuilder<R, T> options(String path) {
        return method(OPTIONS, path);
    }

    static <R, T> ParameterBuilder<R, T> get(String path) {
        return method(GET, path);
    }

    static <R, T> ParameterBuilder<R, T> head(String path) {
        return method(HEAD, path);
    }

    static <R, T> ParameterBuilder<R, T> post(String path) {
        return method(POST, path);
    }

    static <R, T> ParameterBuilder<R, T> put(String path) {
        return method(PUT, path);
    }

    static <R, T> ParameterBuilder<R, T> patch(String path) {
        return method(PATCH, path);
    }

    static <R, T> ParameterBuilder<R, T> delete(String path) {
        return method(DELETE, path);
    }

    static <R, T> ParameterBuilder<R, T> trace(String path) {
        return method(TRACE, path);
    }

    static <R, T> ParameterBuilder<R, T> connect(String path) {
        return method(CONNECT, path);
    }

    static <R, T> ParameterBuilder<R, T> method(HttpMethod method, String path) {
        return handler -> contentType -> route(method, path, handler, contentType);
    }

    interface ParameterBuilder<R, T> {
        ContentTypeBuilder<R> to(Handler<R> handler);

        default RawHandlerBuilder<R> withoutParameters() {
            return this::to;
        }

        default JsonBodyParameterBuilder<R, T> withBody(TypeToken<T> type) {
            return fn -> to(ctx -> ctx.fromJson(type).toPromise().flatMap(fn));
        }

        default JsonBodyParameterBuilder<R, T> withBody(Class<T> type) {
            return withBody(TypeToken.of(type));
        }

        default ContentTypeBuilder<R> toImmediate(Fn1<Result<R>, RequestContext> handler) {
            return withoutParameters().toImmediate(handler);
        }

        default Route<R> toText(Handler<R> handler) {
            return to(handler).asText();
        }

        default Route<R> toText(Supplier<R> handler) {
            return to(_ -> Promise.successful(handler.get())).asText();
        }

        default Route<R> toJson(Handler<R> handler) {
            return to(handler).asJson();
        }

        default Route<R> toJson(Supplier<R> handler) {
            return to(_ -> Promise.successful(handler.get())).asJson();
        }
    }

    interface RawHandlerBuilder<T> {
        ContentTypeBuilder<T> to(Handler<T> handler);

        default ContentTypeBuilder<T> toImmediate(Fn1<Result<T>, RequestContext> handler) {
            return to(ctx -> Promise.resolved(handler.apply(ctx)));
        }
        // No shortcuts are provided. If we're here, we chose to be explicit and therefore need to specify content type
    }

    interface JsonBodyParameterBuilder<R, T> {
        ContentTypeBuilder<R> to(Fn1<Promise<R>, T> handler);

        default Route<R> toJson(Fn1<Promise<R>, T> handler) {
            return to(handler).asJson();
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
