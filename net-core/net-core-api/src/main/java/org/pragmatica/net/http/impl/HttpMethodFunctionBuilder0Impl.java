/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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
 */

package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.List;
import java.util.Map;

/// Implementation of HttpMethodFunctionBuilder0 for functions with no path variables
public record HttpMethodFunctionBuilder0Impl(HttpClient client, String baseUrl, List<String> pathSegments,
                                             UrlBuilder urlBuilder, String contentType)
        implements HttpFunction.HttpMethodFunctionBuilder0 {

    public HttpMethodFunctionBuilder0Impl {
    }

    @Override
    public <R> Fn0<Promise<R>> method(HttpMethod method, Class<R> responseType) {
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, Map.of());
            return client.request()
                         .url(url)
                         .method(method)
                         .header("Content-Type", contentType)
                         .responseType(responseType)
                         .send()
                         .map(HttpResponse::body)
                         .flatMap(Option::async);
        };
    }

    @Override
    public <R> Fn0<Promise<R>> method(HttpMethod method, TypeToken<R> responseType) {
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, Map.of());
            return client.request()
                         .url(url)
                         .method(method)
                         .header("Content-Type", contentType)
                         .responseType(responseType)
                         .send()
                         .map(HttpResponse::body)
                         .flatMap(Option::async);
        };
    }
}