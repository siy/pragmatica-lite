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

package org.pragmatica.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;

import java.util.function.Supplier;

/// Aspect decorator for adding Micrometer metrics to Result-returning functions.
/// Supports timer-based metrics (duration + counts), counter-based metrics (counts only),
/// and combined metrics (timer + separate success/failure counters).
///
/// Usage:
/// ```java
/// var metrics = ResultMetrics.timer("validate.input")
///     .registry(meterRegistry)
///     .tags("validator", "user")
///     .build();
///
/// var validator = metrics.around(inputValidator::validate);
/// ```
public interface ResultMetrics {
    /// Wraps a Result-returning function with metrics collection.
    ///
    /// @param fn  The function to wrap
    /// @param <T> Input type
    /// @param <R> Output type
    ///
    /// @return Wrapped function with metrics
    <T, R> Fn1<Result<R>, T> around(Fn1<Result<R>, T> fn);

    /// Wraps a Result-returning supplier with metrics collection.
    ///
    /// @param supplier The supplier to wrap
    /// @param <R>      Output type
    ///
    /// @return Wrapped supplier with metrics
    <R> Supplier<Result<R>> around(Supplier<Result<R>> supplier);

    /// Creates a timer-based metrics builder. Timer records both duration and counts.
    ///
    /// @param name Metric name
    ///
    /// @return Builder for configuring timer metrics
    static TimerStageRegistry timer(String name) {
        return registry -> new TimerStageTags(name, registry);
    }

    /// Creates a counter-based metrics builder. Counters record only success/failure counts.
    ///
    /// @param name Metric base name (will be suffixed with .success/.failure)
    ///
    /// @return Builder for configuring counter metrics
    static CounterStageRegistry counter(String name) {
        return registry -> new CounterStageTags(name, registry);
    }

    /// Creates a combined metrics builder. Records timer + separate success/failure counters.
    ///
    /// @param name Metric base name
    ///
    /// @return Builder for configuring combined metrics
    static CombinedStageRegistry combined(String name) {
        return registry -> new CombinedStageTags(name, registry);
    }

    // Timer builder stages
    interface TimerStageRegistry {
        TimerStageTags registry(MeterRegistry registry);
    }

    final class TimerStageTags extends StageTags<TimerStageTags> {
        private TimerStageTags(String name, MeterRegistry registry) {
            super(name, registry);
        }

        /// Builds the timer-based metrics wrapper.
        ///
        /// @return ResultMetrics instance
        public ResultMetrics build() {
            return new TimerMetrics(timer(TimerType.SUCCESS),
                                    timer(TimerType.FAILURE));
        }
    }

    // Counter builder stages
    interface CounterStageRegistry {
        CounterStageTags registry(MeterRegistry registry);
    }

    final class CounterStageTags extends StageTags<CounterStageTags> {
        private CounterStageTags(String name, MeterRegistry registry) {
            super(name, registry);
        }

        /// Builds the counter-based metrics wrapper.
        ///
        /// @return ResultMetrics instance
        public ResultMetrics build() {
            return new CounterMetrics(successCounter(),
                                      failureCounter());
        }
    }

    // Combined builder stages
    interface CombinedStageRegistry {
        CombinedStageTags registry(MeterRegistry registry);
    }

    final class CombinedStageTags extends StageTags<CombinedStageTags> {
        private CombinedStageTags(String name, MeterRegistry registry) {
            super(name, registry);
        }

        /// Builds the combined metrics wrapper.
        ///
        /// @return ResultMetrics instance
        public ResultMetrics build() {
            return new CombinedMetrics(timer(TimerType.PLAIN),
                                       successCounter(),
                                       failureCounter());
        }
    }

    // Timer implementation
    record TimerMetrics(Timer successTimer, Timer failureTimer) implements ResultMetrics {
        @Override
        public <T, R> Fn1<Result<R>, T> around(Fn1<Result<R>, T> fn) {
            return input -> {
                var sample = Timer.start();
                var result = fn.apply(input);
                recordResult(sample, result);
                return result;
            };
        }

        @Override
        public <R> Supplier<Result<R>> around(Supplier<Result<R>> supplier) {
            return () -> {
                var sample = Timer.start();
                var result = supplier.get();
                recordResult(sample, result);
                return result;
            };
        }

        private <R> void recordResult(Timer.Sample sample, Result<R> result) {
            switch (result) {
                case Result.Success<R> ignored -> sample.stop(successTimer);
                case Result.Failure<R> ignored -> sample.stop(failureTimer);
            }
        }
    }

    // Counter implementation
    record CounterMetrics(Counter successCounter, Counter failureCounter) implements ResultMetrics {
        @Override
        public <T, R> Fn1<Result<R>, T> around(Fn1<Result<R>, T> fn) {
            return input -> {
                var result = fn.apply(input);
                recordResult(result);
                return result;
            };
        }

        @Override
        public <R> Supplier<Result<R>> around(Supplier<Result<R>> supplier) {
            return () -> {
                var result = supplier.get();
                recordResult(result);
                return result;
            };
        }

        private <R> void recordResult(Result<R> result) {
            switch (result) {
                case Result.Success<R> ignored -> successCounter.increment();
                case Result.Failure<R> ignored -> failureCounter.increment();
            }
        }
    }

    // Combined implementation
    record CombinedMetrics(Timer timer, Counter successCounter, Counter failureCounter) implements ResultMetrics {
        @Override
        public <T, R> Fn1<Result<R>, T> around(Fn1<Result<R>, T> fn) {
            return input -> {
                var sample = Timer.start();
                var result = fn.apply(input);
                recordResult(sample, result);
                return result;
            };
        }

        @Override
        public <R> Supplier<Result<R>> around(Supplier<Result<R>> supplier) {
            return () -> {
                var sample = Timer.start();
                var result = supplier.get();
                recordResult(sample, result);
                return result;
            };
        }

        private <R> void recordResult(Timer.Sample sample, Result<R> result) {
            sample.stop(timer);
            switch (result) {
                case Result.Success<R> ignored -> successCounter.increment();
                case Result.Failure<R> ignored -> failureCounter.increment();
            }
        }
    }
}
