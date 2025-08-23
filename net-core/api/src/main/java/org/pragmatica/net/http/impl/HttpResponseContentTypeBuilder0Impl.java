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

import org.pragmatica.lang.Functions.Fn0;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.List;
import java.util.Map;

/// Implementation of HttpResponseContentTypeBuilder0 for building functions with no path variables.
public record HttpResponseContentTypeBuilder0Impl<R>(
    HttpClient client,
    String baseUrl,
    List<String> pathSegments,
    UrlBuilder urlBuilder,
    HttpClientConfig config,
    ContentType requestContentType,
    HttpMethod method,
    TypeToken<?> bodyType,
    TypeToken<R> responseType
) implements HttpFunction.HttpResponseContentTypeBuilder0<R> {
    
    public HttpResponseContentTypeBuilder0Impl {
        // Ensure immutable collections
        pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
    }
    
    @Override
    public Fn0<Promise<R>> as(ContentType responseContentType) {
        return () -> {
            // Build the URL
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, Map.of());
            
            // Create headers with content type
            var headers = new HttpHeaders();
            if (config != null && config.defaultHeaders() != null) {
                for (var name : config.defaultHeaders().names()) {
                    for (var value : config.defaultHeaders().all(name)) {
                        headers.add(name, value);
                    }
                }
            }
            
            // Set Accept header based on response content type
            if (responseContentType != null) {
                headers.set("Accept", responseContentType.toString());
            }
            
            // Create request
            var request = new HttpRequestImpl<>(null, responseType, url, method, headers, client);
            
            // Execute request and extract body
            return client.exchange(request).map(HttpResponse::body);
        };
    }
}