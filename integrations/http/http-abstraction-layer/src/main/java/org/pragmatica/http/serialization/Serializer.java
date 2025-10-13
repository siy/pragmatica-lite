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

package org.pragmatica.http.serialization;

import org.pragmatica.lang.Result;

import java.nio.charset.StandardCharsets;

/// Generic serialization interface for HTTP response bodies.
/// Implementations provide specific serialization strategies (JSON, XML, etc.).
@FunctionalInterface
public interface Serializer {
    /// Serialize an object to bytes.
    ///
    /// @param value the object to serialize
    /// @return Result containing serialized bytes, or error if serialization fails
    Result<byte[]> serialize(Object value);

    /// Serialize an object to a UTF-8 string.
    ///
    /// @param value the object to serialize
    /// @return Result containing serialized string, or error if serialization fails
    default Result<String> serializeToString(Object value) {
        return serialize(value).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }
}
