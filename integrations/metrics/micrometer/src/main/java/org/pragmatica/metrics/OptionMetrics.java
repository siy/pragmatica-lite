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
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;

import java.util.function.Supplier;

/// Aspect decorator for adding Micrometer metrics to Option-returning functions.
/// Since Option represents presence/absence without failure semantics, only counter-based
/// metrics are supported.
///
/// Usage:
/// ```java
/// var metrics = OptionMetrics.counter("cache.lookup")
///     .registry(meterRegistry)
///     .tags("cache", "user")
///     .build();
///
/// var cacheLookup = metrics.around(cache::get);
/// ```
public interface OptionMetrics {
    /// Wraps an Option-returning function with metrics collection.
    ///
    /// @param fn  The function to wrap
    /// @param <T> Input type
    /// @param <R> Output type
    ///
    /// @return Wrapped function with metrics
    <T, R> Fn1<Option<R>, T> around(Fn1<Option<R>, T> fn);

    /// Wraps an Option-returning supplier with metrics collection.
    ///
    /// @param supplier The supplier to wrap
    /// @param <R>      Output type
    ///
    /// @return Wrapped supplier with metrics
    <R> Supplier<Option<R>> around(Supplier<Option<R>> supplier);

    /// Creates a counter-based metrics builder. Counters record present/absent counts.
    ///
    /// @param name Metric base name (will be suffixed with .present/.absent)
    ///
    /// @return Builder for configuring counter metrics
    static CounterStageRegistry counter(String name) {
        return registry -> new CounterStageTags(name, registry);
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
        /// @return OptionMetrics instance
        public OptionMetrics build() {
            return new CounterMetrics(presentCounter(),
                                      absentCounter());
        }
    }

    // Counter implementation
    record CounterMetrics(Counter presentCounter, Counter absentCounter) implements OptionMetrics {
        @Override
        public <T, R> Fn1<Option<R>, T> around(Fn1<Option<R>, T> fn) {
            return input -> {
                var result = fn.apply(input);
                recordResult(result);
                return result;
            };
        }

        @Override
        public <R> Supplier<Option<R>> around(Supplier<Option<R>> supplier) {
            return () -> {
                var result = supplier.get();
                recordResult(result);
                return result;
            };
        }

        private <R> void recordResult(Option<R> result) {
            switch (result) {
                case Option.Some<R> ignored -> presentCounter.increment();
                case Option.None<R> ignored -> absentCounter.increment();
            }
        }
    }
}
