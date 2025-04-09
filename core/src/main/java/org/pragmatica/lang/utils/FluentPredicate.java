package org.pragmatica.lang.utils;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface FluentPredicate<T> extends Predicate<T> {
    default FluentPredicate<T> ifTrue(T value, Consumer<T> consumer) {
        if (test(value)) {
            consumer.accept(value);
        }
        return this;
    }

    default FluentPredicate<T> ifTrue(T value, Runnable runnable) {
        return ifTrue(value, _ -> runnable.run());
    }

    default FluentPredicate<T> ifFalse(T value, Consumer<T> consumer) {
        if (!test(value)) {
            consumer.accept(value);
        }
        return this;
    }

    default FluentPredicate<T> ifFalse(T value, Runnable runnable) {
        return ifFalse(value, _ -> runnable.run());
    }

    static <T> FluentPredicate<T> from(Predicate<T> predicate) {
        return predicate::test;
    }
}
