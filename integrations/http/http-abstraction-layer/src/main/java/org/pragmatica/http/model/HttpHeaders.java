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

import org.pragmatica.lang.Option;

import java.util.HashMap;
import java.util.Map;

/// Immutable collection of HTTP headers.
/// Header names are case-insensitive and stored in canonical lowercase form.
public record HttpHeaders(Map<HttpHeaderName, String> headers) {
    public HttpHeaders {
        headers = Map.copyOf(headers);
    }

    /// Get a header value by name.
    ///
    /// @param name header name
    /// @return Option containing header value if present
    public Option<String> get(HttpHeaderName name) {
        return Option.option(headers.get(name));
    }

    /// Get a header value by string name.
    /// Name is normalized to lowercase before lookup.
    ///
    /// @param name header name
    /// @return Option containing header value if present
    public Option<String> get(String name) {
        HttpHeaderName headerName = CommonHttpHeaderName.fromString(name);
        if (headerName == null) {
            headerName = HttpHeaderName.httpHeaderName(name);
        }
        return get(headerName);
    }

    /// Create an empty headers collection.
    ///
    /// @return empty HttpHeaders
    public static HttpHeaders empty() {
        return new HttpHeaders(Map.of());
    }

    /// Create headers from a string-keyed map.
    /// Header names are normalized to lowercase and converted to HttpHeaderName.
    ///
    /// @param rawHeaders map of header names to values
    /// @return HttpHeaders instance
    public static HttpHeaders fromMap(Map<String, String> rawHeaders) {
        Map<HttpHeaderName, String> headers = new HashMap<>();
        rawHeaders.forEach((name, value) -> {
            HttpHeaderName headerName = CommonHttpHeaderName.fromString(name);
            if (headerName == null) {
                headerName = HttpHeaderName.httpHeaderName(name);
            }
            headers.put(headerName, value);
        });
        return new HttpHeaders(headers);
    }
}
