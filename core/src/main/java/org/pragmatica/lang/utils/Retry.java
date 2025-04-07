package org.pragmatica.lang.utils;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.io.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.pragmatica.lang.io.Timeout.timeout;

/// A utility class implementing the Retry pattern using Promise for handling asynchronous operations.
/// This implementation uses a staged fluent builder pattern where all parameters are mandatory.
///
/// ```java
/// Retry.create(5, exponential(timeout(100).millis(),
///                             timeout(5).seconds(),
///                             10.0,
///                             false))
///      .execute(operation)
///}
///```
public interface Retry {
    /**
     * Executes an asynchronous operation with retry logic.
     *
     * @param operation The async operation to retry
     * @param <T>       The type of result returned by the operation
     * @return A Promise containing the result of the successful operation
     */
    <T> Promise<T> execute(Supplier<Promise<T>> operation);

    /**
     * Create Retry with specified maximal number of attempts and delay calculation strategy.
     *
     * @param maxAttempts   maximal number of retries
     * @param backoffStrategy the delay calculation strategy to use
     *
     * @return created instance.
     */
    static Retry create(int maxAttempts, BackoffStrategy backoffStrategy) {
        record retry(int maxAttempts, BackoffStrategy backoffStrategy) implements Retry {
            @Override
            public <T> Promise<T> execute(Supplier<Promise<T>> operation) {
                return executeWithLoop(operation, 1, Promise.promise());
            }

            private <T> Promise<T> executeWithLoop(Supplier<Promise<T>> operation, int attempt, Promise<T> output) {
                operation.get()
                         .fold(result -> handle(operation, attempt, output, result));
                return output;
            }

            private <T> Promise<T> handle(Supplier<Promise<T>> operation,
                                          int attempt,
                                          Promise<T> output,
                                          Result<T> result) {
                return switch (result) {
                    case Result.Success<T> success -> output.succeed(success.value());
                    case Result.Failure<T> failure when (attempt >= maxAttempts) -> output.fail(failure.cause());
                    case Result.Failure<T> failure -> {
                        var delay = backoffStrategy.nextTimeout(attempt);

                        log.warn("Operation failed (attempt {}/{}), retrying after {}: {}",
                                 attempt, maxAttempts, delay, failure.cause().message());

                        SCHEDULER.schedule(() -> executeWithLoop(operation,
                                                                 attempt + 1,
                                                                 output),
                                           delay.milliseconds(),
                                           TimeUnit.MILLISECONDS);
                        yield output;
                    }
                };
            }

            private static final Logger log = LoggerFactory.getLogger(Retry.class);
            private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);
        }

        return new retry(maxAttempts, backoffStrategy);
    }

    interface BackoffStrategy {
        /**
         * Calculate the delay for a given retry attempt
         *
         * @param attempt the current attempt number (1-based)
         * @return next attempt delay
         */
        Timeout nextTimeout(int attempt);

        /**
         * Creates a fixed backoff strategy that always returns the same delay
         *
         * @param delay the fixed delay between retry attempts
         * @return a fixed backoff strategy
         */
        static BackoffStrategy fixed(Timeout delay) {
            record fixedBackoffStrategy(Timeout delay) implements BackoffStrategy {
                @Override
                public Timeout nextTimeout(int attempt) {
                    return delay;
                }
            }
            return new fixedBackoffStrategy(delay);
        }

        /**
         * Creates an exponential backoff strategy with configurable parameters
         *
         * @param initialDelay the initial delay
         * @param maxDelay     the maximum delay
         * @param factor       the multiplier for each successive delay
         * @param withJitter   whether to add random jitter
         * @return an exponential backoff strategy
         */
        static BackoffStrategy exponential(
                Timeout initialDelay,
                Timeout maxDelay,
                double factor,
                boolean withJitter) {

            record exponentialBackoffStrategy(Timeout initialDelay, Timeout maxDelay, double factor,
                                              boolean withJitter) implements BackoffStrategy {
                @Override
                public Timeout nextTimeout(int attempt) {
                    var multiplier = Math.pow(factor, attempt - 1);

                    if (withJitter) {
                        // Add jitter between 0.9 and 1.1
                        multiplier *= 0.9 + Math.random() * 0.2;
                    }

                    long delay = (long) (initialDelay.nanoseconds() * multiplier);

                    return timeout(Math.min(delay, maxDelay.nanoseconds())).nanos();
                }
            }
            return new exponentialBackoffStrategy(initialDelay, maxDelay, factor, withJitter);
        }

        /**
         * Creates a linear backoff strategy with configurable parameters
         *
         * @param initialDelay the initial delay
         * @param increment    the increment to add for each retry
         * @param maxDelay     the maximum delay
         * @return a linear backoff strategy
         */
        static BackoffStrategy linear(Timeout initialDelay, Timeout increment, Timeout maxDelay) {
            record linearBackoffStrategy(Timeout initialDelay, Timeout increment, Timeout maxDelay) implements
                    BackoffStrategy {
                @Override
                public Timeout nextTimeout(int attempt) {
                    long delay = initialDelay.nanoseconds() + (increment.nanoseconds() * (attempt - 1));
                    return timeout(Math.min(delay, maxDelay.nanoseconds())).nanos();
                }
            }
            return new linearBackoffStrategy(initialDelay, increment, maxDelay);
        }
    }
}