package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.RateLimiter.RateLimiterError.LimitExceeded;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class RateLimiterTest {

    private RateLimiter rateLimiter;
    private TestTimeSource timeSource;

    private static class TestTimeSource implements TimeSource {
        private TimeSpan currentTime = timeSpan(0).nanos();

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

        rateLimiter = RateLimiter.builder()
                .rate(5)
                .period(timeSpan(1).seconds())
                .burst(0)
                .timeSource(timeSource);
    }

    @Test
    void shouldExecuteOperationWhenPermitAvailable() {
        var callCount = new AtomicInteger(0);

        rateLimiter.execute(() -> {
                    callCount.incrementAndGet();
                    return Promise.success("Success");
                })
                .await()
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals("Success", value));

        assertEquals(1, callCount.get());
    }

    @Test
    void shouldExhaustPermits() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.execute(() -> Promise.success("OK"))
                    .await()
                    .onFailureRun(Assertions::fail);
        }

        // Next request should fail
        rateLimiter.execute(() -> Promise.success("Should not execute"))
                .await()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(LimitExceeded.class, cause));
    }

    @Test
    void shouldRejectExecutionWhenNoPermits() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.execute(() -> Promise.success("OK")).await();
        }

        var callCount = new AtomicInteger(0);

        rateLimiter.execute(() -> {
                    callCount.incrementAndGet();
                    return Promise.success("Should not execute");
                })
                .await()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(LimitExceeded.class, cause));

        assertEquals(0, callCount.get(), "Operation should not be executed when rate limited");
    }

    @Test
    void shouldRefillPermitsAfterPeriod() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.execute(() -> Promise.success("OK")).await();
        }

        // Verify exhausted
        rateLimiter.execute(() -> Promise.success("Fail"))
                .await()
                .onSuccessRun(Assertions::fail);

        // Advance time by one period
        timeSource.advanceTime(1000);

        // Should be able to execute again
        rateLimiter.execute(() -> Promise.success("Success after refill"))
                .await()
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals("Success after refill", value));
    }

    @Test
    void shouldAllowBurstCapacity() {
        var limiterWithBurst = RateLimiter.builder()
                .rate(5)
                .period(timeSpan(1).seconds())
                .burst(3)
                .timeSource(timeSource);

        // Should be able to execute rate + burst = 8 times
        for (int i = 0; i < 8; i++) {
            limiterWithBurst.execute(() -> Promise.success("OK"))
                    .await()
                    .onFailureRun(Assertions::fail);
        }

        // 9th should fail
        limiterWithBurst.execute(() -> Promise.success("Fail"))
                .await()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(LimitExceeded.class, cause));
    }

    @Test
    void shouldProvideRetryAfterInLimitExceeded() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.execute(() -> Promise.success("OK")).await();
        }

        rateLimiter.execute(() -> Promise.success("Fail"))
                .await()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> {
                    if (cause instanceof LimitExceeded limited) {
                        assertTrue(limited.retryAfter().millis() > 0,
                                "Retry after should be positive");
                        assertTrue(limited.retryAfter().millis() <= 1000,
                                "Retry after should not exceed period");
                    } else {
                        Assertions.fail("Unexpected cause type: " + cause.getClass().getName());
                    }
                });
    }

    @Test
    void shouldHandleConcurrentExecutions() {
        var limiter = RateLimiter.builder()
                .rate(100)
                .period(timeSpan(1).seconds())
                .withDefaultTimeSource();

        var successCount = new AtomicInteger(0);
        var failureCount = new AtomicInteger(0);

        var promises = IntStream.range(0, 150)
                .mapToObj(_ -> limiter.execute(() -> Promise.success("OK"))
                        .onSuccess(_ -> successCount.incrementAndGet())
                        .onFailure(_ -> failureCount.incrementAndGet()))
                .toList();

        Promise.allOf(promises)
                .await(timeSpan(2).seconds())
                .onFailureRun(Assertions::fail);

        assertEquals(100, successCount.get(), "Should allow exactly rate permits");
        assertEquals(50, failureCount.get(), "Should reject excess requests");
    }

    @Test
    void shouldCreateSimpleRateLimiter() {
        var simpleLimiter = RateLimiter.create(10, timeSpan(1).seconds());

        for (int i = 0; i < 10; i++) {
            simpleLimiter.execute(() -> Promise.success("OK"))
                    .await()
                    .onFailureRun(Assertions::fail);
        }

        simpleLimiter.execute(() -> Promise.success("Fail"))
                .await()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(LimitExceeded.class, cause));
    }

    @Test
    void shouldComposeWithOtherPromises() {
        rateLimiter.execute(() -> Promise.success(42))
                .map(value -> value * 2)
                .flatMap(value -> Promise.success("Result: " + value))
                .await()
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals("Result: 84", value));
    }

    @Test
    void shouldHaveCorrectErrorMessages() {
        // Exhaust permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.execute(() -> Promise.success("OK")).await();
        }

        rateLimiter.execute(() -> Promise.success("Fail"))
                .await()
                .onFailure(cause -> {
                    assertTrue(cause.message().contains("Rate limit exceeded"));
                    assertTrue(cause.message().contains("Retry after"));
                });
    }

    @Test
    void shouldNotExceedMaxTokensAfterLongIdle() {
        var limiterWithBurst = RateLimiter.builder()
                .rate(5)
                .period(timeSpan(1).seconds())
                .burst(3)
                .timeSource(timeSource);

        // Advance time by 10 periods without using permits
        timeSource.advanceTime(10000);

        // Trigger refill by executing
        int successCount = 0;
        for (int i = 0; i < 20; i++) {
            var result = limiterWithBurst.execute(() -> Promise.success("OK")).await();
            if (result.isSuccess()) {
                successCount++;
            }
        }

        // Should not exceed rate + burst = 8
        assertEquals(8, successCount, "Should not exceed max tokens");
    }
}
