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

/// Route builder for 1 parameter.
final class RouteBuilder1<T1> implements Route.PathStage1<T1>, Route.HandlerStage1<T1> {
    private final List<String> pathSegments;
    private final List<ParameterSpec> parameters;
    private HttpMethod method;
    private ContentType requestContentType = CommonContentType.APPLICATION_JSON;
    private ContentType responseContentType = CommonContentType.APPLICATION_JSON;
    private Object handler;

    RouteBuilder1(List<String> pathSegments, List<ParameterSpec> parameters) {
        this.pathSegments = new ArrayList<>(pathSegments);
        this.parameters = new ArrayList<>(parameters);
    }

    @Override
    public RouteBuilder1<T1> path(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Override
    public <T2> Route.PathStage2<T1, T2> addParam(ParameterType type, String name, TypeToken<T2> token) {
        var newParams = new ArrayList<>(parameters);
        newParams.add(new ParameterSpec(type, name, token));
        return new RouteBuilder2<>(pathSegments, newParams);
    }

    @Override
    public RouteBuilder1<T1> method(HttpMethod httpMethod) {
        this.method = httpMethod;
        return this;
    }

    @Override
    public RouteBuilder1<T1> in(ContentType contentType) {
        this.requestContentType = contentType;
        return this;
    }

    @Override
    public RouteBuilder1<T1> out(ContentType contentType) {
        this.responseContentType = contentType;
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler1<T1> handler) {
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
