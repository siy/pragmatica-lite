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
import org.pragmatica.lang.type.TypeToken;

import java.nio.charset.StandardCharsets;

/// Generic deserialization interface for HTTP request bodies.
/// Implementations provide specific deserialization strategies (JSON, XML, etc.).
/// Only the TypeToken-based method needs to be implemented; Class-based methods delegate to it.
public interface Deserializer {
    /// Deserialize bytes to an object of the specified generic type.
    /// This is the primary method that implementations must provide.
    ///
    /// @param data the bytes to deserialize
    /// @param type the target type token (supports generics like List&lt;User&gt;)
    /// @param <T> the type parameter
    /// @return Result containing deserialized object, or error if deserialization fails
    <T> Result<T> deserialize(byte[] data, TypeToken<T> type);

    /// Deserialize bytes to an object of the specified type.
    /// Delegates to the TypeToken-based method.
    ///
    /// @param data the bytes to deserialize
    /// @param type the target type
    /// @param <T> the type parameter
    /// @return Result containing deserialized object, or error if deserialization fails
    default <T> Result<T> deserialize(byte[] data, Class<T> type) {
        return deserialize(data, TypeToken.of(type));
    }

    /// Deserialize a UTF-8 string to an object of the specified generic type.
    ///
    /// @param data the string to deserialize
    /// @param type the target type token
    /// @param <T> the type parameter
    /// @return Result containing deserialized object, or error if deserialization fails
    default <T> Result<T> deserializeFromString(String data, TypeToken<T> type) {
        return deserialize(data.getBytes(StandardCharsets.UTF_8), type);
    }

    /// Deserialize a UTF-8 string to an object of the specified type.
    ///
    /// @param data the string to deserialize
    /// @param type the target type
    /// @param <T> the type parameter
    /// @return Result containing deserialized object, or error if deserialization fails
    default <T> Result<T> deserializeFromString(String data, Class<T> type) {
        return deserialize(data.getBytes(StandardCharsets.UTF_8), TypeToken.of(type));
    }
}
