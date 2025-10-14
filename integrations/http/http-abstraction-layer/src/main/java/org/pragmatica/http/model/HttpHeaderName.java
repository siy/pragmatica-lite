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

/// HTTP header name interface.
/// Header names are case-insensitive per RFC 7230.
/// Implementations should store the canonical lowercase form.
public interface HttpHeaderName {
    /// Get the canonical lowercase header name.
    ///
    /// @return header name in lowercase
    String headerName();

    /// Create a custom header name from a string.
    /// The name will be normalized to lowercase.
    ///
    /// @param name header name
    /// @return HttpHeaderName instance
    static HttpHeaderName httpHeaderName(String name) {
        return new CustomHeaderName(name.toLowerCase());
    }
}

/// Custom header name implementation for user-defined headers.
record CustomHeaderName(String headerName) implements HttpHeaderName {}
