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

package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

/// HTTP request interface with bi-generic typing for request body (T) and response body (R) types.
/// This provides compile-time type safety for both request payload and expected response format.
public interface HttpRequest<T, R> {
    
    /// Get the complete URL for this request
    String url();
    
    /// Get the HTTP method for this request
    HttpMethod method();
    
    /// Get all headers for this request
    HttpHeaders headers();
    
    /// Get the request body (may be null for methods like GET)
    T body();
    
    /// Get the expected response type information for deserialization
    TypeToken<R> expectedType();
    
    /// Send this request using the associated client and return a Promise of the response
    Promise<HttpResponse<R>> send();
    
    /// Create a new request builder
    static HttpRequestBuilder builder() {
        return HttpRequestBuilder.create();
    }
}