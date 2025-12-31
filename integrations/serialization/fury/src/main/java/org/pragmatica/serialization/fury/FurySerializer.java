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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.apache.fury.ThreadSafeFury;
import org.pragmatica.serialization.ClassRegistrator;
import org.pragmatica.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache Fury-based serializer implementation.
 */
public interface FurySerializer extends Serializer {
    /**
     * Create a Fury serializer with the given class registrators.
     *
     * @param registrators class registrators to apply
     * @return a thread-safe Fury serializer
     */
    static FurySerializer furySerializer(ClassRegistrator... registrators) {
        record furySerializer(ThreadSafeFury fury) implements FurySerializer {
            private static final Logger log = LoggerFactory.getLogger(FurySerializer.class);

            @Override
            public <T> void write(ByteBuf byteBuf, T object) {
                try (var outputStream = new ByteBufOutputStream(byteBuf)) {
                    fury.serialize(outputStream, object);
                } catch (Exception e) {
                    log.error("Error serializing object", e);
                    throw new RuntimeException(e);
                }
            }
        }
        return new furySerializer(FuryFactory.fury(registrators));
    }
}
