/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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
 *
 */

package org.pragmatica.lang.utils;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;

import java.util.function.Supplier;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/// A rate limiter implementation using Token Bucket algorithm.
/// Thread-safe, one instance per protected resource/endpoint.
public interface RateLimiter {

    /// Execute an operation if a permit is available, otherwise fail immediately.
    ///
    /// @param operation The promise-returning operation to execute
    /// @param <T>       The return type of the operation
    /// @return A promise containing the result or a rate limit exceeded failure
    <T> Promise<T> execute(Supplier<Promise<T>> operation);

    /// Execute an operation, waiting for a permit if necessary.
    ///
    /// @param operation The promise-returning operation to execute
    /// @param <T>       The return type of the operation
    /// @return A promise containing the result or a timeout failure
    <T> Promise<T> executeWithWait(Supplier<Promise<T>> operation);

    /// Try to acquire a single permit without blocking.
    ///
    /// @return Success if permit acquired, failure with LimitExceeded otherwise
    Result<Unit> tryAcquire();

    /// Try to acquire multiple permits without blocking.
    ///
    /// @param permits Number of permits to acquire
    /// @return Success if permits acquired, failure with LimitExceeded otherwise
    Result<Unit> tryAcquire(int permits);

    /// Acquire a single permit, waiting if necessary.
    ///
    /// @return A promise that resolves when permit is acquired or timeout occurs
    Promise<Unit> acquire();

    /// Acquire multiple permits, waiting if necessary.
    ///
    /// @param permits Number of permits to acquire
    /// @return A promise that resolves when permits are acquired or timeout occurs
    Promise<Unit> acquire(int permits);

    /// Get the current number of available permits.
    ///
    /// @return Available permit count
    int availablePermits();

    interface TimeSource {
        long nanoTime();
    }

    sealed interface RateLimiterError extends Cause {
        record LimitExceeded(TimeSpan retryAfter) implements RateLimiterError {
            @Override
            public String message() {
                return "Rate limit exceeded. Retry after " + retryAfter;
            }
        }

        record Timeout(TimeSpan waited) implements RateLimiterError {
            @Override
            public String message() {
                return "Timeout after waiting " + waited + " for rate limit permit";
            }
        }
    }

    /// Create a simple rate limiter with default settings.
    ///
    /// @param rate   Number of permits per period
    /// @param period Time period for rate calculation
    /// @return A new rate limiter
    static RateLimiter create(int rate, TimeSpan period) {
        return builder()
                .rate(rate)
                .period(period)
                .withDefaultTimeSource();
    }

    /// Create a rate limiter builder.
    ///
    /// @return A new builder
    static StageRate builder() {
        return rate ->
                period ->
                        new OptionalStage(rate, period, 0, null, null);
    }

    interface StageRate {
        StagePeriod rate(int permits);
    }

    interface StagePeriod {
        OptionalStage period(TimeSpan period);
    }

    record OptionalStage(int rate, TimeSpan period, int burst, TimeSpan timeout, TimeSource timeSource) {

        public OptionalStage burst(int extraPermits) {
            return new OptionalStage(rate, period, extraPermits, timeout, timeSource);
        }

        public OptionalStage timeout(TimeSpan maxWait) {
            return new OptionalStage(rate, period, burst, maxWait, timeSource);
        }

        public RateLimiter timeSource(TimeSource source) {
            return createRateLimiter(rate, period, burst, timeout, source);
        }

        public RateLimiter withDefaultTimeSource() {
            return timeSource(System::nanoTime);
        }
    }

    private static RateLimiter createRateLimiter(int rate,
                                                  TimeSpan period,
                                                  int burst,
                                                  TimeSpan timeout,
                                                  TimeSource timeSource) {
        return new RateLimiterImpl(rate, period, burst, timeout, timeSource);
    }
}

final class RateLimiterImpl implements RateLimiter {
    private final int maxTokens;
    private final int refillRate;
    private final long refillPeriodNanos;
    private final TimeSpan timeout;
    private final TimeSource timeSource;

    private long tokens;
    private long lastRefillTimeNanos;

    RateLimiterImpl(int rate, TimeSpan period, int burst, TimeSpan timeout, TimeSource timeSource) {
        this.refillRate = rate;
        this.maxTokens = rate + burst;
        this.refillPeriodNanos = period.nanos();
        this.timeout = timeout;
        this.timeSource = timeSource;
        this.tokens = maxTokens;
        this.lastRefillTimeNanos = timeSource.nanoTime();
    }

    @Override
    public <T> Promise<T> execute(Supplier<Promise<T>> operation) {
        return tryAcquire().fold(
                Cause::promise,
                _ -> operation.get()
        );
    }

    @Override
    public <T> Promise<T> executeWithWait(Supplier<Promise<T>> operation) {
        return acquire().flatMap(_ -> operation.get());
    }

    @Override
    public synchronized Result<Unit> tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public synchronized Result<Unit> tryAcquire(int permits) {
        refill();

        if (tokens >= permits) {
            tokens -= permits;
            return Result.success(Unit.unit());
        }

        return new RateLimiterError.LimitExceeded(calculateRetryAfter(permits)).result();
    }

    @Override
    public Promise<Unit> acquire() {
        return acquire(1);
    }

    @Override
    public Promise<Unit> acquire(int permits) {
        var result = tryAcquire(permits);

        return result.fold(
                cause -> {
                    if (cause instanceof RateLimiterError.LimitExceeded limited) {
                        var waitTime = limited.retryAfter();

                        if (timeout != null && waitTime.compareTo(timeout) > 0) {
                            return new RateLimiterError.Timeout(timeout).promise();
                        }

                        var promise = Promise.<Unit>promise();
                        SharedScheduler.schedule(
                                () -> acquire(permits)
                                        .onSuccess(unit -> promise.resolve(Result.success(unit)))
                                        .onFailure(c -> promise.resolve(Result.failure(c))),
                                waitTime
                        );
                        return promise;
                    }
                    return cause.promise();
                },
                _ -> Promise.success(Unit.unit())
        );
    }

    @Override
    public synchronized int availablePermits() {
        refill();
        return (int) tokens;
    }

    private void refill() {
        long now = timeSource.nanoTime();
        long elapsed = now - lastRefillTimeNanos;

        if (elapsed >= refillPeriodNanos) {
            long periods = elapsed / refillPeriodNanos;
            long tokensToAdd = periods * refillRate;

            tokens = Math.min(maxTokens, tokens + tokensToAdd);
            lastRefillTimeNanos += periods * refillPeriodNanos;
        }
    }

    private TimeSpan calculateRetryAfter(int permits) {
        long tokensNeeded = permits - tokens;
        long periodsNeeded = (tokensNeeded + refillRate - 1) / refillRate;
        long nanosToWait = periodsNeeded * refillPeriodNanos;

        long now = timeSource.nanoTime();
        long timeSinceLastRefill = now - lastRefillTimeNanos;
        long remainingTimeInCurrentPeriod = refillPeriodNanos - timeSinceLastRefill;

        if (remainingTimeInCurrentPeriod > 0 && periodsNeeded > 0) {
            nanosToWait = remainingTimeInCurrentPeriod + (periodsNeeded - 1) * refillPeriodNanos;
        }

        return timeSpan(Math.max(1, nanosToWait)).nanos();
    }
}
