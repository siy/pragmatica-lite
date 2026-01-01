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

package org.pragmatica.http.server;

import org.pragmatica.http.Headers;
import org.pragmatica.http.HttpMethod;
import org.pragmatica.http.QueryParams;

/**
 * HTTP request context providing access to request data.
 */
public interface RequestContext {
    /**
     * Unique request ID for tracing and logging.
     * Format: req_[ulid] (e.g., req_01hq4x2abc...)
     */
    String requestId();

    /**
     * HTTP method.
     */
    HttpMethod method();

    /**
     * Request path (without query string).
     */
    String path();

    /**
     * Request headers.
     */
    Headers headers();

    /**
     * Query parameters.
     */
    QueryParams queryParams();

    /**
     * Request body as bytes.
     */
    byte[] body();

    /**
     * Request body as UTF-8 string.
     */
    default String bodyAsString() {
        var bytes = body();
        return bytes.length == 0
               ? ""
               : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Check if request has a body.
     */
    default boolean hasBody() {
        return body().length > 0;
    }
}
