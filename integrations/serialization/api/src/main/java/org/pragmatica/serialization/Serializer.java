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

package org.pragmatica.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/// Basic serialization interface for encoding objects to binary format.
///
/// <b>Design Note:</b> This interface intentionally uses exceptions rather than Result types.
/// Serialization failures indicate a fundamental system misconfiguration (missing class registrations,
/// incompatible schema changes, or memory corruption) that cannot be recovered at runtime.
/// Such failures are fatal and should result in immediate application shutdown rather than
/// attempting graceful error handling.
public interface Serializer {
    /// Encode an object to a byte array.
    ///
    /// @param object the object to serialize
    /// @param <T>    the type of the object
    /// @return serialized bytes
    default <T> byte[] encode(T object) {
        var byteBuf = Unpooled.buffer();
        try{
            write(byteBuf, object);
            var bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            return bytes;
        } finally{
            byteBuf.release();
        }
    }

    /// Write an object to a ByteBuf.
    ///
    /// @param byteBuf the buffer to write to
    /// @param object  the object to serialize
    /// @param <T>     the type of the object
    <T> void write(ByteBuf byteBuf, T object);
}
