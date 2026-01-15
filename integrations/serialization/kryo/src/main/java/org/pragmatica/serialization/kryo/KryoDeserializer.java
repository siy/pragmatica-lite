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

package org.pragmatica.serialization.kryo;

import org.pragmatica.serialization.ClassRegistrator;
import org.pragmatica.serialization.Deserializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.util.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Kryo-based deserializer implementation.
public interface KryoDeserializer extends Deserializer {
    /// Create a Kryo deserializer with the given class registrators.
    ///
    /// @param registrators class registrators to apply
    /// @return a thread-safe Kryo deserializer
    static KryoDeserializer kryoDeserializer(ClassRegistrator... registrators) {
        record kryoDeserializer(Pool<Kryo> pool) implements KryoDeserializer {
            private static final Logger log = LoggerFactory.getLogger(KryoDeserializer.class);

            @SuppressWarnings("unchecked")
            @Override
            public <T> T read(ByteBuf byteBuf) {
                var kryo = pool.obtain();
                try (var byteBufInputStream = new ByteBufInputStream(byteBuf);
                     var input = new Input(byteBufInputStream)) {
                    return (T) kryo.readClassAndObject(input);
                } catch (Exception e) {
                    log.error("Error deserializing object", e);
                    throw new RuntimeException(e);
                } finally{
                    pool.free(kryo);
                }
            }
        }
        return new kryoDeserializer(KryoPoolFactory.kryoPool(registrators));
    }
}
