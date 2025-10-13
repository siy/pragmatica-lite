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

import org.pragmatica.http.model.HttpHeaderName;
import org.pragmatica.http.model.HttpRequest;
import org.pragmatica.http.model.HttpResponse;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

import java.util.ArrayList;
import java.util.List;

/// Type-safe route builder with three-stage fluent API:
/// 1. Path Definition - build path structure
/// 2. Parameter Collection - declare parameters (path/query/body)
/// 3. Handler Description - set HTTP method, content types, and handler
///
/// All routes are immutable once built and can be composed hierarchically.
public interface Route {

    /// Match this route against an HTTP request.
    ///
    /// @param request HTTP request to match
    /// @return Option containing matched handler result, or None if no match
    Promise<Option<HttpResponse>> match(HttpRequest request);

    /// Stage 1: Path Definition
    /// Build path structure before collecting parameters.
    interface PathStage extends Route {
        /// Add a path segment (can contain multiple levels like "api/v1").
        ///
        /// @param segment path segment
        /// @return this stage
        PathStage path(String segment);

        /// Define nested routes under this path.
        /// Terminal operation - no further methods can be called.
        ///
        /// @param routes nested routes
        /// @return complete route
        Route subpath(Route... routes);

        /// Declare a path parameter and transition to parameter collection stage.
        ///
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage param(Class<T> type);

        /// Declare a path parameter with generic type.
        ///
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage param(TypeToken<T> type);

        /// Declare a query parameter.
        ///
        /// @param name parameter name
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage queryParam(String name, Class<T> type);

        /// Declare a query parameter with generic type.
        ///
        /// @param name parameter name
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage queryParam(String name, TypeToken<T> type);

        /// Declare a header parameter.
        ///
        /// @param name header name
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage headerParam(HttpHeaderName name, Class<T> type);

        /// Declare a header parameter with generic type.
        ///
        /// @param name header name
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage headerParam(HttpHeaderName name, TypeToken<T> type);

        /// Declare a cookie parameter.
        ///
        /// @param name cookie name
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage cookieParam(String name, Class<T> type);

        /// Declare a cookie parameter with generic type.
        ///
        /// @param name cookie name
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage cookieParam(String name, TypeToken<T> type);

        /// Declare request body.
        ///
        /// @param type body type
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage body(Class<T> type);

        /// Declare request body with generic type.
        ///
        /// @param type body type token
        /// @param <T> type parameter
        /// @return parameter collection stage
        <T> ParameterStage body(TypeToken<T> type);

        /// Transition to handler stage with GET method (no parameters).
        ///
        /// @return handler stage
        HandlerStage get();

        /// Transition to handler stage with POST method (no parameters).
        ///
        /// @return handler stage
        HandlerStage post();

        /// Transition to handler stage with PUT method (no parameters).
        ///
        /// @return handler stage
        HandlerStage put();

        /// Transition to handler stage with DELETE method (no parameters).
        ///
        /// @return handler stage
        HandlerStage delete();

        /// Transition to handler stage with PATCH method (no parameters).
        ///
        /// @return handler stage
        HandlerStage patch();
    }

    /// Stage 2: Parameter Collection
    /// Collect parameters after first param/query/body declaration.
    interface ParameterStage extends Route {
        /// Add another path segment (useful for nested resources like /users/{id}/orders).
        ///
        /// @param segment path segment
        /// @return this stage
        ParameterStage path(String segment);

        /// Declare another path parameter.
        ///
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage param(Class<T> type);

        /// Declare another path parameter with generic type.
        ///
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage param(TypeToken<T> type);

        /// Declare a query parameter.
        ///
        /// @param name parameter name
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage queryParam(String name, Class<T> type);

        /// Declare a query parameter with generic type.
        ///
        /// @param name parameter name
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage queryParam(String name, TypeToken<T> type);

        /// Declare a header parameter.
        ///
        /// @param name header name
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage headerParam(HttpHeaderName name, Class<T> type);

        /// Declare a header parameter with generic type.
        ///
        /// @param name header name
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage headerParam(HttpHeaderName name, TypeToken<T> type);

        /// Declare a cookie parameter.
        ///
        /// @param name cookie name
        /// @param type parameter type
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage cookieParam(String name, Class<T> type);

        /// Declare a cookie parameter with generic type.
        ///
        /// @param name cookie name
        /// @param type parameter type token
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage cookieParam(String name, TypeToken<T> type);

        /// Declare request body.
        ///
        /// @param type body type
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage body(Class<T> type);

        /// Declare request body with generic type.
        ///
        /// @param type body type token
        /// @param <T> type parameter
        /// @return this stage
        <T> ParameterStage body(TypeToken<T> type);

