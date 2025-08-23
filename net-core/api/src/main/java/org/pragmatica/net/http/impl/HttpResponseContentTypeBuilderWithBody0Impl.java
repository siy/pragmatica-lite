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

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.List;
import java.util.Map;

/// Implementation of HttpResponseContentTypeBuilderWithBody0 for building functions with body and no path variables.
public record HttpResponseContentTypeBuilderWithBody0Impl<B, R>(
    HttpClient client,
    String baseUrl,
    List<String> pathSegments,
    UrlBuilder urlBuilder,
    HttpClientConfig config,
    ContentType requestContentType,
    HttpMethod method,
    TypeToken<B> bodyType,
    TypeToken<R> responseType
) implements HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> {
    
    public HttpResponseContentTypeBuilderWithBody0Impl {
        // Ensure immutable collections
        pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
    }
    
    @Override
    public Fn1<Promise<R>, B> as(ContentType responseContentType) {
        return (body) -> {
            // Build the URL
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, Map.of());
            
            // Create headers with content types
            var headers = new HttpHeaders();
            if (config != null && config.defaultHeaders() != null) {
                for (var name : config.defaultHeaders().names()) {
                    for (var value : config.defaultHeaders().all(name)) {
                        headers.add(name, value);
                    }
                }
            }
            
            // Set Content-Type header for request body
            if (requestContentType != null) {
                headers.set("Content-Type", requestContentType.toString());
            }
            
            // Set Accept header for response
            if (responseContentType != null) {
                headers.set("Accept", responseContentType.toString());
            }
            
            // Create request with body
            var request = new HttpRequestImpl<>(body, responseType, url, method, headers, client);
            
            // Execute request and extract body
            return client.exchange(request).map(HttpResponse::body);
        };
    }
}