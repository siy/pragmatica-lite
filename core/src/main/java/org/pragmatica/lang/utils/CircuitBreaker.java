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
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.CircuitBreaker.CircuitBreakerErrors.CircuitBreakerOpenError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/// A circuit breaker implementation to prevent cascading failures.
/// Implementation is thread safe and several threads can simultaneously access
/// service protected by the CircuitBreaker. Each service must have its own instance of
/// CircuitBreaker though.
public interface CircuitBreaker {
    /// Execute an operation through the circuit breaker.
    ///
    /// @param operation The promise-returning operation to execute
    /// @param <T>       The return type of the operation
    ///
    /// @return A promise containing the result or a circuit-open failure
    <T> Promise<T> execute(Supplier<Promise<T>> operation);

    /// Gets the current state of the circuit breaker.
    ///
    /// @return The current state
    State state();

    /// Gets the number of failures since last reset.
    ///
    /// @return The failure count
    long failureCount();

    /// Gets the interval since last state change.
    ///
    /// @return The last state change timestamp
    TimeSpan timeSinceLastStateChange();

    enum State {
        CLOSED,     // Circuit is closed, operations are executed normally
        OPEN,       // Circuit is open, operations are rejected immediately
        HALF_OPEN   // Circuit is testing if service is recovered
    }

    interface TimeSource {
        long nanoTime();
    }

    static CircuitBreaker create(int failureThreshold,
                                 TimeSpan resetTimeout,
                                 int testAttempts,
                                 Predicate<Cause> shouldTrip,
                                 TimeSource timeSource) {
        record circuitBreaker(int failureThreshold,
                              TimeSpan resetTimeout,
                              int testAttempts,
                              FluentPredicate<Cause> shouldTrip,
                              AtomicReference<State> stateRef,
                              AtomicLong failureCountRef,
                              AtomicLong lastStateChangeTimestamp,
                              AtomicLong testSuccessCount,
                              TimeSource timeSource
        ) implements CircuitBreaker {
            private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

            @Override
            public State state() {
                return stateRef.get();
            }

            @Override
            public long failureCount() {
                return failureCountRef.get();
            }

            @Override
            public TimeSpan timeSinceLastStateChange() {
                return timeSpan(timeSource().nanoTime() - lastStateChangeTimestamp.get()).nanos();
            }

            @Override
            public <T> Promise<T> execute(Supplier<Promise<T>> operation) {
                return switch (stateRef.get()) {
                    case CLOSED -> executeClosedState(operation);

                    case OPEN -> {
                        if (isResetTimeoutExpired()) {
                            transitionTo(State.HALF_OPEN);

                            yield executeHalfOpenState(operation);
                        }

                        var timeout = timeSpan(resetTimeout.nanos() - (timeSource().nanoTime() - lastStateChangeTimestamp.get()))
                                .nanos();

                        yield new CircuitBreakerOpenError("Circuit breaker is open. Operation rejected.",
                                                          timeout).promise();
                    }
                    case HALF_OPEN -> executeHalfOpenState(operation);
                };
            }

            private <T> Promise<T> executeClosedState(Supplier<Promise<T>> operation) {
                return operation.get()
                                .onSuccessRun(() -> failureCountRef.set(0))
                                .onFailure(cause -> shouldTrip.ifTrue(cause, this::handleFailure));
            }

            private <T> Promise<T> executeHalfOpenState(Supplier<Promise<T>> operation) {
                return operation.get()
                                .onSuccess(_ -> {
                                    long successCount = testSuccessCount.incrementAndGet();
                                    if (successCount >= testAttempts) {
                                        transitionTo(State.CLOSED);
                                    }
                                })
                                .onFailure(error -> shouldTrip.ifTrue(error, () -> transitionTo(State.OPEN)));
            }

            private void handleFailure() {
                long currentFailures = failureCountRef.incrementAndGet();
                if (currentFailures >= failureThreshold) {
                    transitionTo(State.OPEN);
                }
            }

            private void transitionTo(State newState) {
                var oldState = stateRef.getAndSet(newState);

                if (oldState != newState) {
                    lastStateChangeTimestamp.set(timeSource().nanoTime());
                    log.info("Circuit breaker stateRef changed from {} to {}", oldState, newState);

                    switch (newState) {
                        case OPEN -> scheduleReset();
                        case HALF_OPEN -> testSuccessCount.set(0);
                        case CLOSED -> failureCountRef.set(0);
                    }
                }
            }

            private void scheduleReset() {
                SharedScheduler.schedule(() -> {
                                             if (stateRef.get() == State.OPEN && isResetTimeoutExpired()) {
                                                 transitionTo(State.HALF_OPEN);
                                             }
                                         },
                                         resetTimeout);
            }

            private boolean isResetTimeoutExpired() {
                return timeSource().nanoTime() - lastStateChangeTimestamp.get() >= resetTimeout.nanos();
            }
        }

        return new circuitBreaker(
                failureThreshold,
                resetTimeout,
                testAttempts,
                FluentPredicate.from(shouldTrip),
                new AtomicReference<>(State.CLOSED),
                new AtomicLong(0),
                new AtomicLong(timeSource.nanoTime()),
                new AtomicLong(0),
                timeSource
        );
    }

    sealed interface CircuitBreakerErrors extends Cause {
        record CircuitBreakerOpenError(String message, TimeSpan retryTime) implements CircuitBreakerErrors {
            @Override
            public String message() {
                return "Circuit breaker is open. " + message + ". Will attempt reset in " + retryTime;
            }
        }
    }

    /// Creates a builder for configuring a new CircuitBreaker.
    ///
    /// @return A new builder
    static StageFailureThreshold builder() {
        return failureThreshold ->
                resetTimeout ->
                        testAttempts ->
                                shouldTrip ->
                                        timeSource ->
                                                create(failureThreshold,
                                                       resetTimeout,
                                                       testAttempts,
                                                       shouldTrip,
                                                       timeSource);
    }

    interface StageFailureThreshold {
        StageResetTimeout failureThreshold(int value);
    }

    interface StageResetTimeout {
        StageTestAttempts resetTimeout(TimeSpan value);
    }

    interface StageTestAttempts {
        StageShouldTrip testAttempts(int value);

        default StageShouldTrip withDefaultTestAttempts() {
            return testAttempts(5);
        }
    }

    interface StageShouldTrip {
        StateTimeSource shouldTrip(Predicate<Cause> value);

        default StateTimeSource withDefaultShouldTrip() {
            return shouldTrip(_ -> true); // All errors count
        }
    }

    interface StateTimeSource {
        CircuitBreaker timeSource(TimeSource value);

        default CircuitBreaker withDefaultTimeSource() {
            return timeSource(System::nanoTime);
        }
    }
}
