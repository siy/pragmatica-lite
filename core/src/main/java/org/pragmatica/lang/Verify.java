package org.pragmatica.lang;

import org.pragmatica.lang.utils.Causes;

import java.util.function.Predicate;

public sealed interface Verify {

    static <T> Result<T> satisfies(T value, Predicate<T> predicate) {
        if (predicate.test(value)) {
            return Result.success(value);
        }

        return Causes.forValue("Value {0} does not satisfy the predicate")
                     .apply(value)
                     .result();
    }

    @SuppressWarnings("unused")
    record unused() implements Verify {}
}
