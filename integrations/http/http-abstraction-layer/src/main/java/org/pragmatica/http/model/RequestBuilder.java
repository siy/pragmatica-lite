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

import org.pragmatica.http.serialization.Deserializer;
import org.pragmatica.http.serialization.Serializer;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/// Fluent builder for constructing HttpRequest instances.
/// Request ID is assigned at creation time and cannot be modified.
public final class RequestBuilder {
    private final String requestId;
    private final Deserializer deserializer;
    private final Serializer serializer;
    private String method;
    private String path;
    private final Map<HttpHeaderName, String> headers = new HashMap<>();
    private final Map<String, List<String>> queryParams = new HashMap<>();
    private byte[] body = new byte[0];

    private RequestBuilder(String requestId, Deserializer deserializer, Serializer serializer) {
        this.requestId = requestId;
        this.deserializer = deserializer;
        this.serializer = serializer;
    }

    /// Create a new RequestBuilder with the specified request ID and serialization components.
    ///
    /// @param requestId unique request identifier
    /// @param deserializer deserializer for request bodies
    /// @param serializer serializer for response bodies
    /// @return RequestBuilder instance
    public static RequestBuilder builder(String requestId, Deserializer deserializer, Serializer serializer) {
        return new RequestBuilder(requestId, deserializer, serializer);
    }

    /// Get the pre-assigned request ID.
    ///
    /// @return request ID
    public String requestId() {
        return requestId;
    }

    /// Set the HTTP method.
    ///
    /// @param method HTTP method (GET, POST, PUT, DELETE, etc.)
    /// @return this builder
    public RequestBuilder method(String method) {
        this.method = method;
        return this;
    }

    /// Set the request path.
    ///
    /// @param path request path
    /// @return this builder
    public RequestBuilder path(String path) {
        this.path = path;
        return this;
    }

    /// Add a single header.
    ///
    /// @param name header name
    /// @param value header value
    /// @return this builder
    public RequestBuilder header(HttpHeaderName name, String value) {
        this.headers.put(name, value);
        return this;
    }

    /// Add a single header by string name.
    ///
    /// @param name header name (normalized to lowercase)
    /// @param value header value
    /// @return this builder
    public RequestBuilder header(String name, String value) {
        HttpHeaderName headerName = CommonHttpHeaderName.fromString(name);
        if (headerName == null) {
            headerName = HttpHeaderName.of(name);
        }
        this.headers.put(headerName, value);
        return this;
    }

    /// Add multiple headers from string map.
    ///
    /// @param headers map of header names to values
    /// @return this builder
    public RequestBuilder headers(Map<String, String> headers) {
        headers.forEach(this::header);
        return this;
    }

    /// Add a single query parameter.
    ///
    /// @param name parameter name
    /// @param value parameter value
    /// @return this builder
    public RequestBuilder queryParam(String name, String value) {
        this.queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    /// Add multiple query parameters.
    ///
    /// @param params map of parameter names to value lists
    /// @return this builder
    public RequestBuilder queryParams(Map<String, List<String>> params) {
        params.forEach((k, v) -> this.queryParams.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));
        return this;
    }

    /// Set the request body from bytes.
    ///
    /// @param body request body
    /// @return this builder
    public RequestBuilder body(byte[] body) {
        this.body = body;
        return this;
    }

    /// Set the request body from an InputStream.
    /// Reads all bytes from the stream.
    ///
    /// @param inputStream input stream containing request body
    /// @return this builder
    public RequestBuilder body(InputStream inputStream) {
        try {
            this.body = inputStream.readAllBytes();
        } catch (IOException e) {
            // Store empty body on error - validation will happen in build()
            this.body = new byte[0];
        }
        return this;
    }

    /// Build the HttpRequest.
    /// Validates that method and path are set.
    ///
    /// @return Result containing HttpRequest, or error if validation fails
    public Result<HttpRequest> build() {
        return Verify.ensure(method, Verify.Is::notNull)
            .flatMap(_ -> Verify.ensure(path, Verify.Is::notNull))
            .map(_ -> new HttpRequest(
                requestId,
                method,
                path,
                new HttpHeaders(headers),
                QueryParameters.fromMap(queryParams),
                Cookies.fromHeaders(new HttpHeaders(headers)),
                body,
                deserializer
            ));
    }
}
