package org.pragmatica.http.routing;

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.http.routing.HttpMethod.*;

/**
 * Type-safe HTTP route definition with support for path parameters, query parameters, and request body.
 * <p>
 * Routes are built using a fluent builder API that ensures compile-time type safety for all parameter
 * combinations. The total number of parameters (path + query + body) is limited to 5.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Path parameters only
 * Route.get("/users/{id}")
 *      .withPath(aLong())
 *      .to(userId -> fetchUser(userId))
 *      .asJson();
 *
 * // Path + query parameters
 * Route.get("/users/{id}/orders")
 *      .withPath(aLong())
 *      .withQuery(aInteger("page"), aInteger("size"))
 *      .to((userId, page, size) -> fetchOrders(userId, page, size))
 *      .asJson();
 *
 * // Path + body
 * Route.post("/users/{id}")
 *      .withPath(aLong())
 *      .withBody(UpdateUserRequest.class)
 *      .to((userId, body) -> updateUser(userId, body))
 *      .asJson();
 *
 * // Query + body
 * Route.post("/search")
 *      .withQuery(aBoolean("includeDeleted"))
 *      .withBody(SearchRequest.class)
 *      .to((includeDeleted, body) -> search(includeDeleted, body))
 *      .asJson();
 * }</pre>
 *
 * @param <T> the response type
 */
@SuppressWarnings("unused")
public interface Route<T> extends RouteSource {
    HttpMethod method();

    String path();

    Handler<T> handler();

    ContentType contentType();

    /**
     * Returns the spacer patterns for this route.
     * Spacers are literal path segments that must appear in the URL.
     * Used for distinguishing routes with the same base path but different trailing segments.
     */
    default List<String> spacers() {
        return List.of();
    }

    @Override
    default Stream<Route<?>> routes() {
        return Stream.of(this);
    }

    @Override
    RouteSource withPrefix(String prefix);

    static <T> Route<T> route(HttpMethod method, String path, Handler<T> handler, ContentType contentType) {
        return route(method, path, handler, contentType, List.of());
    }

    static <T> Route<T> route(HttpMethod method,
                              String path,
                              Handler<T> handler,
                              ContentType contentType,
                              List<String> spacers) {
        record route<T>(HttpMethod method,
                        String path,
                        Handler<T> handler,
                        ContentType contentType,
                        List<String> spacers) implements Route<T> {
            @Override
            public RouteSource withPrefix(String prefix) {
                return new route<>(method, PathUtils.normalize(prefix + path), handler, contentType, spacers);
            }

            @Override
            public String toString() {
                var spacerStr = spacers.isEmpty()
                                ? ""
                                : " [spacers: " + String.join(", ", spacers) + "]";
                return "Route: " + method + " " + path + spacerStr + ", " + contentType;
            }
        }
        return new route<>(method, PathUtils.normalize(path), handler, contentType, spacers);
    }

    static Subroutes in(String path) {
        return () -> path;
    }

    /**
     * Creates a route source for serving static files from the classpath.
     *
     * @param urlPrefix       the URL prefix to match (e.g., "/static")
     * @param classpathPrefix the classpath prefix where files are located (e.g., "/web")
     * @return a route source that serves static files
     */
    static RouteSource staticFiles(String urlPrefix, String classpathPrefix) {
        return StaticFileRouteSource.staticFiles(urlPrefix, classpathPrefix);
    }

    interface Subroutes {
        String path();

        default RouteSource serve(RouteSource... subroutes) {
            return () -> Stream.of(subroutes)
                               .map(route -> route.withPrefix(path()))
                               .flatMap(RouteSource::routes);
        }
    }

    // HTTP method factories
    static <R> ParameterBuilder<R> options(String path) {
        return method(OPTIONS, path);
    }

    static <R> ParameterBuilder<R> get(String path) {
        return method(GET, path);
    }

    static <R> ParameterBuilder<R> head(String path) {
        return method(HEAD, path);
    }

    static <R> ParameterBuilder<R> post(String path) {
        return method(POST, path);
    }

    static <R> ParameterBuilder<R> put(String path) {
        return method(PUT, path);
    }

    static <R> ParameterBuilder<R> patch(String path) {
        return method(PATCH, path);
    }

    static <R> ParameterBuilder<R> delete(String path) {
        return method(DELETE, path);
    }

    static <R> ParameterBuilder<R> trace(String path) {
        return method(TRACE, path);
    }

    static <R> ParameterBuilder<R> connect(String path) {
        return method(CONNECT, path);
    }

    static <R> ParameterBuilder<R> method(HttpMethod method, String path) {
        return new ParameterBuilderImpl<>(method, path);
    }

    // ===================================================================================
    // Entry Point Builder
    // ===================================================================================
    /**
     * Entry point builder - provides access to all parameter configuration options.
     */
    interface ParameterBuilder<R> {
        ContentTypeBuilder<R> to(Handler<R> handler);

        default RawHandlerBuilder<R> withoutParameters() {
            return this::to;
        }

        // Path parameters (1-5)
        <P1> PathBuilder1<R, P1> withPath(PathParameter<P1> p1);

        <P1, P2> PathBuilder2<R, P1, P2> withPath(PathParameter<P1> p1, PathParameter<P2> p2);

        <P1, P2, P3> PathBuilder3<R, P1, P2, P3> withPath(PathParameter<P1> p1,
                                                          PathParameter<P2> p2,
                                                          PathParameter<P3> p3);

        <P1, P2, P3, P4> PathBuilder4<R, P1, P2, P3, P4> withPath(PathParameter<P1> p1,
                                                                  PathParameter<P2> p2,
                                                                  PathParameter<P3> p3,
                                                                  PathParameter<P4> p4);

        <P1, P2, P3, P4, P5> PathBuilder5<R, P1, P2, P3, P4, P5> withPath(PathParameter<P1> p1,
                                                                          PathParameter<P2> p2,
                                                                          PathParameter<P3> p3,
                                                                          PathParameter<P4> p4,
                                                                          PathParameter<P5> p5);

        // Body only
        <B> BodyBuilder<R, B> withBody(TypeToken<B> type);

        default <B> BodyBuilder<R, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }

        // Query parameters only (1-5)
        <Q1> QueryBuilder1<R, Q1> withQuery(QueryParameter<Q1> q1);

        <Q1, Q2> QueryBuilder2<R, Q1, Q2> withQuery(QueryParameter<Q1> q1, QueryParameter<Q2> q2);

        <Q1, Q2, Q3> QueryBuilder3<R, Q1, Q2, Q3> withQuery(QueryParameter<Q1> q1,
                                                            QueryParameter<Q2> q2,
                                                            QueryParameter<Q3> q3);

