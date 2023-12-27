package com.github.pgasync.async;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ThrowingPromise<T> extends CompletableFuture<T> {
    public static <U> ThrowingPromise<U> successful(U value) {
        return ThrowingPromise.<U>create().succeed(value);
    }

    public static <U> ThrowingPromise<U> failed(ThrowableCause ex) {
        var promise = ThrowingPromise.<U>create();
        promise.fail(ex);
        return promise;
    }

    public static <T> ThrowingPromise<Unit> allOf(Stream<ThrowingPromise<T>> cfs) {
        var promises = cfs.toArray(CompletableFuture<?>[]::new);

        var promise = ThrowingPromise.<Unit>create();

        CompletableFuture.allOf(promises)
                         .whenComplete((_, th) -> {
                             if (th != null) {
                                 promise.fail(ThrowableCause.asCause(th));
                             } else {
                                 promise.succeed(Unit.aUnit());
                             }
                         });
        return promise;
    }

    @Override
    public <U> ThrowingPromise<U> newIncompleteFuture() {
        return create();
    }

    @SuppressWarnings("unchecked")
    public <U> ThrowingPromise<U> map(Fn1<? extends U, ? super T> fn) {
        return (ThrowingPromise<U>) thenApply(fn::apply);
    }

    public <U> ThrowingPromise<U> fold(Fn1<ThrowingPromise<U>, Result<T>> fn) {
        return (ThrowingPromise<U>) handle((value, th) -> fn.apply(buildResult(value, th)))
            .thenCompose(Function.identity());
    }

    public ThrowingPromise<T> onResult(Consumer<Result<T>> action) {
        return (ThrowingPromise<T>) whenComplete((value, th) -> action.accept(buildResult(value, th)));
    }

    private static <T> Result<T> buildResult(T value, Throwable th) {
        if (th != null) {
            return Result.failure(ThrowableCause.asCause(th));
        } else {
            return Result.success(value);
        }
    }

    public <U> ThrowingPromise<U> flatMap(Fn1<? extends ThrowingPromise<U>, ? super T> fn) {
        return (ThrowingPromise<U>) thenCompose(fn::apply);
    }

    public ThrowingPromise<Unit> onSuccess(Consumer<? super T> action) {
        return (ThrowingPromise<Unit>) thenAccept(action)
            .thenApply(Unit::unit);
    }

    public void fail(ThrowableCause cause) {
        completeExceptionally(cause.throwable());
    }

    public void failAsync(Supplier<? extends ThrowableCause> supplier) {
        defaultExecutor()
            .execute(() -> fail(supplier.get()));
    }

    public ThrowingPromise<T> succeed(T value) {
        complete(value);
        return this;
    }

    public void succeedAsync(Supplier<? extends T> supplier) {
        defaultExecutor()
            .execute(() -> complete(supplier.get()));
    }

    //recover and replace exception with new completion stage
    public ThrowingPromise<T> tryRecover(Fn1<? extends T, ThrowableCause> fn) {
        return (ThrowingPromise<T>) exceptionally(th -> fn.apply(ThrowableCause.asCause(th)));
    }

    public T await() {
        return join();
    }

    public static <T> ThrowingPromise<T> create() {
        return new ThrowingPromise<>();
    }
}
