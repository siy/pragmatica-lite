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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Core generator interface for property-based testing.
///
/// An Arbitrary generates random values of type T along with shrinking strategies
/// for minimizing failing test cases. Generators are composable via map, flatMap,
/// and filter combinators.
///
/// @param <T> the type of values generated
public interface Arbitrary<T> {
    // Error causes
    /// Error cause for filter exhaustion.
    Cause FILTER_EXHAUSTED = () -> "Could not generate value satisfying predicate";

    /// Error cause for factory failure.
    Cause FACTORY_FAILED = () -> "Factory failed to produce a valid value";

    /// Error cause for empty values list in oneOf.
    Cause EMPTY_VALUES = () -> "oneOf requires at least one value";

    /// Error cause for empty arbitraries list in oneOf.
    Cause EMPTY_ARBITRARIES = () -> "oneOf requires at least one arbitrary";

    /// Error cause for empty weighted list in frequency.
    Cause EMPTY_WEIGHTED = () -> "frequency requires at least one weighted arbitrary";

    // Character sets for string generation
    String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    String DEFAULT_CHARS = ALPHANUMERIC_CHARS + " !@#$%^&*()_+-=[]{}|;':\",./<>?";

    /// Generate a shrinkable value using the provided random source.
    ///
    /// @param random the source of randomness
    /// @return a shrinkable value
    Shrinkable<T> generate(RandomSource random);

    /// Transform generated values using the provided function.
    ///
    /// @param fn the mapping function
    /// @param <R> the result type
    /// @return a new Arbitrary generating transformed values
    default <R> Arbitrary<R> map(Fn1<R, T> fn) {
        Objects.requireNonNull(fn, "fn must not be null");
        return random -> {
            Shrinkable<T> shrinkable = generate(random);
            return Shrinkable.shrinkable(fn.apply(shrinkable.value()),
                                         () -> shrinkable.shrink()
                                                         .map(s -> Shrinkable.unshrinkable(fn.apply(s.value()))));
        };
    }

    /// Chain generators using the provided function.
    ///
    /// @param fn the function producing the next Arbitrary
    /// @param <R> the result type
    /// @return a new Arbitrary generating chained values
    default <R> Arbitrary<R> flatMap(Fn1<Arbitrary<R>, T> fn) {
        Objects.requireNonNull(fn, "fn must not be null");
        return random -> {
            Shrinkable<T> shrinkable = generate(random);
            Arbitrary<R> nextArbitrary = fn.apply(shrinkable.value());
            Objects.requireNonNull(nextArbitrary, "flatMap fn must not return null");
            return nextArbitrary.generate(random);
        };
    }

    /// Filter generated values using the provided predicate.
    /// Uses default max attempts of 100.
    ///
    /// @param predicate the filter predicate
    /// @return a new Arbitrary generating only values satisfying the predicate wrapped in Result
    default Arbitrary<Result<T>> filter(Fn1<Boolean, T> predicate) {
        return filter(predicate, 100);
    }

