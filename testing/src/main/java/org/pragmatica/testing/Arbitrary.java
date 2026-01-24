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

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;

/// Core generator interface for property-based testing.
public interface Arbitrary<T> {
    /// Generate a shrinkable value using the provided random source.
    Shrinkable<T> generate(RandomSource random);

    /// Transform generated values using the provided function.
    default <R> Arbitrary<R> map(Fn1<R, T> fn) {
        return random -> {
            Shrinkable<T> shrinkable = generate(random);
            return Shrinkable.shrinkable(fn.apply(shrinkable.value()),
                                         () -> shrinkable.shrink()
                                                         .map(s -> Shrinkable.unshrinkable(fn.apply(s.value()))));
        };
    }

    /// Chain generators using the provided function.
    default <R> Arbitrary<R> flatMap(Fn1<Arbitrary<R>, T> fn) {
        return random -> {
            Shrinkable<T> shrinkable = generate(random);
            Arbitrary<R> nextArbitrary = fn.apply(shrinkable.value());
            return nextArbitrary.generate(random);
        };
    }

    /// Filter generated values using the provided predicate.
    default Arbitrary<T> filter(Fn1<Boolean, T> predicate) {
        return filter(predicate, 100);
    }

    /// Filter generated values using the provided predicate with max attempts.
    default Arbitrary<T> filter(Fn1<Boolean, T> predicate, int maxAttempts) {
        return random -> {
            for (int i = 0; i < maxAttempts; i++) {
                Shrinkable<T> shrinkable = generate(random);
                if (predicate.apply(shrinkable.value())) {
                    return Shrinkable.shrinkable(shrinkable.value(),
                                                 () -> shrinkable.shrink()
                                                                 .filter(s -> predicate.apply(s.value())));
                }
            }
            throw new GenerationException("Could not generate value satisfying predicate after " + maxAttempts
                                          + " attempts");
        };
    }

    /// Create an arbitrary from a Result-based factory (for value objects).
    static <T> Arbitrary<T> fromFactory(Arbitrary<String> inputGen, Fn1<Result<T>, String> factory) {
        return inputGen.flatMap(input -> random -> factory.apply(input)
                                                          .fold(cause -> {
                                                                    throw new GenerationException("Factory failed: " + cause.message());
                                                                },
                                                                Shrinkable::unshrinkable));
    }
}
