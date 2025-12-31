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

import java.util.stream.Stream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;

/**
 * Factory for creating thread-safe Kryo instance pools.
 */
public sealed interface KryoPoolFactory {
    /**
     * Create a Kryo pool with the given class registrators.
     *
     * @param registrators class registrators to apply to each Kryo instance
     * @return a thread-safe Kryo pool
     */
    static Pool<Kryo> kryoPool(ClassRegistrator... registrators) {
        return new Pool<>(true,
                          false,
                          Runtime.getRuntime()
                                 .availableProcessors() * 2) {
            @Override
            protected Kryo create() {
                var kryo = new Kryo();
                Stream.of(registrators)
                      .forEach(registrator -> registrator.registerClasses(kryo::register));
                return kryo;
            }
        };
    }

    @SuppressWarnings("unused")
    record unused() implements KryoPoolFactory {}
}
