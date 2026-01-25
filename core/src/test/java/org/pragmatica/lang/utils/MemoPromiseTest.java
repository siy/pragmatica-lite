package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Promise;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.pragmatica.lang.utils.MemoPromiseTest.TestUtils.awaitCondition;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class MemoPromiseTest {
    private static final Cause TEST_ERROR = () -> "Test error";

    @Nested
    class UnboundedCache {

        @Test
        void get_cachesSuccessfulResults() {
            var computeCount = new AtomicInteger(0);
            var cache = MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
            });

            cache.get("key1")
                 .await()
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            cache.get("key1")
                 .await()
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            assertEquals(1, computeCount.get(), "Should compute only once");
            assertEquals(1, cache.hitCount());
            assertEquals(1, cache.missCount());
        }

        @Test
        void get_doesNotCacheFailures() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            var cache = MemoPromise.<String, String>memoPromise(key -> {
                int count = computeCount.incrementAndGet();
                if (count < 3) {
                    return TEST_ERROR.promise();
                }
                return Promise.success("value-" + key);
            });

            // First two calls fail
            cache.get("key1").await().onSuccessRun(Assertions::fail);
            // Wait for async failure handler to remove entry
            awaitCondition(() -> cache.size() == 0, 1000);

            cache.get("key1").await().onSuccessRun(Assertions::fail);
            // Wait for async failure handler to remove entry
            awaitCondition(() -> cache.size() == 0, 1000);

            // Third call succeeds
            cache.get("key1")
                 .await()
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            assertEquals(3, computeCount.get(), "Should recompute on failure");
        }

        @Test
        void get_handlesDifferentKeys() {
            var computeCount = new AtomicInteger(0);
            var cache = MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
            });

            cache.get("key1")
                 .await()
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            cache.get("key2")
                 .await()
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key2", value));

            assertEquals(2, computeCount.get());
            assertEquals(2, cache.size());
        }

        @Test
        void invalidate_removesCachedEntry() {
            var computeCount = new AtomicInteger(0);
            var cache = MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
            });

            cache.get("key1").await();
            assertEquals(1, cache.size());

            cache.invalidate("key1");
            assertEquals(0, cache.size());

            cache.get("key1").await();
            assertEquals(2, computeCount.get(), "Should recompute after invalidation");
        }

        @Test
        void invalidateAll_removesAllEntries() {
            var cache = MemoPromise.memoPromise(key -> Promise.success("value-" + key));

            cache.get("key1").await();
            cache.get("key2").await();
            cache.get("key3").await();
            assertEquals(3, cache.size());

            cache.invalidateAll();
            assertEquals(0, cache.size());
        }

        @Test
        void counters_trackHitsAndMisses() {
            var cache = MemoPromise.memoPromise(key -> Promise.success("value-" + key));

            cache.get("key1").await(); // miss
            cache.get("key1").await(); // hit
            cache.get("key1").await(); // hit
            cache.get("key2").await(); // miss
            cache.get("key2").await(); // hit

            assertEquals(3, cache.hitCount());
            assertEquals(2, cache.missCount());
        }
    }

    @Nested
    class BoundedCache {

        @Test
        void get_evictsLeastRecentlyUsed() {
            MemoPromise.memoPromise(key -> Promise.success("value-" + key), 2)
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    cache.get("key1").await();
                    cache.get("key2").await();
                    assertEquals(2, cache.size());

                    // Adding key3 should evict key1 (LRU)
                    cache.get("key3").await();
                    assertEquals(2, cache.size());
                });

            // key1 should be recomputed (was evicted)
            var computeCount = new AtomicInteger(0);
            MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
            }, 2)
                .onFailureRun(Assertions::fail)
                .onSuccess(trackedCache -> {
                    trackedCache.get("key1").await();
                    trackedCache.get("key2").await();
                    trackedCache.get("key3").await();  // evicts key1
                    trackedCache.get("key1").await();  // recomputes

                    assertEquals(4, computeCount.get());
                });
        }

        @Test
        void get_accessUpdatesRecency() {
            var computeCount = new AtomicInteger(0);
            MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
            }, 2)
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    cache.get("key1").await();
                    cache.get("key2").await();
                    cache.get("key1").await();  // Access key1, making key2 the LRU
                    cache.get("key3").await();  // Should evict key2

                    // key1 should still be cached
                    cache.get("key1").await();
                    assertEquals(3, computeCount.get(), "key1 should not be recomputed");

                    // key2 was evicted
                    cache.get("key2").await();
                    assertEquals(4, computeCount.get(), "key2 should be recomputed");
                });
        }

        @Test
        void create_rejectsNonPositiveMaxSize() {
            MemoPromise.memoPromise(key -> Promise.success(key), 0)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertEquals("maxSize must be positive", cause.message()));

            MemoPromise.memoPromise(key -> Promise.success(key), -1)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertEquals("maxSize must be positive", cause.message()));
        }

        @Test
        void get_doesNotCacheFailures() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            MemoPromise.<String, String>memoPromise(key -> {
                int count = computeCount.incrementAndGet();
                if (count < 3) {
                    return TEST_ERROR.promise();
                }
                return Promise.success("value-" + key);
            }, 10)
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    try {
                        cache.get("key1").await().onSuccessRun(Assertions::fail);
                        // Wait for async failure handler to remove entry
                        awaitCondition(() -> cache.size() == 0, 1000);

                        cache.get("key1").await().onSuccessRun(Assertions::fail);
                        // Wait for async failure handler to remove entry
                        awaitCondition(() -> cache.size() == 0, 1000);

                        cache.get("key1")
                             .await()
                             .onFailureRun(Assertions::fail)
                             .onSuccess(value -> assertEquals("value-key1", value));

                        assertEquals(3, computeCount.get());
                    } catch (InterruptedException | AssertionError e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        fail("Test failed: " + e.getMessage());
                    }
                });
        }
    }

    @Nested
    class ConcurrentRequests {

        @Test
        void get_returnsSamePromiseForConcurrentRequests() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            var startLatch = new CountDownLatch(1);
            var computeLatch = new CountDownLatch(1);

            var cache = MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                // Create a promise that resolves after we allow it
                return Promise.<String>promise()
                              .async(p -> {
                                  try {
                                      computeLatch.await();
                                      p.succeed("value-" + key);
                                  } catch (InterruptedException e) {
                                      Thread.currentThread().interrupt();
                                  }
                              });
            });

            var promises = new Promise[10];
            var threads = IntStream.range(0, 10)
                                   .mapToObj(i -> new Thread(() -> {
                                       try {
                                           startLatch.await();
                                       } catch (InterruptedException e) {
                                           Thread.currentThread().interrupt();
                                           return;
                                       }
                                       promises[i] = cache.get("shared-key");
                                   }))
                                   .toList();

            threads.forEach(Thread::start);
            startLatch.countDown();

            // Wait for all threads to request the key
            for (var thread : threads) {
                thread.join(1000);
            }

            // Allow computation to complete
            computeLatch.countDown();

            // All promises should be the same instance
            var firstPromise = promises[0];
            for (int i = 1; i < promises.length; i++) {
                assertSame(firstPromise, promises[i], "All concurrent requests should get same Promise");
            }

            // Verify the result
            firstPromise.await()
                        .onFailureRun(Assertions::fail)
                        .onSuccess(value -> assertEquals("value-shared-key", value));
        }

        @Test
        void get_removesEntryOnFailureThenRetriesSuccessfully() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            var cache = MemoPromise.<String, String>memoPromise(key -> {
                int count = computeCount.incrementAndGet();
                if (count == 1) {
                    return TEST_ERROR.promise();
                }
                return Promise.success("value-" + key);
            });

            // First call fails
            cache.get("key1").await().onSuccessRun(Assertions::fail);

            // Wait for failure handler to remove entry
            awaitCondition(() -> cache.size() == 0, 1000);

            // Second call succeeds
            cache.get("key1")
                 .await()
                 .onFailureRun(Assertions::fail)
                 .onSuccess(value -> assertEquals("value-key1", value));

            assertEquals(1, cache.size());
        }
    }

    @Nested
    class ThreadSafety {

        @Test
        void unbounded_handlesConvergentAccess() throws InterruptedException {
            var computeCount = new AtomicInteger(0);
            var cache = MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
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
                                            .await()
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
            var cache = MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
            });

            var completionLatch = new CountDownLatch(20);

            var threads = IntStream.range(0, 20)
                                   .mapToObj(i -> new Thread(() -> {
                                       cache.get("key-" + i)
                                            .await()
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
            MemoPromise.memoPromise(key -> {
                computeCount.incrementAndGet();
                return Promise.success("value-" + key);
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
                                                    .await()
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
            MemoPromise.memoPromise(
                key -> Promise.success("value-" + key),
                10
            )
                .onFailureRun(Assertions::fail)
                .onSuccess(cache -> {
                    var completionLatch = new CountDownLatch(50);

                    var threads = IntStream.range(0, 50)
                                           .mapToObj(i -> new Thread(() -> {
                                               cache.get("key-" + i)
                                                    .await()
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
            var cache = MemoPromise.memoPromise(key -> Promise.success("value-" + key));

            var completionLatch = new CountDownLatch(100);

            // 100 threads each accessing 10 keys (0-9), each key accessed 10 times
            var threads = IntStream.range(0, 100)
                                   .mapToObj(i -> new Thread(() -> {
                                       cache.get("key-" + (i % 10)).await();
                                       completionLatch.countDown();
                                   }))
                                   .toList();

            threads.forEach(Thread::start);

            assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

            // Total accesses = 100
            // Due to races in putIfAbsent, misses may be more than 10, but we should
            // eventually have exactly 10 cached entries
            assertEquals(100, cache.hitCount() + cache.missCount());
            assertTrue(cache.missCount() >= 10);
            assertEquals(10, cache.size());
        }
    }

    /// Test utilities to avoid Thread.sleep in tests.
    static class TestUtils {
        /// Await a condition with timeout, polling at fixed intervals.
        static void awaitCondition(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!condition.getAsBoolean()) {
                if (System.currentTimeMillis() >= deadline) {
                    throw new AssertionError("Condition not met within " + timeoutMs + "ms");
                }
                Thread.sleep(5);
            }
        }
    }
}