        /// Transition to handler stage with GET method.
        ///
        /// @return handler stage
        HandlerStage get();

        /// Transition to handler stage with POST method.
        ///
        /// @return handler stage
        HandlerStage post();

        /// Transition to handler stage with PUT method.
        ///
        /// @return handler stage
        HandlerStage put();

        /// Transition to handler stage with DELETE method.
        ///
        /// @return handler stage
        HandlerStage delete();

        /// Transition to handler stage with PATCH method.
        ///
        /// @return handler stage
        HandlerStage patch();
    }

    /// Stage 3: Handler Description
    /// Set content types and handler function.
    interface HandlerStage extends Route {
        /// Set request content type (defaults to application/json).
        ///
        /// @param mimeType MIME type
        /// @return this stage
        HandlerStage in(String mimeType);

        /// Set response content type (defaults to application/json).
        ///
        /// @param mimeType MIME type
        /// @return this stage
        HandlerStage out(String mimeType);

        /// Set the handler function (no parameters).
        ///
        /// @param handler handler function
        /// @return complete route
        Route handler(Handler0 handler);

        /// Set the handler function (1 parameter).
        ///
        /// @param handler handler function
        /// @param <T1> first parameter type
        /// @return complete route
        <T1> Route handler(Handler1<T1> handler);

        /// Set the handler function (2 parameters).
        ///
        /// @param handler handler function
        /// @param <T1> first parameter type
        /// @param <T2> second parameter type
        /// @return complete route
        <T1, T2> Route handler(Handler2<T1, T2> handler);

        /// Set the handler function (3 parameters).
        ///
        /// @param handler handler function
        /// @param <T1> first parameter type
        /// @param <T2> second parameter type
        /// @param <T3> third parameter type
        /// @return complete route
        <T1, T2, T3> Route handler(Handler3<T1, T2, T3> handler);

        /// Set the handler function (4 parameters).
        ///
        /// @param handler handler function
        /// @param <T1> first parameter type
        /// @param <T2> second parameter type
        /// @param <T3> third parameter type
        /// @param <T4> fourth parameter type
        /// @return complete route
        <T1, T2, T3, T4> Route handler(Handler4<T1, T2, T3, T4> handler);

        /// Set the handler function (5 parameters).
        ///
        /// @param handler handler function
        /// @param <T1> first parameter type
        /// @param <T2> second parameter type
        /// @param <T3> third parameter type
        /// @param <T4> fourth parameter type
        /// @param <T5> fifth parameter type
        /// @return complete route
        <T1, T2, T3, T4, T5> Route handler(Handler5<T1, T2, T3, T4, T5> handler);
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

    /// Entry point: start building a route with a path.
    ///
    /// @param segment initial path segment
    /// @return path stage
    static PathStage path(String segment) {
        return new RouteBuilder().path(segment);
    }

    /// Entry point: start building a route with a path parameter.
    ///
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return parameter stage
    static <T> ParameterStage param(Class<T> type) {
        return new RouteBuilder().param(type);
    }

    /// Entry point: start building a route with a path parameter (generic).
    ///
    /// @param type parameter type token
    /// @param <T> type parameter
    /// @return parameter stage
    static <T> ParameterStage param(TypeToken<T> type) {
        return new RouteBuilder().param(type);
    }

    /// Entry point: start building a route with a query parameter.
    ///
    /// @param name parameter name
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return parameter stage
    static <T> ParameterStage queryParam(String name, Class<T> type) {
        return new RouteBuilder().queryParam(name, type);
    }

    /// Entry point: start building a route with a header parameter.
    ///
    /// @param name header name
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return parameter stage
    static <T> ParameterStage headerParam(HttpHeaderName name, Class<T> type) {
        return new RouteBuilder().headerParam(name, type);
    }

    /// Entry point: start building a route with a cookie parameter.
    ///
    /// @param name cookie name
    /// @param type parameter type
    /// @param <T> type parameter
    /// @return parameter stage
    static <T> ParameterStage cookieParam(String name, Class<T> type) {
        return new RouteBuilder().cookieParam(name, type);
    }

    /// Entry point: start building a route with a body.
    ///
    /// @param type body type
    /// @param <T> type parameter
    /// @return parameter stage
    static <T> ParameterStage body(Class<T> type) {
        return new RouteBuilder().body(type);
    }

    /// Entry point: start building a route with a body (generic).
    ///
    /// @param type body type token
    /// @param <T> type parameter
    /// @return parameter stage
    static <T> ParameterStage body(TypeToken<T> type) {
        return new RouteBuilder().body(type);
    }
}
