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
public sealed interface HttpError extends Cause {
    
    /// Returns the HTTP status code as enum
    HttpStatusCode code();
    
    /// Returns the status text
    String text();
    
    /// Default message implementation using enum's default message
    default String message() {
        return String.format("HTTP %d %s", code().code(), code().defaultMessage());
    }
    
    /// Factory method to create HttpError from status code using functional chaining
    static HttpError fromCode(int statusCode, String statusText) {
        return HttpStatusCode.fromCode(statusCode)
                           .map(code -> code.asError(statusText))
                           .recover(error -> UnknownStatusCode.create(statusCode, statusText));
    }
    
    /// Factory method from response
    static <T> HttpError fromResponse(HttpResponse<T> response) {
        return response.status().asError(response.status().defaultMessage());
    }
    
    // === Sealed Interface Groups ===
    
    /// 2xx Success responses that still represent errors in business logic
    sealed interface Success extends HttpError {
        
        record Ok(String text) implements Success {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.OK; }
        }
        
        record Created(String text) implements Success {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.CREATED; }
        }
        
        record Accepted(String text) implements Success {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.ACCEPTED; }
        }
        
        record NoContent(String text) implements Success {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.NO_CONTENT; }
        }
        
        record ResetContent(String text) implements Success {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.RESET_CONTENT; }
        }
        
        record PartialContent(String text) implements Success {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.PARTIAL_CONTENT; }
        }
    }
    
    /// 3xx Redirection responses  
    sealed interface Redirection extends HttpError {
        
        record MultipleChoices(String text) implements Redirection {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.MULTIPLE_CHOICES; }
        }
        
        record MovedPermanently(String text) implements Redirection {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.MOVED_PERMANENTLY; }
        }
        
        record Found(String text) implements Redirection {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.FOUND; }
        }
        
        record SeeOther(String text) implements Redirection {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.SEE_OTHER; }
        }
        
        record NotModified(String text) implements Redirection {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.NOT_MODIFIED; }
        }
        
        record TemporaryRedirect(String text) implements Redirection {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.TEMPORARY_REDIRECT; }
        }
        
        record PermanentRedirect(String text) implements Redirection {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.PERMANENT_REDIRECT; }
        }
    }
    
    /// 4xx Client Error responses
    sealed interface ClientError extends HttpError {
        
        record BadRequest(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.BAD_REQUEST; }
        }
        
        record Unauthorized(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.UNAUTHORIZED; }
        }
        
        record PaymentRequired(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.PAYMENT_REQUIRED; }
        }
        
        record Forbidden(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.FORBIDDEN; }
        }
        
        record NotFound(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.NOT_FOUND; }
        }
        
        record MethodNotAllowed(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.METHOD_NOT_ALLOWED; }
        }
        
        record NotAcceptable(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.NOT_ACCEPTABLE; }
        }
        
        record ProxyAuthenticationRequired(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.PROXY_AUTHENTICATION_REQUIRED; }
        }
        
        record RequestTimeout(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.REQUEST_TIMEOUT; }
        }
        
        record Conflict(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.CONFLICT; }
        }
        
        record Gone(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.GONE; }
        }
        
        record LengthRequired(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.LENGTH_REQUIRED; }
        }
        
        record PreconditionFailed(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.PRECONDITION_FAILED; }
        }
        
        record PayloadTooLarge(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.PAYLOAD_TOO_LARGE; }
        }
        
        record UriTooLong(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.URI_TOO_LONG; }
        }
        
        record UnsupportedMediaType(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.UNSUPPORTED_MEDIA_TYPE; }
        }
        
        record RangeNotSatisfiable(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.RANGE_NOT_SATISFIABLE; }
        }
        
        record ExpectationFailed(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.EXPECTATION_FAILED; }
        }
        
        record ImATeapot(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.IM_A_TEAPOT; }
        }
        
        record UnprocessableEntity(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.UNPROCESSABLE_ENTITY; }
        }
        
        record TooManyRequests(String text) implements ClientError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.TOO_MANY_REQUESTS; }
        }
    }
    
    /// 5xx Server Error responses
    sealed interface ServerError extends HttpError {
        
        record InternalServerError(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.INTERNAL_SERVER_ERROR; }
        }
        
        record NotImplemented(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.NOT_IMPLEMENTED; }
        }
        
        record BadGateway(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.BAD_GATEWAY; }
        }
        
        record ServiceUnavailable(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.SERVICE_UNAVAILABLE; }
        }
        
        record GatewayTimeout(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.GATEWAY_TIMEOUT; }
        }
        
        record HttpVersionNotSupported(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED; }
        }
        
        record InsufficientStorage(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.INSUFFICIENT_STORAGE; }
        }
        
        record LoopDetected(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.LOOP_DETECTED; }
        }
        
        record NetworkAuthenticationRequired(String text) implements ServerError {
            @Override
            public HttpStatusCode code() { return HttpStatusCode.NETWORK_AUTHENTICATION_REQUIRED; }
        }
    }
    
    /// Unknown status code error
    record UnknownStatusCode(int statusCode, String text) implements HttpError {
        @Override
        public HttpStatusCode code() {
            // Use a default fallback since the code is unknown
            return HttpStatusCode.INTERNAL_SERVER_ERROR;
        }
        
        @Override
        public String message() {
            return String.format("HTTP %d %s (Unknown Status Code)", statusCode, text);
        }
        
        public static UnknownStatusCode create(int statusCode) {
            return new UnknownStatusCode(statusCode, "Unknown Status Code");
        }
        
        public static UnknownStatusCode create(int statusCode, String text) {
            return new UnknownStatusCode(statusCode, text);
        }
    }
}