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

/// HTTP request methods as defined in RFC 7231 and common extensions.
/// These represent the standardized HTTP verbs used in REST APIs and web applications.
public enum HttpMethod {
    
    /// GET method - retrieve data, should be safe and idempotent
    GET("GET"),
    
    /// POST method - submit data, typically creates new resources
    POST("POST"),
    
    /// PUT method - replace/create resource, should be idempotent
    PUT("PUT"),
    
    /// DELETE method - remove resource, should be idempotent
    DELETE("DELETE"),
    
    /// PATCH method - partial update of resource
    PATCH("PATCH"),
    
    /// HEAD method - like GET but only returns headers, no body
    HEAD("HEAD"),
    
    /// OPTIONS method - describe communication options for the target resource
    OPTIONS("OPTIONS"),
    
    /// TRACE method - diagnostic tool, echoes received request
    TRACE("TRACE");
    
    private final String value;
    
    HttpMethod(String value) {
        this.value = value;
    }
    
    /// Get the string representation of this HTTP method
    public String value() {
        return value;
    }
    
    /// Parse an HTTP method from string (case-insensitive)
    public static HttpMethod fromString(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method cannot be null or blank");
        }
        
        var upperMethod = method.toUpperCase();
        for (var httpMethod : values()) {
            if (httpMethod.value.equals(upperMethod)) {
                return httpMethod;
            }
        }
        
        throw new IllegalArgumentException("Unknown HTTP method: " + method);
    }
    
    /// Check if this method typically allows a request body
    public boolean allowsBody() {
        return switch (this) {
            case POST, PUT, PATCH -> true;
            case GET, DELETE, HEAD, OPTIONS, TRACE -> false;
        };
    }
    
    /// Check if this method should be idempotent (safe to repeat)
    public boolean isIdempotent() {
        return switch (this) {
            case GET, PUT, DELETE, HEAD, OPTIONS, TRACE -> true;
            case POST, PATCH -> false;
        };
    }
    
    /// Check if this method should be safe (no side effects)
    public boolean isSafe() {
        return switch (this) {
            case GET, HEAD, OPTIONS, TRACE -> true;
            case POST, PUT, DELETE, PATCH -> false;
        };
    }
    
    @Override
    public String toString() {
        return value;
    }
}