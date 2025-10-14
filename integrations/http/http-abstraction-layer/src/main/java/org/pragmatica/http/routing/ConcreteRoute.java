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

import java.util.List;

final class ConcreteRoute implements Route {
    private final String path;
    private final String method;
    private final List<ParameterSpec> parameters;
    private final ContentType requestContentType;
    private final ContentType responseContentType;
    private final Object handler;

    ConcreteRoute(
        String path,
        String method,
        List<ParameterSpec> parameters,
        ContentType requestContentType,
        ContentType responseContentType,
        Object handler
    ) {
        this.path = path;
        this.method = method;
        this.parameters = parameters;
        this.requestContentType = requestContentType;
        this.responseContentType = responseContentType;
        this.handler = handler;
    }

    String path() { return path; }
    String method() { return method; }
    List<ParameterSpec> parameters() { return parameters; }
    ContentType requestContentType() { return requestContentType; }
    ContentType responseContentType() { return responseContentType; }
    Object handler() { return handler; }


    @Override
    public Promise<Option<HttpResponse>> match(HttpRequest request) {
        return Promise.success(Option.none());
    }
}
