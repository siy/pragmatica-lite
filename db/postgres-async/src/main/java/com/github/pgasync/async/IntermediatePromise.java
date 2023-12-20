package com.github.pgasync.async;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

//Temporary replacement which going to be used to simplify refactoring
@SuppressWarnings("unused")
public class IntermediatePromise<T> extends java.util.concurrent.CompletableFuture<T> {
    public static <U> IntermediatePromise<U> successful(U value) {
        return IntermediatePromise.<U>create().succeed(value);
    }

    public static <U> IntermediatePromise<U> failed(Throwable ex) {
        var promise = IntermediatePromise.<U>create();
        promise.fail(ex);
        return promise;
    }

    public static <T> IntermediatePromise<Void> allOf(Stream<IntermediatePromise<T>> cfs) {
        var promises = cfs.toArray(java.util.concurrent.CompletableFuture<?>[]::new);

        var promise = IntermediatePromise.<Void>create();
        java.util.concurrent.CompletableFuture.allOf(promises).whenComplete((v, th) -> {
            if (th != null) {
                promise.fail(th);
            } else {
                promise.succeed(null);
            }
        });
        return promise;
    }

    @Override
    public <U> IntermediatePromise<U> newIncompleteFuture() {
        return create();
    }

    @SuppressWarnings("unchecked")
    public <U> IntermediatePromise<U> map(Function<? super T, ? extends U> fn) {
        return (IntermediatePromise<U>) this.thenApply(fn);
    }

    @SuppressWarnings("unchecked")
    public <U> IntermediatePromise<U> fold(BiFunction<? super T, Throwable, ? extends U> fn) {
        return (IntermediatePromise<U>) this.handle(fn);
    }

    public <U> IntermediatePromise<U> flatMap(Function<? super T, ? extends IntermediatePromise<U>> fn) {
        return (IntermediatePromise<U>) this.thenCompose(fn);
    }

    public IntermediatePromise<Void> onSuccess(Consumer<? super T> action) {
        return (IntermediatePromise<Void>) this.thenAccept(action);
    }

    public void fail(Throwable ex) {
        this.completeExceptionally(ex);
    }

    public void resolveAsync(Supplier<? extends T> supplier) {
        this.defaultExecutor()
            .execute(() -> this.succeed(supplier.get()));
    }

    //recover and replace exception with new completion stage
    public IntermediatePromise<T> fail(Function<Throwable, ? extends T> fn) {
        return (IntermediatePromise<T>) this.exceptionally(fn);
    }

    public IntermediatePromise<T> succeed(T value) {
        this.complete(value);
        return this;
    }

    public IntermediatePromise<T> onResult(BiConsumer<? super T, ? super Throwable> action) {
        return (IntermediatePromise<T>) this.whenComplete(action);
    }

    public T await() {
        return this.join();
    }

    public static <T> IntermediatePromise<T> create() {
        return new IntermediatePromise<>();
    }
}
