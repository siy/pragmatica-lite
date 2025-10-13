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

import java.util.List;
import java.util.Map;

/// Immutable HTTP response representation.
/// Created by handlers and returned to framework adapters.
///
/// @param requestId unique identifier correlating with the request
/// @param status    HTTP status code (200, 404, 500, etc.)
/// @param headers   HTTP headers (header name → list of values)
/// @param body      response body as bytes
public record HttpResponse(
    String requestId,
    int status,
    Map<String, List<String>> headers,
    byte[] body
) {
    /// Create a 200 OK response with the given body.
    ///
    /// @param requestId request correlation ID
    /// @param body response body
    /// @return HttpResponse with status 200
    public static HttpResponse ok(String requestId, byte[] body) {
        return new HttpResponse(requestId, 200, Map.of(), body);
    }

    /// Create a 201 Created response with the given body.
    ///
    /// @param requestId request correlation ID
    /// @param body response body
    /// @return HttpResponse with status 201
    public static HttpResponse created(String requestId, byte[] body) {
        return new HttpResponse(requestId, 201, Map.of(), body);
    }

    /// Create a 204 No Content response.
    ///
    /// @param requestId request correlation ID
    /// @return HttpResponse with status 204 and empty body
    public static HttpResponse noContent(String requestId) {
        return new HttpResponse(requestId, 204, Map.of(), new byte[0]);
    }

    /// Create a 404 Not Found response.
    ///
    /// @param requestId request correlation ID
    /// @return HttpResponse with status 404 and empty body
    public static HttpResponse notFound(String requestId) {
        return new HttpResponse(requestId, 404, Map.of(), new byte[0]);
    }

    /// Create a 500 Internal Server Error response.
    ///
    /// @param requestId request correlation ID
    /// @return HttpResponse with status 500 and empty body
    public static HttpResponse internalServerError(String requestId) {
        return new HttpResponse(requestId, 500, Map.of(), new byte[0]);
    }
}
