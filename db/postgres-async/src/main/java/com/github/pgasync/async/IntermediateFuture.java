package com.github.pgasync.async;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

//Temporary replacement which going to be used to simplify refactoring
@SuppressWarnings("unused")
public interface IntermediateFuture<T> {
    static <U> IntermediateFuture<U> completedFuture(U value) {
        return IntermediateFuture.<U>create().complete(value);
    }

    static <U> IntermediateFuture<U> failedFuture(Throwable ex) {
        var future = IntermediateFuture.<U>create();
        future.completeExceptionally(ex);
        return future;
    }

    static <T> IntermediateFuture<Void> allOf(Stream<IntermediateFuture<T>> cfs) {
        var futures = cfs.toArray(CompletableFuture<?>[]::new);

        return CompletableFuture.allOf(futures);
    }

    <U> IntermediateFuture<U> thenApply(Function<? super T, ? extends U> fn);

    <U> IntermediateFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    <U> IntermediateFuture<U> thenCompose(Function<? super T, ? extends IntermediateFuture<U>> fn);

    IntermediateFuture<Void> thenAccept(Consumer<? super T> action);

    boolean completeExceptionally(Throwable ex);

    default IntermediateFuture<T> completeAsync(Supplier<? extends T> supplier) {
        ((CompletableFuture<T>) this).defaultExecutor().execute(() -> this.complete(supplier.get()));
        return this;
    }

    //recover and replace exception with new completion stage
    IntermediateFuture<T> exceptionally(Function<Throwable, ? extends T> fn);

    IntermediateFuture<T> complete(T value);

    IntermediateFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    T join();

    static <T> IntermediateFuture<T> create() {
        return new CompletableFuture<>();
    }
}
