package org.pragmatica.http.server.util;

public sealed interface Either<T1, T2> {
    static <T1, T2> Left<T1, T2> left(T1 value) {
        return new Left<>(value);
    }

    static <T1, T2> Right<T1, T2> right(T2 value) {
        return new Right<>(value);
    }

    record Left<T1, T2>(T1 value) implements Either<T1, T2> {}

    record Right<T1, T2>(T2 value) implements Either<T1, T2> {}
}
