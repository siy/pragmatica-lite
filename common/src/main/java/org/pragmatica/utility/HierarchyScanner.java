package org.pragmatica.utility;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public sealed interface HierarchyScanner {
    @SuppressWarnings("unchecked")
    static <T> Set<Class<? extends T>> concreteSubtypes(Class<T> type) {
        var result = new HashSet<Class<? extends T>>();

        if (!type.isInterface()) {
            result.add(type);
            return result;
        }

        var queue = new LinkedBlockingQueue<Class<?>>();
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
                    result.add((Class<? extends T>) clazz);
                }
            }
        }
        return result;
    }

    static <T> Set<Class<? extends T>> walkUpTheTree(Collection<Class<? extends T>> classes) {
        var interfaces = new HashSet<Class<?>>();
        var collected = new HashSet<Class<? extends T>>();

        for (var clazz : classes) {
            scanSingle(clazz, collected, interfaces);
        }

        return collected;
    }

    @SuppressWarnings("unchecked")
    private static <T> void scanSingle(Class<? extends T> clazz,
                                       Set<Class<? extends T>> result,
                                       Set<Class<?>> interfaces) {
        var queue = new LinkedBlockingQueue<Class<?>>();

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
            Class<?> type = queue.poll();

            Stream.of(clazz.getInterfaces())
                  .forEach(e -> {
                      if (interfaces.add(e)) {
                          queue.offer(e);
                      }
                  });

            concreteSubtypes(type)
                    .stream()
                    .map(cls -> (Class<? extends T>) cls)
                    .forEach(result::add);
        }
    }

    @SuppressWarnings("unused")
    record unused() implements HierarchyScanner {}
}
