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

/// HTTP response interface providing access to status, headers, and typed response body.
/// The generic type T represents the deserialized response body type.
public interface HttpResponse<T> {
    
    /// Get the HTTP status code and reason phrase
    HttpStatus status();
    
    /// Get all response headers
    HttpHeaders headers();
    
    /// Get the deserialized response body
    /// May be null for responses with no body or void responses
    T body();
    
    /// Check if the response indicates success (2xx status codes)
    default boolean isSuccess() {
        return status().isSuccess();
    }
    
    /// Check if the response indicates a client error (4xx status codes)
    default boolean isClientError() {
        return status().isClientError();
    }
    
    /// Check if the response indicates a server error (5xx status codes)
    default boolean isServerError() {
        return status().isServerError();
    }
    
    /// Check if the response indicates any kind of error (4xx or 5xx status codes)
    default boolean isError() {
        return isClientError() || isServerError();
    }
}