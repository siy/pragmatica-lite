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

package org.pragmatica.http.error;

import org.pragmatica.http.model.HttpResponse;
import org.pragmatica.http.model.ResponseBuilder;
import org.pragmatica.lang.Result;

import java.util.Map;

/// Built-in HTTP errors following RFC 7807 Problem Details format.
public enum HttpError implements HttpStatusCause {
    BAD_REQUEST(400, "bad_request", "Bad Request"),
    UNAUTHORIZED(401, "unauthorized", "Unauthorized"),
    FORBIDDEN(403, "forbidden", "Forbidden"),
    NOT_FOUND(404, "not_found", "Not Found"),
    METHOD_NOT_ALLOWED(405, "method_not_allowed", "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "not_acceptable", "Not Acceptable"),
    REQUEST_TIMEOUT(408, "request_timeout", "Request Timeout"),
    CONFLICT(409, "conflict", "Conflict"),
    UNSUPPORTED_MEDIA_TYPE(415, "unsupported_media_type", "Unsupported Media Type"),
    TOO_MANY_REQUESTS(429, "too_many_requests", "Too Many Requests"),
    INTERNAL_SERVER_ERROR(500, "internal_server_error", "Internal Server Error"),
    NOT_IMPLEMENTED(501, "not_implemented", "Not Implemented"),
    SERVICE_UNAVAILABLE(503, "service_unavailable", "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "gateway_timeout", "Gateway Timeout");

    private final int status;
    private final String type;
    private final String title;

    HttpError(int status, String type, String title) {
        this.status = status;
        this.type = type;
        this.title = title;
    }

    @Override
    public int httpStatus() {
        return status;
    }

    @Override
    public Result<ResponseBuilder> fillResponse(ResponseBuilder builder) {
        return builder.status(status)
            .flatMap(b -> b.contentType("application/problem+json"))
            .flatMap(b -> b.json(Map.of(
                "type", type,
                "title", title,
                "status", status,
                "request-id", builder.requestId()
            )));
    }

    @Override
    public String message() {
        return title;
    }

    /// Convert this error to an HttpResponse.
    /// Helper method for router fallback error handling.
    ///
    /// @param requestId request correlation ID
    /// @return Result containing HttpResponse
    public Result<HttpResponse> toResponse(String requestId) {
        return ResponseBuilder.builder(requestId, null)
            .flatMap(this::fillResponse)
            .map(ResponseBuilder::build);
    }
}
