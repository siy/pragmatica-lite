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

/// Route builder for 8 parameters.
final class RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> implements Route.PathStage8<T1, T2, T3, T4, T5, T6, T7, T8>, Route.HandlerStage8<T1, T2, T3, T4, T5, T6, T7, T8> {
    private final RouteBuilder builder;

    RouteBuilder8(RouteBuilder builder) {
        this.builder = builder;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> path(String segment) {
        builder.addSegment(segment);
        return this;
    }

    @Override
    public <T9> Route.PathStage9<T1, T2, T3, T4, T5, T6, T7, T8, T9> addParam(ParameterType type, String name, TypeToken<T9> token) {
        return new RouteBuilder9<>(builder.withParameter(type, name, token));
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> method(HttpMethod httpMethod) {
        builder.method(httpMethod);
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> in(ContentType contentType) {
        builder.in(contentType);
        return this;
    }

    @Override
    public RouteBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> out(ContentType contentType) {
        builder.out(contentType);
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler8<T1, T2, T3, T4, T5, T6, T7, T8> handler) {
        return builder.buildWithHandler(handler);
    }

    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
        throw new UnsupportedOperationException("Builder cannot match requests");
    }
}
