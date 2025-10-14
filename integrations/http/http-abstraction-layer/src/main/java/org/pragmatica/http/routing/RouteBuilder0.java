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

/// Route builder for 0 parameters.
final class RouteBuilder0 implements Route.PathStage0, Route.HandlerStage0 {
    private final List<String> pathSegments;
    private final List<ParameterSpec> parameters;
    private String method;
    private ContentType requestContentType = CommonContentType.APPLICATION_JSON;
    private ContentType responseContentType = CommonContentType.APPLICATION_JSON;
    private Object handler;

    private RouteBuilder0() {
        this.pathSegments = new ArrayList<>();
        this.parameters = new ArrayList<>();
    }

    private RouteBuilder0(List<String> pathSegments, List<ParameterSpec> parameters) {
        this.pathSegments = new ArrayList<>(pathSegments);
        this.parameters = new ArrayList<>(parameters);
    }

    static RouteBuilder0 builder() {
        return new RouteBuilder0();
    }

    @Override
    public RouteBuilder0 path(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Override
    public Route subpath(Route... routes) {
        return new CompositeRoute(String.join("/", pathSegments), List.of(routes));
    }

    @Override
    public <T1> Route.PathStage1<T1> param(Class<T1> type) {
        return param(TypeToken.of(type));
    }

    @Override
    public <T1> Route.PathStage1<T1> param(TypeToken<T1> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.PATH, null, type));
        return new RouteBuilder1<>(pathSegments, newParams);
    }

    @Override
    public <T1> Route.PathStage1<T1> queryParam(String name, Class<T1> type) {
        return queryParam(name, TypeToken.of(type));
    }

    @Override
    public <T1> Route.PathStage1<T1> queryParam(String name, TypeToken<T1> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.QUERY, name, type));
        return new RouteBuilder1<>(pathSegments, newParams);
    }

    @Override
    public <T1> Route.PathStage1<T1> headerParam(HttpHeaderName name, Class<T1> type) {
        return headerParam(name, TypeToken.of(type));
    }

    @Override
    public <T1> Route.PathStage1<T1> headerParam(HttpHeaderName name, TypeToken<T1> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.HEADER, name.headerName(), type));
        return new RouteBuilder1<>(pathSegments, newParams);
    }

    @Override
    public <T1> Route.PathStage1<T1> cookieParam(String name, Class<T1> type) {
        return cookieParam(name, TypeToken.of(type));
    }

    @Override
    public <T1> Route.PathStage1<T1> cookieParam(String name, TypeToken<T1> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.COOKIE, name, type));
        return new RouteBuilder1<>(pathSegments, newParams);
    }

    @Override
    public <T1> Route.PathStage1<T1> body(Class<T1> type) {
        return body(TypeToken.of(type));
    }

    @Override
    public <T1> Route.PathStage1<T1> body(TypeToken<T1> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.BODY, null, type));
        return new RouteBuilder1<>(pathSegments, newParams);
    }

    @Override
    public RouteBuilder0 get() {
        this.method = "GET";
        return this;
    }

    @Override
    public RouteBuilder0 post() {
        this.method = "POST";
        return this;
    }

    @Override
    public RouteBuilder0 put() {
        this.method = "PUT";
        return this;
    }

    @Override
    public RouteBuilder0 delete() {
        this.method = "DELETE";
        return this;
    }

    @Override
    public RouteBuilder0 patch() {
        this.method = "PATCH";
        return this;
    }

    @Override
    public RouteBuilder0 in(ContentType contentType) {
        this.requestContentType = contentType;
        return this;
    }

    @Override
    public RouteBuilder0 out(ContentType contentType) {
        this.responseContentType = contentType;
        return this;
    }

    @Override
    public Route handler(Route.Handler0 handler) {
        this.handler = handler;
        return build();
    }

    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
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
