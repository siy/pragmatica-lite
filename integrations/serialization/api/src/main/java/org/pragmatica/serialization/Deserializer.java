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

/**
 * Basic deserialization interface for decoding objects from binary format.
 */
public interface Deserializer {
    /**
     * Decode an object from a byte array.
     *
     * @param bytes the serialized bytes
     * @param <T>   the type of the object
     * @return the deserialized object
     */
    default <T> T decode(byte[] bytes) {
        var byteBuf = Unpooled.wrappedBuffer(bytes);
        try {
            return read(byteBuf);
        } finally {
            byteBuf.release();
        }
    }

    /**
     * Read an object from a ByteBuf.
     *
     * @param byteBuf the buffer to read from
     * @param <T>     the type of the object
     * @return the deserialized object
     */
    <T> T read(ByteBuf byteBuf);
}
