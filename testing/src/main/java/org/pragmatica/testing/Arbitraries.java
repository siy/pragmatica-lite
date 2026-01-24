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

import org.pragmatica.lang.Tuple;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Factory for creating common generators.
public sealed interface Arbitraries {
    String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    String DEFAULT_CHARS = ALPHANUMERIC_CHARS + " !@#$%^&*()_+-=[]{}|;':\",./<>?";

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
        return lists(elements, 0, 10);
    }

    /// Generate random lists of elements with specified size range.
    static <T> Arbitrary<List<T>> lists(Arbitrary<T> elements, int minSize, int maxSize) {
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
    static <T> Arbitrary<T> oneOf(T... values) {
        return random -> {
            int index = random.nextInt(values.length);
            return Shrinkable.unshrinkable(values[index]);
        };
    }

    /// Generate from one of the provided arbitraries.
    static <T> Arbitrary<T> oneOf(List<Arbitrary<T>> arbitraries) {
        return random -> {
            int index = random.nextInt(arbitraries.size());
            return arbitraries.get(index)
                              .generate(random);
        };
    }

    /// Generate from weighted arbitraries.
    static <T> Arbitrary<T> frequency(List<Tuple2<Integer, Arbitrary<T>>> weighted) {
        int totalWeight = weighted.stream()
                                  .mapToInt(Tuple2::first)
                                  .sum();
        return random -> {
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
        };
    }

    /// Generate a constant value.
    static <T> Arbitrary<T> constant(T value) {
        return _ -> Shrinkable.unshrinkable(value);
    }

    record unused() implements Arbitraries {}
}
