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
import org.pragmatica.http.model.HttpRequest;
import org.pragmatica.http.model.HttpResponse;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;

/// Route builder for 0 parameters.
final class RouteBuilder0 implements Route.PathStage0, Route.HandlerStage0 {
    private final RouteBuilder builder = new RouteBuilder();

    private RouteBuilder0() {
    }

    static RouteBuilder0 builder() {
        return new RouteBuilder0();
    }

    @Override
    public RouteBuilder0 path(String segment) {
        builder.addSegment(segment);
        return this;
    }

    @Override
    public RouteMatcher subpath(RouteMatcher... routes) {
        return new CompositeRoute(builder.fullPath(), List.of(routes));
    }

    @Override
    public <T1> Route.PathStage1<T1> addParam(ParameterType type, String name, TypeToken<T1> token) {
        return new RouteBuilder1<>(builder.withParameter(type, name, token));
    }

    @Override
    public RouteBuilder0 method(HttpMethod httpMethod) {
        builder.method(httpMethod);
        return this;
    }

    @Override
    public RouteBuilder0 in(ContentType contentType) {
        builder.in(contentType);
        return this;
    }

    @Override
    public RouteBuilder0 out(ContentType contentType) {
        builder.out(contentType);
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler0 handler) {
        return builder.buildWithHandler(handler);
    }

    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
        throw new UnsupportedOperationException("Builder cannot match requests");
    }
}
