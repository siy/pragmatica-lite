package org.pragmatica.lite.interfaces;

import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public sealed interface Maybe<T> {
    default <U> Maybe<U> map(Function<? super T, U> mapper) {
        return fold(Maybe::nothing, t -> just(mapper.apply(t)));
    }
    default <U> Maybe<U> flatMap(Function<? super T, Maybe<U>> mapper) {
        return fold(Maybe::nothing, mapper);
    }

    <R> R fold(Supplier<? extends R> nothingMapper, Function<? super T, ? extends R> justMapper);

    static <T> Just<T> just(T value) {
        return new Just<>(value);
    }

    @SuppressWarnings("unchecked")
    static <T> Nothing<T> nothing() {
        return (Nothing<T>) Nothing.INSTANCE;
    }

    static <T> Maybe<T> maybe(T value) {
        return value == null ? nothing() : just(value);
    }

    record Just<T>(T value) implements Maybe<T> {
        public  <R> R fold(Supplier<? extends R> nothingMapper, Function<? super T, ? extends R> justMapper) {
            return justMapper.apply(value);
        }
    }

    record Nothing<T>() implements Maybe<T> {
        static final Nothing<?> INSTANCE = new Nothing<>();

        @Override
        public <R> R fold(Supplier<? extends R> nothingMapper, Function<? super T, ? extends R> justMapper) {
            return nothingMapper.get();
        }
    }
}
