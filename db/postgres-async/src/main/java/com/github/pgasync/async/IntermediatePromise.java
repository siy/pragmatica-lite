package com.github.pgasync.async;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

//Temporary replacement which going to be used to simplify refactoring
@SuppressWarnings("unused")
public class IntermediatePromise<T> extends CompletableFuture<T> {
    public static <U> IntermediatePromise<U> successful(U value) {
        return IntermediatePromise.<U>create().succeed(value);
    }

    public static <U> IntermediatePromise<U> failed(Throwable ex) {
        var promise = IntermediatePromise.<U>create();
        promise.fail(ex);
        return promise;
    }

    public static <T> IntermediatePromise<Unit> allOf(Stream<IntermediatePromise<T>> cfs) {
        var promises = cfs.toArray(CompletableFuture<?>[]::new);

        var promise = IntermediatePromise.<Unit>create();

        CompletableFuture.allOf(promises)
                         .whenComplete((v, th) -> {
                             if (th != null) {
                                 promise.fail(th);
                             } else {
                                 promise.succeed(Unit.aUnit());
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
        return (IntermediatePromise<U>) thenApply(fn::apply);
    }

    public <U> IntermediatePromise<U> fold(Fn1<IntermediatePromise<U>, Result<T>> fn) {
        return (IntermediatePromise<U>) handle((value, th) -> fn.apply(buildResult(value, th)))
            .thenCompose(Function.identity());
    }

    public IntermediatePromise<T> onResult(Consumer<Result<T>> action) {
        return (IntermediatePromise<T>) whenComplete((value, th) -> action.accept(buildResult(value, th)));
    }

    private static <T> Result<T> buildResult(T value, Throwable th) {
        if (th != null) {
            return Result.failure(ThrowableCause.fromThrowable(th));
        } else {
            return Result.success(value);
        }
    }

    public <U> IntermediatePromise<U> flatMap(Fn1<? extends IntermediatePromise<U>, ? super T> fn) {
        return (IntermediatePromise<U>) thenCompose(fn::apply);
    }

    public IntermediatePromise<Unit> onSuccess(Consumer<? super T> action) {
        return (IntermediatePromise<Unit>) thenAccept(action)
            .thenApply(Unit::unit);
    }

    public void fail(Throwable ex) {
        completeExceptionally(ex);
    }

    public void failAsync(Supplier<? extends Throwable> supplier) {
        defaultExecutor()
            .execute(() -> completeExceptionally(supplier.get()));
    }

    public IntermediatePromise<T> succeed(T value) {
        complete(value);
        return this;
    }

    public void succeedAsync(Supplier<? extends T> supplier) {
        defaultExecutor()
            .execute(() -> complete(supplier.get()));
    }

    //recover and replace exception with new completion stage
    public IntermediatePromise<T> tryRecover(Fn1<? extends T, Throwable> fn) {
        return (IntermediatePromise<T>) exceptionally(fn::apply);
    }

    public T await() {
        return join();
    }

    public static <T> IntermediatePromise<T> create() {
        return new IntermediatePromise<>();
    }
}
