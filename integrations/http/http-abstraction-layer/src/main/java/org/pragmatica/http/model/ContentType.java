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

/// Content type interface for HTTP Content-Type header.
/// Content types follow RFC 2046 format: type/subtype
public interface ContentType {
    /// Get the MIME type string.
    ///
    /// @return content type as string (e.g. "application/json")
    String mimeType();

    /// Create a custom content type from a string.
    ///
    /// @param mimeType MIME type string
    /// @return ContentType instance
    static ContentType contentType(String mimeType) {
        return new CustomContentType(mimeType);
    }
}

/// Custom content type implementation for user-defined types.
record CustomContentType(String mimeType) implements ContentType {}
