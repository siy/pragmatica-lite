package com.github.pgasync.async;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;

import java.util.function.Consumer;
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
    public <U> IntermediatePromise<U> map(Fn1<? extends U, ? super T> fn) {
        return (IntermediatePromise<U>) this.thenApply(fn::apply);
    }

    @SuppressWarnings("unchecked")
    public <U> IntermediatePromise<U> fold(Fn1<? extends U, Result<T>> fn) {
        return (IntermediatePromise<U>) this.handle((value, th) -> fn.apply(buildResult(value, th)));
    }

    public IntermediatePromise<T> onResult(Consumer<Result<T>> action) {
        return (IntermediatePromise<T>) this.whenComplete((value, th) -> action.accept(buildResult(value, th)));
    }

    private static <T> Result<T> buildResult(T value, Throwable th) {
        if (th != null) {
            return Result.failure(ThrowableCause.fromThrowable(th));
        } else {
            return Result.success(value);
        }
    }

    public <U> IntermediatePromise<U> flatMap(Fn1<? extends IntermediatePromise<U>, ? super T> fn) {
        return (IntermediatePromise<U>) this.thenCompose(fn::apply);
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
    public IntermediatePromise<T> tryRecover(Fn1<? extends T, Throwable> fn) {
        return (IntermediatePromise<T>) this.exceptionally(fn::apply);
    }

    public IntermediatePromise<T> succeed(T value) {
        this.complete(value);
        return this;
    }

    public T await() {
        return this.join();
    }

    public static <T> IntermediatePromise<T> create() {
        return new IntermediatePromise<>();
    }
}
