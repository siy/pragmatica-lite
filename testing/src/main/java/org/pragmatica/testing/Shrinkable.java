/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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
 */

package org.pragmatica.testing;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/// A value that can be shrunk to find smaller failing examples.
public interface Shrinkable<T> {
    /// Get the current value.
    T value();

    /// Get a stream of shrunk alternatives, ordered from smallest to largest.
    Stream<Shrinkable<T>> shrink();

    /// Create a shrinkable with a custom shrinking function.
    static <T> Shrinkable<T> shrinkable(T value, Supplier<Stream<Shrinkable<T>>> shrinker) {
        Objects.requireNonNull(shrinker, "shrinker must not be null");
        return new ShrinkableImpl<>(value, shrinker);
    }

    /// Create a shrinkable that cannot be shrunk.
    static <T> Shrinkable<T> unshrinkable(T value) {
        return new ShrinkableImpl<>(value, Stream::empty);
    }
}

record ShrinkableImpl<T>(T value, Supplier<Stream<Shrinkable<T>>> shrinker) implements Shrinkable<T> {
    @Override
    public Stream<Shrinkable<T>> shrink() {
        return shrinker.get();
    }
}