    /// Filter generated values using the provided predicate with max attempts.
    /// Returns Result.failure if no satisfying value is found within maxAttempts.
    ///
    /// @param predicate the filter predicate
    /// @param maxAttempts maximum generation attempts before giving up
    /// @return a new Arbitrary generating Result containing values satisfying the predicate
    default Arbitrary<Result<T>> filter(Fn1<Boolean, T> predicate, int maxAttempts) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return random -> {
            for (int i = 0; i < maxAttempts; i++) {
                Shrinkable<T> shrinkable = generate(random);
                if (predicate.apply(shrinkable.value())) {
                    return Shrinkable.shrinkable(Result.success(shrinkable.value()),
                                                 () -> shrinkable.shrink()
                                                                 .filter(s -> predicate.apply(s.value()))
                                                                 .map(s -> Shrinkable.unshrinkable(Result.success(s.value()))));
                }
            }
            return Shrinkable.unshrinkable(FILTER_EXHAUSTED.result());
        };
    }

    /// Try to generate a value satisfying the predicate, returning Option.none() if exhausted.
    ///
    /// @param predicate the filter predicate
    /// @param maxAttempts maximum generation attempts
    /// @return Option containing the Shrinkable if found, or none if exhausted
    default Option<Shrinkable<T>> tryGenerate(RandomSource random, Fn1<Boolean, T> predicate, int maxAttempts) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        for (int i = 0; i < maxAttempts; i++) {
            Shrinkable<T> shrinkable = generate(random);
            if (predicate.apply(shrinkable.value())) {
                return Option.option(Shrinkable.shrinkable(shrinkable.value(),
                                                           () -> shrinkable.shrink()
                                                                           .filter(s -> predicate.apply(s.value()))));
            }
        }
        return Option.none();
    }

    // Static factory methods
    /// Create an arbitrary from a Result-based factory (for value objects).
    /// Retries generation until the factory produces a successful result.
    ///
    /// @param inputGen the generator for factory input
    /// @param factory the Result-returning factory function
    /// @param <T> the result type
    /// @return a new Arbitrary generating Result values via the factory
    static <T> Arbitrary<Result<T>> fromFactory(Arbitrary<String> inputGen, Fn1<Result<T>, String> factory) {
        Objects.requireNonNull(inputGen, "inputGen must not be null");
        Objects.requireNonNull(factory, "factory must not be null");
        return fromFactory(inputGen, factory, 100);
    }

    /// Create an arbitrary from a Result-based factory with custom max attempts.
    ///
    /// @param inputGen the generator for factory input
    /// @param factory the Result-returning factory function
    /// @param maxAttempts maximum attempts to find a successful factory result
    /// @param <T> the result type
    /// @return a new Arbitrary generating Result values via the factory
    static <T> Arbitrary<Result<T>> fromFactory(Arbitrary<String> inputGen,
                                                Fn1<Result<T>, String> factory,
                                                int maxAttempts) {
        Objects.requireNonNull(inputGen, "inputGen must not be null");
        Objects.requireNonNull(factory, "factory must not be null");
        return random -> {
            for (int i = 0; i < maxAttempts; i++) {
                String input = inputGen.generate(random)
                                       .value();
                var result = factory.apply(input);
                if (result instanceof Result.Success<T> success) {
                    return Shrinkable.unshrinkable(Result.success(success.value()));
                }
            }
            return Shrinkable.unshrinkable(FACTORY_FAILED.result());
        };
    }

    /// Generate random integers in full range.
    static Arbitrary<Integer> integers() {
        return random -> {
            int value = random.nextInt();
            return Shrinkable.shrinkable(value, () -> Shrinkers.shrinkInteger(value));
        };
    }

    /// Generate random integers in range [min, max].
    static Arbitrary<Integer> integers(int min, int max) {
        return random -> {
            int value = random.nextInt(min, max);
            return Shrinkable.shrinkable(value,
                                         () -> Shrinkers.shrinkInteger(value)
                                                        .filter(s -> s.value() >= min && s.value() <= max));
        };
    }

    /// Generate random longs in full range.
    static Arbitrary<Long> longs() {
        return random -> {
            long value = random.nextLong();
            return Shrinkable.shrinkable(value, () -> Shrinkers.shrinkLong(value));
        };
    }

    /// Generate random longs in range [min, max].
    static Arbitrary<Long> longs(long min, long max) {
        return random -> {
            long value = random.nextLong(min, max);
            return Shrinkable.shrinkable(value,
                                         () -> Shrinkers.shrinkLong(value)
                                                        .filter(s -> s.value() >= min && s.value() <= max));
        };
    }

    /// Generate random doubles in range [0.0, 1.0).
    static Arbitrary<Double> doubles() {
        return random -> Shrinkable.unshrinkable(random.nextDouble());
    }

    /// Generate random booleans.
    static Arbitrary<Boolean> booleans() {
        return random -> Shrinkable.unshrinkable(random.nextBoolean());
    }

    /// Generate random strings with default length and characters.
    static Arbitrary<String> strings() {
        return strings(0, 100);
    }

    /// Generate random strings with specified length range.
    static Arbitrary<String> strings(int minLen, int maxLen) {
        return strings(minLen, maxLen, DEFAULT_CHARS);
    }

    /// Generate random strings with specified length range and alphabet.
    static Arbitrary<String> strings(int minLen, int maxLen, String alphabet) {
        Objects.requireNonNull(alphabet, "alphabet must not be null");
        return random -> {
            int len = random.nextInt(minLen, maxLen);
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String value = sb.toString();
            return Shrinkable.shrinkable(value,
                                         () -> Shrinkers.shrinkString(value)
                                                        .filter(s -> s.value()
                                                                      .length() >= minLen));
        };
    }

    /// Generate random alphanumeric strings with specified length range.
    static Arbitrary<String> alphanumeric(int minLen, int maxLen) {
        return strings(minLen, maxLen, ALPHANUMERIC_CHARS);
    }

    /// Generate random lists of elements.
    static <T> Arbitrary<List<T>> lists(Arbitrary<T> elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        return lists(elements, 0, 10);
    }

    /// Generate random lists of elements with specified size range.
    static <T> Arbitrary<List<T>> lists(Arbitrary<T> elements, int minSize, int maxSize) {
        Objects.requireNonNull(elements, "elements must not be null");
        return random -> {
            int size = random.nextInt(minSize, maxSize);
            List<Shrinkable<T>> shrinkables = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                shrinkables.add(elements.generate(random));
            }
            List<T> values = shrinkables.stream()
                                        .map(Shrinkable::value)
                                        .toList();
            return Shrinkable.shrinkable(values,
                                         () -> Shrinkers.shrinkList(shrinkables)
                                                        .filter(s -> s.value()
                                                                      .size() >= minSize));
        };
    }

    /// Generate random sets of elements with specified size range.
    static <T> Arbitrary<Set<T>> sets(Arbitrary<T> elements, int minSize, int maxSize) {
        Objects.requireNonNull(elements, "elements must not be null");
        return random -> {
            int targetSize = random.nextInt(minSize, maxSize);
            Set<T> set = new HashSet<>();
            int attempts = 0;
            int maxAttempts = targetSize * 10;
            while (set.size() < targetSize && attempts < maxAttempts) {
                set.add(elements.generate(random)
                                .value());
                attempts++;
            }
            return Shrinkable.unshrinkable(set);
        };
    }

    /// Generate one of the provided values.
    @SafeVarargs
    static <T> Result<Arbitrary<T>> oneOf(T... values) {
        Objects.requireNonNull(values, "values must not be null");
        if (values.length == 0) {
            return EMPTY_VALUES.result();
        }
        return Result.success(random -> {
                                  int index = random.nextInt(values.length);
                                  return Shrinkable.unshrinkable(values[index]);
                              });
    }

    /// Generate from one of the provided arbitraries.
    static <T> Result<Arbitrary<T>> oneOf(List<Arbitrary<T>> arbitraries) {
        Objects.requireNonNull(arbitraries, "arbitraries must not be null");
        if (arbitraries.isEmpty()) {
            return EMPTY_ARBITRARIES.result();
        }
        return Result.success(random -> {
                                  int index = random.nextInt(arbitraries.size());
                                  return arbitraries.get(index)
                                                    .generate(random);
                              });
    }

    /// Generate from weighted arbitraries.
    static <T> Result<Arbitrary<T>> frequency(List<Tuple2<Integer, Arbitrary<T>>> weighted) {
        Objects.requireNonNull(weighted, "weighted must not be null");
        if (weighted.isEmpty()) {
            return EMPTY_WEIGHTED.result();
        }
        int totalWeight = weighted.stream()
                                  .mapToInt(Tuple2::first)
                                  .sum();
        return Result.success(random -> {
                                  int target = random.nextInt(totalWeight);
                                  int cumulative = 0;
                                  for (Tuple2<Integer, Arbitrary<T>> entry : weighted) {
                                      cumulative += entry.first();
                                      if (target < cumulative) {
                                          return entry.last()
                                                      .generate(random);
                                      }
                                  }
                                  return weighted.getLast()
                                                 .last()
                                                 .generate(random);
                              });
    }

    /// Generate a constant value.
    static <T> Arbitrary<T> constant(T value) {
        return _ -> Shrinkable.unshrinkable(value);
    }
}
