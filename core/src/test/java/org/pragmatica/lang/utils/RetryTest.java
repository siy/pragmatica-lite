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

        var retry = create().attempts(5)
                            .strategy(exponential().initialDelay(timeSpan(initialDelay).millis())
                                                   .maxDelay(timeSpan(1).seconds())
                                                   .factor(factor)
                                                   .withoutJitter());

        retry.execute(operation)
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

        var retry = create().attempts(5)
                            .strategy(linear().initialDelay(timeSpan(initialDelay).millis())
                                              .increment(timeSpan(increment).millis())
                                              .maxDelay(timeSpan(1).seconds()));
        retry.execute(operation)
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

        var retry = create().attempts(5)
                            .strategy(exponential()
                                              .initialDelay(timeSpan(initialDelay).millis())
                                              .maxDelay(timeSpan(maxDelay).millis())
                                              .factor(10.0)
                                              .withoutJitter());
        retry.execute(operation)
             .await();

        assertTrue(maxInterval.get() <= maxDelay * 1.2,
                   "Expected max interval to be no more than " + maxDelay + "ms but was " + maxInterval.get() + "ms");
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

        // We expect at least half of samples to be different with jitter enabled
        assertTrue(differentValues >= samples * 0.5,
                   "Expected most delays to be different with jitter enabled, but got only "
                           + differentValues + " different values out of " + samples);
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
