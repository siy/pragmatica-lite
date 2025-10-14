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
import org.pragmatica.lang.type.TypeToken;

import java.util.ArrayList;
import java.util.List;

class RouteBuilder {
    private final List<String> pathSegments = new ArrayList<>();
    private final List<ParameterSpec> parameters = new ArrayList<>();
    private HttpMethod method;
    private ContentType requestContentType = CommonContentType.APPLICATION_JSON;
    private ContentType responseContentType = CommonContentType.APPLICATION_JSON;
    private Route.Handler handler;


    public void addSegment(String segment) {
        pathSegments.add(segment);
    }

    public String fullPath() {
        return String.join("/", pathSegments);
    }

    public <T1> RouteBuilder withParameter(ParameterType type, String name, TypeToken<T1> token) {
        parameters.add(new ParameterSpec(type, name, token));
        return this;
    }

    public void method(HttpMethod httpMethod) {
        this.method = httpMethod;
    }

    public void in(ContentType contentType) {
        this.requestContentType = contentType;
    }

    public void out(ContentType contentType) {
        this.responseContentType = contentType;
    }

    public RouteMatcher buildWithHandler(Route.Handler handler) {
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
