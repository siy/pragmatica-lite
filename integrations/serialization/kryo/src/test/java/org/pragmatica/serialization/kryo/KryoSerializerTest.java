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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KryoSerializerTest {

    @Test
    void roundtrip_serializes_simple_object() {
        var serializer = KryoSerializer.kryoSerializer();
        var deserializer = KryoDeserializer.kryoDeserializer();

        var original = "Hello, World!";
        var bytes = serializer.encode(original);
        String result = deserializer.decode(bytes);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void roundtrip_serializes_record() {
        record Person(String name, int age) {}

        var serializer = KryoSerializer.kryoSerializer(c -> c.accept(Person.class));
        var deserializer = KryoDeserializer.kryoDeserializer(c -> c.accept(Person.class));

        var original = new Person("Alice", 30);
        var bytes = serializer.encode(original);
        Person result = deserializer.decode(bytes);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void roundtrip_serializes_list() {
        var registrator = (org.pragmatica.serialization.ClassRegistrator) c -> {
            c.accept(java.util.ArrayList.class);
        };
        var serializer = KryoSerializer.kryoSerializer(registrator);
        var deserializer = KryoDeserializer.kryoDeserializer(registrator);

        var original = new java.util.ArrayList<>(List.of("one", "two", "three"));
        var bytes = serializer.encode(original);
        List<String> result = deserializer.decode(bytes);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void serializer_is_thread_safe() throws InterruptedException {
        var serializer = KryoSerializer.kryoSerializer();
        var deserializer = KryoDeserializer.kryoDeserializer();

        var threads = new Thread[10];
        var errors = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threads.length; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    var original = "Thread-" + id + "-" + j;
                    var bytes = serializer.encode(original);
                    String result = deserializer.decode(bytes);
                    if (!original.equals(result)) {
                        errors.incrementAndGet();
                    }
                }
            });
        }

        for (var thread : threads) {
            thread.start();
        }
        for (var thread : threads) {
            thread.join();
        }

        assertThat(errors.get()).isZero();
    }
}
