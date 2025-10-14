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

/// Route builder for 8 parameters.
final class RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> implements Route.PathStage8<T1, T2, T3, T4, T5, T6, T7, T8>, Route.HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> {
    private final List<String> pathSegments;
    private final List<ParameterSpec> parameters;
    private String method;
    private ContentType requestContentType = CommonContentType.APPLICATION_JSON;
    private ContentType responseContentType = CommonContentType.APPLICATION_JSON;
    private Object handler;

    RouteBuilder8(List<String> pathSegments, List<ParameterSpec> parameters) {
        this.pathSegments = new ArrayList<>(pathSegments);
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> path(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> param(Class<T9> type) {
        return param(TypeToken.of(type));
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> param(TypeToken<T9> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.PATH, null, type));
        return new RouteBuilder9<>(pathSegments, newParams);
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> queryParam(String name, Class<T9> type) {
        return queryParam(name, TypeToken.of(type));
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> queryParam(String name, TypeToken<T9> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.QUERY, name, type));
        return new RouteBuilder9<>(pathSegments, newParams);
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> headerParam(HttpHeaderName name, Class<T9> type) {
        return headerParam(name, TypeToken.of(type));
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> headerParam(HttpHeaderName name, TypeToken<T9> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.HEADER, name.headerName(), type));
        return new RouteBuilder9<>(pathSegments, newParams);
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> cookieParam(String name, Class<T9> type) {
        return cookieParam(name, TypeToken.of(type));
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> cookieParam(String name, TypeToken<T9> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.COOKIE, name, type));
        return new RouteBuilder9<>(pathSegments, newParams);
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> body(Class<T9> type) {
        return body(TypeToken.of(type));
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> body(TypeToken<T9> type) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(ParameterType.BODY, null, type));
        return new RouteBuilder9<>(pathSegments, newParams);
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> get() {
        this.method = "GET";
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> post() {
        this.method = "POST";
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> put() {
        this.method = "PUT";
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> delete() {
        this.method = "DELETE";
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> patch() {
        this.method = "PATCH";
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> in(ContentType contentType) {
        this.requestContentType = contentType;
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> out(ContentType contentType) {
        this.responseContentType = contentType;
        return this;
    }

    @Override
    public Route handler(Route.Handler8<T1, T2, T3, T4, T5, T6, T7, T8> handler) {
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
