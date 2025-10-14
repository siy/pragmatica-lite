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

/// Route builder for 7 parameters.
final class RouteBuilder7<T1, T2, T3, T4, T5, T6, T7> implements Route.PathStage7<T1, T2, T3, T4, T5, T6, T7>, Route.HandlerStage7<T1, T2, T3, T4, T5, T6, T7> {
    private final RouteBuilder builder;

    RouteBuilder7(RouteBuilder builder) {
        this.builder = builder;
    }

    @Override
    public RouteBuilder7<T1, T2, T3, T4, T5, T6, T7> path(String segment) {
        builder.addSegment(segment);
        return this;
    }

    @Override
    public <T8> Route.PathStage8<T1, T2, T3, T4, T5, T6, T7, T8> addParam(ParameterType type, String name, TypeToken<T8> token) {
        return new RouteBuilder8<>(builder.withParameter(type, name, token));
    }

    @Override
    public RouteBuilder7<T1, T2, T3, T4, T5, T6, T7> method(HttpMethod httpMethod) {
        builder.method(httpMethod);
        return this;
    }

    @Override
    public RouteBuilder7<T1, T2, T3, T4, T5, T6, T7> in(ContentType contentType) {
        builder.in(contentType);
        return this;
    }

    @Override
    public RouteBuilder7<T1, T2, T3, T4, T5, T6, T7> out(ContentType contentType) {
        builder.out(contentType);
        return this;
    }

    @Override
    public RouteMatcher handler(Route.Handler7<T1, T2, T3, T4, T5, T6, T7> handler) {
        return builder.buildWithHandler(handler);
    }

    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
        throw new UnsupportedOperationException("Builder cannot match requests");
    }
}
