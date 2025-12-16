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
    ///
    /// @return A promise containing the result or a rate limit exceeded failure
    <T> Promise<T> execute(Supplier<Promise<T>> operation);

    sealed interface RateLimiterError extends Cause {
        record LimitExceeded(TimeSpan retryAfter) implements RateLimiterError {
            @Override
            public String message() {
                return "Rate limit exceeded. Retry after " + retryAfter;
            }
        }
    }

    /// Create a simple rate limiter with default settings.
    ///
    /// @param rate   Number of permits per period
    /// @param period Time period for rate calculation
    ///
    /// @return A new rate limiter
    static RateLimiter create(int rate, TimeSpan period) {
        return builder().rate(rate)
                        .period(period)
                        .withDefaultTimeSource();
    }

    /// Create a rate limiter builder.
    ///
    /// @return A new builder
    static StageRate builder() {
        return rate -> period -> new OptionalStage(rate, period, 0, null);
    }

    interface StageRate {
        StagePeriod rate(int permits);
    }

    interface StagePeriod {
        OptionalStage period(TimeSpan period);
    }

    record OptionalStage(int rate, TimeSpan period, int burst, TimeSource timeSource) {

        public OptionalStage burst(int extraPermits) {
            return new OptionalStage(rate, period, extraPermits, timeSource);
        }

        public RateLimiter timeSource(TimeSource source) {
            return createRateLimiter(rate, period, burst, source);
        }

        public RateLimiter withDefaultTimeSource() {
            return timeSource(TimeSource.system());
        }
    }

    private static RateLimiter createRateLimiter(int rate,
                                                 TimeSpan period,
                                                 int burst,
                                                 TimeSource timeSource) {
        record rateLimiter(int maxTokens,
                           int refillRate,
                           long refillPeriodNanos,
                           TimeSource timeSource,
                           long[] state // [0] = tokens, [1] = lastRefillTimeNanos
        ) implements RateLimiter {

            @Override
            public <T> Promise<T> execute(Supplier<Promise<T>> operation) {
                return tryAcquire().async()
                                   .flatMap(operation);
            }

            private synchronized Result<Unit> tryAcquire() {
                refill();

                if (state[0] >= 1) {
                    state[0]--;
                    return Result.unitResult();
                }

                return new RateLimiterError.LimitExceeded(calculateRetryAfter()).result();
            }

            private void refill() {
                long now = timeSource.nanoTime();
                long elapsed = now - state[1];

                if (elapsed >= refillPeriodNanos) {
                    long periods = elapsed / refillPeriodNanos;
                    long tokensToAdd = periods * refillRate;

                    state[0] = Math.min(maxTokens, state[0] + tokensToAdd);
                    state[1] += periods * refillPeriodNanos;
                }
            }

            private TimeSpan calculateRetryAfter() {
                long now = timeSource.nanoTime();
                long timeSinceLastRefill = now - state[1];
                long remainingTimeInCurrentPeriod = refillPeriodNanos - timeSinceLastRefill;

                return timeSpan(Math.max(1, remainingTimeInCurrentPeriod)).nanos();
            }
        }

        int maxTokens = rate + burst;
        long[] state = {maxTokens, timeSource.nanoTime()};

        return new rateLimiter(maxTokens, rate, period.nanos(), timeSource, state);
    }
}
