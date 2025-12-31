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

package org.pragmatica.serialization.fury;

import org.pragmatica.serialization.ClassRegistrator;
import org.pragmatica.serialization.Deserializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.io.FuryInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache Fury-based deserializer implementation.
 */
public interface FuryDeserializer extends Deserializer {
    /**
     * Create a Fury deserializer with the given class registrators.
     *
     * @param registrators class registrators to apply
     * @return a thread-safe Fury deserializer
     */
    static FuryDeserializer furyDeserializer(ClassRegistrator... registrators) {
        record furyDeserializer(ThreadSafeFury fury) implements FuryDeserializer {
            private static final Logger log = LoggerFactory.getLogger(FuryDeserializer.class);

            @SuppressWarnings("unchecked")
            @Override
            public <T> T read(ByteBuf byteBuf) {
                try (var stream = new FuryInputStream(new ByteBufInputStream(byteBuf))) {
                    return (T) fury.deserialize(stream);
                } catch (Exception e) {
                    log.error("Error deserializing object", e);
                    throw new RuntimeException(e);
                }
            }
        }
        return new furyDeserializer(FuryFactory.fury(registrators));
    }
}
