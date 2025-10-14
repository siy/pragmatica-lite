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

/// Route builder for 5 parameters.
final class RouteBuilder5<T1, T2, T3, T4, T5> implements Route.PathStage5<T1, T2, T3, T4, T5>, Route.HandlerStage5<T1, T2, T3, T4, T5> {
    private final RouteBuilder builder;

    RouteBuilder5(RouteBuilder builder) {
        this.builder = builder;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> path(String segment) {
        builder.addSegment(segment);
        return this;
    }

    @Override
    public <T6> Route.PathStage6<T1, T2, T3, T4, T5, T6> addParam(ParameterType type, String name, TypeToken<T6> token) {
        return new RouteBuilder6<>(builder.withParameter(type, name, token));
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> method(HttpMethod httpMethod) {
        builder.method(httpMethod);
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> in(ContentType contentType) {
        builder.in(contentType);
        return this;
    }

    @Override
    public RouteBuilder5<T1, T2, T3, T4, T5> out(ContentType contentType) {
        builder.out(contentType);
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler5<T1, T2, T3, T4, T5> handler) {
        return builder.buildWithHandler(handler);
    }

    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
        throw new UnsupportedOperationException("Builder cannot match requests");
    }
}
