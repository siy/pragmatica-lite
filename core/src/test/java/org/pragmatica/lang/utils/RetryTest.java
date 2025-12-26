package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.TimeSpan;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.Promise.success;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;
import static org.pragmatica.lang.utils.Causes.cause;
import static org.pragmatica.lang.utils.Retry.BackoffStrategy.*;
import static org.pragmatica.lang.utils.Retry.create;

class RetryTest {
    private static final TimeSpan SHORT = timeSpan(10).millis();
    private static final TimeSpan MEDIUM = timeSpan(50).millis();
    private static final TimeSpan LONG = timeSpan(100).millis();

    @Test
    void shouldRetryAndEventuallySucceed() {
        var attempts = new AtomicInteger(0);

        Supplier<Promise<String>> operation = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                return cause("Simulated failure " + attempt).promise();
            } else {
                return success("Success on attempt " + attempt);
            }
        };

        var retry = create().attempts(5)
                            .strategy(fixed().interval(SHORT));

        retry.execute(operation)
             .await()
             .onFailureRun(Assertions::fail)
             .onSuccess(value -> assertEquals("Success on attempt 3", value));

        assertEquals(3, attempts.get());
    }

    @Test
    void shouldFailAfterMaxAttempts() {
        var attempts = new AtomicInteger(0);

        Supplier<Promise<String>> operation = () -> cause("Simulated failure " + attempts.incrementAndGet()).promise();

        var retry = create().attempts(3)
                            .strategy(fixed().interval(SHORT));

        retry.execute(operation)
             .await()
             .onSuccessRun(Assertions::fail)
             .onFailure(cause -> assertTrue(cause.message().contains("Simulated failure 3")));

        assertEquals(3, attempts.get());
    }

    @Test
    void shouldNotRetryOnSuccess() {
        var attempts = new AtomicInteger(0);

        Supplier<Promise<String>> operation = () -> {
            attempts.incrementAndGet();

            return success("Immediate success");
        };

        var retry = create().attempts(5)
                            .strategy(fixed().interval(SHORT));

        retry.execute(operation)
             .await()
             .onFailureRun(Assertions::fail)
             .onSuccess(value -> assertEquals("Immediate success", value));

        assertEquals(1, attempts.get());
    }

    @Test
    void shouldUseFixedBackoffStrategy() {
        var attempts = new AtomicInteger(0);
        var lastExecutionTime = new AtomicReference<>(System.currentTimeMillis());
        var intervalBetweenAttempts = new AtomicReference<Long>();

        Supplier<Promise<String>> operation = () -> {
            int attempt = attempts.incrementAndGet();
            long now = System.currentTimeMillis();

            if (attempt > 1) {
                intervalBetweenAttempts.set(now - lastExecutionTime.get());
            }
            lastExecutionTime.set(now);

            if (attempt < 3) {
                return cause("Simulated failure").promise();
            } else {
                return success("Success");
            }
        };

        var fixedDuration = 100L;

        var retry = create().attempts(5)
                            .strategy(fixed().interval(timeSpan(fixedDuration).millis()));

        retry.execute(operation)
             .await();

        var interval = intervalBetweenAttempts.get();

        assertNotNull(interval);

        assertTrue(interval >= fixedDuration * 0.8,
                   "Expected interval to be approximately " + fixedDuration + "ms but was " + interval + "ms");
    }

    @Test
    void shouldUseExponentialBackoffStrategy() {
        // Test strategy calculation directly (not actual elapsed time which is system-dependent)
        var initialDelay = timeSpan(50).millis();
        var factor = 2.0;

        var strategy = exponential()
                .initialDelay(initialDelay)
                .maxDelay(timeSpan(1).seconds())
                .factor(factor)
                .withoutJitter();

        // Verify exponential progression
        var first = strategy.nextTimeout(1);
        var second = strategy.nextTimeout(2);
        var third = strategy.nextTimeout(3);

        assertEquals(initialDelay, first);
        assertEquals(timeSpan(initialDelay.nanos() * 2).nanos(), second);
        assertEquals(timeSpan(initialDelay.nanos() * 4).nanos(), third);

        // Verify the ratio between consecutive timeouts
        var ratio = (double) second.nanos() / first.nanos();
        assertEquals(factor, ratio, 0.01, "Expected ratio to be " + factor);
    }

    @Test
    void shouldUseLinearBackoffStrategy() {
        // Test strategy calculation directly (not actual elapsed time which is system-dependent)
        var initialDelay = timeSpan(50).millis();
        var increment = timeSpan(50).millis();

        var strategy = linear()
                .initialDelay(initialDelay)
                .increment(increment)
                .maxDelay(timeSpan(1).seconds());

        // Verify linear progression
        var first = strategy.nextTimeout(1);
        var second = strategy.nextTimeout(2);
        var third = strategy.nextTimeout(3);

        assertEquals(initialDelay, first);
        assertEquals(timeSpan(initialDelay.nanos() + increment.nanos()).nanos(), second);
        assertEquals(timeSpan(initialDelay.nanos() + 2 * increment.nanos()).nanos(), third);

        // Verify constant difference between consecutive timeouts
        var diff1 = second.nanos() - first.nanos();
        var diff2 = third.nanos() - second.nanos();
        assertEquals(increment.nanos(), diff1);
        assertEquals(increment.nanos(), diff2);
    }

    @Test
    void shouldRespectMaxDelay() {
        // Test strategy's capping behavior directly (not actual elapsed time which is system-dependent)
        var initialDelay = timeSpan(10).millis();
        var maxDelay = timeSpan(100).millis();

        var strategy = exponential()
                .initialDelay(initialDelay)
                .maxDelay(maxDelay)
                .factor(10.0)
                .withoutJitter();

        // Attempt 1: 10ms
        assertEquals(initialDelay, strategy.nextTimeout(1));

        // Attempt 2: 10 * 10 = 100ms (at max)
        assertEquals(maxDelay, strategy.nextTimeout(2));

        // Attempt 3+: would be 1000ms+ but capped at 100ms
        for (int attempt = 3; attempt <= 10; attempt++) {
            var delay = strategy.nextTimeout(attempt);
            assertTrue(delay.compareTo(maxDelay) <= 0,
                       "Expected delay for attempt " + attempt + " to be capped at " + maxDelay + " but was " + delay);
        }
    }

    @Test
    void shouldHandlePromiseCompletingLater() {
        var attempts = new AtomicInteger(0);

        Supplier<Promise<String>> operation = () -> {
            var attempt = attempts.incrementAndGet();
            return Promise.promise(timeSpan(50).millis(),
                                   promise -> {
                                       if (attempt < 3) {
                                           promise.fail(cause("Simulated failure " + attempt));
                                       } else {
                                           promise.succeed("Success on attempt " + attempt);
                                       }
                                   });

        };

        var retry = create().attempts(5)
                            .strategy(fixed().interval(SHORT));
        retry.execute(operation)
             .await()
             .onFailureRun(Assertions::fail)
             .onSuccess(value -> assertEquals("Success on attempt 3", value));

        assertEquals(3, attempts.get());
    }

    @Test
    void exponentialBackoffWithJitterShouldHaveVariableDelays() {
        var strategy = exponential().initialDelay(LONG)
                                    .maxDelay(LONG.plus(LONG))
                                    .factor(2.0)
                                    .withJitter();

        // With jitter, generating multiple delays should produce some variation
        // We collect unique values and expect more than one distinct delay
        int samples = 20;
        var uniqueDelays = new java.util.HashSet<TimeSpan>();

        for (int i = 0; i < samples; i++) {
            uniqueDelays.add(strategy.nextTimeout(2));
        }

        // With jitter enabled, we should see at least 2 different delay values
        assertTrue(uniqueDelays.size() >= 2,
                   "Expected jitter to produce variable delays, but got only "
                           + uniqueDelays.size() + " unique values out of " + samples + " samples");
    }

    @Test
    void backoffStrategyShouldCalculateCorrectDelays() {
        var fixedStrategy = fixed().interval(MEDIUM);

        assertEquals(MEDIUM, fixedStrategy.nextTimeout(1));
        assertEquals(MEDIUM, fixedStrategy.nextTimeout(2));
        assertEquals(MEDIUM, fixedStrategy.nextTimeout(10));

        var initialDelay = SHORT;
        var increment = SHORT;
        var maxDelay = LONG;

        var linearStrategy = linear().initialDelay(initialDelay)
                                     .increment(increment)
                                     .maxDelay(maxDelay);

        assertEquals(initialDelay, linearStrategy.nextTimeout(1));
        assertEquals(timeSpan(initialDelay.nanos() + increment.nanos()).nanos(),
                     linearStrategy.nextTimeout(2));
        assertEquals(timeSpan(initialDelay.nanos() + 2 * increment.nanos()).nanos(),
                     linearStrategy.nextTimeout(3));

        var delayForHighAttempt = linearStrategy.nextTimeout(15);

        assertTrue(delayForHighAttempt.compareTo(maxDelay) <= 0,
                   "Expected delay to be capped at " + maxDelay + " but was " + delayForHighAttempt);

        var exponentialStrategy = exponential().initialDelay(SHORT)
                                               .maxDelay(LONG)
                                               .factor(2.0)
                                               .withoutJitter();

        assertEquals(SHORT, exponentialStrategy.nextTimeout(1));
        assertEquals(timeSpan(SHORT.nanos() * 2).nanos(), exponentialStrategy.nextTimeout(2));
        assertEquals(timeSpan(SHORT.nanos() * 4).nanos(), exponentialStrategy.nextTimeout(3));

        var expDelayForHighAttempt = exponentialStrategy.nextTimeout(20);

        assertTrue(expDelayForHighAttempt.compareTo(LONG) <= 0,
                   "Expected delay to be capped at " + LONG + " but was " + expDelayForHighAttempt);
    }

    @Test
    void shouldHandleConcurrentOperations() {
        var operationCount = 5;
        var attemptCounts = new AtomicInteger[operationCount];

        for (int i = 0; i < operationCount; i++) {
            attemptCounts[i] = new AtomicInteger(0);
        }

        var retry = create().attempts(3)
                            .strategy(fixed().interval(SHORT));

        var promises = IntStream.range(0, operationCount)
                                .mapToObj(index -> retry.execute(() -> {
                                    var attempt = attemptCounts[index].incrementAndGet();

                                    if (attempt < 3) {
                                        return cause("Failure " + index + ":" + attempt).promise();
                                    } else {
                                        return success("Success " + index);
                                    }
                                }))
                                .toList();

        Promise.allOf(promises)
               .await()
               .onFailureRun(Assertions::fail);

        for (int i = 0; i < operationCount; i++) {
            assertEquals(3, attemptCounts[i].get(), "Operation " + i + " should have exactly 3 attempts");
        }
    }
}
