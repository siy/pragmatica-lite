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

package org.pragmatica.http;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

/**
 * HTTP request methods.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    TRACE,
    CONNECT;
    /**
     * Parse HTTP method from string.
     *
     * @param method HTTP method string
     * @return Result containing the HttpMethod or an error for unknown methods
     */
    public static Result<HttpMethod> from(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> Result.success(GET);
            case "POST" -> Result.success(POST);
            case "PUT" -> Result.success(PUT);
            case "DELETE" -> Result.success(DELETE);
            case "PATCH" -> Result.success(PATCH);
            case "HEAD" -> Result.success(HEAD);
            case "OPTIONS" -> Result.success(OPTIONS);
            case "TRACE" -> Result.success(TRACE);
            case "CONNECT" -> Result.success(CONNECT);
            default -> new UnknownMethod(method).result();
        };
    }
    /// Error for unknown HTTP method.
    public record UnknownMethod(String method) implements Cause {
        @Override
        public String message() {
            return "Unknown HTTP method: " + method;
        }
    }
}
