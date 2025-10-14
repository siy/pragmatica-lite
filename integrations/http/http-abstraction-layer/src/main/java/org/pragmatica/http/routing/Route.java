/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.http.routing;

import org.pragmatica.http.model.ContentType;
import org.pragmatica.http.model.HttpHeaderName;
import org.pragmatica.http.model.HttpRequest;
import org.pragmatica.http.model.HttpResponse;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

/// Type-safe route builder with compile-time parameter tracking.
///
/// Each parameter declaration transitions to next generic stage (PathStage0 → PathStage1 → ...),
/// ensuring handler signature matches accumulated parameter types at compile time.
///
/// Maximum 9 parameters supported (PathStage0-9, HandlerStage0-9).
public interface Route {

    /// Match this route against an HTTP request.
    ///
    /// @param request HTTP request to match
    /// @return Option containing matched handler result, or None if no match
    Promise<Option<HttpResponse>> match(HttpRequest request);

    /// Stage 0: No parameters yet
    interface PathStage0 extends Route {
        PathStage0 path(String segment);
        Route subpath(Route... routes);
        <T1> PathStage1<T1> param(Class<T1> type);
        <T1> PathStage1<T1> param(TypeToken<T1> type);
        <T1> PathStage1<T1> queryParam(String name, Class<T1> type);
        <T1> PathStage1<T1> queryParam(String name, TypeToken<T1> type);
        <T1> PathStage1<T1> headerParam(HttpHeaderName name, Class<T1> type);
        <T1> PathStage1<T1> headerParam(HttpHeaderName name, TypeToken<T1> type);
        <T1> PathStage1<T1> cookieParam(String name, Class<T1> type);
        <T1> PathStage1<T1> cookieParam(String name, TypeToken<T1> type);
        <T1> PathStage1<T1> body(Class<T1> type);
        <T1> PathStage1<T1> body(TypeToken<T1> type);
        HandlerStage0 get();
        HandlerStage0 post();
        HandlerStage0 put();
        HandlerStage0 delete();
        HandlerStage0 patch();
    }

    /// Stage 1: One parameter accumulated
    interface PathStage1<T1> extends Route {
        PathStage1<T1> path(String segment);
        <T2> PathStage2<T1, T2> param(Class<T2> type);
        <T2> PathStage2<T1, T2> param(TypeToken<T2> type);
        <T2> PathStage2<T1, T2> queryParam(String name, Class<T2> type);
        <T2> PathStage2<T1, T2> queryParam(String name, TypeToken<T2> type);
        <T2> PathStage2<T1, T2> headerParam(HttpHeaderName name, Class<T2> type);
        <T2> PathStage2<T1, T2> headerParam(HttpHeaderName name, TypeToken<T2> type);
        <T2> PathStage2<T1, T2> cookieParam(String name, Class<T2> type);
        <T2> PathStage2<T1, T2> cookieParam(String name, TypeToken<T2> type);
        <T2> PathStage2<T1, T2> body(Class<T2> type);
        <T2> PathStage2<T1, T2> body(TypeToken<T2> type);
        HandlerStage1<T1> get();
        HandlerStage1<T1> post();
        HandlerStage1<T1> put();
        HandlerStage1<T1> delete();
        HandlerStage1<T1> patch();
    }

    /// Stage 2: Two parameters accumulated
    interface PathStage2<T1, T2> extends Route {
        PathStage2<T1, T2> path(String segment);
        <T3> PathStage3<T1, T2, T3> param(Class<T3> type);
        <T3> PathStage3<T1, T2, T3> param(TypeToken<T3> type);
        <T3> PathStage3<T1, T2, T3> queryParam(String name, Class<T3> type);
        <T3> PathStage3<T1, T2, T3> queryParam(String name, TypeToken<T3> type);
        <T3> PathStage3<T1, T2, T3> headerParam(HttpHeaderName name, Class<T3> type);
        <T3> PathStage3<T1, T2, T3> headerParam(HttpHeaderName name, TypeToken<T3> type);
        <T3> PathStage3<T1, T2, T3> cookieParam(String name, Class<T3> type);
        <T3> PathStage3<T1, T2, T3> cookieParam(String name, TypeToken<T3> type);
        <T3> PathStage3<T1, T2, T3> body(Class<T3> type);
        <T3> PathStage3<T1, T2, T3> body(TypeToken<T3> type);
        HandlerStage2<T1, T2> get();
        HandlerStage2<T1, T2> post();
        HandlerStage2<T1, T2> put();
        HandlerStage2<T1, T2> delete();
        HandlerStage2<T1, T2> patch();
    }

