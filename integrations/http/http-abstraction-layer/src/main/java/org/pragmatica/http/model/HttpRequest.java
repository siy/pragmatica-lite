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
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

/// Immutable HTTP request representation.
/// Created by framework adapters and passed through the routing pipeline.
///
/// @param requestId    unique identifier for request correlation
/// @param method       HTTP method (GET, POST, PUT, DELETE, etc.)
/// @param path         request path (/api/v1/users/123)
/// @param headers      HTTP headers
/// @param queryParams  query parameters
/// @param cookies      parsed cookies
/// @param body         request body as bytes
/// @param deserializer deserializer for body content
public record HttpRequest(
    String requestId,
    String method,
    String path,
    HttpHeaders headers,
    QueryParameters queryParams,
    Cookies cookies,
    byte[] body,
    Deserializer deserializer
) {
    /// Get a header value by name.
    ///
    /// @param name header name
    /// @return Option containing header value if present
    public Option<String> header(HttpHeaderName name) {
        return headers.get(name);
    }

    /// Get a header value by string name (case-insensitive).
    ///
    /// @param name header name
    /// @return Option containing header value if present
    public Option<String> header(String name) {
        return headers.get(name);
    }

    /// Get a single query parameter value by name.
    ///
    /// @param name query parameter name
    /// @return Option containing parameter value if present
    public Option<String> queryParam(String name) {
        return queryParams.get(name);
    }

    /// Get all query parameter values by name.
    ///
    /// @param name query parameter name
    /// @return Option containing list of values if present
    public Option<java.util.List<String>> queryParams(String name) {
        return queryParams.getAll(name);
    }

    /// Get a cookie value by name.
    ///
    /// @param name cookie name
    /// @return Option containing cookie value if present
    public Option<String> cookie(String name) {
        return cookies.get(name);
    }

    /// Deserialize request body to the specified generic type.
    ///
    /// @param type target type token (supports generics)
    /// @param <T> type parameter
    /// @return Result containing deserialized object, or error if deserialization fails
    public <T> Result<T> body(TypeToken<T> type) {
        return deserializer.deserialize(body, type);
    }

    /// Deserialize request body to the specified type.
    /// Delegates to the TypeToken-based method.
    ///
    /// @param type target type
    /// @param <T> type parameter
    /// @return Result containing deserialized object, or error if deserialization fails
    public <T> Result<T> body(Class<T> type) {
        return body(TypeToken.of(type));
    }
}
