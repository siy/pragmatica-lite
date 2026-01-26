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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// Shrinking strategies for common types.
public sealed interface Shrinkers {
    /// Shrink an integer toward 0 using binary search.
    /// Example: 42 -> [0, 21, 32, 37, 40, 41]
    static Stream<Shrinkable<Integer>> shrinkInteger(int value) {
        if (value == 0) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(Shrinkable.shrinkable(0, () -> Stream.empty())),
                             binarySearchShrink(value));
    }

    private static Stream<Shrinkable<Integer>> binarySearchShrink(int value) {
        var results = new ArrayList<Integer>();
        int target = 0;
        int current = value;
        while (Math.abs(current - target) > 1) {
            int mid = target + (current - target) / 2;
            if (mid != 0) {
                results.add(mid);
            }
            target = mid;
        }
        return results.stream()
                      .map(v -> Shrinkable.shrinkable(v,
                                                      () -> shrinkInteger(v)));
    }

    /// Shrink a long toward 0 using binary search.
    static Stream<Shrinkable<Long>> shrinkLong(long value) {
        if (value == 0L) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(Shrinkable.shrinkable(0L, () -> Stream.empty())),
                             binarySearchShrinkLong(value));
    }

    private static Stream<Shrinkable<Long>> binarySearchShrinkLong(long value) {
        var results = new ArrayList<Long>();
        long target = 0L;
        long current = value;
        while (Math.abs(current - target) > 1L) {
            long mid = target + (current - target) / 2L;
            if (mid != 0L) {
                results.add(mid);
            }
            target = mid;
        }
        return results.stream()
                      .map(v -> Shrinkable.shrinkable(v,
                                                      () -> shrinkLong(v)));
    }

    /// Shrink a string toward empty by removing characters.
    /// Example: "hello" -> ["", "ello", "hllo", "helo", "hell"]
    static Stream<Shrinkable<String>> shrinkString(String value) {
        if (value.isEmpty()) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(Shrinkable.shrinkable("", () -> Stream.empty())),
                             removeEachChar(value));
    }

    private static Stream<Shrinkable<String>> removeEachChar(String value) {
        return IntStream.range(0,
                               value.length())
                        .mapToObj(i -> {
                                      String shrunk = value.substring(0, i) + value.substring(i + 1);
                                      return Shrinkable.shrinkable(shrunk,
                                                                   () -> shrinkString(shrunk));
                                  });
    }

    /// Shrink a list toward empty by removing elements.
    /// Example: [1,2,3] -> [[], [2,3], [1,3], [1,2], shrunk elements...]
    static <T> Stream<Shrinkable<List<T>>> shrinkList(List<Shrinkable<T>> elements) {
        if (elements.isEmpty()) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(Shrinkable.shrinkable(List.of(), () -> Stream.empty())),
                             Stream.concat(removeEachElement(elements), shrinkEachElement(elements)));
    }

    private static <T> Stream<Shrinkable<List<T>>> removeEachElement(List<Shrinkable<T>> elements) {
        return IntStream.range(0,
                               elements.size())
                        .mapToObj(i -> {
                                      List<Shrinkable<T>> shrunk = new ArrayList<>(elements);
                                      shrunk.remove(i);
                                      List<T> values = shrunk.stream()
                                                             .map(Shrinkable::value)
                                                             .toList();
                                      return Shrinkable.shrinkable(values,
                                                                   () -> shrinkList(shrunk));
                                  });
    }

    private static <T> Stream<Shrinkable<List<T>>> shrinkEachElement(List<Shrinkable<T>> elements) {
        return IntStream.range(0,
                               elements.size())
                        .boxed()
                        .flatMap(i -> elements.get(i)
                                              .shrink()
                                              .map(shrunkElement -> {
                                                       List<Shrinkable<T>> newElements = new ArrayList<>(elements);
                                                       newElements.set(i, shrunkElement);
                                                       List<T> values = newElements.stream()
                                                                                   .map(Shrinkable::value)
                                                                                   .toList();
                                                       return Shrinkable.shrinkable(values,
                                                                                    () -> shrinkList(newElements));
                                                   }));
    }

    record unused() implements Shrinkers {}
}
