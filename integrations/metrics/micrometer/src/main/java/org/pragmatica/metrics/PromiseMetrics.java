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

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/// Aspect decorator for adding Micrometer metrics to Promise-returning functions.
/// Supports timer-based metrics (duration + counts), counter-based metrics (counts only),
/// and combined metrics (timer + separate success/failure counters).
///
/// Usage:
/// ```java
/// var metrics = PromiseMetrics.timer("order.process")
///     .registry(meterRegistry)
///     .tags("service", "orders")
///     .build();
///
/// var processor = metrics.around(orderRepository::process);
///```
public interface PromiseMetrics {
    /// Wraps a Promise-returning function with the metrics collection.
    ///
    /// @param fn  The function to wrap
    /// @param <T> Input type
    /// @param <R> Output type
    ///
    /// @return Wrapped function with metrics
    <T, R> Fn1<Promise<R>, T> around(Fn1<Promise<R>, T> fn);

    /// Wraps a Promise-returning supplier with the metrics collection.
    ///
    /// @param supplier The supplier to wrap
    /// @param <R>      Output type
    ///
    /// @return Wrapped supplier with metrics
    <R> Supplier<Promise<R>> around(Supplier<Promise<R>> supplier);

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
        /// @return PromiseMetrics instance
        public PromiseMetrics build() {
            return new TimerMetrics(timer(TimerType.SUCCESS), timer(TimerType.FAILURE));
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
        /// @return PromiseMetrics instance
        public PromiseMetrics build() {
            return new CounterMetrics(successCounter(), failureCounter());
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
        /// @return PromiseMetrics instance
        public PromiseMetrics build() {
            return new CombinedMetrics(timer(TimerType.PLAIN), successCounter(), failureCounter());
        }
    }

    // Timer implementation
    record TimerMetrics(Timer successTimer, Timer failureTimer) implements PromiseMetrics {
        @Override
        public <T, R> Fn1<Promise<R>, T> around(Fn1<Promise<R>, T> fn) {
            return input -> {
                var sample = Timer.start();
                return fn.apply(input)
                         .onResult(result -> recordResult(sample, result));
            };
        }

        @Override
        public <R> Supplier<Promise<R>> around(Supplier<Promise<R>> supplier) {
            return () -> {
                var sample = Timer.start();
                return supplier.get()
                               .onResult(result -> recordResult(sample, result));
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
    record CounterMetrics(Counter successCounter, Counter failureCounter) implements PromiseMetrics {
        @Override
        public <T, R> Fn1<Promise<R>, T> around(Fn1<Promise<R>, T> fn) {
            return input -> fn.apply(input)
                              .onResult(this::recordResult);
        }

        @Override
        public <R> Supplier<Promise<R>> around(Supplier<Promise<R>> supplier) {
            return () -> supplier.get()
                                 .onResult(this::recordResult);
        }

        private <R> void recordResult(Result<R> result) {
            switch (result) {
                case Result.Success<R> ignored -> successCounter.increment();
                case Result.Failure<R> ignored -> failureCounter.increment();
            }
        }
    }

    // Combined implementation
    record CombinedMetrics(Timer timer, Counter successCounter, Counter failureCounter) implements PromiseMetrics {
        @Override
        public <T, R> Fn1<Promise<R>, T> around(Fn1<Promise<R>, T> fn) {
            return input -> {
                var sample = Timer.start();
                return fn.apply(input)
                         .onResult(result -> recordResult(sample, result));
            };
        }

        @Override
        public <R> Supplier<Promise<R>> around(Supplier<Promise<R>> supplier) {
            return () -> {
                var sample = Timer.start();
                return supplier.get()
                               .onResult(result -> recordResult(sample, result));
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
