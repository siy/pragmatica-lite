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

package org.pragmatica.http.model;

import org.pragmatica.http.serialization.Serializer;
import org.pragmatica.lang.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Fluent builder for constructing HttpResponse instances.
/// All methods return Result for consistent error propagation.
public final class ResponseBuilder {
    private final String requestId;
    private final Serializer serializer;
    private int status = 200;
    private final Map<String, List<String>> headers = new HashMap<>();
    private byte[] body = new byte[0];

    private ResponseBuilder(String requestId, Serializer serializer) {
        this.requestId = requestId;
        this.serializer = serializer;
    }

    /// Create a new ResponseBuilder with the specified request ID and serializer.
    ///
    /// @param requestId request correlation ID
    /// @param serializer serializer for response bodies
    /// @return Result containing ResponseBuilder
    public static Result<ResponseBuilder> builder(String requestId, Serializer serializer) {
        return Result.success(new ResponseBuilder(requestId, serializer));
    }

    /// Get the request ID.
    ///
    /// @return request ID
    public String requestId() {
        return requestId;
    }

    /// Set the HTTP status code.
    ///
    /// @param status HTTP status code
    /// @return Result containing this builder
    public Result<ResponseBuilder> status(int status) {
        this.status = status;
        return Result.success(this);
    }

    /// Add a single header.
    ///
    /// @param name header name
    /// @param value header value
    /// @return Result containing this builder
    public Result<ResponseBuilder> header(String name, String value) {
        this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return Result.success(this);
    }

    /// Set the Content-Type header.
    ///
    /// @param mimeType content type (e.g., "application/json")
    /// @return Result containing this builder
    public Result<ResponseBuilder> contentType(String mimeType) {
        return header("Content-Type", mimeType);
    }

    /// Serialize an object to JSON and set as response body.
    /// Also sets Content-Type to application/json.
    ///
    /// @param obj object to serialize
    /// @return Result containing this builder, or error if serialization fails
    public Result<ResponseBuilder> json(Object obj) {
        return serializer.serialize(obj)
            .map(bytes -> {
                this.body = bytes;
                return this;
            })
            .flatMap(self -> contentType("application/json"));
    }

    /// Set the response body from bytes.
    ///
    /// @param body response body
    /// @return Result containing this builder
    public Result<ResponseBuilder> body(byte[] body) {
        this.body = body;
        return Result.success(this);
    }

    /// Build the HttpResponse.
    ///
    /// @return HttpResponse instance
    public HttpResponse build() {
        return new HttpResponse(requestId, status, Map.copyOf(headers), body);
    }
}
