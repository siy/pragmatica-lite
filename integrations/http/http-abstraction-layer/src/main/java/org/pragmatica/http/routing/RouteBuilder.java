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

/// Internal route builder implementation.
/// Implements all three stages and accumulates route configuration.
final class RouteBuilder implements Route.PathStage, Route.ParameterStage, Route.HandlerStage {
    private final List<String> pathSegments = new ArrayList<>();
    private final List<ParameterSpec> parameters = new ArrayList<>();
    private String method;
    private String requestContentType = "application/json";
    private String responseContentType = "application/json";
    private Object handler;

    @Override
    public RouteBuilder path(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Override
    public Route subpath(Route... routes) {
        return new CompositeRoute(String.join("/", pathSegments), List.of(routes));
    }

    @Override
    public <T> RouteBuilder param(Class<T> type) {
        parameters.add(new ParameterSpec(ParameterType.PATH, null, TypeToken.of(type)));
        return this;
    }

    @Override
    public <T> RouteBuilder param(TypeToken<T> type) {
        parameters.add(new ParameterSpec(ParameterType.PATH, null, type));
        return this;
    }

    @Override
    public <T> RouteBuilder queryParam(String name, Class<T> type) {
        parameters.add(new ParameterSpec(ParameterType.QUERY, name, TypeToken.of(type)));
        return this;
    }

    @Override
    public <T> RouteBuilder queryParam(String name, TypeToken<T> type) {
        parameters.add(new ParameterSpec(ParameterType.QUERY, name, type));
        return this;
    }

    @Override
    public <T> RouteBuilder headerParam(HttpHeaderName name, Class<T> type) {
        parameters.add(new ParameterSpec(ParameterType.HEADER, name.headerName(), TypeToken.of(type)));
        return this;
    }

    @Override
    public <T> RouteBuilder headerParam(HttpHeaderName name, TypeToken<T> type) {
        parameters.add(new ParameterSpec(ParameterType.HEADER, name.headerName(), type));
        return this;
    }

    @Override
    public <T> RouteBuilder cookieParam(String name, Class<T> type) {
        parameters.add(new ParameterSpec(ParameterType.COOKIE, name, TypeToken.of(type)));
        return this;
    }

    @Override
    public <T> RouteBuilder cookieParam(String name, TypeToken<T> type) {
        parameters.add(new ParameterSpec(ParameterType.COOKIE, name, type));
        return this;
    }

    @Override
    public <T> RouteBuilder body(Class<T> type) {
        parameters.add(new ParameterSpec(ParameterType.BODY, null, TypeToken.of(type)));
        return this;
    }

    @Override
    public <T> RouteBuilder body(TypeToken<T> type) {
        parameters.add(new ParameterSpec(ParameterType.BODY, null, type));
        return this;
    }

    @Override
    public RouteBuilder get() {
        this.method = "GET";
        return this;
    }

    @Override
    public RouteBuilder post() {
        this.method = "POST";
        return this;
    }

    @Override
    public RouteBuilder put() {
        this.method = "PUT";
        return this;
    }

    @Override
    public RouteBuilder delete() {
        this.method = "DELETE";
        return this;
    }

    @Override
    public RouteBuilder patch() {
        this.method = "PATCH";
        return this;
    }

    @Override
    public RouteBuilder in(String mimeType) {
        this.requestContentType = mimeType;
        return this;
    }

    @Override
    public RouteBuilder out(String mimeType) {
        this.responseContentType = mimeType;
        return this;
    }

    @Override
    public Route handler(Route.Handler0 handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public <T1> Route handler(Route.Handler1<T1> handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public <T1, T2> Route handler(Route.Handler2<T1, T2> handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public <T1, T2, T3> Route handler(Route.Handler3<T1, T2, T3> handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public <T1, T2, T3, T4> Route handler(Route.Handler4<T1, T2, T3, T4> handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public <T1, T2, T3, T4, T5> Route handler(Route.Handler5<T1, T2, T3, T4, T5> handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
        // Implemented in ConcreteRoute
        throw new UnsupportedOperationException("Builder cannot match requests");
    }

    private Route build() {
        return new ConcreteRoute(
            String.join("/", pathSegments),
            method,
            parameters,
            requestContentType,
            responseContentType,
            handler
        );
    }
}