        <Q1, Q2, Q3, Q4> QueryBuilder4<R, Q1, Q2, Q3, Q4> withQuery(QueryParameter<Q1> q1,
                                                                    QueryParameter<Q2> q2,
                                                                    QueryParameter<Q3> q3,
                                                                    QueryParameter<Q4> q4);

        <Q1, Q2, Q3, Q4, Q5> QueryBuilder5<R, Q1, Q2, Q3, Q4, Q5> withQuery(QueryParameter<Q1> q1,
                                                                            QueryParameter<Q2> q2,
                                                                            QueryParameter<Q3> q3,
                                                                            QueryParameter<Q4> q4,
                                                                            QueryParameter<Q5> q5);

        // Convenience methods
        default ContentTypeBuilder<R> toImmediate(Fn1<Result<R>, RequestContext> handler) {
            return withoutParameters().toImmediate(handler);
        }

        default Route<R> toText(Handler<R> handler) {
            return to(handler).asText();
        }

        default Route<R> toText(Supplier<R> handler) {
            return to(_ -> Promise.success(handler.get())).asText();
        }

        default Route<R> toJson(Handler<R> handler) {
            return to(handler).asJson();
        }

        default Route<R> toJson(Supplier<R> handler) {
            return to(_ -> Promise.success(handler.get())).asJson();
        }
    }

    // ===================================================================================
    // Path Builders (1-5 path params, can chain to body or query)
    // ===================================================================================
    interface PathBuilder1<R, P1> {
        ContentTypeBuilder<R> to(Fn1<Promise<R>, P1> fn);

        default ContentTypeBuilder<R> toResult(Fn1<Result<R>, P1> fn) {
            return to(p1 -> Promise.resolved(fn.apply(p1)));
        }

        default ContentTypeBuilder<R> toValue(Fn1<R, P1> fn) {
            return to(p1 -> Promise.success(fn.apply(p1)));
        }

        <B> PathBodyBuilder1<R, P1, B> withBody(TypeToken<B> type);

        default <B> PathBodyBuilder1<R, P1, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }

        <Q1> PathQueryBuilder1_1<R, P1, Q1> withQuery(QueryParameter<Q1> q1);

        <Q1, Q2> PathQueryBuilder1_2<R, P1, Q1, Q2> withQuery(QueryParameter<Q1> q1, QueryParameter<Q2> q2);

        <Q1, Q2, Q3> PathQueryBuilder1_3<R, P1, Q1, Q2, Q3> withQuery(QueryParameter<Q1> q1,
                                                                      QueryParameter<Q2> q2,
                                                                      QueryParameter<Q3> q3);

        <Q1, Q2, Q3, Q4> PathQueryBuilder1_4<R, P1, Q1, Q2, Q3, Q4> withQuery(QueryParameter<Q1> q1,
                                                                              QueryParameter<Q2> q2,
                                                                              QueryParameter<Q3> q3,
                                                                              QueryParameter<Q4> q4);
    }

    interface PathBuilder2<R, P1, P2> {
        ContentTypeBuilder<R> to(Fn2<Promise<R>, P1, P2> fn);

        default ContentTypeBuilder<R> toResult(Fn2<Result<R>, P1, P2> fn) {
            return to((p1, p2) -> Promise.resolved(fn.apply(p1, p2)));
        }

        default ContentTypeBuilder<R> toValue(Fn2<R, P1, P2> fn) {
            return to((p1, p2) -> Promise.success(fn.apply(p1, p2)));
        }

        <B> PathBodyBuilder2<R, P1, P2, B> withBody(TypeToken<B> type);

        default <B> PathBodyBuilder2<R, P1, P2, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }

        <Q1> PathQueryBuilder2_1<R, P1, P2, Q1> withQuery(QueryParameter<Q1> q1);

        <Q1, Q2> PathQueryBuilder2_2<R, P1, P2, Q1, Q2> withQuery(QueryParameter<Q1> q1, QueryParameter<Q2> q2);

        <Q1, Q2, Q3> PathQueryBuilder2_3<R, P1, P2, Q1, Q2, Q3> withQuery(QueryParameter<Q1> q1,
                                                                          QueryParameter<Q2> q2,
                                                                          QueryParameter<Q3> q3);
    }

    interface PathBuilder3<R, P1, P2, P3> {
        ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, P2, P3> fn);

        default ContentTypeBuilder<R> toResult(Fn3<Result<R>, P1, P2, P3> fn) {
            return to((p1, p2, p3) -> Promise.resolved(fn.apply(p1, p2, p3)));
        }

        default ContentTypeBuilder<R> toValue(Fn3<R, P1, P2, P3> fn) {
            return to((p1, p2, p3) -> Promise.success(fn.apply(p1, p2, p3)));
        }

        <B> PathBodyBuilder3<R, P1, P2, P3, B> withBody(TypeToken<B> type);

        default <B> PathBodyBuilder3<R, P1, P2, P3, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }

        <Q1> PathQueryBuilder3_1<R, P1, P2, P3, Q1> withQuery(QueryParameter<Q1> q1);

        <Q1, Q2> PathQueryBuilder3_2<R, P1, P2, P3, Q1, Q2> withQuery(QueryParameter<Q1> q1, QueryParameter<Q2> q2);
    }

    interface PathBuilder4<R, P1, P2, P3, P4> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, P3, P4> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, P1, P2, P3, P4> fn) {
            return to((p1, p2, p3, p4) -> Promise.resolved(fn.apply(p1, p2, p3, p4)));
        }

        default ContentTypeBuilder<R> toValue(Fn4<R, P1, P2, P3, P4> fn) {
            return to((p1, p2, p3, p4) -> Promise.success(fn.apply(p1, p2, p3, p4)));
        }

        <B> PathBodyBuilder4<R, P1, P2, P3, P4, B> withBody(TypeToken<B> type);

        default <B> PathBodyBuilder4<R, P1, P2, P3, P4, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }

        <Q1> PathQueryBuilder4_1<R, P1, P2, P3, P4, Q1> withQuery(QueryParameter<Q1> q1);
    }

    interface PathBuilder5<R, P1, P2, P3, P4, P5> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, P3, P4, P5> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, P1, P2, P3, P4, P5> fn) {
            return to((p1, p2, p3, p4, p5) -> Promise.resolved(fn.apply(p1, p2, p3, p4, p5)));
        }

        default ContentTypeBuilder<R> toValue(Fn5<R, P1, P2, P3, P4, P5> fn) {
            return to((p1, p2, p3, p4, p5) -> Promise.success(fn.apply(p1, p2, p3, p4, p5)));
        }
    }

    // ===================================================================================
    // Body Builder (body only, terminal)
    // ===================================================================================
    interface BodyBuilder<R, B> {
        ContentTypeBuilder<R> to(Fn1<Promise<R>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn1<Result<R>, B> fn) {
            return to(body -> Promise.resolved(fn.apply(body)));
        }

        default Route<R> toJson(Fn1<Promise<R>, B> fn) {
            return to(fn).asJson();
        }
    }

    // ===================================================================================
    // Query Builders (1-5 query params, can chain to body)
    // ===================================================================================
    interface QueryBuilder1<R, Q1> {
        ContentTypeBuilder<R> to(Fn1<Promise<R>, Option<Q1>> fn);

        default ContentTypeBuilder<R> toResult(Fn1<Result<R>, Option<Q1>> fn) {
            return to(q1 -> Promise.resolved(fn.apply(q1)));
        }

        default ContentTypeBuilder<R> toValue(Fn1<R, Option<Q1>> fn) {
            return to(q1 -> Promise.success(fn.apply(q1)));
        }

        <B> QueryBodyBuilder1<R, Q1, B> withBody(TypeToken<B> type);

        default <B> QueryBodyBuilder1<R, Q1, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }
    }

    interface QueryBuilder2<R, Q1, Q2> {
        ContentTypeBuilder<R> to(Fn2<Promise<R>, Option<Q1>, Option<Q2>> fn);

        default ContentTypeBuilder<R> toResult(Fn2<Result<R>, Option<Q1>, Option<Q2>> fn) {
            return to((q1, q2) -> Promise.resolved(fn.apply(q1, q2)));
        }

        default ContentTypeBuilder<R> toValue(Fn2<R, Option<Q1>, Option<Q2>> fn) {
            return to((q1, q2) -> Promise.success(fn.apply(q1, q2)));
        }

        <B> QueryBodyBuilder2<R, Q1, Q2, B> withBody(TypeToken<B> type);

        default <B> QueryBodyBuilder2<R, Q1, Q2, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }
    }

    interface QueryBuilder3<R, Q1, Q2, Q3> {
        ContentTypeBuilder<R> to(Fn3<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>> fn);

        default ContentTypeBuilder<R> toResult(Fn3<Result<R>, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return to((q1, q2, q3) -> Promise.resolved(fn.apply(q1, q2, q3)));
        }

        default ContentTypeBuilder<R> toValue(Fn3<R, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return to((q1, q2, q3) -> Promise.success(fn.apply(q1, q2, q3)));
        }

        <B> QueryBodyBuilder3<R, Q1, Q2, Q3, B> withBody(TypeToken<B> type);

        default <B> QueryBodyBuilder3<R, Q1, Q2, Q3, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }
    }

    interface QueryBuilder4<R, Q1, Q2, Q3, Q4> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn) {
            return to((q1, q2, q3, q4) -> Promise.resolved(fn.apply(q1, q2, q3, q4)));
        }

        default ContentTypeBuilder<R> toValue(Fn4<R, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn) {
            return to((q1, q2, q3, q4) -> Promise.success(fn.apply(q1, q2, q3, q4)));
        }

        <B> QueryBodyBuilder4<R, Q1, Q2, Q3, Q4, B> withBody(TypeToken<B> type);

        default <B> QueryBodyBuilder4<R, Q1, Q2, Q3, Q4, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }
    }

    interface QueryBuilder5<R, Q1, Q2, Q3, Q4, Q5> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>, Option<Q5>> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>, Option<Q5>> fn) {
            return to((q1, q2, q3, q4, q5) -> Promise.resolved(fn.apply(q1, q2, q3, q4, q5)));
        }

        default ContentTypeBuilder<R> toValue(Fn5<R, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>, Option<Q5>> fn) {
            return to((q1, q2, q3, q4, q5) -> Promise.success(fn.apply(q1, q2, q3, q4, q5)));
        }
    }

    // ===================================================================================
    // Path + Body Builders (terminal)
    // ===================================================================================
    interface PathBodyBuilder1<R, P1, B> {
        ContentTypeBuilder<R> to(Fn2<Promise<R>, P1, B> fn);

        default ContentTypeBuilder<R> toResult(Fn2<Result<R>, P1, B> fn) {
            return to((p1, body) -> Promise.resolved(fn.apply(p1, body)));
        }

        default Route<R> toJson(Fn2<Promise<R>, P1, B> fn) {
            return to(fn).asJson();
        }
    }

    interface PathBodyBuilder2<R, P1, P2, B> {
        ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, P2, B> fn);

        default ContentTypeBuilder<R> toResult(Fn3<Result<R>, P1, P2, B> fn) {
            return to((p1, p2, body) -> Promise.resolved(fn.apply(p1, p2, body)));
        }

        default Route<R> toJson(Fn3<Promise<R>, P1, P2, B> fn) {
            return to(fn).asJson();
        }
    }

    interface PathBodyBuilder3<R, P1, P2, P3, B> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, P3, B> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, P1, P2, P3, B> fn) {
            return to((p1, p2, p3, body) -> Promise.resolved(fn.apply(p1, p2, p3, body)));
        }

        default Route<R> toJson(Fn4<Promise<R>, P1, P2, P3, B> fn) {
            return to(fn).asJson();
        }
    }

    interface PathBodyBuilder4<R, P1, P2, P3, P4, B> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, P3, P4, B> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, P1, P2, P3, P4, B> fn) {
            return to((p1, p2, p3, p4, body) -> Promise.resolved(fn.apply(p1, p2, p3, p4, body)));
        }

        default Route<R> toJson(Fn5<Promise<R>, P1, P2, P3, P4, B> fn) {
            return to(fn).asJson();
        }
    }

    // ===================================================================================
    // Query + Body Builders (terminal)
    // ===================================================================================
    interface QueryBodyBuilder1<R, Q1, B> {
        ContentTypeBuilder<R> to(Fn2<Promise<R>, Option<Q1>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn2<Result<R>, Option<Q1>, B> fn) {
            return to((q1, body) -> Promise.resolved(fn.apply(q1, body)));
        }

        default Route<R> toJson(Fn2<Promise<R>, Option<Q1>, B> fn) {
            return to(fn).asJson();
        }
    }

    interface QueryBodyBuilder2<R, Q1, Q2, B> {
        ContentTypeBuilder<R> to(Fn3<Promise<R>, Option<Q1>, Option<Q2>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn3<Result<R>, Option<Q1>, Option<Q2>, B> fn) {
            return to((q1, q2, body) -> Promise.resolved(fn.apply(q1, q2, body)));
        }

        default Route<R> toJson(Fn3<Promise<R>, Option<Q1>, Option<Q2>, B> fn) {
            return to(fn).asJson();
        }
    }

    interface QueryBodyBuilder3<R, Q1, Q2, Q3, B> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, Option<Q1>, Option<Q2>, Option<Q3>, B> fn) {
            return to((q1, q2, q3, body) -> Promise.resolved(fn.apply(q1, q2, q3, body)));
        }

        default Route<R> toJson(Fn4<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, B> fn) {
            return to(fn).asJson();
        }
    }

    interface QueryBodyBuilder4<R, Q1, Q2, Q3, Q4, B> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>, B> fn) {
            return to((q1, q2, q3, q4, body) -> Promise.resolved(fn.apply(q1, q2, q3, q4, body)));
        }

        default Route<R> toJson(Fn5<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>, B> fn) {
            return to(fn).asJson();
        }
    }

    // ===================================================================================
    // Path + Query Builders (can chain to body)
    // ===================================================================================
    // 1 path + 1-4 query
    interface PathQueryBuilder1_1<R, P1, Q1> {
        ContentTypeBuilder<R> to(Fn2<Promise<R>, P1, Option<Q1>> fn);

        default ContentTypeBuilder<R> toResult(Fn2<Result<R>, P1, Option<Q1>> fn) {
            return to((p1, q1) -> Promise.resolved(fn.apply(p1, q1)));
        }

        default ContentTypeBuilder<R> toValue(Fn2<R, P1, Option<Q1>> fn) {
            return to((p1, q1) -> Promise.success(fn.apply(p1, q1)));
        }

        <B> PathQueryBodyBuilder1_1<R, P1, Q1, B> withBody(TypeToken<B> type);

        default <B> PathQueryBodyBuilder1_1<R, P1, Q1, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }
    }

    interface PathQueryBuilder1_2<R, P1, Q1, Q2> {
        ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, Option<Q1>, Option<Q2>> fn);

        default ContentTypeBuilder<R> toResult(Fn3<Result<R>, P1, Option<Q1>, Option<Q2>> fn) {
            return to((p1, q1, q2) -> Promise.resolved(fn.apply(p1, q1, q2)));
        }

        default ContentTypeBuilder<R> toValue(Fn3<R, P1, Option<Q1>, Option<Q2>> fn) {
            return to((p1, q1, q2) -> Promise.success(fn.apply(p1, q1, q2)));
        }

        <B> PathQueryBodyBuilder1_2<R, P1, Q1, Q2, B> withBody(TypeToken<B> type);

        default <B> PathQueryBodyBuilder1_2<R, P1, Q1, Q2, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }
    }

    interface PathQueryBuilder1_3<R, P1, Q1, Q2, Q3> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, Option<Q1>, Option<Q2>, Option<Q3>> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, P1, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return to((p1, q1, q2, q3) -> Promise.resolved(fn.apply(p1, q1, q2, q3)));
        }

        default ContentTypeBuilder<R> toValue(Fn4<R, P1, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return to((p1, q1, q2, q3) -> Promise.success(fn.apply(p1, q1, q2, q3)));
        }
    }

    interface PathQueryBuilder1_4<R, P1, Q1, Q2, Q3, Q4> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, P1, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn) {
            return to((p1, q1, q2, q3, q4) -> Promise.resolved(fn.apply(p1, q1, q2, q3, q4)));
        }

        default ContentTypeBuilder<R> toValue(Fn5<R, P1, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn) {
            return to((p1, q1, q2, q3, q4) -> Promise.success(fn.apply(p1, q1, q2, q3, q4)));
        }
    }

    // 2 path + 1-3 query
    interface PathQueryBuilder2_1<R, P1, P2, Q1> {
        ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, P2, Option<Q1>> fn);

        default ContentTypeBuilder<R> toResult(Fn3<Result<R>, P1, P2, Option<Q1>> fn) {
            return to((p1, p2, q1) -> Promise.resolved(fn.apply(p1, p2, q1)));
        }

        default ContentTypeBuilder<R> toValue(Fn3<R, P1, P2, Option<Q1>> fn) {
            return to((p1, p2, q1) -> Promise.success(fn.apply(p1, p2, q1)));
        }

        <B> PathQueryBodyBuilder2_1<R, P1, P2, Q1, B> withBody(TypeToken<B> type);

        default <B> PathQueryBodyBuilder2_1<R, P1, P2, Q1, B> withBody(Class<B> type) {
            return withBody(TypeToken.of(type));
        }
    }

    interface PathQueryBuilder2_2<R, P1, P2, Q1, Q2> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, Option<Q1>, Option<Q2>> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, P1, P2, Option<Q1>, Option<Q2>> fn) {
            return to((p1, p2, q1, q2) -> Promise.resolved(fn.apply(p1, p2, q1, q2)));
        }

        default ContentTypeBuilder<R> toValue(Fn4<R, P1, P2, Option<Q1>, Option<Q2>> fn) {
            return to((p1, p2, q1, q2) -> Promise.success(fn.apply(p1, p2, q1, q2)));
        }
    }

    interface PathQueryBuilder2_3<R, P1, P2, Q1, Q2, Q3> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, Option<Q1>, Option<Q2>, Option<Q3>> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, P1, P2, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return to((p1, p2, q1, q2, q3) -> Promise.resolved(fn.apply(p1, p2, q1, q2, q3)));
        }

        default ContentTypeBuilder<R> toValue(Fn5<R, P1, P2, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return to((p1, p2, q1, q2, q3) -> Promise.success(fn.apply(p1, p2, q1, q2, q3)));
        }
    }

    // 3 path + 1-2 query
    interface PathQueryBuilder3_1<R, P1, P2, P3, Q1> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, P3, Option<Q1>> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, P1, P2, P3, Option<Q1>> fn) {
            return to((p1, p2, p3, q1) -> Promise.resolved(fn.apply(p1, p2, p3, q1)));
        }

        default ContentTypeBuilder<R> toValue(Fn4<R, P1, P2, P3, Option<Q1>> fn) {
            return to((p1, p2, p3, q1) -> Promise.success(fn.apply(p1, p2, p3, q1)));
        }
    }

    interface PathQueryBuilder3_2<R, P1, P2, P3, Q1, Q2> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, P3, Option<Q1>, Option<Q2>> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, P1, P2, P3, Option<Q1>, Option<Q2>> fn) {
            return to((p1, p2, p3, q1, q2) -> Promise.resolved(fn.apply(p1, p2, p3, q1, q2)));
        }

        default ContentTypeBuilder<R> toValue(Fn5<R, P1, P2, P3, Option<Q1>, Option<Q2>> fn) {
            return to((p1, p2, p3, q1, q2) -> Promise.success(fn.apply(p1, p2, p3, q1, q2)));
        }
    }

    // 4 path + 1 query
    interface PathQueryBuilder4_1<R, P1, P2, P3, P4, Q1> {
        ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, P3, P4, Option<Q1>> fn);

        default ContentTypeBuilder<R> toResult(Fn5<Result<R>, P1, P2, P3, P4, Option<Q1>> fn) {
            return to((p1, p2, p3, p4, q1) -> Promise.resolved(fn.apply(p1, p2, p3, p4, q1)));
        }

        default ContentTypeBuilder<R> toValue(Fn5<R, P1, P2, P3, P4, Option<Q1>> fn) {
            return to((p1, p2, p3, p4, q1) -> Promise.success(fn.apply(p1, p2, p3, p4, q1)));
        }
    }

    // ===================================================================================
    // Path + Query + Body Builders (terminal)
    // ===================================================================================
    // 1 path + 1 query + body
    interface PathQueryBodyBuilder1_1<R, P1, Q1, B> {
        ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, Option<Q1>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn3<Result<R>, P1, Option<Q1>, B> fn) {
            return to((p1, q1, body) -> Promise.resolved(fn.apply(p1, q1, body)));
        }

        default Route<R> toJson(Fn3<Promise<R>, P1, Option<Q1>, B> fn) {
            return to(fn).asJson();
        }
    }

    // 1 path + 2 query + body
    interface PathQueryBodyBuilder1_2<R, P1, Q1, Q2, B> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, Option<Q1>, Option<Q2>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, P1, Option<Q1>, Option<Q2>, B> fn) {
            return to((p1, q1, q2, body) -> Promise.resolved(fn.apply(p1, q1, q2, body)));
        }

        default Route<R> toJson(Fn4<Promise<R>, P1, Option<Q1>, Option<Q2>, B> fn) {
            return to(fn).asJson();
        }
    }

    // 2 path + 1 query + body
    interface PathQueryBodyBuilder2_1<R, P1, P2, Q1, B> {
        ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, Option<Q1>, B> fn);

        default ContentTypeBuilder<R> toResult(Fn4<Result<R>, P1, P2, Option<Q1>, B> fn) {
            return to((p1, p2, q1, body) -> Promise.resolved(fn.apply(p1, p2, q1, body)));
        }

        default Route<R> toJson(Fn4<Promise<R>, P1, P2, Option<Q1>, B> fn) {
            return to(fn).asJson();
        }
    }

    // ===================================================================================
    // Terminal Builders
    // ===================================================================================
    interface RawHandlerBuilder<T> {
        ContentTypeBuilder<T> to(Handler<T> handler);

        default ContentTypeBuilder<T> toImmediate(Fn1<Result<T>, RequestContext> handler) {
            return to(ctx -> Promise.resolved(handler.apply(ctx)));
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

    // ===================================================================================
    // Implementation
    // ===================================================================================
    record ParameterBuilderImpl<R>(HttpMethod method, String path, List<String> spacers) implements ParameterBuilder<R> {
        ParameterBuilderImpl(HttpMethod method, String path) {
            this(method, path, List.of());
        }

        @Override
        public ContentTypeBuilder<R> to(Handler<R> handler) {
            return contentType -> route(method, path, handler, contentType, spacers);
        }

        // Path parameters - collect spacers from path parameter definitions
        @Override
        public <P1> PathBuilder1<R, P1> withPath(PathParameter<P1> p1) {
            var collected = collectSpacers(p1);
            return new PathBuilder1Impl<>(new ParameterBuilderImpl<>(method, path, collected), p1);
        }

        @Override
        public <P1, P2> PathBuilder2<R, P1, P2> withPath(PathParameter<P1> p1, PathParameter<P2> p2) {
            var collected = collectSpacers(p1, p2);
            return new PathBuilder2Impl<>(new ParameterBuilderImpl<>(method, path, collected), p1, p2);
        }

        @Override
        public <P1, P2, P3> PathBuilder3<R, P1, P2, P3> withPath(PathParameter<P1> p1,
                                                                 PathParameter<P2> p2,
                                                                 PathParameter<P3> p3) {
            var collected = collectSpacers(p1, p2, p3);
            return new PathBuilder3Impl<>(new ParameterBuilderImpl<>(method, path, collected), p1, p2, p3);
        }

        @Override
        public <P1, P2, P3, P4> PathBuilder4<R, P1, P2, P3, P4> withPath(PathParameter<P1> p1,
                                                                         PathParameter<P2> p2,
                                                                         PathParameter<P3> p3,
                                                                         PathParameter<P4> p4) {
            var collected = collectSpacers(p1, p2, p3, p4);
            return new PathBuilder4Impl<>(new ParameterBuilderImpl<>(method, path, collected), p1, p2, p3, p4);
        }

        @Override
        public <P1, P2, P3, P4, P5> PathBuilder5<R, P1, P2, P3, P4, P5> withPath(PathParameter<P1> p1,
                                                                                 PathParameter<P2> p2,
                                                                                 PathParameter<P3> p3,
                                                                                 PathParameter<P4> p4,
                                                                                 PathParameter<P5> p5) {
            var collected = collectSpacers(p1, p2, p3, p4, p5);
            return new PathBuilder5Impl<>(new ParameterBuilderImpl<>(method, path, collected), p1, p2, p3, p4, p5);
        }

        // Helper to collect spacer text from path parameters
        @SafeVarargs
        private static List<String> collectSpacers(PathParameter<?>... params) {
            var spacers = new ArrayList<String>();
            for (var param : params) {
                if (param instanceof PathParameter.Spacer s) {
                    spacers.add(s.text());
                }
            }
            return List.copyOf(spacers);
        }

        // Body only
        @Override
        public <B> BodyBuilder<R, B> withBody(TypeToken<B> type) {
            return fn -> to(ctx -> ctx.fromJson(type)
                                      .async()
                                      .flatMap(fn));
        }

        // Query parameters
        @Override
        public <Q1> QueryBuilder1<R, Q1> withQuery(QueryParameter<Q1> q1) {
            return new QueryBuilder1Impl<>(this, q1);
        }

        @Override
        public <Q1, Q2> QueryBuilder2<R, Q1, Q2> withQuery(QueryParameter<Q1> q1, QueryParameter<Q2> q2) {
            return new QueryBuilder2Impl<>(this, q1, q2);
        }

        @Override
        public <Q1, Q2, Q3> QueryBuilder3<R, Q1, Q2, Q3> withQuery(QueryParameter<Q1> q1,
                                                                   QueryParameter<Q2> q2,
                                                                   QueryParameter<Q3> q3) {
            return new QueryBuilder3Impl<>(this, q1, q2, q3);
        }

        @Override
        public <Q1, Q2, Q3, Q4> QueryBuilder4<R, Q1, Q2, Q3, Q4> withQuery(QueryParameter<Q1> q1,
                                                                           QueryParameter<Q2> q2,
                                                                           QueryParameter<Q3> q3,
                                                                           QueryParameter<Q4> q4) {
            return new QueryBuilder4Impl<>(this, q1, q2, q3, q4);
        }

        @Override
        public <Q1, Q2, Q3, Q4, Q5> QueryBuilder5<R, Q1, Q2, Q3, Q4, Q5> withQuery(QueryParameter<Q1> q1,
                                                                                   QueryParameter<Q2> q2,
                                                                                   QueryParameter<Q3> q3,
                                                                                   QueryParameter<Q4> q4,
                                                                                   QueryParameter<Q5> q5) {
            return new QueryBuilder5Impl<>(this, q1, q2, q3, q4, q5);
        }
    }

    // Path Builder Implementations
    record PathBuilder1Impl<R, P1>(ParameterBuilder<R> parent, PathParameter<P1> p1) implements PathBuilder1<R, P1> {
        @Override
        public ContentTypeBuilder<R> to(Fn1<Promise<R>, P1> fn) {
            return parent.to(ctx -> ctx.matchPath(p1)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> PathBodyBuilder1<R, P1, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchPath(p1)
                                             .flatMap(v1 -> ctx.fromJson(type)
                                                               .map(body -> fn.apply(v1, body)))
                                             .async()
                                             .flatMap(p -> p));
        }

        @Override
        public <Q1> PathQueryBuilder1_1<R, P1, Q1> withQuery(QueryParameter<Q1> q1) {
            return new PathQueryBuilder1_1Impl<>(parent, p1, q1);
        }

        @Override
        public <Q1, Q2> PathQueryBuilder1_2<R, P1, Q1, Q2> withQuery(QueryParameter<Q1> q1, QueryParameter<Q2> q2) {
            return new PathQueryBuilder1_2Impl<>(parent, p1, q1, q2);
        }

        @Override
        public <Q1, Q2, Q3> PathQueryBuilder1_3<R, P1, Q1, Q2, Q3> withQuery(QueryParameter<Q1> q1,
                                                                             QueryParameter<Q2> q2,
                                                                             QueryParameter<Q3> q3) {
            return new PathQueryBuilder1_3Impl<>(parent, p1, q1, q2, q3);
        }

        @Override
        public <Q1, Q2, Q3, Q4> PathQueryBuilder1_4<R, P1, Q1, Q2, Q3, Q4> withQuery(QueryParameter<Q1> q1,
                                                                                     QueryParameter<Q2> q2,
                                                                                     QueryParameter<Q3> q3,
                                                                                     QueryParameter<Q4> q4) {
            return new PathQueryBuilder1_4Impl<>(parent, p1, q1, q2, q3, q4);
        }
    }

    record PathBuilder2Impl<R, P1, P2>(ParameterBuilder<R> parent, PathParameter<P1> p1, PathParameter<P2> p2)
    implements PathBuilder2<R, P1, P2> {
        @Override
        public ContentTypeBuilder<R> to(Fn2<Promise<R>, P1, P2> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> PathBodyBuilder2<R, P1, P2, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchPath(p1, p2)
                                             .flatMap((v1, v2) -> ctx.fromJson(type)
                                                                     .map(body -> fn.apply(v1, v2, body)))
                                             .async()
                                             .flatMap(p -> p));
        }

        @Override
        public <Q1> PathQueryBuilder2_1<R, P1, P2, Q1> withQuery(QueryParameter<Q1> q1) {
            return new PathQueryBuilder2_1Impl<>(parent, p1, p2, q1);
        }

        @Override
        public <Q1, Q2> PathQueryBuilder2_2<R, P1, P2, Q1, Q2> withQuery(QueryParameter<Q1> q1, QueryParameter<Q2> q2) {
            return new PathQueryBuilder2_2Impl<>(parent, p1, p2, q1, q2);
        }

        @Override
        public <Q1, Q2, Q3> PathQueryBuilder2_3<R, P1, P2, Q1, Q2, Q3> withQuery(QueryParameter<Q1> q1,
                                                                                 QueryParameter<Q2> q2,
                                                                                 QueryParameter<Q3> q3) {
            return new PathQueryBuilder2_3Impl<>(parent, p1, p2, q1, q2, q3);
        }
    }

    record PathBuilder3Impl<R, P1, P2, P3>(ParameterBuilder<R> parent,
                                           PathParameter<P1> p1,
                                           PathParameter<P2> p2,
                                           PathParameter<P3> p3) implements PathBuilder3<R, P1, P2, P3> {
        @Override
        public ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, P2, P3> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2, p3)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> PathBodyBuilder3<R, P1, P2, P3, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchPath(p1, p2, p3)
                                             .flatMap((v1, v2, v3) -> ctx.fromJson(type)
                                                                         .map(body -> fn.apply(v1, v2, v3, body)))
                                             .async()
                                             .flatMap(p -> p));
        }

        @Override
        public <Q1> PathQueryBuilder3_1<R, P1, P2, P3, Q1> withQuery(QueryParameter<Q1> q1) {
            return new PathQueryBuilder3_1Impl<>(parent, p1, p2, p3, q1);
        }

        @Override
        public <Q1, Q2> PathQueryBuilder3_2<R, P1, P2, P3, Q1, Q2> withQuery(QueryParameter<Q1> q1,
                                                                             QueryParameter<Q2> q2) {
            return new PathQueryBuilder3_2Impl<>(parent, p1, p2, p3, q1, q2);
        }
    }

    record PathBuilder4Impl<R, P1, P2, P3, P4>(ParameterBuilder<R> parent,
                                               PathParameter<P1> p1,
                                               PathParameter<P2> p2,
                                               PathParameter<P3> p3,
                                               PathParameter<P4> p4)
    implements PathBuilder4<R, P1, P2, P3, P4> {
        @Override
        public ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, P3, P4> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2, p3, p4)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> PathBodyBuilder4<R, P1, P2, P3, P4, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchPath(p1, p2, p3, p4)
                                             .flatMap((v1, v2, v3, v4) -> ctx.fromJson(type)
                                                                             .map(body -> fn.apply(v1, v2, v3, v4, body)))
                                             .async()
                                             .flatMap(p -> p));
        }

        @Override
        public <Q1> PathQueryBuilder4_1<R, P1, P2, P3, P4, Q1> withQuery(QueryParameter<Q1> q1) {
            return new PathQueryBuilder4_1Impl<>(parent, p1, p2, p3, p4, q1);
        }
    }

    record PathBuilder5Impl<R, P1, P2, P3, P4, P5>(ParameterBuilder<R> parent,
                                                   PathParameter<P1> p1,
                                                   PathParameter<P2> p2,
                                                   PathParameter<P3> p3,
                                                   PathParameter<P4> p4,
                                                   PathParameter<P5> p5)
    implements PathBuilder5<R, P1, P2, P3, P4, P5> {
        @Override
        public ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, P3, P4, P5> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2, p3, p4, p5)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    // Query Builder Implementations
    record QueryBuilder1Impl<R, Q1>(ParameterBuilder<R> parent, QueryParameter<Q1> q1) implements QueryBuilder1<R, Q1> {
        @Override
        public ContentTypeBuilder<R> to(Fn1<Promise<R>, Option<Q1>> fn) {
            return parent.to(ctx -> ctx.matchQuery(q1)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> QueryBodyBuilder1<R, Q1, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchQuery(q1)
                                             .flatMap(v1 -> ctx.fromJson(type)
                                                               .map(body -> fn.apply(v1, body)))
                                             .async()
                                             .flatMap(p -> p));
        }
    }

    record QueryBuilder2Impl<R, Q1, Q2>(ParameterBuilder<R> parent, QueryParameter<Q1> q1, QueryParameter<Q2> q2)
    implements QueryBuilder2<R, Q1, Q2> {
        @Override
        public ContentTypeBuilder<R> to(Fn2<Promise<R>, Option<Q1>, Option<Q2>> fn) {
            return parent.to(ctx -> ctx.matchQuery(q1, q2)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> QueryBodyBuilder2<R, Q1, Q2, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchQuery(q1, q2)
                                             .flatMap((v1, v2) -> ctx.fromJson(type)
                                                                     .map(body -> fn.apply(v1, v2, body)))
                                             .async()
                                             .flatMap(p -> p));
        }
    }

    record QueryBuilder3Impl<R, Q1, Q2, Q3>(ParameterBuilder<R> parent,
                                            QueryParameter<Q1> q1,
                                            QueryParameter<Q2> q2,
                                            QueryParameter<Q3> q3) implements QueryBuilder3<R, Q1, Q2, Q3> {
        @Override
        public ContentTypeBuilder<R> to(Fn3<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return parent.to(ctx -> ctx.matchQuery(q1, q2, q3)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> QueryBodyBuilder3<R, Q1, Q2, Q3, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchQuery(q1, q2, q3)
                                             .flatMap((v1, v2, v3) -> ctx.fromJson(type)
                                                                         .map(body -> fn.apply(v1, v2, v3, body)))
                                             .async()
                                             .flatMap(p -> p));
        }
    }

    record QueryBuilder4Impl<R, Q1, Q2, Q3, Q4>(ParameterBuilder<R> parent,
                                                QueryParameter<Q1> q1,
                                                QueryParameter<Q2> q2,
                                                QueryParameter<Q3> q3,
                                                QueryParameter<Q4> q4)
    implements QueryBuilder4<R, Q1, Q2, Q3, Q4> {
        @Override
        public ContentTypeBuilder<R> to(Fn4<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn) {
            return parent.to(ctx -> ctx.matchQuery(q1, q2, q3, q4)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> QueryBodyBuilder4<R, Q1, Q2, Q3, Q4, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchQuery(q1, q2, q3, q4)
                                             .flatMap((v1, v2, v3, v4) -> ctx.fromJson(type)
                                                                             .map(body -> fn.apply(v1, v2, v3, v4, body)))
                                             .async()
                                             .flatMap(p -> p));
        }
    }

    record QueryBuilder5Impl<R, Q1, Q2, Q3, Q4, Q5>(ParameterBuilder<R> parent,
                                                    QueryParameter<Q1> q1,
                                                    QueryParameter<Q2> q2,
                                                    QueryParameter<Q3> q3,
                                                    QueryParameter<Q4> q4,
                                                    QueryParameter<Q5> q5)
    implements QueryBuilder5<R, Q1, Q2, Q3, Q4, Q5> {
        @Override
        public ContentTypeBuilder<R> to(Fn5<Promise<R>, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>, Option<Q5>> fn) {
            return parent.to(ctx -> ctx.matchQuery(q1, q2, q3, q4, q5)
                                       .map(fn)
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    // Path + Query Builder Implementations
    record PathQueryBuilder1_1Impl<R, P1, Q1>(ParameterBuilder<R> parent, PathParameter<P1> p1, QueryParameter<Q1> q1)
    implements PathQueryBuilder1_1<R, P1, Q1> {
        @Override
        public ContentTypeBuilder<R> to(Fn2<Promise<R>, P1, Option<Q1>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1)
                                       .flatMap(pv -> ctx.matchQuery(q1)
                                                         .map(qv -> fn.apply(pv, qv)))
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> PathQueryBodyBuilder1_1<R, P1, Q1, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchPath(p1)
                                             .flatMap(pv -> ctx.matchQuery(q1)
                                                               .flatMap(qv -> ctx.fromJson(type)
                                                                                 .map(body -> fn.apply(pv, qv, body))))
                                             .async()
                                             .flatMap(p -> p));
        }
    }

    record PathQueryBuilder1_2Impl<R, P1, Q1, Q2>(ParameterBuilder<R> parent,
                                                  PathParameter<P1> p1,
                                                  QueryParameter<Q1> q1,
                                                  QueryParameter<Q2> q2)
    implements PathQueryBuilder1_2<R, P1, Q1, Q2> {
        @Override
        public ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, Option<Q1>, Option<Q2>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1)
                                       .flatMap(pv -> ctx.matchQuery(q1, q2)
                                                         .map((qv1, qv2) -> fn.apply(pv, qv1, qv2)))
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> PathQueryBodyBuilder1_2<R, P1, Q1, Q2, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchPath(p1)
                                             .flatMap(pv -> ctx.matchQuery(q1, q2)
                                                               .flatMap((qv1, qv2) -> ctx.fromJson(type)
                                                                                         .map(body -> fn.apply(pv,
                                                                                                               qv1,
                                                                                                               qv2,
                                                                                                               body))))
                                             .async()
                                             .flatMap(p -> p));
        }
    }

    record PathQueryBuilder1_3Impl<R, P1, Q1, Q2, Q3>(ParameterBuilder<R> parent,
                                                      PathParameter<P1> p1,
                                                      QueryParameter<Q1> q1,
                                                      QueryParameter<Q2> q2,
                                                      QueryParameter<Q3> q3)
    implements PathQueryBuilder1_3<R, P1, Q1, Q2, Q3> {
        @Override
        public ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1)
                                       .flatMap(pv -> ctx.matchQuery(q1, q2, q3)
                                                         .map((qv1, qv2, qv3) -> fn.apply(pv, qv1, qv2, qv3)))
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    record PathQueryBuilder1_4Impl<R, P1, Q1, Q2, Q3, Q4>(ParameterBuilder<R> parent,
                                                          PathParameter<P1> p1,
                                                          QueryParameter<Q1> q1,
                                                          QueryParameter<Q2> q2,
                                                          QueryParameter<Q3> q3,
                                                          QueryParameter<Q4> q4)
    implements PathQueryBuilder1_4<R, P1, Q1, Q2, Q3, Q4> {
        @Override
        public ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, Option<Q1>, Option<Q2>, Option<Q3>, Option<Q4>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1)
                                       .flatMap(pv -> ctx.matchQuery(q1, q2, q3, q4)
                                                         .map((qv1, qv2, qv3, qv4) -> fn.apply(pv, qv1, qv2, qv3, qv4)))
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    record PathQueryBuilder2_1Impl<R, P1, P2, Q1>(ParameterBuilder<R> parent,
                                                  PathParameter<P1> p1,
                                                  PathParameter<P2> p2,
                                                  QueryParameter<Q1> q1) implements PathQueryBuilder2_1<R, P1, P2, Q1> {
        @Override
        public ContentTypeBuilder<R> to(Fn3<Promise<R>, P1, P2, Option<Q1>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2)
                                       .flatMap((pv1, pv2) -> ctx.matchQuery(q1)
                                                                 .map(qv -> fn.apply(pv1, pv2, qv)))
                                       .async()
                                       .flatMap(p -> p));
        }

        @Override
        public <B> PathQueryBodyBuilder2_1<R, P1, P2, Q1, B> withBody(TypeToken<B> type) {
            return fn -> parent.to(ctx -> ctx.matchPath(p1, p2)
                                             .flatMap((pv1, pv2) -> ctx.matchQuery(q1)
                                                                       .flatMap(qv -> ctx.fromJson(type)
                                                                                         .map(body -> fn.apply(pv1,
                                                                                                               pv2,
                                                                                                               qv,
                                                                                                               body))))
                                             .async()
                                             .flatMap(p -> p));
        }
    }

    record PathQueryBuilder2_2Impl<R, P1, P2, Q1, Q2>(ParameterBuilder<R> parent,
                                                      PathParameter<P1> p1,
                                                      PathParameter<P2> p2,
                                                      QueryParameter<Q1> q1,
                                                      QueryParameter<Q2> q2)
    implements PathQueryBuilder2_2<R, P1, P2, Q1, Q2> {
        @Override
        public ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, Option<Q1>, Option<Q2>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2)
                                       .flatMap((pv1, pv2) -> ctx.matchQuery(q1, q2)
                                                                 .map((qv1, qv2) -> fn.apply(pv1, pv2, qv1, qv2)))
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    record PathQueryBuilder2_3Impl<R, P1, P2, Q1, Q2, Q3>(ParameterBuilder<R> parent,
                                                          PathParameter<P1> p1,
                                                          PathParameter<P2> p2,
                                                          QueryParameter<Q1> q1,
                                                          QueryParameter<Q2> q2,
                                                          QueryParameter<Q3> q3)
    implements PathQueryBuilder2_3<R, P1, P2, Q1, Q2, Q3> {
        @Override
        public ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, Option<Q1>, Option<Q2>, Option<Q3>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2)
                                       .flatMap((pv1, pv2) -> ctx.matchQuery(q1, q2, q3)
                                                                 .map((qv1, qv2, qv3) -> fn.apply(pv1,
                                                                                                  pv2,
                                                                                                  qv1,
                                                                                                  qv2,
                                                                                                  qv3)))
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    record PathQueryBuilder3_1Impl<R, P1, P2, P3, Q1>(ParameterBuilder<R> parent,
                                                      PathParameter<P1> p1,
                                                      PathParameter<P2> p2,
                                                      PathParameter<P3> p3,
                                                      QueryParameter<Q1> q1)
    implements PathQueryBuilder3_1<R, P1, P2, P3, Q1> {
        @Override
        public ContentTypeBuilder<R> to(Fn4<Promise<R>, P1, P2, P3, Option<Q1>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2, p3)
                                       .flatMap((pv1, pv2, pv3) -> ctx.matchQuery(q1)
                                                                      .map(qv -> fn.apply(pv1, pv2, pv3, qv)))
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    record PathQueryBuilder3_2Impl<R, P1, P2, P3, Q1, Q2>(ParameterBuilder<R> parent,
                                                          PathParameter<P1> p1,
                                                          PathParameter<P2> p2,
                                                          PathParameter<P3> p3,
                                                          QueryParameter<Q1> q1,
                                                          QueryParameter<Q2> q2)
    implements PathQueryBuilder3_2<R, P1, P2, P3, Q1, Q2> {
        @Override
        public ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, P3, Option<Q1>, Option<Q2>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2, p3)
                                       .flatMap((pv1, pv2, pv3) -> ctx.matchQuery(q1, q2)
                                                                      .map((qv1, qv2) -> fn.apply(pv1,
                                                                                                  pv2,
                                                                                                  pv3,
                                                                                                  qv1,
                                                                                                  qv2)))
                                       .async()
                                       .flatMap(p -> p));
        }
    }

    record PathQueryBuilder4_1Impl<R, P1, P2, P3, P4, Q1>(ParameterBuilder<R> parent,
                                                          PathParameter<P1> p1,
                                                          PathParameter<P2> p2,
                                                          PathParameter<P3> p3,
                                                          PathParameter<P4> p4,
                                                          QueryParameter<Q1> q1)
    implements PathQueryBuilder4_1<R, P1, P2, P3, P4, Q1> {
        @Override
        public ContentTypeBuilder<R> to(Fn5<Promise<R>, P1, P2, P3, P4, Option<Q1>> fn) {
            return parent.to(ctx -> ctx.matchPath(p1, p2, p3, p4)
                                       .flatMap((pv1, pv2, pv3, pv4) -> ctx.matchQuery(q1)
                                                                           .map(qv -> fn.apply(pv1, pv2, pv3, pv4, qv)))
                                       .async()
                                       .flatMap(p -> p));
        }
    }
}
