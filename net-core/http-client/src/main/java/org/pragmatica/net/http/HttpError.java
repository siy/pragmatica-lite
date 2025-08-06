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
import org.pragmatica.lang.Option;

import java.util.Objects;

/// HTTP error representation implementing Cause for integration with Result type
public final class HttpError implements Cause {
    private final int statusCode;
    private final String statusText;
    private final String body;
    private final Option<Cause> source;
    
    private HttpError(int statusCode, String statusText, String body, Option<Cause> source) {
        this.statusCode = statusCode;
        this.statusText = Objects.requireNonNull(statusText, "Status text cannot be null");
        this.body = Objects.requireNonNull(body, "Body cannot be null");
        this.source = Objects.requireNonNull(source, "Source cannot be null");
    }
    
    /// Create HTTP error from status code and text
    public static HttpError httpError(int statusCode, String statusText, String body) {
        return new HttpError(statusCode, statusText, body, Option.empty());
    }
    
    /// Create HTTP error with underlying cause
    public static HttpError httpError(int statusCode, String statusText, String body, Cause source) {
        Objects.requireNonNull(source, "Source cause cannot be null");
        return new HttpError(statusCode, statusText, body, Option.present(source));
    }
    
    /// Create HTTP error from HTTP status
    public static HttpError fromStatus(HttpStatus status) {
        Objects.requireNonNull(status, "Status cannot be null");
        return httpError(status.code(), status.reasonPhrase(), "");
    }
    
    /// Create error for empty body when body was expected
    public static HttpError emptyBody(HttpStatus status) {
        Objects.requireNonNull(status, "Status cannot be null");
        return httpError(status.code(), status.reasonPhrase(), "Response body is empty");
    }
    
    /// Create HTTP error from response
    public static <T> HttpError fromResponse(HttpResponse<T> response) {
        Objects.requireNonNull(response, "Response cannot be null");
        var bodyStr = response.body().fold(() -> "", Object::toString);
        return httpError(response.status().code(), response.status().reasonPhrase(), bodyStr);
    }
    
    public int statusCode() {
        return statusCode;
    }
    
    public String statusText() {
        return statusText;
    }
    
    public String body() {
        return body;
    }
    
    @Override
    public String message() {
        return String.format("HTTP %d %s: %s", statusCode, statusText, body);
    }
    
    @Override
    public Option<Cause> source() {
        return source;
    }
    
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    @Override
    public String toString() {
        return message();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HttpError other)) return false;
        return statusCode == other.statusCode 
            && Objects.equals(statusText, other.statusText)
            && Objects.equals(body, other.body)
            && Objects.equals(source, other.source);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(statusCode, statusText, body, source);
    }
}