    /// Stage 3: Three parameters accumulated
    interface PathStage3<T1, T2, T3> extends Route {
        PathStage3<T1, T2, T3> path(String segment);
        <T4> PathStage4<T1, T2, T3, T4> param(Class<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> param(TypeToken<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> queryParam(String name, Class<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> queryParam(String name, TypeToken<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> headerParam(HttpHeaderName name, Class<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> headerParam(HttpHeaderName name, TypeToken<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> cookieParam(String name, Class<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> cookieParam(String name, TypeToken<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> body(Class<T4> type);
        <T4> PathStage4<T1, T2, T3, T4> body(TypeToken<T4> type);
        HandlerStage3<T1, T2, T3> get();
        HandlerStage3<T1, T2, T3> post();
        HandlerStage3<T1, T2, T3> put();
        HandlerStage3<T1, T2, T3> delete();
        HandlerStage3<T1, T2, T3> patch();
    }

    /// Stage 4: Four parameters accumulated
    interface PathStage4<T1, T2, T3, T4> extends Route {
        PathStage4<T1, T2, T3, T4> path(String segment);
        <T5> PathStage5<T1, T2, T3, T4, T5> param(Class<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> param(TypeToken<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> queryParam(String name, Class<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> queryParam(String name, TypeToken<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> headerParam(HttpHeaderName name, Class<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> headerParam(HttpHeaderName name, TypeToken<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> cookieParam(String name, Class<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> cookieParam(String name, TypeToken<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> body(Class<T5> type);
        <T5> PathStage5<T1, T2, T3, T4, T5> body(TypeToken<T5> type);
        HandlerStage4<T1, T2, T3, T4> get();
        HandlerStage4<T1, T2, T3, T4> post();
        HandlerStage4<T1, T2, T3, T4> put();
        HandlerStage4<T1, T2, T3, T4> delete();
        HandlerStage4<T1, T2, T3, T4> patch();
    }

    /// Stage 5: Five parameters accumulated
    interface PathStage5<T1, T2, T3, T4, T5> extends Route {
        PathStage5<T1, T2, T3, T4, T5> path(String segment);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> param(Class<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> param(TypeToken<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> queryParam(String name, Class<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> queryParam(String name, TypeToken<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> headerParam(HttpHeaderName name, Class<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> headerParam(HttpHeaderName name, TypeToken<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> cookieParam(String name, Class<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> cookieParam(String name, TypeToken<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> body(Class<T6> type);
        <T6> PathStage6<T1, T2, T3, T4, T5, T6> body(TypeToken<T6> type);
        HandlerStage5<T1, T2, T3, T4, T5> get();
        HandlerStage5<T1, T2, T3, T4, T5> post();
        HandlerStage5<T1, T2, T3, T4, T5> put();
        HandlerStage5<T1, T2, T3, T4, T5> delete();
        HandlerStage5<T1, T2, T3, T4, T5> patch();
    }

    /// Stage 6: Six parameters accumulated
    interface PathStage6<T1, T2, T3, T4, T5, T6> extends Route {
        PathStage6<T1, T2, T3, T4, T5, T6> path(String segment);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> param(Class<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> param(TypeToken<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> queryParam(String name, Class<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> queryParam(String name, TypeToken<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> headerParam(HttpHeaderName name, Class<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> headerParam(HttpHeaderName name, TypeToken<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> cookieParam(String name, Class<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> cookieParam(String name, TypeToken<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> body(Class<T7> type);
        <T7> PathStage7<T1, T2, T3, T4, T5, T6, T7> body(TypeToken<T7> type);
        HandlerStage6<T1, T2, T3, T4, T5, T6> get();
        HandlerStage6<T1, T2, T3, T4, T5, T6> post();
        HandlerStage6<T1, T2, T3, T4, T5, T6> put();
        HandlerStage6<T1, T2, T3, T4, T5, T6> delete();
        HandlerStage6<T1, T2, T3, T4, T5, T6> patch();
    }

    /// Stage 7: Seven parameters accumulated
    interface PathStage7<T1, T2, T3, T4, T5, T6, T7> extends Route {
        PathStage7<T1, T2, T3, T4, T5, T6, T7> path(String segment);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> param(Class<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> param(TypeToken<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> queryParam(String name, Class<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> queryParam(String name, TypeToken<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> headerParam(HttpHeaderName name, Class<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> headerParam(HttpHeaderName name, TypeToken<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> cookieParam(String name, Class<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> cookieParam(String name, TypeToken<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> body(Class<T8> type);
        <T8> PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> body(TypeToken<T8> type);
        HandlerStage7<T1, T2, T3, T4, T5, T6, T7> get();
        HandlerStage7<T1, T2, T3, T4, T5, T6, T7> post();
        HandlerStage7<T1, T2, T3, T4, T5, T6, T7> put();
        HandlerStage7<T1, T2, T3, T4, T5, T6, T7> delete();
        HandlerStage7<T1, T2, T3, T4, T5, T6, T7> patch();
    }

    /// Stage 8: Eight parameters accumulated
    interface PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> extends Route {
        PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> path(String segment);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> param(Class<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> param(TypeToken<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> queryParam(String name, Class<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> queryParam(String name, TypeToken<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> headerParam(HttpHeaderName name, Class<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> headerParam(HttpHeaderName name, TypeToken<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> cookieParam(String name, Class<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> cookieParam(String name, TypeToken<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> body(Class<T9> type);
        <T9> PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> body(TypeToken<T9> type);
        HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> get();
        HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> post();
        HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> put();
        HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> delete();
        HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> patch();
    }

    /// Stage 9: Nine parameters accumulated (maximum)
    interface PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Route {
        PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> path(String segment);
        HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> get();
        HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> post();
        HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> put();
        HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> delete();
        HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> patch();
    }

    /// Handler stage 0: No parameters
    interface HandlerStage0 extends Route {
        HandlerStage0 in(ContentType contentType);
        HandlerStage0 out(ContentType contentType);
        Route handler(Handler0 handler);
    }

    /// Handler stage 1: One parameter
    interface HandlerStage1<T1> extends Route {
        HandlerStage1<T1> in(ContentType contentType);
        HandlerStage1<T1> out(ContentType contentType);
        Route handler(Handler1<T1> handler);
    }

    /// Handler stage 2: Two parameters
    interface HandlerStage2<T1, T2> extends Route {
        HandlerStage2<T1, T2> in(ContentType contentType);
        HandlerStage2<T1, T2> out(ContentType contentType);
        Route handler(Handler2<T1, T2> handler);
    }

    /// Handler stage 3: Three parameters
    interface HandlerStage3<T1, T2, T3> extends Route {
        HandlerStage3<T1, T2, T3> in(ContentType contentType);
        HandlerStage3<T1, T2, T3> out(ContentType contentType);
        Route handler(Handler3<T1, T2, T3> handler);
    }

    /// Handler stage 4: Four parameters
    interface HandlerStage4<T1, T2, T3, T4> extends Route {
        HandlerStage4<T1, T2, T3, T4> in(ContentType contentType);
        HandlerStage4<T1, T2, T3, T4> out(ContentType contentType);
        Route handler(Handler4<T1, T2, T3, T4> handler);
    }

    /// Handler stage 5: Five parameters
    interface HandlerStage5<T1, T2, T3, T4, T5> extends Route {
        HandlerStage5<T1, T2, T3, T4, T5> in(ContentType contentType);
        HandlerStage5<T1, T2, T3, T4, T5> out(ContentType contentType);
        Route handler(Handler5<T1, T2, T3, T4, T5> handler);
    }

    /// Handler stage 6: Six parameters
    interface HandlerStage6<T1, T2, T3, T4, T5, T6> extends Route {
        HandlerStage6<T1, T2, T3, T4, T5, T6> in(ContentType contentType);
        HandlerStage6<T1, T2, T3, T4, T5, T6> out(ContentType contentType);
        Route handler(Handler6<T1, T2, T3, T4, T5, T6> handler);
    }

    /// Handler stage 7: Seven parameters
    interface HandlerStage7<T1, T2, T3, T4, T5, T6, T7> extends Route {
        HandlerStage7<T1, T2, T3, T4, T5, T6, T7> in(ContentType contentType);
        HandlerStage7<T1, T2, T3, T4, T5, T6, T7> out(ContentType contentType);
        Route handler(Handler7<T1, T2, T3, T4, T5, T6, T7> handler);
    }

    /// Handler stage 8: Eight parameters
    interface HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> extends Route {
        HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> in(ContentType contentType);
        HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> out(ContentType contentType);
        Route handler(Handler8<T1, T2, T3, T4, T5, T6, T7, T8> handler);
    }

    /// Handler stage 9: Nine parameters (maximum)
    interface HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Route {
        HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> in(ContentType contentType);
        HandlerStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> out(ContentType contentType);
        Route handler(Handler9<T1, T2, T3, T4, T5, T6, T7, T8, T9> handler);
    }

    /// Handler function interfaces
    @FunctionalInterface
    interface Handler0 {
        Promise<HttpResponse> handle();
    }

    @FunctionalInterface
    interface Handler1<T1> {
        Promise<HttpResponse> handle(Option<T1> p1);
    }

    @FunctionalInterface
    interface Handler2<T1, T2> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2);
    }

    @FunctionalInterface
    interface Handler3<T1, T2, T3> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2, Option<T3> p3);
    }

    @FunctionalInterface
    interface Handler4<T1, T2, T3, T4> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2, Option<T3> p3, Option<T4> p4);
    }

    @FunctionalInterface
    interface Handler5<T1, T2, T3, T4, T5> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2, Option<T3> p3, Option<T4> p4, Option<T5> p5);
    }

    @FunctionalInterface
    interface Handler6<T1, T2, T3, T4, T5, T6> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2, Option<T3> p3, Option<T4> p4, Option<T5> p5, Option<T6> p6);
    }

    @FunctionalInterface
    interface Handler7<T1, T2, T3, T4, T5, T6, T7> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2, Option<T3> p3, Option<T4> p4, Option<T5> p5, Option<T6> p6, Option<T7> p7);
    }

    @FunctionalInterface
    interface Handler8<T1, T2, T3, T4, T5, T6, T7, T8> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2, Option<T3> p3, Option<T4> p4, Option<T5> p5, Option<T6> p6, Option<T7> p7, Option<T8> p8);
    }

    @FunctionalInterface
    interface Handler9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        Promise<HttpResponse> handle(Option<T1> p1, Option<T2> p2, Option<T3> p3, Option<T4> p4, Option<T5> p5, Option<T6> p6, Option<T7> p7, Option<T8> p8, Option<T9> p9);
    }

    /// Entry point: start building a route with a path.
    ///
    /// @param segment initial path segment
    /// @return path stage with no parameters
    static PathStage0 path(String segment) {
        return RouteBuilder0.builder().path(segment);
    }

    /// Entry point: start building a route with a path parameter.
    ///
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return path stage with one parameter
    static <T> PathStage1<T> param(Class<T> type) {
        return RouteBuilder0.builder().param(type);
    }

    /// Entry point: start building a route with a path parameter (generic).
    ///
    /// @param type parameter type token
    /// @param <T> type parameter
    /// @return path stage with one parameter
    static <T> PathStage1<T> param(TypeToken<T> type) {
        return RouteBuilder0.builder().param(type);
    }

    /// Entry point: start building a route with a query parameter.
    ///
    /// @param name parameter name
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return path stage with one parameter
    static <T> PathStage1<T> queryParam(String name, Class<T> type) {
        return RouteBuilder0.builder().queryParam(name, type);
    }

    /// Entry point: start building a route with a header parameter.
    ///
    /// @param name header name
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return path stage with one parameter
    static <T> PathStage1<T> headerParam(HttpHeaderName name, Class<T> type) {
        return RouteBuilder0.builder().headerParam(name, type);
    }

    /// Entry point: start building a route with a cookie parameter.
    ///
    /// @param name cookie name
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return path stage with one parameter
    static <T> PathStage1<T> cookieParam(String name, Class<T> type) {
        return RouteBuilder0.builder().cookieParam(name, type);
    }

    /// Entry point: start building a route with a body.
    ///
    /// @param type body type
    /// @param <T> type parameter
    /// @return path stage with one parameter
    static <T> PathStage1<T> body(Class<T> type) {
        return RouteBuilder0.builder().body(type);
    }

    /// Entry point: start building a route with a body (generic).
    ///
    /// @param type body type token
    /// @param <T> type parameter
    /// @return path stage with one parameter
    static <T> PathStage1<T> body(TypeToken<T> type) {
        return RouteBuilder0.builder().body(type);
    }
}
