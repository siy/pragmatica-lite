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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShrinkerTest {

    @Nested
    class IntegerShrinking {
        @Test
        void shrinkInteger_includesZeroFirst() {
            List<Shrinkable<Integer>> shrinks = Shrinkers.shrinkInteger(42).toList();

            assertFalse(shrinks.isEmpty());
            assertEquals(0, shrinks.getFirst().value());
        }

        @Test
        void shrinkInteger_approachesZero() {
            List<Integer> values = Shrinkers.shrinkInteger(42)
                .map(Shrinkable::value)
                .toList();

            // Should include 0 and intermediate values
            assertTrue(values.contains(0));
            assertTrue(values.size() >= 2);
        }

        @Test
        void shrinkInteger_handlesZero() {
            List<Shrinkable<Integer>> shrinks = Shrinkers.shrinkInteger(0).toList();
            assertTrue(shrinks.isEmpty());
        }

        @Test
        void shrinkInteger_handlesNegative() {
            List<Integer> values = Shrinkers.shrinkInteger(-42)
                .map(Shrinkable::value)
                .toList();

            assertTrue(values.contains(0));
        }
    }

    @Nested
    class LongShrinking {
        @Test
        void shrinkLong_includesZeroFirst() {
            List<Shrinkable<Long>> shrinks = Shrinkers.shrinkLong(100L).toList();

            assertFalse(shrinks.isEmpty());
            assertEquals(0L, shrinks.getFirst().value());
        }

        @Test
        void shrinkLong_handlesZero() {
            List<Shrinkable<Long>> shrinks = Shrinkers.shrinkLong(0L).toList();
            assertTrue(shrinks.isEmpty());
        }
    }

    @Nested
    class StringShrinking {
        @Test
        void shrinkString_includesEmptyFirst() {
            List<Shrinkable<String>> shrinks = Shrinkers.shrinkString("hello").toList();

            assertFalse(shrinks.isEmpty());
            assertEquals("", shrinks.getFirst().value());
        }

        @Test
        void shrinkString_removesEachCharacter() {
            List<String> values = Shrinkers.shrinkString("abc")
                .map(Shrinkable::value)
                .toList();

            assertTrue(values.contains(""));
            assertTrue(values.contains("bc"));
            assertTrue(values.contains("ac"));
            assertTrue(values.contains("ab"));
        }

        @Test
        void shrinkString_handlesEmpty() {
            List<Shrinkable<String>> shrinks = Shrinkers.shrinkString("").toList();
            assertTrue(shrinks.isEmpty());
        }

        @Test
        void shrinkString_handlesSingleChar() {
            List<String> values = Shrinkers.shrinkString("x")
                .map(Shrinkable::value)
                .toList();

            // First is explicit empty, second is from removing the char
            assertEquals(2, values.size());
            assertEquals("", values.getFirst());
        }
    }

    @Nested
    class ListShrinking {
        @Test
        void shrinkList_includesEmptyFirst() {
            List<Shrinkable<Integer>> elements = List.of(
                Shrinkable.shrinkable(1, () -> Shrinkers.shrinkInteger(1)),
                Shrinkable.shrinkable(2, () -> Shrinkers.shrinkInteger(2)),
                Shrinkable.shrinkable(3, () -> Shrinkers.shrinkInteger(3))
            );

            List<Shrinkable<List<Integer>>> shrinks = Shrinkers.shrinkList(elements).toList();

            assertFalse(shrinks.isEmpty());
            assertEquals(List.of(), shrinks.getFirst().value());
        }

        @Test
        void shrinkList_removesEachElement() {
            List<Shrinkable<Integer>> elements = List.of(
                Shrinkable.unshrinkable(1),
                Shrinkable.unshrinkable(2),
                Shrinkable.unshrinkable(3)
            );

            List<List<Integer>> values = Shrinkers.shrinkList(elements)
                .map(Shrinkable::value)
                .toList();

            assertTrue(values.contains(List.of()));
            assertTrue(values.contains(List.of(2, 3)));
            assertTrue(values.contains(List.of(1, 3)));
            assertTrue(values.contains(List.of(1, 2)));
        }

        @Test
        void shrinkList_handlesEmpty() {
            List<Shrinkable<Integer>> elements = List.of();
            List<Shrinkable<List<Integer>>> shrinks = Shrinkers.shrinkList(elements).toList();
            assertTrue(shrinks.isEmpty());
        }
    }

    @Nested
    class ShrinkableInterface {
        @Test
        void unshrinkable_returnsEmptyStream() {
            Shrinkable<String> unshrinkable = Shrinkable.unshrinkable("test");

            assertEquals("test", unshrinkable.value());
            assertTrue(unshrinkable.shrink().toList().isEmpty());
        }

        @Test
        void shrinkable_usesProvidedShrinker() {
            Shrinkable<Integer> shrinkable = Shrinkable.shrinkable(10, () -> Shrinkers.shrinkInteger(10));

            assertEquals(10, shrinkable.value());
            assertFalse(shrinkable.shrink().toList().isEmpty());
        }
    }
}
