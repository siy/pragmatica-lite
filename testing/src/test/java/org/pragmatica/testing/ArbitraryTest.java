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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArbitraryTest {

    @Nested
    class IntegerGeneration {
        @Test
        void integers_generates_differentValues() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<Integer> ints = Arbitraries.integers();

            Set<Integer> values = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                values.add(ints.generate(random).value());
            }

            assertTrue(values.size() > 50, "Should generate diverse values");
        }

        @Test
        void integers_withRange_respectsBounds() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<Integer> ints = Arbitraries.integers(10, 20);

            for (int i = 0; i < 100; i++) {
                int value = ints.generate(random).value();
                assertTrue(value >= 10 && value <= 20,
                    "Value " + value + " should be in range [10, 20]");
            }
        }
    }

    @Nested
    class LongGeneration {
        @Test
        void longs_withRange_respectsBounds() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<Long> longs = Arbitraries.longs(1000L, 2000L);

            for (int i = 0; i < 100; i++) {
                long value = longs.generate(random).value();
                assertTrue(value >= 1000L && value <= 2000L,
                    "Value " + value + " should be in range [1000, 2000]");
            }
        }
    }

    @Nested
    class StringGeneration {
        @Test
        void strings_withLengthRange_respectsBounds() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<String> strings = Arbitraries.strings(5, 10);

            for (int i = 0; i < 100; i++) {
                String value = strings.generate(random).value();
                assertTrue(value.length() >= 5 && value.length() <= 10,
                    "Length " + value.length() + " should be in range [5, 10]");
            }
        }

        @Test
        void alphanumeric_usesCorrectCharacters() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<String> alphanumeric = Arbitraries.alphanumeric(10, 20);

            for (int i = 0; i < 100; i++) {
                String value = alphanumeric.generate(random).value();
                for (char c : value.toCharArray()) {
                    assertTrue(Character.isLetterOrDigit(c),
                        "Character '" + c + "' should be alphanumeric");
                }
            }
        }
    }

    @Nested
    class ListGeneration {
        @Test
        void lists_withSizeRange_respectsBounds() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<java.util.List<Integer>> lists = Arbitraries.lists(Arbitraries.integers(), 3, 7);

            for (int i = 0; i < 100; i++) {
                int size = lists.generate(random).value().size();
                assertTrue(size >= 3 && size <= 7,
                    "Size " + size + " should be in range [3, 7]");
            }
        }
    }

    @Nested
    class Combinators {
        @Test
        void map_transformsValues() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<Integer> doubled = Arbitraries.integers(1, 10).map(n -> n * 2);

            for (int i = 0; i < 100; i++) {
                int value = doubled.generate(random).value();
                assertTrue(value >= 2 && value <= 20 && value % 2 == 0,
                    "Value " + value + " should be even and in range [2, 20]");
            }
        }

        @Test
        void flatMap_chainsGenerators() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<String> sized = Arbitraries.integers(1, 5)
                .flatMap(n -> Arbitraries.alphanumeric(n, n));

            for (int i = 0; i < 100; i++) {
                String value = sized.generate(random).value();
                assertTrue(value.length() >= 1 && value.length() <= 5,
                    "Length " + value.length() + " should be in range [1, 5]");
            }
        }

        @Test
        void filter_rejectsInvalidValues() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<Integer> evens = Arbitraries.integers(1, 100).filter(n -> n % 2 == 0);

            for (int i = 0; i < 100; i++) {
                int value = evens.generate(random).value();
                assertEquals(0, value % 2, "Value " + value + " should be even");
            }
        }

        @Test
        void oneOf_selectsFromValues() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<String> colors = Arbitraries.oneOf("red", "green", "blue");

            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                seen.add(colors.generate(random).value());
            }

            assertEquals(Set.of("red", "green", "blue"), seen);
        }

        @Test
        void constant_alwaysReturnsSameValue() {
            RandomSource random = RandomSource.seeded(42);
            Arbitrary<String> constant = Arbitraries.constant("always");

            for (int i = 0; i < 10; i++) {
                assertEquals("always", constant.generate(random).value());
            }
        }
    }

    @Nested
    class Reproducibility {
        @Test
        void sameSeed_producesSameSequence() {
            Arbitrary<Integer> ints = Arbitraries.integers();

            RandomSource random1 = RandomSource.seeded(12345);
            RandomSource random2 = RandomSource.seeded(12345);

            for (int i = 0; i < 100; i++) {
                assertEquals(
                    ints.generate(random1).value(),
                    ints.generate(random2).value(),
                    "Same seed should produce same sequence"
                );
            }
        }

        @Test
        void differentSeeds_produceDifferentSequences() {
            Arbitrary<Integer> ints = Arbitraries.integers();

            RandomSource random1 = RandomSource.seeded(12345);
            RandomSource random2 = RandomSource.seeded(54321);

            int differences = 0;
            for (int i = 0; i < 100; i++) {
                if (!ints.generate(random1).value().equals(ints.generate(random2).value())) {
                    differences++;
                }
            }

            assertTrue(differences > 90, "Different seeds should produce different sequences");
        }
    }
}
