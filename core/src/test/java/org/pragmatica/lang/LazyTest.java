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

package org.pragmatica.lang;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class LazyTest {

    @Nested
    class LazyFactory {
        @Test
        void lazy_isNotComputedInitially() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(counter::incrementAndGet);

            assertFalse(lazy.isComputed());
            assertEquals(0, counter.get());
        }

        @Test
        void lazy_computesOnFirstAccess() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(counter::incrementAndGet);

            assertEquals(1, lazy.get());
            assertTrue(lazy.isComputed());
            assertEquals(1, counter.get());
        }

        @Test
        void lazy_computesOnlyOnce() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(counter::incrementAndGet);

            assertEquals(1, lazy.get());
            assertEquals(1, lazy.get());
            assertEquals(1, lazy.get());

            assertEquals(1, counter.get());
        }

        @Test
        void lazy_propagatesExceptions() {
            var lazy = Lazy.lazy(() -> {
                throw new RuntimeException("computation failed");
            });

            var exception = assertThrows(RuntimeException.class, lazy::get);
            assertEquals("computation failed", exception.getMessage());
        }

        @Test
        void lazy_withNullValue() {
            var lazy = Lazy.lazy(() -> null);

            assertNull(lazy.get());
            assertTrue(lazy.isComputed());
            assertNull(lazy.get());
        }
    }

    @Nested
    class ValueFactory {
        @Test
        void value_isAlreadyComputed() {
            var lazy = Lazy.value(42);

            assertTrue(lazy.isComputed());
            assertEquals(42, lazy.get());
        }

        @Test
        void value_returnsSameValueOnMultipleAccess() {
            var lazy = Lazy.value("hello");

            assertEquals("hello", lazy.get());
            assertEquals("hello", lazy.get());
        }

        @Test
        void value_withNullValue() {
            var lazy = Lazy.value(null);

            assertTrue(lazy.isComputed());
            assertNull(lazy.get());
        }
    }

    @Nested
    class MapCombinator {
        @Test
        void map_isLazy() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(counter::incrementAndGet);
            var mapped = lazy.map(n -> n * 2);

            assertFalse(lazy.isComputed());
            assertFalse(mapped.isComputed());
            assertEquals(0, counter.get());
        }

        @Test
        void map_computesOnAccess() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(counter::incrementAndGet);
            var mapped = lazy.map(n -> n * 2);

            assertEquals(2, mapped.get());
            assertTrue(lazy.isComputed());
            assertTrue(mapped.isComputed());
        }

        @Test
        void map_chainsCorrectly() {
            var lazy = Lazy.lazy(() -> 5);
            var result = lazy.map(n -> n * 2)
                             .map(n -> n + 3)
                             .map(Object::toString);

            assertEquals("13", result.get());
        }

        @Test
        void map_onPreComputedValue_isLazy() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.value(5);
            var mapped = lazy.map(n -> {
                counter.incrementAndGet();
                return n * 2;
            });

            assertEquals(0, counter.get());
            assertEquals(10, mapped.get());
            assertEquals(1, counter.get());
        }
    }

    @Nested
    class FlatMapCombinator {
        @Test
        void flatMap_isLazy() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(counter::incrementAndGet);
            var flatMapped = lazy.flatMap(n -> Lazy.lazy(() -> n * 10));

            assertFalse(lazy.isComputed());
            assertFalse(flatMapped.isComputed());
            assertEquals(0, counter.get());
        }

        @Test
        void flatMap_computesOnAccess() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(counter::incrementAndGet);
            var flatMapped = lazy.flatMap(n -> Lazy.lazy(() -> n * 10));

            assertEquals(10, flatMapped.get());
            assertTrue(lazy.isComputed());
            assertTrue(flatMapped.isComputed());
        }

        @Test
        void flatMap_chainsCorrectly() {
            var lazy = Lazy.lazy(() -> 5);
            var result = lazy.flatMap(n -> Lazy.lazy(() -> n * 2))
                             .flatMap(n -> Lazy.value(n + 3));

            assertEquals(13, result.get());
        }

        @Test
        void flatMap_onPreComputedValue_isLazy() {
            var counter = new AtomicInteger(0);
            var lazy = Lazy.value(5);
            var flatMapped = lazy.flatMap(n -> {
                counter.incrementAndGet();
                return Lazy.value(n * 2);
            });

            assertEquals(0, counter.get());
            assertEquals(10, flatMapped.get());
            assertEquals(1, counter.get());
        }
    }

    @Nested
    class ThreadSafety {
        @Test
        void lazy_threadSafeUnderConcurrentAccess() throws InterruptedException {
            var threadCount = 100;
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(() -> {
                counter.incrementAndGet();
                return 42;
            });

            var latch = new CountDownLatch(threadCount);
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        try {
                            assertEquals(42, lazy.get());
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertTrue(latch.await(2, TimeUnit.SECONDS));
            }

            assertEquals(1, counter.get());
        }

        @Test
        void lazy_threadSafeWithMapUnderConcurrentAccess() throws InterruptedException {
            var threadCount = 100;
            var counter = new AtomicInteger(0);
            var lazy = Lazy.lazy(() -> {
                counter.incrementAndGet();
                return 21;
            });
            var mapped = lazy.map(n -> n * 2);

            var latch = new CountDownLatch(threadCount);
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        try {
                            assertEquals(42, mapped.get());
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                assertTrue(latch.await(2, TimeUnit.SECONDS));
            }

            assertEquals(1, counter.get());
        }
    }

    @Nested
    class ToStringBehavior {
        @Test
        void lazy_toStringBeforeComputation() {
            var lazy = Lazy.lazy(() -> 42);
            assertEquals("Lazy(<not computed>)", lazy.toString());
        }

        @Test
        void lazy_toStringAfterComputation() {
            var lazy = Lazy.lazy(() -> 42);
            lazy.get();
            assertEquals("Lazy(42)", lazy.toString());
        }

        @Test
        void value_toString() {
            var lazy = Lazy.value("hello");
            assertEquals("Lazy(hello)", lazy.toString());
        }
    }
}
