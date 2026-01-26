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

package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class MemoResultTest {
    private static final Cause TEST_ERROR = () -> "Test error";

    @Nested
    class UnboundedCache {

        @Test
        void get_cachesSuccessfulResults() {
            var computeCount = new AtomicInteger(0);
            var cache = MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            });

            cache.get("key1")
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            cache.get("key1")
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            assertEquals(1, computeCount.get(), "Should compute only once");
            assertEquals(1, cache.hitCount());
            assertEquals(1, cache.missCount());
        }

        @Test
        void get_doesNotCacheFailures() {
            var computeCount = new AtomicInteger(0);
            var cache = MemoResult.<String, String>memoResult(key -> {
                int count = computeCount.incrementAndGet();
                if (count < 3) {
                    return TEST_ERROR.result();
                }
                return Result.success("value-" + key);
            });

            // First two calls fail
            cache.get("key1").onSuccessRun(Assertions::fail);
            cache.get("key1").onSuccessRun(Assertions::fail);

            // Third call succeeds
            cache.get("key1")
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            assertEquals(3, computeCount.get(), "Should recompute on failure");
            assertEquals(0, cache.hitCount());
            assertEquals(3, cache.missCount());
        }

        @Test
        void get_handlesDifferentKeys() {
            var computeCount = new AtomicInteger(0);
            var cache = MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            });

            cache.get("key1")
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            cache.get("key2")
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key2", value));

            assertEquals(2, computeCount.get());
            assertEquals(2, cache.size());
        }

        @Test
        void invalidate_removesCachedEntry() {
            var computeCount = new AtomicInteger(0);
            var cache = MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            });

            cache.get("key1");
            assertEquals(1, cache.size());

            cache.invalidate("key1");
            assertEquals(0, cache.size());

            cache.get("key1");
            assertEquals(2, computeCount.get(), "Should recompute after invalidation");
        }

        @Test
        void invalidateAll_removesAllEntries() {
            var cache = MemoResult.memoResult(key -> Result.success("value-" + key));

            cache.get("key1");
            cache.get("key2");
            cache.get("key3");
            assertEquals(3, cache.size());

            cache.invalidateAll();
            assertEquals(0, cache.size());
        }

        @Test
        void counters_trackHitsAndMisses() {
            var cache = MemoResult.memoResult(key -> Result.success("value-" + key));

            cache.get("key1"); // miss
            cache.get("key1"); // hit
            cache.get("key1"); // hit
            cache.get("key2"); // miss
            cache.get("key2"); // hit

            assertEquals(3, cache.hitCount());
            assertEquals(2, cache.missCount());
        }
    }

    @Nested
    class BoundedCache {

        @Test
        void get_evictsLeastRecentlyUsed() {
            MemoResult.memoResult(key -> Result.success("value-" + key), 2)
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    cache.get("key1");
                    cache.get("key2");
                    assertEquals(2, cache.size());

                    // Adding key3 should evict key1 (LRU)
                    cache.get("key3");
                    assertEquals(2, cache.size());
                });

            // key1 should be recomputed (was evicted)
            var computeCount = new AtomicInteger(0);
            MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            }, 2)
                .onFailureRun(Assertions::fail)
                .onSuccess(trackedCache -> {
                    trackedCache.get("key1");
                    trackedCache.get("key2");
                    trackedCache.get("key3");  // evicts key1
                    trackedCache.get("key1");  // recomputes

                    assertEquals(4, computeCount.get());
                });
        }

        @Test
        void get_accessUpdatesRecency() {
            var computeCount = new AtomicInteger(0);
            MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            }, 2)
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    cache.get("key1");
                    cache.get("key2");
                    cache.get("key1");  // Access key1, making key2 the LRU
                    cache.get("key3");  // Should evict key2

                    // key1 should still be cached
                    cache.get("key1");
                    assertEquals(3, computeCount.get(), "key1 should not be recomputed");

                    // key2 was evicted
                    cache.get("key2");
                    assertEquals(4, computeCount.get(), "key2 should be recomputed");
                });
        }

        @Test
        void create_rejectsNonPositiveMaxSize() {
            MemoResult.memoResult(key -> Result.success(key), 0)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertEquals("maxSize must be positive", cause.message()));

            MemoResult.memoResult(key -> Result.success(key), -1)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertEquals("maxSize must be positive", cause.message()));
        }

        @Test
        void get_doesNotCacheFailures() {
            var computeCount = new AtomicInteger(0);
            MemoResult.<String, String>memoResult(key -> {
                int count = computeCount.incrementAndGet();
                if (count < 3) {
                    return TEST_ERROR.result();
                }
                return Result.success("value-" + key);
            }, 10)
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    cache.get("key1").onSuccessRun(Assertions::fail);
                    cache.get("key1").onSuccessRun(Assertions::fail);
                    cache.get("key1")
                         .onFailureRun(Assertions::fail)
                         .onSuccess(value -> assertEquals("value-key1", value));

                    assertEquals(3, computeCount.get());
                });
        }
    }

    @Nested
    class ThreadSafety {

        @Test
        void unbounded_handlesConvergentAccess() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            var cache = MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            });

            var startLatch = new CountDownLatch(1);
            var completionLatch = new CountDownLatch(10);

            var threads = IntStream.range(0, 10)
                                   .mapToObj(_ -> new Thread(() -> {
                                       try {
                                           startLatch.await();
                                       } catch (InterruptedException e) {
                                           Thread.currentThread().interrupt();
                                           return;
                                       }
                                       cache.get("shared-key")
                                            .onFailureRun(Assertions::fail)
                                            .onSuccess(value -> assertEquals("value-shared-key", value));
                                       completionLatch.countDown();
                                   }))
                                   .toList();

            threads.forEach(Thread::start);
            startLatch.countDown();

            assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

            // Due to races, we might compute more than once, but should cache eventually
            assertTrue(computeCount.get() >= 1);
            assertEquals(1, cache.size());
        }

        @Test
        void unbounded_handlesDivergentAccess() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            var cache = MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            });

            var completionLatch = new CountDownLatch(20);

            var threads = IntStream.range(0, 20)
                                   .mapToObj(i -> new Thread(() -> {
                                       cache.get("key-" + i)
                                            .onFailureRun(Assertions::fail);
                                       completionLatch.countDown();
                                   }))
                                   .toList();

            threads.forEach(Thread::start);

            assertTrue(completionLatch.await(2, TimeUnit.SECONDS));
            assertEquals(20, computeCount.get());
            assertEquals(20, cache.size());
        }

        @Test
        void bounded_handlesConvergentAccess() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            MemoResult.memoResult(key -> {
                computeCount.incrementAndGet();
                return Result.success("value-" + key);
            }, 100)
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    var startLatch = new CountDownLatch(1);
                    var completionLatch = new CountDownLatch(10);

                    var threads = IntStream.range(0, 10)
                                           .mapToObj(_ -> new Thread(() -> {
                                               try {
                                                   startLatch.await();
                                               } catch (InterruptedException e) {
                                                   Thread.currentThread().interrupt();
                                                   return;
                                               }
                                               cache.get("shared-key")
                                                    .onFailureRun(Assertions::fail)
                                                    .onSuccess(value -> assertEquals("value-shared-key", value));
                                               completionLatch.countDown();
                                           }))
                                           .toList();

                    threads.forEach(Thread::start);
                    startLatch.countDown();

                    try {
                        assertTrue(completionLatch.await(2, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Interrupted");
                    }
                    assertTrue(computeCount.get() >= 1);
                    assertEquals(1, cache.size());
                });
        }

        @Test
        void bounded_handlesDivergentAccessWithEviction() throws InterruptedException {
            MemoResult.memoResult(
                key -> Result.success("value-" + key),
                10
            )
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    var completionLatch = new CountDownLatch(50);

                    var threads = IntStream.range(0, 50)
                                           .mapToObj(i -> new Thread(() -> {
                                               cache.get("key-" + i)
                                                    .onFailureRun(Assertions::fail);
                                               completionLatch.countDown();
                                           }))
                                           .toList();

                    threads.forEach(Thread::start);

                    try {
                        assertTrue(completionLatch.await(2, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Interrupted");
                    }
                    assertTrue(cache.size() <= 10, "Size should not exceed maxSize");
                });
        }
    }

    @Nested
    class Observability {

        @Test
        void counters_areAccurateUnderConcurrency() throws InterruptedException {
            var cache = MemoResult.memoResult(key -> Result.success("value-" + key));

            var completionLatch = new CountDownLatch(100);

            // 100 threads each accessing 10 keys (0-9), each key accessed 10 times
            var threads = IntStream.range(0, 100)
                                   .mapToObj(i -> new Thread(() -> {
                                       cache.get("key-" + (i % 10));
                                       completionLatch.countDown();
                                   }))
                                   .toList();

            threads.forEach(Thread::start);

            assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

            // Total accesses = 100
            // Misses = at least 10 (one per unique key)
            // Hits = total - misses
            assertEquals(100, cache.hitCount() + cache.missCount());
            assertTrue(cache.missCount() >= 10);
            assertEquals(10, cache.size());
        }
    }
}
