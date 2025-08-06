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

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

/// HTTP response with optional body.
/// The body is wrapped in Option to handle cases where no response body is expected or received.
public interface HttpResponse<T> {
    
    /// HTTP status of the response
    HttpStatus status();
    
    /// HTTP headers of the response
    HttpHeaders headers();
    
    /// Response body (may be absent for responses like 204 No Content)
    Option<T> body();
    
    // === Convenience Methods ===
    
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
    
    /// Convert response to Result, considering both HTTP status and body presence
    default Result<T> result() {
        if (isSuccess()) {
            return body().fold(
                () -> Result.failure(HttpError.emptyBody(status())),
                Result::success
            );
        } else {
            return Result.failure(HttpError.fromStatus(status()));
        }
    }
    
    /// Get the body or throw an exception if not present
    default T bodyOrThrow() {
        return body().fold(
            () -> { throw new IllegalStateException("Response body is not present"); },
            value -> value
        );
    }
    
    /// Get the body or return a default value if not present
    default T bodyOrElse(T defaultValue) {
        return body().or(defaultValue);
    }
}