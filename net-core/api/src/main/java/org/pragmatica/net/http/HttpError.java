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

/// HTTP-specific error types for the HTTP client.
/// Extends the core Cause interface to provide structured error handling for HTTP operations.
public interface HttpError extends Cause {
    
    /// HTTP status-related errors
    record HttpStatusError(HttpStatus status, String message, Throwable cause) implements HttpError {
        
        public HttpStatusError(HttpStatus status, String message) {
            this(status, message, null);
        }
        
        @Override
        public String description() {
            return message != null ? message : "HTTP error: " + status;
        }
        
        @Override
        public Throwable cause() {
            return cause;
        }
        
        public static HttpStatusError create(HttpStatus status, String message) {
            return new HttpStatusError(status, message);
        }
        
        public static HttpStatusError create(HttpStatus status, String message, Throwable cause) {
            return new HttpStatusError(status, message, cause);
        }
    }
    
    /// Network-related errors (connection, timeout, etc.)
    record NetworkError(String message, Throwable cause) implements HttpError {
        
        public NetworkError(String message) {
            this(message, null);
        }
        
        @Override
        public String description() {
            return message != null ? message : "Network error";
        }
        
        @Override
        public Throwable cause() {
            return cause;
        }
        
        public static NetworkError create(String message) {
            return new NetworkError(message);
        }
        
        public static NetworkError create(String message, Throwable cause) {
            return new NetworkError(message, cause);
        }
    }
    
    /// Serialization/deserialization errors
    record SerializationError(String message, Throwable cause) implements HttpError {
        
        public SerializationError(String message) {
            this(message, null);
        }
        
        @Override
        public String description() {
            return message != null ? message : "Serialization error";
        }
        
        @Override
        public Throwable cause() {
            return cause;
        }
        
        public static SerializationError create(String message) {
            return new SerializationError(message);
        }
        
        public static SerializationError create(String message, Throwable cause) {
            return new SerializationError(message, cause);
        }
    }
    
    /// Configuration errors
    record ConfigurationError(String message, Throwable cause) implements HttpError {
        
        public ConfigurationError(String message) {
            this(message, null);
        }
        
        @Override
        public String description() {
            return message != null ? message : "Configuration error";
        }
        
        @Override
        public Throwable cause() {
            return cause;
        }
        
        public static ConfigurationError create(String message) {
            return new ConfigurationError(message);
        }
        
        public static ConfigurationError create(String message, Throwable cause) {
            return new ConfigurationError(message, cause);
        }
    }
    
    /// Timeout errors
    record TimeoutError(String message, Throwable cause) implements HttpError {
        
        public TimeoutError(String message) {
            this(message, null);
        }
        
        @Override
        public String description() {
            return message != null ? message : "Request timeout";
        }
        
        @Override
        public Throwable cause() {
            return cause;
        }
        
        public static TimeoutError create(String message) {
            return new TimeoutError(message);
        }
        
        public static TimeoutError create(String message, Throwable cause) {
            return new TimeoutError(message, cause);
        }
    }
    
    /// Unknown or unspecified HTTP status code errors
    record UnknownStatusCode(int statusCode, String message, Throwable cause) implements HttpError {
        
        public UnknownStatusCode(int statusCode, String message) {
            this(statusCode, message, null);
        }
        
        @Override
        public String description() {
            return message != null ? message : "Unknown status code: " + statusCode;
        }
        
        @Override
        public Throwable cause() {
            return cause;
        }
        
        public static UnknownStatusCode create(int statusCode, String message) {
            return new UnknownStatusCode(statusCode, message);
        }
        
        public static UnknownStatusCode create(int statusCode, String message, Throwable cause) {
            return new UnknownStatusCode(statusCode, message, cause);
        }
    }
}