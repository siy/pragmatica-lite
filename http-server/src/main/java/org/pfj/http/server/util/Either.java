package org.pfj.http.server.util;

public sealed interface Either<T1, T2> {

    static <T1, T2> Left<T1, T2> left(T1 value) {
        return new Left<>(value);
    }

    static <T1, T2> Right<T1, T2> right(T2 value) {
        return new Right<>(value);
    }

    final class Left<T1, T2> implements Either<T1, T2> {
        private final T1 value;

        private Left(T1 value) {
            this.value = value;
        }

        public T1 left() {
            return value;
        }
    }

    final class Right<T1, T2> implements Either<T1, T2> {
        private final T2 value;

        private Right(T2 value) {
            this.value = value;
        }

        public T2 right() {
            return value;
        }
    }
}
