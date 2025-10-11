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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.option;

class OptionMetricsTest {
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @Test
    void counterMetrics_recordsPresentValue() {
        var metrics = OptionMetrics.counter("test.operation")
                                   .registry(registry)
                                   .tags("type", "test")
                                   .build();

        var operation = metrics.around(() -> option("result"));

        operation.get();

        var presentCounter = registry.find("test.operation.present")
                                    .tag("type", "test")
                                    .counter();

        assertThat(presentCounter).isNotNull();
        assertThat(presentCounter.count()).isEqualTo(1);
    }

    @Test
    void counterMetrics_recordsAbsentValue() {
        var metrics = OptionMetrics.counter("test.operation")
                                   .registry(registry)
                                   .tags("type", "test")
                                   .build();

        var operation = metrics.around(() -> none());

        operation.get();

        var absentCounter = registry.find("test.operation.absent")
                                   .tag("type", "test")
                                   .counter();

        assertThat(absentCounter).isNotNull();
        assertThat(absentCounter.count()).isEqualTo(1);
    }

    @Test
    void counterMetrics_recordsMultipleOperations() {
        var metrics = OptionMetrics.counter("test.operation")
                                   .registry(registry)
                                   .build();

        var operation = metrics.around(() -> option("result"));

        operation.get();
        operation.get();
        operation.get();

        var presentCounter = registry.find("test.operation.present").counter();

        assertThat(presentCounter).isNotNull();
        assertThat(presentCounter.count()).isEqualTo(3);
    }

    @Test
    void counterMetrics_worksWithFunction() {
        var metrics = OptionMetrics.counter("test.operation")
                                   .registry(registry)
                                   .build();

        var function = metrics.around((String input) -> option(input.toUpperCase()));

        function.apply("test");

        var counter = registry.find("test.operation.present").counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void counterMetrics_separatesPresentAndAbsentCounts() {
        var metrics = OptionMetrics.counter("test.operation")
                                   .registry(registry)
                                   .build();

        var presentOp = metrics.around(() -> option("result"));
        var absentOp = metrics.around(() -> none());

        presentOp.get();
        presentOp.get();
        absentOp.get();

        var presentCounter = registry.find("test.operation.present").counter();
        var absentCounter = registry.find("test.operation.absent").counter();

        assertThat(presentCounter.count()).isEqualTo(2);
        assertThat(absentCounter.count()).isEqualTo(1);
    }

    @Test
    void optionMetrics_preservesOptionValue() {
        var metrics = OptionMetrics.counter("test.operation")
                                   .registry(registry)
                                   .build();

        var function = metrics.around((String input) -> option(input.toUpperCase()));

        var result = function.apply("test");

        assertThat(result.isPresent()).isTrue();
        result.onPresent(value -> assertThat(value).isEqualTo("TEST"));
    }

    @Test
    void counterMetrics_worksWithNullableInput() {
        var metrics = OptionMetrics.counter("cache.lookup")
                                   .registry(registry)
                                   .tags("cache", "user")
                                   .build();

        var cacheLookup = metrics.around((String key) -> option(key.equals("found") ? "value" : null));

        cacheLookup.apply("found");
        cacheLookup.apply("missing");

        var presentCounter = registry.find("cache.lookup.present")
                                    .tag("cache", "user")
                                    .counter();
        var absentCounter = registry.find("cache.lookup.absent")
                                   .tag("cache", "user")
                                   .counter();

        assertThat(presentCounter.count()).isEqualTo(1);
        assertThat(absentCounter.count()).isEqualTo(1);
    }
}
