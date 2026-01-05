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
import org.pragmatica.serialization.Serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Kryo-based serializer implementation.
public interface KryoSerializer extends Serializer {
    /// Create a Kryo serializer with the given class registrators.
    ///
    /// @param registrators class registrators to apply
    /// @return a thread-safe Kryo serializer
    static KryoSerializer kryoSerializer(ClassRegistrator... registrators) {
        record kryoSerializer(Pool<Kryo> pool) implements KryoSerializer {
            private static final Logger log = LoggerFactory.getLogger(KryoSerializer.class);

            @Override
            public <T> void write(ByteBuf byteBuf, T object) {
                var kryo = pool.obtain();
                try (var byteBufOutputStream = new ByteBufOutputStream(byteBuf);
                     var output = new Output(byteBufOutputStream)) {
                    kryo.writeClassAndObject(output, object);
                } catch (Exception e) {
                    log.error("Error serializing object", e);
                    throw new RuntimeException(e);
                } finally{
                    pool.free(kryo);
                }
            }
        }
        return new kryoSerializer(KryoPoolFactory.kryoPool(registrators));
    }
}
