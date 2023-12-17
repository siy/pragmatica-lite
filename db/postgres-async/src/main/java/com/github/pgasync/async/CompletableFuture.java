package com.github.pgasync.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.*;

//Temporary replacement which going to be used to simplify refactoring
@SuppressWarnings("unused")
public class CompletableFuture<T> {

    public static <U> CompletableFuture<U> completedFuture(U value) {
        return null;
    }
    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        return null;
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return null;
    }

    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return null;
    }

    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return null;
    }

    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletableFuture<U>> fn) {
        return null;
    }

    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return null;
    }

    public boolean completeExceptionally(Throwable ex) {
        return false;
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return null;
    }

    //recover and replace exception with new completion stage
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return null;
    }
    public boolean complete(T value) {
        return false;
    }

    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return null;
    }

    public T get() throws InterruptedException, ExecutionException {
        return null;
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public T join() {
        return null;
    }
}
