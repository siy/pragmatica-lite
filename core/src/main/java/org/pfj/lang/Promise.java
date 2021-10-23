package org.pfj.lang;

import org.pfj.lang.Functions.FN1;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class Promise<T> extends CompletableFuture<Result<T>> {
    private Promise() {
    }

    private Promise(Result<T> value) {
        complete(value);
    }

    public static <R> Promise<R> promise() {
        return new Promise<>();
    }

    public static <R> Promise<R> promise(Function<? super Throwable, ? extends Cause> errorMapper, CompletableFuture<R> future) {
        var promise = new Promise<R>();

        future.whenComplete(
            (value, exception) -> promise.resolve(exception != null
                ? Result.failure(errorMapper.apply(exception))
                : Result.success(value))
        );
        return promise;
    }

    public static <R> Promise<R> promise(CompletableFuture<Result<R>> future) {
        var promise = new Promise<R>();
        future.thenAccept(promise::resolve);
        return promise;
    }

    public static <R> Promise<R> promise(Consumer<Promise<R>> setupLambda) {
        var promise = new Promise<R>();
        setupLambda.accept(promise);
        return promise;
    }

    public static <R> Promise<R> promise(Result<R> value) {
        return new Promise<>(value);
    }

    public static <R> Promise<R> success(R value) {
        return promise(Result.success(value));
    }

    public static <R> Promise<R> failure(Cause failure) {
        return promise(Result.failure(failure));
    }

    public Promise<T> succeed(T value) {
        complete(Result.success(value));
        return this;
    }

    public Promise<T> fail(Cause failure) {
        complete(failure.result());
        return this;
    }

    public Promise<T> resolve(Result<T> value) {
        complete(value);
        return this;
    }

    public Promise<T> onResult(Consumer<Result<T>> action) {
        whenComplete(
            (value, exception) -> action.accept(exception != null
                ? Causes.fromThrowable(exception).result()
                : value)
        );
        return this;
    }

    public Promise<T> onResultDo(Runnable runnable) {
        onResult(__ -> runnable.run());
        return this;
    }

    public Promise<T> onSuccess(Consumer<T> action) {
        return onResult(result -> result.onSuccess(action));
    }

    public Promise<T> onFailure(Consumer<? super Cause> action) {
        return onResult(result -> result.onFailure(action));
    }

    public <R> Promise<R> map(FN1<R, ? super T> mapper) {
        var resultPromise = Promise.<R>promise();

        onResult(result -> resultPromise.resolve(result.map(mapper)));

        return resultPromise;
    }

    @SuppressWarnings("unchecked")
    public <R> Promise<R> flatMap(Function<? super T, Promise<R>> mapper) {
        var resultPromise = new Promise[1];

        onResult(result -> resultPromise[0] = result.fold(failure -> this, mapper::apply));

        return resultPromise[0];
    }

    public Promise<T> async(Consumer<Promise<T>> consumer) {
        runAsync(() -> consumer.accept(this));
        return this;
    }
}
