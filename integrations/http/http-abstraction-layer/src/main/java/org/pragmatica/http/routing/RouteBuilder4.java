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

/// Route builder for 4 parameters.
final class RouteBuilder4<T1, T2, T3, T4> implements Route.PathStage4<T1, T2, T3, T4>, Route.HandlerStage4<T1, T2, T3, T4> {
    private final List<String> pathSegments;
    private final List<ParameterSpec> parameters;
    private HttpMethod method;
    private ContentType requestContentType = CommonContentType.APPLICATION_JSON;
    private ContentType responseContentType = CommonContentType.APPLICATION_JSON;
    private Object handler;

    RouteBuilder4(List<String> pathSegments, List<ParameterSpec> parameters) {
        this.pathSegments = new ArrayList<>(pathSegments);
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> path(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Override
    public <T5> Route.PathStage5<T1, T2, T3, T4, T5> addParam(ParameterType type, String name, TypeToken<T5> token) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(type, name, token));
        return new RouteBuilder5<>(pathSegments, newParams);
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> get() {
        this.method = HttpMethod.GET;
        return this;
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> post() {
        this.method = HttpMethod.POST;
        return this;
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> put() {
        this.method = HttpMethod.PUT;
        return this;
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> delete() {
        this.method = HttpMethod.DELETE;
        return this;
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> patch() {
        this.method = HttpMethod.PATCH;
        return this;
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> in(ContentType contentType) {
        this.requestContentType = contentType;
        return this;
    }

    @Override
    public RouteBuilder4<T1, T2, T3, T4> out(ContentType contentType) {
        this.responseContentType = contentType;
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler4<T1, T2, T3, T4> handler) {
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
