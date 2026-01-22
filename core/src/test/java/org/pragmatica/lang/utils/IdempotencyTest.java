package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.TimeSpan;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class IdempotencyTest {
    private static final Cause TEST_ERROR = () -> "Test error";

    private Idempotency idempotency;
    private TestTimeSource timeSource;

    private static class TestTimeSource implements TimeSource {
        private volatile TimeSpan currentTime = timeSpan(0).nanos();

        @Override
        public long nanoTime() {
            return currentTime.nanos();
        }

        public void advanceTime(long millis) {
            currentTime = currentTime.plus(millis, TimeUnit.MILLISECONDS);
        }
    }

    @BeforeEach
    void setUp() {
        timeSource = new TestTimeSource();

        idempotency = Idempotency.create(timeSpan(100).millis(), timeSource)
                                 .onFailure(_ -> fail("Setup should not fail"))
                                 .fold(_ -> null, v -> v);
    }

    @Test
    void create_succeeds_forPositiveTtl() {
        Idempotency.create(timeSpan(100).millis())
                   .onFailureRun(Assertions::fail)
                   .onSuccess(Assertions::assertNotNull);
    }

    @Test
    void create_fails_forZeroTtl() {
        Idempotency.create(timeSpan(0).millis())
                   .onSuccess(_ -> Assertions.fail())
                   .onFailure(cause -> assertInstanceOf(Idempotency.IdempotencyError.InvalidTtl.class, cause));
    }

    @Test
    void create_fails_forNegativeTtl() {
        Idempotency.create(timeSpan(-100).millis())
                   .onSuccess(_ -> Assertions.fail())
                   .onFailure(cause -> assertInstanceOf(Idempotency.IdempotencyError.InvalidTtl.class, cause));
    }

    @Test
    void shouldExecuteOperationOnce() {
        var callCount = new AtomicInteger(0);

        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result1");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result1", value));

        assertEquals(1, callCount.get());
    }

    @Test
    void shouldReturnCachedResultForSameKey() {
        var callCount = new AtomicInteger(0);

        // First call
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result1");
                   })
                   .await()
                   .onFailureRun(Assertions::fail);

        // Second call with same key should return cached result
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result2");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result1", value));

        assertEquals(1, callCount.get(), "Operation should only be called once");
    }

    @Test
    void shouldExecuteDifferentOperationsForDifferentKeys() {
        var callCount = new AtomicInteger(0);

        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result1");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result1", value));

        idempotency.execute("key2", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result2");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result2", value));

        assertEquals(2, callCount.get(), "Both operations should be executed");
    }

    @Test
    void shouldAllowReExecutionAfterTtlExpires() {
        var callCount = new AtomicInteger(0);

        // First call
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result1");
                   })
                   .await()
                   .onFailureRun(Assertions::fail);

        // Advance time past TTL
        timeSource.advanceTime(150);

        // Second call should execute again
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result2");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result2", value));

        assertEquals(2, callCount.get(), "Operation should be executed twice after TTL expires");
    }

    @Test
    void shouldNotCacheFailedOperations() {
        var callCount = new AtomicInteger(0);

        // First call fails
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return TEST_ERROR.promise();
                   })
                   .await()
                   .onSuccessRun(Assertions::fail);

        // Second call should execute again (failure not cached)
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result", value));

        assertEquals(2, callCount.get(), "Operation should be executed twice since first call failed");
    }

    @Test
    void shouldSharePromiseForConcurrentRequests() throws InterruptedException {
        var operationCount = new AtomicInteger(0);
        var startLatch = new CountDownLatch(1);
        var completionLatch = new CountDownLatch(5);

        // Create idempotency with real time source for concurrency test
        var concurrentIdempotency = Idempotency.create(timeSpan(10).seconds())
                                               .onFailure(_ -> fail("Setup should not fail"))
                                               .fold(_ -> null, v -> v);

        // Operation that waits for signal before completing
        var delayedPromise = Promise.<String>promise();

        // Start multiple threads that all request the same key
        var threads = IntStream.range(0, 5)
                               .mapToObj(i -> new Thread(() -> {
                                   try {
                                       startLatch.await();
                                   } catch (InterruptedException e) {
                                       Thread.currentThread().interrupt();
                                       return;
                                   }

                                   concurrentIdempotency.execute("shared-key", () -> {
                                                            operationCount.incrementAndGet();
                                                            return delayedPromise;
                                                        })
                                                        .await()
                                                        .onFailureRun(Assertions::fail)
                                                        .onSuccess(value -> assertEquals("shared-result", value));

                                   completionLatch.countDown();
                               }))
                               .toList();

        threads.forEach(Thread::start);

        // Release all threads simultaneously
        startLatch.countDown();

        // Give threads time to call execute
        Thread.sleep(50);

        // Complete the operation
        delayedPromise.succeed("shared-result");

        // Wait for all threads to complete
        assertTrue(completionLatch.await(2, TimeUnit.SECONDS), "All threads should complete");

        assertEquals(1, operationCount.get(), "Operation should only be executed once");
    }

    @Test
    void shouldHandleMultipleDifferentKeysConcurrently() throws InterruptedException {
        var operationCount = new AtomicInteger(0);
        var completionLatch = new CountDownLatch(10);

        var concurrentIdempotency = Idempotency.create(timeSpan(10).seconds())
                                               .onFailure(_ -> fail("Setup should not fail"))
                                               .fold(_ -> null, v -> v);

        var threads = IntStream.range(0, 10)
                               .mapToObj(i -> new Thread(() -> {
                                   concurrentIdempotency.execute("key-" + i, () -> {
                                                            operationCount.incrementAndGet();
                                                            return Promise.success("result-" + i);
                                                        })
                                                        .await()
                                                        .onFailureRun(Assertions::fail);
                                   completionLatch.countDown();
                               }))
                               .toList();

        threads.forEach(Thread::start);

        assertTrue(completionLatch.await(2, TimeUnit.SECONDS), "All threads should complete");

        assertEquals(10, operationCount.get(), "Each unique key should execute operation once");
    }

    @Test
    void shouldReExecuteAfterTtlExpires() {
        var operationCount = new AtomicInteger(0);

        // First call
        idempotency.execute("key1",
                           () -> {
                               operationCount.incrementAndGet();
                               return Promise.success("result1");
                           })
                   .await()
                   .onFailureRun(Assertions::fail);

        assertEquals(1, operationCount.get());

        // Advance time past TTL
        timeSource.advanceTime(110);

        // Second call - operation re-executes
        idempotency.execute("key1",
                           () -> {
                               operationCount.incrementAndGet();
                               return Promise.success("result2");
                           })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result2", value));

        assertEquals(2, operationCount.get(), "Operation should re-execute after TTL");
    }

    @Test
    void shouldUseSystemTimeSourceByDefault() {
        var defaultIdempotency = Idempotency.create(timeSpan(100).millis())
                                            .onFailure(_ -> fail("Setup should not fail"))
                                            .fold(_ -> null, v -> v);

        defaultIdempotency.execute("key1", () -> Promise.success("result"))
                          .await()
                          .onFailureRun(Assertions::fail)
                          .onSuccess(value -> assertEquals("result", value));
    }

    @Test
    void shouldUseCustomTimeSource() {
        var callCount = new AtomicInteger(0);

        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result");
                   })
                   .await()
                   .onFailureRun(Assertions::fail);

        // Immediate second call should return cached
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result2");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result", value));

        assertEquals(1, callCount.get());
    }

    @Test
    void shouldNotExpireInFlightOperation() throws InterruptedException {
        var operationCount = new AtomicInteger(0);
        var delayedPromise = Promise.<String>promise();

        // Start operation that won't complete
        var promise1 = idempotency.execute("key1", () -> {
            operationCount.incrementAndGet();
            return delayedPromise;
        });

        // Advance time past TTL while operation is in-flight
        timeSource.advanceTime(150);

        // Second call should still share the same Promise (in-flight operations never expire)
        var promise2 = idempotency.execute("key1", () -> {
            operationCount.incrementAndGet();
            return Promise.success("different");
        });

        // Complete the original operation
        delayedPromise.succeed("original");

        promise1.await()
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals("original", value));

        promise2.await()
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals("original", value));

        assertEquals(1, operationCount.get(), "Operation should only be executed once, in-flight ops never expire");
    }

    @Test
    void ttlBoundary_shouldCacheJustBeforeTtl() {
        var callCount = new AtomicInteger(0);

        // First call
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result1");
                   })
                   .await()
                   .onFailureRun(Assertions::fail);

        // Advance time to just before TTL (99ms out of 100ms)
        timeSource.advanceTime(99);

        // Just before TTL, entry should still be valid
        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result2");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result1", value));

        assertEquals(1, callCount.get(), "Should return cached just before TTL");

        // Advance to exactly TTL - entry expires at >= expiresAtNanos
        timeSource.advanceTime(1);

        idempotency.execute("key1", () -> {
                       callCount.incrementAndGet();
                       return Promise.success("result3");
                   })
                   .await()
                   .onFailureRun(Assertions::fail)
                   .onSuccess(value -> assertEquals("result3", value));

        assertEquals(2, callCount.get(), "Should re-execute at exact TTL boundary");
    }

    @Test
    void cleanupScheduler_shouldRemoveExpiredEntries() throws InterruptedException {
        // Create idempotency with short TTL and cleanup interval
        var cleanupTimeSource = new TestTimeSource();
        var shortTtlIdempotency = Idempotency.create(timeSpan(50).millis(), cleanupTimeSource)
                                              .onFailure(_ -> fail("Setup should not fail"))
                                              .fold(_ -> null, v -> v);

        var callCount = new AtomicInteger(0);

        // Execute operation
        shortTtlIdempotency.execute("key1", () -> {
                               callCount.incrementAndGet();
                               return Promise.success("result");
                           })
                           .await()
                           .onFailureRun(Assertions::fail);

        assertEquals(1, callCount.get());

        // Advance time past TTL
        cleanupTimeSource.advanceTime(60);

        // Wait for cleanup scheduler to run (cleanup interval is min(TTL/5, 1min) = 10ms)
        Thread.sleep(100);

        // Entry should be cleaned up, so next call executes
        shortTtlIdempotency.execute("key1", () -> {
                               callCount.incrementAndGet();
                               return Promise.success("result2");
                           })
                           .await()
                           .onFailureRun(Assertions::fail)
                           .onSuccess(value -> assertEquals("result2", value));

        assertEquals(2, callCount.get(), "Cleanup should have removed expired entry");
    }
}
