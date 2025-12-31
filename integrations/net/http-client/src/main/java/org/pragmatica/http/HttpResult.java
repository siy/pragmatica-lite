/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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
 *
 */

package org.pragmatica.http;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;

/// Typed HTTP response wrapper containing status, headers, and body.
///
/// @param statusCode HTTP status code
/// @param headers Response headers
/// @param body Response body (typed)
/// @param <T> Body type
public record HttpResult<T>(int statusCode, HttpHeaders headers, T body) {
    /// Creates HttpResult from JDK HttpResponse.
    ///
    /// @param response JDK HttpResponse
    /// @param <T> Body type
    ///
    /// @return HttpResult wrapping the response
    public static <T> HttpResult<T> from(HttpResponse<T> response) {
        return new HttpResult<>(response.statusCode(), response.headers(), response.body());
    }

    /// Checks if the response indicates success (2xx status code).
    ///
    /// @return true if status code is 2xx
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /// Checks if the response indicates client error (4xx status code).
    ///
    /// @return true if status code is 4xx
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /// Checks if the response indicates server error (5xx status code).
    ///
    /// @return true if status code is 5xx
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /// Gets the first value for a header.
    ///
    /// @param name Header name
    ///
    /// @return First header value, or empty if not present
    public Option<String> header(String name) {
        return Option.option(headers.firstValue(name)
                                    .orElse(null));
    }

    /// Gets all values for a header.
    ///
    /// @param name Header name
    ///
    /// @return List of header values
    public List<String> headerValues(String name) {
        return headers.allValues(name);
    }

    /// Converts to Result, failing if status code indicates error.
    ///
    /// @return Success with body if 2xx, Failure with HttpError otherwise
    public Result<T> toResult() {
        if (isSuccess()) {
            return Result.success(body);
        }
        return new HttpError.RequestFailed(statusCode, statusMessage()).result();
    }

    /// Gets human-readable status message.
    ///
    /// @return Status message based on status code
    public String statusMessage() {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "HTTP " + statusCode;
        };
    }
}
