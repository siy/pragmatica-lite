package org.pragmatica.http.routing;

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.http.routing.HttpMethod.*;

@SuppressWarnings("unused")
public interface Route<T> extends RouteSource {
    HttpMethod method();
    String path();
    Handler<T> handler();
    ContentType contentType();

    @Override
    default Stream<Route< ?>> routes() {
        return Stream.of(this);
    }

    @Override
    RouteSource withPrefix(String prefix);

    static <T> Route<T> route(HttpMethod method, String path, Handler<T> handler, ContentType contentType) {
        record route<T>(HttpMethod method, String path, Handler<T> handler, ContentType contentType) implements Route<T> {
            @Override
            public RouteSource withPrefix(String prefix) {
                return new route<>(method, PathUtils.normalize(prefix + path), handler, contentType);
            }

            @Override
            public String toString() {
                return "Route: " + method + " " + path + ", " + contentType;
            }
        }
        return new route<>(method, PathUtils.normalize(path), handler, contentType);
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

        default <T1> PathParameterBuilder1<R, T, T1> withPath(PathParameter<T1> pathParameter) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2> PathParameterBuilder2<R, T, T1, T2> withPath(PathParameter<T1> pathParameter1,
                                                                      PathParameter<T2> pathParameter2) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1, pathParameter2)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2, T3> PathParameterBuilder3<R, T, T1, T2, T3> withPath(PathParameter<T1> pathParameter1,
                                                                              PathParameter<T2> pathParameter2,
                                                                              PathParameter<T3> pathParameter3) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1, pathParameter2, pathParameter3)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2, T3, T4> PathParameterBuilder4<R, T, T1, T2, T3, T4> withPath(PathParameter<T1> pathParameter1,
                                                                                      PathParameter<T2> pathParameter2,
                                                                                      PathParameter<T3> pathParameter3,
                                                                                      PathParameter<T4> pathParameter4) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1, pathParameter2, pathParameter3, pathParameter4)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2, T3, T4, T5> PathParameterBuilder5<R, T, T1, T2, T3, T4, T5> withPath(PathParameter<T1> pathParameter1,
                                                                                              PathParameter<T2> pathParameter2,
                                                                                              PathParameter<T3> pathParameter3,
                                                                                              PathParameter<T4> pathParameter4,
                                                                                              PathParameter<T5> pathParameter5) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1,
                                                 pathParameter2,
                                                 pathParameter3,
                                                 pathParameter4,
                                                 pathParameter5)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2, T3, T4, T5, T6> PathParameterBuilder6<R, T, T1, T2, T3, T4, T5, T6> withPath(PathParameter<T1> pathParameter1,
                                                                                                      PathParameter<T2> pathParameter2,
                                                                                                      PathParameter<T3> pathParameter3,
                                                                                                      PathParameter<T4> pathParameter4,
                                                                                                      PathParameter<T5> pathParameter5,
                                                                                                      PathParameter<T6> pathParameter6) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1,
                                                 pathParameter2,
                                                 pathParameter3,
                                                 pathParameter4,
                                                 pathParameter5,
                                                 pathParameter6)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2, T3, T4, T5, T6, T7> PathParameterBuilder7<R, T, T1, T2, T3, T4, T5, T6, T7> withPath(PathParameter<T1> pathParameter1,
                                                                                                              PathParameter<T2> pathParameter2,
                                                                                                              PathParameter<T3> pathParameter3,
                                                                                                              PathParameter<T4> pathParameter4,
                                                                                                              PathParameter<T5> pathParameter5,
                                                                                                              PathParameter<T6> pathParameter6,
                                                                                                              PathParameter<T7> pathParameter7) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1,
                                                 pathParameter2,
                                                 pathParameter3,
                                                 pathParameter4,
                                                 pathParameter5,
                                                 pathParameter6,
                                                 pathParameter7)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2, T3, T4, T5, T6, T7, T8> PathParameterBuilder8<R, T, T1, T2, T3, T4, T5, T6, T7, T8> withPath(PathParameter<T1> pathParameter1,
                                                                                                                      PathParameter<T2> pathParameter2,
                                                                                                                      PathParameter<T3> pathParameter3,
                                                                                                                      PathParameter<T4> pathParameter4,
                                                                                                                      PathParameter<T5> pathParameter5,
                                                                                                                      PathParameter<T6> pathParameter6,
                                                                                                                      PathParameter<T7> pathParameter7,
                                                                                                                      PathParameter<T8> pathParameter8) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1,
                                                 pathParameter2,
                                                 pathParameter3,
                                                 pathParameter4,
                                                 pathParameter5,
                                                 pathParameter6,
                                                 pathParameter7,
                                                 pathParameter8)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        default <T1, T2, T3, T4, T5, T6, T7, T8, T9>
        PathParameterBuilder9<R, T, T1, T2, T3, T4, T5, T6, T7, T8, T9> withPath(PathParameter<T1> pathParameter1,
                                                                                 PathParameter<T2> pathParameter2,
                                                                                 PathParameter<T3> pathParameter3,
                                                                                 PathParameter<T4> pathParameter4,
                                                                                 PathParameter<T5> pathParameter5,
                                                                                 PathParameter<T6> pathParameter6,
                                                                                 PathParameter<T7> pathParameter7,
                                                                                 PathParameter<T8> pathParameter8,
                                                                                 PathParameter<T9> pathParameter9) {
            return fn -> to(ctx -> ctx.matchPath(pathParameter1,
                                                 pathParameter2,
                                                 pathParameter3,
                                                 pathParameter4,
                                                 pathParameter5,
                                                 pathParameter6,
                                                 pathParameter7,
                                                 pathParameter8,
                                                 pathParameter9)
                                      .map(fn)
                                      .async()
                                      .flatMap(p -> p));
        }

        interface PathParameterBuilder1<R, T, T1> {
            ContentTypeBuilder<R> to(Fn1<Promise<R>, T1> fn);

            default ContentTypeBuilder<R> toResult(Fn1<Result<R>, T1> handler) {
                return to(p1 -> Promise.resolved(handler.apply(p1)));
            }

            default ContentTypeBuilder<R> toValue(Fn1<R, T1> handler) {
                return to(p1 -> Promise.success(handler.apply(p1)));
            }
        }

        interface PathParameterBuilder2<R, T, T1, T2> {
            ContentTypeBuilder<R> to(Fn2<Promise<R>, T1, T2> fn);

            default ContentTypeBuilder<R> toResult(Fn2<Result<R>, T1, T2> handler) {
                return to((p1, p2) -> Promise.resolved(handler.apply(p1, p2)));
            }

            default ContentTypeBuilder<R> toValue(Fn2<R, T1, T2> handler) {
                return to((p1, p2) -> Promise.success(handler.apply(p1, p2)));
            }
        }

        interface PathParameterBuilder3<R, T, T1, T2, T3> {
            ContentTypeBuilder<R> to(Fn3<Promise<R>, T1, T2, T3> fn);

            default ContentTypeBuilder<R> toResult(Fn3<Result<R>, T1, T2, T3> handler) {
                return to((p1, p2, p3) -> Promise.resolved(handler.apply(p1, p2, p3)));
            }

            default ContentTypeBuilder<R> toValue(Fn3<R, T1, T2, T3> handler) {
                return to((p1, p2, p3) -> Promise.success(handler.apply(p1, p2, p3)));
            }
        }

        interface PathParameterBuilder4<R, T, T1, T2, T3, T4> {
            ContentTypeBuilder<R> to(Fn4<Promise<R>, T1, T2, T3, T4> fn);

            default ContentTypeBuilder<R> toResult(Fn4<Result<R>, T1, T2, T3, T4> handler) {
                return to((p1, p2, p3, p4) -> Promise.resolved(handler.apply(p1, p2, p3, p4)));
            }

            default ContentTypeBuilder<R> toValue(Fn4<R, T1, T2, T3, T4> handler) {
                return to((p1, p2, p3, p4) -> Promise.success(handler.apply(p1, p2, p3, p4)));
            }
        }

        interface PathParameterBuilder5<R, T, T1, T2, T3, T4, T5> {
            ContentTypeBuilder<R> to(Fn5<Promise<R>, T1, T2, T3, T4, T5> fn);

            default ContentTypeBuilder<R> toResult(Fn5<Result<R>, T1, T2, T3, T4, T5> handler) {
                return to((p1, p2, p3, p4, p5) -> Promise.resolved(handler.apply(p1, p2, p3, p4, p5)));
            }

            default ContentTypeBuilder<R> toValue(Fn5<R, T1, T2, T3, T4, T5> handler) {
                return to((p1, p2, p3, p4, p5) -> Promise.success(handler.apply(p1, p2, p3, p4, p5)));
            }
        }

        interface PathParameterBuilder6<R, T, T1, T2, T3, T4, T5, T6> {
            ContentTypeBuilder<R> to(Fn6<Promise<R>, T1, T2, T3, T4, T5, T6> fn);

            default ContentTypeBuilder<R> toResult(Fn6<Result<R>, T1, T2, T3, T4, T5, T6> handler) {
                return to((p1, p2, p3, p4, p5, p6) -> Promise.resolved(handler.apply(p1, p2, p3, p4, p5, p6)));
            }

            default ContentTypeBuilder<R> toValue(Fn6<R, T1, T2, T3, T4, T5, T6> handler) {
                return to((p1, p2, p3, p4, p5, p6) -> Promise.success(handler.apply(p1, p2, p3, p4, p5, p6)));
            }
        }

        interface PathParameterBuilder7<R, T, T1, T2, T3, T4, T5, T6, T7> {
            ContentTypeBuilder<R> to(Fn7<Promise<R>, T1, T2, T3, T4, T5, T6, T7> fn);

            default ContentTypeBuilder<R> toResult(Fn7<Result<R>, T1, T2, T3, T4, T5, T6, T7> handler) {
                return to((p1, p2, p3, p4, p5, p6, p7) -> Promise.resolved(handler.apply(p1, p2, p3, p4, p5, p6, p7)));
            }

            default ContentTypeBuilder<R> toValue(Fn7<R, T1, T2, T3, T4, T5, T6, T7> handler) {
                return to((p1, p2, p3, p4, p5, p6, p7) -> Promise.success(handler.apply(p1, p2, p3, p4, p5, p6, p7)));
            }
        }

        interface PathParameterBuilder8<R, T, T1, T2, T3, T4, T5, T6, T7, T8> {
            ContentTypeBuilder<R> to(Fn8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8> fn);

            default ContentTypeBuilder<R> toResult(Fn8<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8> handler) {
                return to((p1, p2, p3, p4, p5, p6, p7, p8) -> Promise.resolved(handler.apply(p1,
                                                                                             p2,
                                                                                             p3,
                                                                                             p4,
                                                                                             p5,
                                                                                             p6,
                                                                                             p7,
                                                                                             p8)));
            }

            default ContentTypeBuilder<R> toValue(Fn8<R, T1, T2, T3, T4, T5, T6, T7, T8> handler) {
                return to((p1, p2, p3, p4, p5, p6, p7, p8) -> Promise.success(handler.apply(p1,
                                                                                            p2,
                                                                                            p3,
                                                                                            p4,
                                                                                            p5,
                                                                                            p6,
                                                                                            p7,
                                                                                            p8)));
            }
        }

        interface PathParameterBuilder9<R, T, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
            ContentTypeBuilder<R> to(Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> fn);

            default ContentTypeBuilder<R> toResult(Fn9<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> handler) {
                return to((p1, p2, p3, p4, p5, p6, p7, p8, p9) -> Promise.resolved(handler.apply(p1,
                                                                                                 p2,
                                                                                                 p3,
                                                                                                 p4,
                                                                                                 p5,
                                                                                                 p6,
                                                                                                 p7,
                                                                                                 p8,
                                                                                                 p9)));
            }

            default ContentTypeBuilder<R> toValue(Fn9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> handler) {
                return to((p1, p2, p3, p4, p5, p6, p7, p8, p9) -> Promise.success(handler.apply(p1,
                                                                                                p2,
                                                                                                p3,
                                                                                                p4,
                                                                                                p5,
                                                                                                p6,
                                                                                                p7,
                                                                                                p8,
                                                                                                p9)));
            }
        }

        default JsonBodyParameterBuilder<R, T> withBody(TypeToken<T> type) {
            return fn -> to(ctx -> ctx.fromJson(type)
                                      .async()
                                      .flatMap(fn));
        }

        default JsonBodyParameterBuilder<R, T> withBody(Class<T> type) {
            return withBody(TypeToken.of(type));
        }

        default ContentTypeBuilder<R> toImmediate(Fn1<Result<R>, RequestContext> handler) {
            return withoutParameters()
                                    .toImmediate(handler);
        }

        default Route<R> toText(Handler<R> handler) {
            return to(handler)
                     .asText();
        }

        default Route<R> toText(Supplier<R> handler) {
            return to(_ -> Promise.success(handler.get()))
                     .asText();
        }

        default Route<R> toJson(Handler<R> handler) {
            return to(handler)
                     .asJson();
        }

        default Route<R> toJson(Supplier<R> handler) {
            return to(_ -> Promise.success(handler.get()))
                     .asJson();
        }
    }

    interface RawHandlerBuilder<T> {
        ContentTypeBuilder<T> to(Handler<T> handler);

        default ContentTypeBuilder<T> toImmediate(Fn1<Result<T>, RequestContext> handler) {
            return to(ctx -> Promise.resolved(handler.apply(ctx)));
        }
    }

    interface JsonBodyParameterBuilder<R, T> {
        ContentTypeBuilder<R> to(Fn1<Promise<R>, T> handler);

        default ContentTypeBuilder<R> toResult(Fn1<Result<R>, T> handler) {
            return to(body -> handler.apply(body)
                                     .async());
        }

        default Route<R> toJson(Fn1<Promise<R>, T> handler) {
            return to(handler)
                     .asJson();
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
