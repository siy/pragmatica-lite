package org.pragmatica.lang.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.Timeout;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.Promise.success;
import static org.pragmatica.lang.io.Timeout.timeout;
import static org.pragmatica.lang.utils.Causes.cause;
import static org.pragmatica.lang.utils.Retry.BackoffStrategy.*;
import static org.pragmatica.lang.utils.Retry.create;

class RetryTest {
    private static final Timeout SHORT_DURATION = timeout(10).millis();
    private static final Timeout MEDIUM_DURATION = timeout(50).millis();
    private static final Timeout LONG_DURATION = timeout(100).millis();

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

        create(5, fixed(SHORT_DURATION))
                .execute(operation)
                .await()
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals("Success on attempt 3", value));

        assertEquals(3, attempts.get());
    }

    @Test
    void shouldFailAfterMaxAttempts() {
        var attempts = new AtomicInteger(0);

        Supplier<Promise<String>> operation = () -> cause("Simulated failure " + attempts.incrementAndGet()).promise();

        create(3, fixed(SHORT_DURATION))
                .execute(operation)
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

        create(5, fixed(SHORT_DURATION))
                .execute(operation)
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

        create(5, fixed(timeout(fixedDuration).millis()))
                .execute(operation)
                .await();

        var interval = intervalBetweenAttempts.get();

        assertNotNull(interval);

        assertTrue(interval >= fixedDuration * 0.8,
                   "Expected interval to be approximately " + fixedDuration + "ms but was " + interval + "ms");
    }

    @Test
    void shouldUseExponentialBackoffStrategy() {
        var attempts = new AtomicInteger(0);
        var lastExecutionTime = new AtomicReference<>(System.currentTimeMillis());
        var firstInterval = new AtomicReference<Long>();
        var secondInterval = new AtomicReference<Long>();

        Supplier<Promise<String>> operation = () -> {
            var attempt = attempts.incrementAndGet();
            var now = System.currentTimeMillis();
            var timeSinceLastExecution = now - lastExecutionTime.get();

            lastExecutionTime.set(now);

            if (attempt == 2) {
                firstInterval.set(timeSinceLastExecution);
            } else if (attempt == 3) {
                secondInterval.set(timeSinceLastExecution);
            }

            if (attempt < 4) {
                return cause("Simulated failure").promise();
            } else {
                return success("Success");
            }
        };

        var initialDelay = 50L;
        var factor = 2.0;

        create(5,
               exponential(timeout(initialDelay).millis(),
                           timeout(1).seconds(),
                           factor,
                           false))

                .execute(operation)
                .await();

        // Assert that the second interval is approximately twice the first
        var first = firstInterval.get();
        var second = secondInterval.get();

        assertNotNull(first);
        assertNotNull(second);

        var ratio = (double) second / first;

        assertTrue(ratio >= factor * 0.8 && ratio <= factor * 1.2,
                   "Expected ratio close to " + factor + " but was " + ratio);
    }

    @Test
    void shouldUseLinearBackoffStrategy() {
        var attempts = new AtomicInteger(0);
        var lastExecutionTime = new AtomicReference<>(System.currentTimeMillis());
        var firstInterval = new AtomicReference<Long>();
        var secondInterval = new AtomicReference<Long>();

        Supplier<Promise<String>> operation = () -> {
            var attempt = attempts.incrementAndGet();
            var now = System.currentTimeMillis();
            var timeSinceLastExecution = now - lastExecutionTime.get();

            lastExecutionTime.set(now);

            if (attempt == 2) {
                firstInterval.set(timeSinceLastExecution);
            } else if (attempt == 3) {
                secondInterval.set(timeSinceLastExecution);
            }

            if (attempt < 4) {
                return cause("Simulated failure").promise();
            } else {
                return success("Success");
            }
        };

        var initialDelay = 50L;
        var increment = 50L;

        create(5,
               linear(timeout(initialDelay).millis(),
                      timeout(increment).millis(),
                      timeout(1).seconds()))
                .execute(operation)
                .await();

        var first = firstInterval.get();
        var second = secondInterval.get();

        assertNotNull(first);
        assertNotNull(second);

        var actualDifference = second - first;

        assertTrue(Math.abs(actualDifference - increment) < increment * 0.3,
                   "Expected difference close to " + increment + "ms but was " + actualDifference + "ms");
    }

    @Test
    void shouldRespectMaxDelay() {
        var attempts = new AtomicInteger(0);
        var lastExecutionTime = new AtomicReference<>(System.currentTimeMillis());
        var maxInterval = new AtomicReference<>(0L);

        Supplier<Promise<String>> operation = () -> {
            var attempt = attempts.incrementAndGet();
            var now = System.currentTimeMillis();
            var interval = now - lastExecutionTime.get();

            lastExecutionTime.set(now);

            if (attempt > 1) {
                if (interval > maxInterval.get()) {
                    maxInterval.set(interval);
                }
            }

            return cause("Always fails").promise();
        };

        var initialDelay = 10L;
        var maxDelay = 100L;

        create(5,
               exponential(timeout(initialDelay).millis(),
                           timeout(maxDelay).millis(),
                           10.0,
                           false))
                .execute(operation)
                .await();

        assertTrue(maxInterval.get() <= maxDelay * 1.2,
                   "Expected max interval to be no more than " + maxDelay + "ms but was " + maxInterval.get() + "ms");
    }

    @Test
    void shouldHandlePromiseCompletingLater() {
        var attempts = new AtomicInteger(0);

        Supplier<Promise<String>> operation = () -> {
            var attempt = attempts.incrementAndGet();
            return Promise.promise(timeout(50).millis(),
                                   promise -> {
                                       if (attempt < 3) {
                                           promise.fail(cause("Simulated failure " + attempt));
                                       } else {
                                           promise.succeed("Success on attempt " + attempt);
                                       }
                                   });

        };

        create(5, fixed(SHORT_DURATION))
                .execute(operation)
                .await()
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals("Success on attempt 3", value));

        assertEquals(3, attempts.get());
    }

    @Test
    void exponentialBackoffWithJitterShouldHaveVariableDelays() {
        var strategy = exponential(
                MEDIUM_DURATION,
                LONG_DURATION,
                2.0,
                true);

        // Then - With jitter, two calls with the same attempt should usually produce different delays
        // Note: There's a small chance they could be the same by coincidence
        int samples = 10;
        int differentValues = 0;

        for (int i = 0; i < samples; i++) {
            var delayA = strategy.nextTimeout(2);
            var delayB = strategy.nextTimeout(2);

            if (!delayA.equals(delayB)) {
                differentValues++;
            }
        }

        // We expect the majority of samples to be different with jitter enabled
        assertTrue(differentValues > samples * 0.6,
                   "Expected most delays to be different with jitter enabled, but got only "
                           + differentValues + " different values out of " + samples);
    }

    @Test
    void backoffStrategyShouldCalculateCorrectDelays() {
        var fixedStrategy = fixed(MEDIUM_DURATION);

        assertEquals(MEDIUM_DURATION, fixedStrategy.nextTimeout(1));
        assertEquals(MEDIUM_DURATION, fixedStrategy.nextTimeout(2));
        assertEquals(MEDIUM_DURATION, fixedStrategy.nextTimeout(10));

        var initialDelay = SHORT_DURATION;
        var increment = SHORT_DURATION;
        var maxDelay = LONG_DURATION;

        var linearStrategy = linear(
                initialDelay,
                increment,
                maxDelay);

        assertEquals(initialDelay, linearStrategy.nextTimeout(1));
        assertEquals(timeout(initialDelay.nanoseconds() + increment.nanoseconds()).nanos(),
                     linearStrategy.nextTimeout(2));
        assertEquals(timeout(initialDelay.nanoseconds() + 2 * increment.nanoseconds()).nanos(),
                     linearStrategy.nextTimeout(3));

        var delayForHighAttempt = linearStrategy.nextTimeout(15);

        assertTrue(delayForHighAttempt.compareTo(maxDelay) <= 0,
                   "Expected delay to be capped at " + maxDelay + " but was " + delayForHighAttempt);

        var exponentialStrategy = exponential(
                SHORT_DURATION,
                LONG_DURATION,
                2.0,
                false);

        assertEquals(SHORT_DURATION, exponentialStrategy.nextTimeout(1));
        assertEquals(timeout(SHORT_DURATION.nanoseconds() * 2).nanos(), exponentialStrategy.nextTimeout(2));
        assertEquals(timeout(SHORT_DURATION.nanoseconds() * 4).nanos(), exponentialStrategy.nextTimeout(3));

        var expDelayForHighAttempt = exponentialStrategy.nextTimeout(20);

        assertTrue(expDelayForHighAttempt.compareTo(LONG_DURATION) <= 0,
                   "Expected delay to be capped at " + LONG_DURATION + " but was " + expDelayForHighAttempt);
    }

    @Test
    void shouldHandleConcurrentOperations() {
        var operationCount = 5;
        var attemptCounts = new AtomicInteger[operationCount];

        for (int i = 0; i < operationCount; i++) {
            attemptCounts[i] = new AtomicInteger(0);
        }

        Retry retry = create(3, fixed(SHORT_DURATION));

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