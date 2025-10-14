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

import org.pragmatica.http.model.CommonContentType;
import org.pragmatica.http.model.ContentType;
import org.pragmatica.http.model.HttpHeaderName;
import org.pragmatica.http.model.HttpRequest;
import org.pragmatica.http.model.HttpResponse;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

import java.util.ArrayList;
import java.util.List;

/// Route builder for 5 parameters.
final class RouteBuilder5<T1, T2, T3, T4, T5> implements Route.PathStage5<T1, T2, T3, T4, T5>, Route.HandlerStage5<T1, T2, T3, T4, T5> {
    private final List<String> pathSegments;
    private final List<ParameterSpec> parameters;
    private HttpMethod method;
    private ContentType requestContentType = CommonContentType.APPLICATION_JSON;
    private ContentType responseContentType = CommonContentType.APPLICATION_JSON;
    private Object handler;

    RouteBuilder5(List<String> pathSegments, List<ParameterSpec> parameters) {
        this.pathSegments = new ArrayList<>(pathSegments);
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> path(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Override
    public <T6> Route.PathStage6<T1, T2, T3, T4, T5, T6> addParam(ParameterType type, String name, TypeToken<T6> token) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(type, name, token));
        return new RouteBuilder6<>(pathSegments, newParams);
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> get() {
        this.method = HttpMethod.GET;
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> post() {
        this.method = HttpMethod.POST;
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> put() {
        this.method = HttpMethod.PUT;
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> delete() {
        this.method = HttpMethod.DELETE;
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> patch() {
        this.method = HttpMethod.PATCH;
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> in(ContentType contentType) {
        this.requestContentType = contentType;
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> out(ContentType contentType) {
        this.responseContentType = contentType;
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler5<T1, T2, T3, T4, T5> handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
        throw new UnsupportedOperationException("Builder cannot match requests");
    }

    private RouteMatcher build() {
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
