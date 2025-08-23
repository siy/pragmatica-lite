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

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

/// Implementation of HttpRequest interface.
/// Immutable representation of an HTTP request with all necessary information for execution.
public record HttpRequestImpl<T, R>(
    T body,
    TypeToken<R> expectedType,
    String url,
    HttpMethod method,
    HttpHeaders headers,
    HttpClient client
) implements HttpRequest<T, R> {
    
    public HttpRequestImpl {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }
        if (method == null) {
            throw new IllegalArgumentException("HTTP method cannot be null");
        }
        if (headers == null) {
            throw new IllegalArgumentException("Headers cannot be null");
        }
        if (expectedType == null) {
            throw new IllegalArgumentException("Expected type cannot be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("HttpClient cannot be null");
        }
    }
    
    @Override
    public Promise<HttpResponse<R>> send() {
        return client.exchange(this);
    }
}