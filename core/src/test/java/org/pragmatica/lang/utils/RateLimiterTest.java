package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.RateLimiter.RateLimiterError.LimitExceeded;
import org.pragmatica.lang.utils.RateLimiter.RateLimiterError.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class RateLimiterTest {

    private RateLimiter rateLimiter;
    private TestTimeSource timeSource;

    private static class TestTimeSource implements RateLimiter.TimeSource {
        private TimeSpan currentTime = timeSpan(0).nanos();

        @Override
        public long nanoTime() {
            return currentTime.nanos();
        }

        public void advanceTime(long millis) {
            currentTime = currentTime.plus(millis, TimeUnit.MILLISECONDS);
        }

        public void advanceTimeNanos(long nanos) {
            currentTime = currentTime.plus(nanos, TimeUnit.NANOSECONDS);
        }
    }

    @BeforeEach
    void setUp() {
        timeSource = new TestTimeSource();

        rateLimiter = RateLimiter.builder()
                .rate(5)
                .period(timeSpan(1).seconds())
                .burst(0)
                .timeout(timeSpan(5).seconds())
                .timeSource(timeSource);
    }

    @Test
    void shouldAcquirePermitsWhenAvailable() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire()
                    .onFailureRun(Assertions::fail);
        }

        assertEquals(0, rateLimiter.availablePermits());
    }

    @Test
    void shouldRejectWhenNoPermitsAvailable() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire().onFailureRun(Assertions::fail);
        }

        // Next request should fail
        rateLimiter.tryAcquire()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(LimitExceeded.class, cause));
    }

    @Test
    void shouldRefillPermitsAfterPeriod() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire().onFailureRun(Assertions::fail);
        }

        assertEquals(0, rateLimiter.availablePermits());

        // Advance time by one period
        timeSource.advanceTime(1000);

        // Permits should be refilled
        assertEquals(5, rateLimiter.availablePermits());

        // Should be able to acquire again
        rateLimiter.tryAcquire().onFailureRun(Assertions::fail);
    }

    @Test
    void shouldAllowBurstCapacity() {
        var limiterWithBurst = RateLimiter.builder()
                .rate(5)
                .period(timeSpan(1).seconds())
                .burst(3)
                .timeSource(timeSource);

        // Should be able to acquire rate + burst = 8 permits
        for (int i = 0; i < 8; i++) {
            final int iteration = i;
            limiterWithBurst.tryAcquire()
                    .onFailure(cause -> Assertions.fail("Should have " + (8 - iteration) + " permits available"));
        }

        // 9th should fail
        limiterWithBurst.tryAcquire()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(LimitExceeded.class, cause));
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
    void shouldRejectExecutionWhenNoPermits() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire().onFailureRun(Assertions::fail);
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
    void shouldProvideRetryAfterInLimitExceeded() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire().onFailureRun(Assertions::fail);
        }

        rateLimiter.tryAcquire()
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
    void shouldAcquireMultiplePermitsAtOnce() {
        rateLimiter.tryAcquire(3)
                .onFailureRun(Assertions::fail);

        assertEquals(2, rateLimiter.availablePermits());

        rateLimiter.tryAcquire(3)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(LimitExceeded.class, cause));
    }

    @Test
    void shouldPartiallyRefillTokens() {
        // Exhaust all permits
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire().onFailureRun(Assertions::fail);
        }

        // Advance time by half a period
        timeSource.advanceTime(500);

        // Should still have 0 permits (refill happens at period boundaries)
        assertEquals(0, rateLimiter.availablePermits());

        // Advance to full period
        timeSource.advanceTime(500);

        // Now should have all permits back
        assertEquals(5, rateLimiter.availablePermits());
    }

    @Test
    void shouldAccumulatePermitsOverMultiplePeriods() {
        var limiterWithBurst = RateLimiter.builder()
                .rate(5)
                .period(timeSpan(1).seconds())
                .burst(10)
                .timeSource(timeSource);

        // Use some permits
        for (int i = 0; i < 10; i++) {
            limiterWithBurst.tryAcquire().onFailureRun(Assertions::fail);
        }

        assertEquals(5, limiterWithBurst.availablePermits());

        // Advance time by 2 periods
        timeSource.advanceTime(2000);

        // Should have min(maxTokens=15, 5 + 10) = 15 permits
        assertEquals(15, limiterWithBurst.availablePermits());
    }

    @Test
    void shouldNotExceedMaxTokens() {
        var limiterWithBurst = RateLimiter.builder()
                .rate(5)
                .period(timeSpan(1).seconds())
                .burst(3)
                .timeSource(timeSource);

        // Advance time by 10 periods without using permits
        timeSource.advanceTime(10000);

        // Should not exceed rate + burst = 8
        assertEquals(8, limiterWithBurst.availablePermits());
    }

    @Test
    void shouldHandleConcurrentAcquisitions() {
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
            simpleLimiter.tryAcquire().onFailureRun(Assertions::fail);
        }

        simpleLimiter.tryAcquire()
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
            rateLimiter.tryAcquire().onFailureRun(Assertions::fail);
        }

        rateLimiter.tryAcquire()
                .onFailure(cause -> {
                    assertTrue(cause.message().contains("Rate limit exceeded"));
                    assertTrue(cause.message().contains("Retry after"));
                });

        var timeoutError = new Timeout(timeSpan(5).seconds());
        assertTrue(timeoutError.message().contains("Timeout"));
        assertTrue(timeoutError.message().contains("waiting"));
    }

    @Test
    void shouldRespectTimeoutInAcquire() {
        // Create limiter with short timeout
        var limiterWithTimeout = RateLimiter.builder()
                .rate(1)
                .period(timeSpan(10).seconds())  // Very long period
                .timeout(timeSpan(100).millis())  // Short timeout
                .timeSource(timeSource);

        // Exhaust the permit
        limiterWithTimeout.tryAcquire().onFailureRun(Assertions::fail);

        // Acquire should timeout since it would need to wait 10 seconds
        limiterWithTimeout.acquire()
                .await()
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertInstanceOf(Timeout.class, cause));
    }
}
