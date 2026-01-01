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

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

/// Base class for metrics builder stages providing common tag management and meter creation.
/// Uses self-bounded generics to enable fluent API with correct return types.
public class StageTags<T extends StageTags<T>> {
    private final String name;
    private final MeterRegistry registry;
    private final List<Tag> tags = new ArrayList<>();

    protected enum TimerType {
        SUCCESS,
        FAILURE,
        PLAIN
    }

    protected StageTags(String name, MeterRegistry registry) {
        this.name = name;
        this.registry = registry;
    }

    protected String name() {
        return name;
    }

    protected MeterRegistry registry() {
        return registry;
    }

    protected List<Tag> tags() {
        return tags;
    }

    /// Adds tags to the timer.
    ///
    /// @param keyValues Alternating key-value pairs
    ///
    /// @return This builder
    @SuppressWarnings("unchecked")
    public T tags(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Tags must be provided as key-value pairs");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            tags.add(Tag.of(keyValues[i], keyValues[i + 1]));
        }
        return ( T) this;
    }

    protected Counter failureCounter() {
        return Counter.builder(name() + ".failure")
                      .tags(tags())
                      .register(registry());
    }

    protected Counter successCounter() {
        return Counter.builder(name() + ".success")
                      .tags(tags())
                      .register(registry());
    }

    protected Counter presentCounter() {
        return Counter.builder(name() + ".present")
                      .tags(tags())
                      .register(registry());
    }

    protected Counter absentCounter() {
        return Counter.builder(name() + ".absent")
                      .tags(tags())
                      .register(registry());
    }

    protected Timer timer(TimerType type) {
        var builder = Timer.builder(name())
                           .tags(tags());
        return ( switch (type) {
            case SUCCESS -> builder.tag("result", "success");
            case FAILURE -> builder.tag("result", "failure");
            case PLAIN -> builder;
        }).register(registry);
    }
}
