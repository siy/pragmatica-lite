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

package org.pragmatica.utility;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

/**
 * Utility for scanning class hierarchies, particularly useful for sealed interfaces.
 */
public sealed interface HierarchyScanner {
    /**
     * Find all concrete subtypes of a sealed interface or class.
     */
    @SuppressWarnings("unchecked")
    static <T> Set<Class< ? extends T>> concreteSubtypes(Class<T> type) {
        var result = new HashSet<Class< ? extends T>>();
        if (!type.isInterface()) {
            result.add(type);
            return result;
        }
        var queue = new LinkedBlockingQueue<Class< ? >>();
        queue.offer(type);
        while (!queue.isEmpty()) {
            var currentInterface = queue.poll();
            if (!currentInterface.isSealed()) {
                continue;
            }
            for (var clazz : currentInterface.getPermittedSubclasses()) {
                if (clazz.isInterface()) {
                    queue.offer(clazz);
                } else {
                    result.add((Class< ? extends T>) clazz);
                }
            }
        }
        return result;
    }

    /**
     * Walk up the type hierarchy from a collection of classes.
     */
    static <T> Set<Class< ? extends T>> walkUpTheTree(Collection<Class< ? extends T>> classes) {
        var interfaces = new HashSet<Class< ? >>();
        var collected = new HashSet<Class< ? extends T>>();
        for (var clazz : classes) {
            scanSingle(clazz, collected, interfaces);
        }
        return collected;
    }

    @SuppressWarnings("unchecked")
    private static <T> void scanSingle(Class< ? extends T> clazz,
                                       Set<Class< ? extends T>> result,
                                       Set<Class< ? >> interfaces) {
        var queue = new LinkedBlockingQueue<Class< ? >>();
        if (clazz.isInterface()) {
            if (interfaces.add(clazz)) {
                queue.offer(clazz);
            }
        } else {
            result.add(clazz);
            Stream.of(clazz.getInterfaces())
                  .forEach(e -> {
                      if (interfaces.add(e)) {
                          queue.offer(e);
                      }
                  });
        }
        while (!queue.isEmpty()) {
            Class< ? > type = queue.poll();
            Stream.of(clazz.getInterfaces())
                  .forEach(e -> {
                      if (interfaces.add(e)) {
                          queue.offer(e);
                      }
                  });
            concreteSubtypes(type)
                            .stream()
                            .map(cls -> (Class< ? extends T>) cls)
                            .forEach(result::add);
        }
    }

    @SuppressWarnings("unused")
    record unused() implements HierarchyScanner {}
}
