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

import org.pragmatica.lang.Cause;

/// Sealed interface representing HTTP errors organized by status code groups
public sealed interface HttpError extends Cause 
    permits HttpError.Success, HttpError.Redirection, HttpError.ClientError, HttpError.ServerError {
    
    /// Returns the HTTP status code as enum
    HttpStatusCode code();
    
    /// Returns the status text
    String text();
    
    /// Default message implementation using enum's default message
    default String message() {
        return String.format("HTTP %d %s", code().code(), code().defaultMessage());
    }
    
    /// Factory method to create HttpError from status code
    static HttpError fromCode(int statusCode, String statusText) {
        var code = HttpStatusCode.fromCode(statusCode);
        return switch (statusCode / 100) {
            case 2 -> new SuccessError(statusText, code);
            case 3 -> new RedirectionError(statusText, code);
            case 4 -> new ClientErrorImpl(statusText, code);
            case 5 -> new ServerErrorImpl(statusText, code);
            default -> new ServerErrorImpl(statusText, HttpStatusCode.INTERNAL_SERVER_ERROR);
        };
    }
    
    /// Factory method from response
    static <T> HttpError fromResponse(HttpResponse<T> response) {
        return fromCode(response.statusCode(), response.statusText());
    }
    
    // === Sealed Interface Implementations ===
    
    /// 2xx Success responses that still represent errors in business logic
    sealed interface Success extends HttpError permits SuccessError {
    }
    
    /// 3xx Redirection responses  
    sealed interface Redirection extends HttpError permits RedirectionError {
    }
    
    /// 4xx Client Error responses
    sealed interface ClientError extends HttpError permits ClientErrorImpl {
    }
    
    /// 5xx Server Error responses
    sealed interface ServerError extends HttpError permits ServerErrorImpl {
    }
    
    // === Record Implementations ===
    
    /// Success error implementation
    record SuccessError(String text, HttpStatusCode statusCode) implements Success {
        @Override
        public HttpStatusCode code() {
            return statusCode;
        }
    }
    
    /// Redirection error implementation  
    record RedirectionError(String text, HttpStatusCode statusCode) implements Redirection {
        @Override
        public HttpStatusCode code() {
            return statusCode;
        }
    }
    
    /// Client error implementation
    record ClientErrorImpl(String text, HttpStatusCode statusCode) implements ClientError {
        @Override
        public HttpStatusCode code() {
            return statusCode;
        }
    }
    
    /// Server error implementation
    record ServerErrorImpl(String text, HttpStatusCode statusCode) implements ServerError {
        @Override
        public HttpStatusCode code() {
            return statusCode;
        }
    }
}