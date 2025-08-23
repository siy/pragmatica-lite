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

import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpResponse;
import org.pragmatica.net.http.HttpStatus;

/// Implementation of HttpResponse interface.
/// Immutable representation of an HTTP response with status, headers, and body.
public record HttpResponseImpl<T>(
    HttpStatus status,
    HttpHeaders headers,
    T body
) implements HttpResponse<T> {
    
    public HttpResponseImpl {
        if (status == null) {
            throw new IllegalArgumentException("HTTP status cannot be null");
        }
        if (headers == null) {
            throw new IllegalArgumentException("Headers cannot be null");
        }
        // body can be null for responses with no content
    }
    
    /// Create a response with the given components
    public static <T> HttpResponse<T> of(HttpStatus status, HttpHeaders headers, T body) {
        return new HttpResponseImpl<>(status, headers, body);
    }
    
    /// Create a response with status and body, empty headers
    public static <T> HttpResponse<T> of(HttpStatus status, T body) {
        return new HttpResponseImpl<>(status, new HttpHeaders(), body);
    }
}