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

/// Route builder for 6 parameters.
final class RouteBuilder6<T1, T2, T3, T4, T5, T6> implements Route.PathStage6<T1, T2, T3, T4, T5, T6>, Route.HandlerStage6<T1, T2, T3, T4, T5, T6> {
    private final List<String> pathSegments;
    private final List<ParameterSpec> parameters;
    private String method;
    private ContentType requestContentType = CommonContentType.APPLICATION_JSON;
    private ContentType responseContentType = CommonContentType.APPLICATION_JSON;
    private Object handler;

    RouteBuilder6(List<String> pathSegments, List<ParameterSpec> parameters) {
        this.pathSegments = new ArrayList<>(pathSegments);
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> path(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> param(Class<T7> type) {
        return param(TypeToken.of(type));
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> param(TypeToken<T7> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.PATH, null, type));
        return new RouteBuilder7<>(pathSegments, newParams);
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> queryParam(String name, Class<T7> type) {
        return queryParam(name, TypeToken.of(type));
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> queryParam(String name, TypeToken<T7> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.QUERY, name, type));
        return new RouteBuilder7<>(pathSegments, newParams);
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> headerParam(HttpHeaderName name, Class<T7> type) {
        return headerParam(name, TypeToken.of(type));
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> headerParam(HttpHeaderName name, TypeToken<T7> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.HEADER, name.headerName(), type));
        return new RouteBuilder7<>(pathSegments, newParams);
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> cookieParam(String name, Class<T7> type) {
        return cookieParam(name, TypeToken.of(type));
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> cookieParam(String name, TypeToken<T7> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.COOKIE, name, type));
        return new RouteBuilder7<>(pathSegments, newParams);
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> body(Class<T7> type) {
        return body(TypeToken.of(type));
    }

    @Override
    public <T7> Route.PathStage7<T1, T2, T3, T4, T5, T6, T7> body(TypeToken<T7> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.BODY, null, type));
        return new RouteBuilder7<>(pathSegments, newParams);
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> get() {
        this.method = "GET";
        return this;
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> post() {
        this.method = "POST";
        return this;
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> put() {
        this.method = "PUT";
        return this;
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> delete() {
        this.method = "DELETE";
        return this;
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> patch() {
        this.method = "PATCH";
        return this;
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> in(ContentType contentType) {
        this.requestContentType = contentType;
        return this;
    }

    @Override
    public RouteBuilder6<T1, T2, T3, T4, T5, T6> out(ContentType contentType) {
        this.responseContentType = contentType;
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler6<T1, T2, T3, T4, T5, T6> handler) {
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
