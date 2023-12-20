/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.lang;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn2;
import org.pragmatica.lang.Functions.Fn3;
import org.pragmatica.lang.Functions.Fn4;
import org.pragmatica.lang.Functions.Fn5;
import org.pragmatica.lang.Functions.Fn6;
import org.pragmatica.lang.Functions.Fn7;
import org.pragmatica.lang.Functions.Fn8;
import org.pragmatica.lang.Functions.Fn9;
import org.pragmatica.lang.Functions.FnX;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.io.CoreError;
import org.pragmatica.lang.io.Timeout;
import org.pragmatica.lang.utils.ResultCollector;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.pragmatica.lang.Result.unitResult;
import static org.pragmatica.lang.utils.ActionableThreshold.threshold;
import static org.pragmatica.lang.utils.ResultCollector.resultCollector;

/**
 * The (perhaps not yet available) result of the asynchronous operation.
 */
@SuppressWarnings("unused")
public interface Promise<T> {
    /**
     * Resolve current instance. This action can be performed only once, all subsequent attempts will be ignored and state of the promise will remain
     * unchanged.
     *
     * @param value The bufferSize to resolve the Promise instance.
     *
     * @return Current instance
     */
    Promise<T> resolve(Result<T> value);

    /**
     * Resolve current instance with {@link CoreError.Cancelled} error.
     *
     * @return Current instance
     */
    default Promise<T> cancel() {
        return failure(new CoreError.Cancelled("Promise cancelled"));
    }

    boolean isResolved();

    /**
     * Transform current instance into the instance containing another type using provided transformation function.
     *
     * @param mapper The transformation function
     *
     * @return Transformed instance
     */
    <U> Promise<U> map(Fn1<U, ? super T> mapper);

    /**
     * Transform current instance by replacing stored value with one returned by provided supplier.
     *
     * @param mapper The replacement value supplier. Invoked only if current instance is resolved with success.
     *
     * @return Transformed instance
     */
    default <U> Promise<U> map(Supplier<U> mapper) {
        return map(_ -> mapper.get());
    }

    /**
     * Compose current instance with the function which returns a Promise of another type.
     *
     * @param mapper The function to compose with
     *
     * @return Composed instance
     */
    <U> Promise<U> flatMap(Fn1<Promise<U>, ? super T> mapper);

    /**
     * Transform current instance by replacing stored result with one returned by provided supplier.
     *
     * @param mapper The replacement result supplier. Invoked only if current instance is resolved with success.
     *
     * @return Transformed instance
     */
    default <U> Promise<U> flatMap(Supplier<Promise<U>> mapper) {
        return flatMap(_ -> mapper.get());
    }

    Promise<T> mapError(Fn1<Cause, Cause> mapper);

    /**
     * Compose current instance with the function which returns a Promise of another type. Unlike {@link Promise#flatMap(Fn1)}, the function will be
     * invoked in any case, regardless of the result of the current instance.
     *
     * @param mapper the mapper
     *
     * @return Transformed instance
     */
    <U> Promise<U> fold(Fn1<Promise<U>, Result<T>> mapper);

    /**
     * General purpose method to start new virtual thread.
     *
     * @param runnable The {@link Runnable} to run
     */
    static void runAsync(Runnable runnable) {
        AsyncExecutor.INSTANCE.runAsync(runnable);
    }

    /**
     * General purpose method to run provided {@link Runnable} after specified timeout.
     *
     * @param timeout  The timeout
     * @param runnable The {@link Runnable} to run
     */
    static void runAsync(Timeout timeout, Runnable runnable) {
        runAsync(() -> {
            try {
                Thread.sleep(timeout.duration());
            } catch (InterruptedException e) {
                // ignore
            }
            runnable.run();
        });
    }

    /**
     * Run asynchronous task. The task will receive current instance of Promise as a parameter.
     *
     * @param action The task to run
     *
     * @return Current instance
     */
    Promise<T> async(Consumer<Promise<T>> action);

    /**
     * Run asynchronous task after specified timeout. The task will receive current instance of Promise as a parameter.
     *
     * @param action The task to run
     *
     * @return Current instance
     */
    Promise<T> async(Timeout timeout, Consumer<Promise<T>> action);

    /**
     * Wait indefinitely for completion of the Promise and all attached actions (see {@link #onResult(Consumer)}).
     *
     * @return Value of the Promise
     */
    Result<T> await();

    /**
     * Wait for completion of the Promise and all attached actions. The waiting time is limited to specified timeout. If timeout expires before
     * Promise is resolved, then returned result contains {@link CoreError.Timeout} error.
     * <p>
     * Note that if return signals that timeout is expired, this does not change state of the promise.
     *
     * @param timeout How long to wait for promise resolution
     *
     * @return Value of the Promise or timeout error.
     */
    Result<T> await(Timeout timeout);

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance. If promise is already resolved by the time of
     * invocation of this method, then provided action will be executed immediately.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    Promise<T> onResult(Consumer<Result<T>> action);

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance. If promise is already resolved by the time of
     * invocation of this method, then provided action will be executed immediately. Note that unlike {@link Promise#onResult(Consumer)}, the action
     * passed to this method does not receive parameter.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onResultRun(Runnable action) {
        return onResult(_ -> action.run());
    }

    default <U> Promise<T> onResultDo(Fn1<Promise<U>, Result<? super T>> mapper) {
        return fold(result -> mapper.apply(result)
                                    .flatMap(_ -> Promise.this));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing
     * {@link Result.Success}. If instance is resolved with {@link Result} containing {@link Result.Failure}, then action will not be invoked. If
     * promise is already resolved by the time of invocation of this method and bufferSize is a {@link Result.Success}, then provided action will be
     * executed immediately.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onSuccess(Consumer<T> action) {
        return onResult(result -> result.onSuccess(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing
     * {@link Result.Success}. If instance is resolved with {@link Result} containing {@link Result.Failure}, then action will not be invoked. If
     * promise is already resolved by the time of invocation of this method and bufferSize is a {@link Result.Success}, then provided action will be
     * executed immediately. Note that unlike {@link Promise#onSuccess(Consumer)}, the action passed to this method does not receive parameter.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onSuccessRun(Runnable action) {
        return onResult(result -> result.onSuccessRun(action));
    }

    default <U> Promise<T> onSuccessDo(Fn1<Promise<U>, ? super T> mapper) {
        return fold(result -> result.fold(_ -> Promise.this,
                                          value -> mapper.apply(value)
                                                         .flatMap(_ -> Promise.this)));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing
     * {@link Result.Failure}. If instance is resolved with {@link Result} containing {@link Result.Success}, then action will not be invoked. If
     * promise is already resolved by the time of invocation of this method and bufferSize is a {@link Result.Failure}, then provided action will be
     * executed immediately.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onFailure(Consumer<? super Cause> action) {
        return onResult(result -> result.onFailure(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing
     * {@link Result.Failure}. If instance is resolved with {@link Result} containing {@link Result.Success}, then action will not be invoked. If
     * promise is already resolved by the time of invocation of this method and bufferSize is a {@link Result.Failure}, then provided action will be
     * executed immediately. Note that unlike {@link Promise#onFailure(Consumer)}, the action passed to this method does not receive parameter.
     *
     * @param action the action to execute
     *
     * @return Current instance
     */
    default Promise<T> onFailureRun(Runnable action) {
        return onResult(result -> result.onFailureRun(action));
    }

    /**
     * Attach a side effect action which will be executed upon resolution of the current instance with {@link Result} containing
     * {@link Result.Failure}. Unlike {@link Promise#onFailureRun(Runnable)}, the action passed to this method returns yet another
     * {@link Promise} and the instance of the {@link Promise} returned by this method will be resolved only after resolution of
     * the instance returned by the action. This is useful, for example, for resource cleanup.
     *
     * @param mapper the action to execute
     *
     * @return Current instance
     */

    default <U> Promise<T> onFailureDo(Fn1<Promise<U>, ? super Cause> mapper) {
        return fold(result -> result.fold(cause -> mapper.apply(cause)
                                                         .flatMap(_ -> Promise.this),
                                          _ -> Promise.this));
    }

    /**
     * Resolve current instance with the {@link Result} containing {@link Result.Success}. If current instance is already resolved, then this method
     * invocation has no effect.
     *
     * @param value the bufferSize to resolve.
     *
     * @return Current instance
     */
    default Promise<T> success(T value) {
        return resolve(Result.success(value));
    }

    /**
     * Resolve current instance with the {@link Result} containing {@link Result.Failure}. If current instance is already resolved, then this method
     * invocation has no effect.
     *
     * @param cause the failure cause.
     *
     * @return Current instance
     */
    default Promise<T> failure(Cause cause) {
        return resolve(cause.result());
    }

    static <T> void failAll(Cause cause, Promise<T> ... promises) {
        failAll(cause, List.of(promises));
    }

    static <T> void failAll(Cause cause, List<Promise<T>> promises) {
        promises.forEach(promise -> promise.failure(cause));
    }

    /**
     * Create an unresolved instance.
     *
     * @return Created instance.
     */
    static <R> Promise<R> promise() {
        return new PromiseImpl<>(null);
    }

    /**
     * Create an unresolved instance and run asynchronous action which will receive created instance as a parameter.
     *
     * @param consumer The action to run
     *
     * @return Created instance
     */
    static <R> Promise<R> promise(Consumer<Promise<R>> consumer) {
        var promise = Promise.<R>promise();
        runAsync(() -> consumer.accept(promise));
        return promise;
    }

    /**
     * Create a resolved instance.
     *
     * @param value The bufferSize which will be stored in the created instance
     *
     * @return Created instance
     */
    static <R> Promise<R> resolved(Result<R> value) {
        return new PromiseImpl<>(value);
    }

    static <R> Promise<R> successful(R value) {
        return new PromiseImpl<>(Result.success(value));
    }

    static <R> Promise<R> failed(Cause failure) {
        return new PromiseImpl<>(Result.failure(failure));
    }

    Promise<?> UNIT = Promise.resolved(unitResult());

    @SuppressWarnings("unchecked")
    static <R> Promise<R> unitPromise() {
        return (Promise<R>) UNIT;
    }

    default Promise<Unit> mapToUnit() {
        return map(Unit::unit);
    }

    /**
     * Return promise which will be resolved when any of the provided promises will be resolved. Remaining promises will be cancelled.
     *
     * @param promises Input promises.
     *
     * @return Created promise.
     */
    @SafeVarargs
    static <R> Promise<R> any(Promise<R>... promises) {
        return Promise.promise(result -> List.of(promises)
                                             .forEach(promise -> promise.onResult(result::resolve)
                                                                        .onResultRun(() -> cancelAll(promises))));
    }

    /**
     * Return promise which will be resolved once any of the promises provided as a parameters will be resolved with success. If none of the promises
     * will be resolved with success, then created instance will be resolved with provided {@code failureResult}.
     *
     * @param failureResult Result in case if no instances were resolved with success
     * @param promises      Input promises
     *
     * @return Created instance
     */
    @SafeVarargs
    static <T> Promise<T> anySuccess(Result<T> failureResult, Promise<T>... promises) {
        return Promise.promise(anySuccess -> threshold(promises.length, () -> anySuccess.resolve(failureResult))
            .apply(at -> List.of(promises)
                             .forEach(promise -> promise.onResult(result -> result.onSuccess(anySuccess::success)
                                                                                  .onSuccessRun(() -> cancelAll(promises)))
                                                        .onResultRun(at::registerEvent))));
    }

    static <T> Promise<T> anySuccess(Result<T> failureResult, List<Promise<T>> promises) {
        return Promise.promise(anySuccess -> threshold(promises.size(), () -> anySuccess.resolve(failureResult))
            .apply(at -> promises.forEach(promise -> promise.onResult(result -> result.onSuccess(anySuccess::success)
                                                                                      .onSuccessRun(() -> cancelAll(promises)))
                                                            .onResultRun(at::registerEvent))));
    }

    /**
     * Return promise which will be resolved once any of the promises provided as a parameters will be resolved with success. If none of the promises
     * will be resolved with success, then created instance will be resolved with {@link CoreError.Cancelled}.
     *
     * @param promises Input promises
     *
     * @return Created instance
     */
    @SafeVarargs
    static <T> Promise<T> anySuccess(Promise<T>... promises) {
        return anySuccess(Result.failure(new CoreError.Cancelled("Other Promise instance succeeded")), promises);
    }

    static <T> Promise<T> anySuccess(List<Promise<T>> promises) {
        return anySuccess(Result.failure(new CoreError.Cancelled("Other Promise instance succeeded")), promises);
    }

    /**
     * Cancel all provided promises.
     *
     * @param promises Input promises.
     */
    @SafeVarargs
    static <T> void cancelAll(Promise<T>... promises) {
        cancelAll(List.of(promises));
    }

    /**
     * Cancel all provided promises.
     *
     * @param promises Input promises.
     */
    static <T> void cancelAll(List<Promise<T>> promises) {
        failAll(new CoreError.Cancelled("Promise cancelled"), promises);
    }

    @SuppressWarnings("unchecked")
    static <T> Promise<List<Result<T>>> allOf(Collection<Promise<T>> promises) {
        var array = promises.toArray(new Promise[0]);
        var promise = Promise.promise();
        var collector = ResultCollector.resultCollector(promises.size(),
                                                        values -> promise.success(List.of(values)));
        IntStream.range(0, promises.size())
                 .forEach(index -> array[index].onResult(result -> collector.registerEvent(index, result)));

        return promise.map(list -> (List<Result<T>>) list);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     * <p>
     * The function for single input promise is provided for completeness.
     *
     * @param promise1 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    static <T1> Mapper1<T1> all(Promise<T1> promise1) {
        return () -> promise1.map(Tuple::tuple);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2> Mapper2<T1, T2> all(Promise<T1> promise1, Promise<T2> promise2) {
        return () -> setup(values -> Tuple.tuple((T1) values[0], (T2) values[1]), promise1, promise2);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     * @param promise3 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, T3> Mapper3<T1, T2, T3> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3) {

        return () -> setup(values -> Tuple.tuple((T1) values[0], (T2) values[1], (T3) values[2]), promise1, promise2, promise3);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     * @param promise3 Input promise
     * @param promise4 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4> Mapper4<T1, T2, T3, T4> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4) {

        return () -> setup(values -> Tuple.tuple((T1) values[0], (T2) values[1], (T3) values[2], (T4) values[3]),
                           promise1, promise2, promise3, promise4);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     * @param promise3 Input promise
     * @param promise4 Input promise
     * @param promise5 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5> Mapper5<T1, T2, T3, T4, T5> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4, Promise<T5> promise5) {

        return () -> setup(values -> Tuple.tuple((T1) values[0], (T2) values[1], (T3) values[2], (T4) values[3], (T5) values[4]),
                           promise1, promise2, promise3, promise4, promise5);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     * @param promise3 Input promise
     * @param promise4 Input promise
     * @param promise5 Input promise
     * @param promise6 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6> Mapper6<T1, T2, T3, T4, T5, T6> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4,
        Promise<T5> promise5, Promise<T6> promise6) {

        return () -> setup(values -> Tuple.tuple(
                               (T1) values[0], (T2) values[1], (T3) values[2], (T4) values[3], (T5) values[4], (T6) values[5]),
                           promise1, promise2, promise3, promise4, promise5, promise6);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     * @param promise3 Input promise
     * @param promise4 Input promise
     * @param promise5 Input promise
     * @param promise6 Input promise
     * @param promise7 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7> Mapper7<T1, T2, T3, T4, T5, T6, T7> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4,
        Promise<T5> promise5, Promise<T6> promise6, Promise<T7> promise7) {

        return () -> setup(values -> Tuple.tuple(
                               (T1) values[0], (T2) values[1], (T3) values[2], (T4) values[3],
                               (T5) values[4], (T6) values[5], (T7) values[6]),
                           promise1, promise2, promise3, promise4, promise5, promise6, promise7);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     * @param promise3 Input promise
     * @param promise4 Input promise
     * @param promise5 Input promise
     * @param promise6 Input promise
     * @param promise7 Input promise
     * @param promise8 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4,
        Promise<T5> promise5, Promise<T6> promise6, Promise<T7> promise7, Promise<T8> promise8) {

        return () -> setup(values -> Tuple.tuple(
                               (T1) values[0], (T2) values[1], (T3) values[2], (T4) values[3],
                               (T5) values[4], (T6) values[5], (T7) values[6], (T8) values[7]),
                           promise1, promise2, promise3, promise4, promise5, promise6, promise7, promise8);
    }

    /**
     * Return a promise which will be resolved when all promises passed as a parameter will be resolved. If any of the provided promises will be
     * resolved with error, then resulting promise will be also resolved with error.
     *
     * @param promise1 Input promise
     * @param promise2 Input promise
     * @param promise3 Input promise
     * @param promise4 Input promise
     * @param promise5 Input promise
     * @param promise6 Input promise
     * @param promise7 Input promise
     * @param promise8 Input promise
     * @param promise9 Input promise
     *
     * @return Promise instance, which will be resolved with all collected results.
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> all(
        Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3, Promise<T4> promise4, Promise<T5> promise5,
        Promise<T6> promise6, Promise<T7> promise7, Promise<T8> promise8, Promise<T9> promise9) {

        return () -> setup(values -> Tuple.tuple(
                               (T1) values[0], (T2) values[1], (T3) values[2], (T4) values[3], (T5) values[4],
                               (T6) values[5], (T7) values[6], (T8) values[7], (T9) values[8]),
                           promise1, promise2, promise3, promise4, promise5, promise6, promise7, promise8, promise9);
    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper1
     */
    interface Mapper1<T1> {

        Promise<Tuple.Tuple1<T1>> id();

        default <R> Promise<R> map(Fn1<R, T1> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn1<Promise<R>, T1> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper2
     */
    interface Mapper2<T1, T2> {

        Promise<Tuple.Tuple2<T1, T2>> id();

        default <R> Promise<R> map(Fn2<R, T1, T2> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn2<Promise<R>, T1, T2> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper3
     */
    interface Mapper3<T1, T2, T3> {

        Promise<Tuple.Tuple3<T1, T2, T3>> id();

        default <R> Promise<R> map(Fn3<R, T1, T2, T3> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn3<Promise<R>, T1, T2, T3> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper4
     */
    interface Mapper4<T1, T2, T3, T4> {

        Promise<Tuple.Tuple4<T1, T2, T3, T4>> id();

        default <R> Promise<R> map(Fn4<R, T1, T2, T3, T4> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn4<Promise<R>, T1, T2, T3, T4> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper5
     */
    interface Mapper5<T1, T2, T3, T4, T5> {

        Promise<Tuple.Tuple5<T1, T2, T3, T4, T5>> id();

        default <R> Promise<R> map(Fn5<R, T1, T2, T3, T4, T5> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn5<Promise<R>, T1, T2, T3, T4, T5> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper6
     */
    interface Mapper6<T1, T2, T3, T4, T5, T6> {

        Promise<Tuple.Tuple6<T1, T2, T3, T4, T5, T6>> id();

        default <R> Promise<R> map(Fn6<R, T1, T2, T3, T4, T5, T6> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn6<Promise<R>, T1, T2, T3, T4, T5, T6> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper7
     */
    interface Mapper7<T1, T2, T3, T4, T5, T6, T7> {

        Promise<Tuple.Tuple7<T1, T2, T3, T4, T5, T6, T7>> id();

        default <R> Promise<R> map(Fn7<R, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn7<Promise<R>, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper8
     */
    interface Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> {

        Promise<Tuple.Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> id();

        default <R> Promise<R> map(Fn8<R, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper9
     */
    interface Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {

        Promise<Tuple.Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> id();

        default <R> Promise<R> map(Fn9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

    }

    private static <R> Promise<R> setup(FnX<R> transformer, Promise<?>... promises) {
        var promise = Promise.<R>promise();
        var collector = resultCollector(promises.length, values -> promise.success(transformer.apply(values)));

        int count = 0;
        for (var p : promises) {
            final var index = count++;
            p.onResult(result -> result.accept(promise::failure, value -> collector.registerEvent(index, value)));
        }

        return promise;
    }

    final class PromiseImpl<T> implements Promise<T> {
        @SuppressWarnings("rawtypes")
        private static final CompletionAction NOP = new CompletionAction<>(Functions::unitFn, null);

        @SuppressWarnings({"unchecked", "FieldMayBeFinal", "CanBeFinal"})
        private volatile CompletionAction<T> head = NOP;
        private volatile CompletionAction<T> processed;
        @SuppressWarnings("FieldMayBeFinal")
        private volatile Result<T> value;

        private static final VarHandle HEAD;
        private static final VarHandle VALUE;

        private static class CompletionAction<T> {
            private volatile CompletionAction<T> next;
            private final Consumer<Result<T>> action;
            private final PromiseImpl<?> dependency;

            private CompletionAction(Consumer<Result<T>> action, PromiseImpl<?> dependency) {
                this.action = action;
                this.dependency = dependency;
            }

            @Override
            public String toString() {
                return this == NOP ? "NOP" : STR."Action(\{dependency == null ? "free" : dependency.toString()}\{')'}";
            }
        }

        static {
            try {
                final var lookup = MethodHandles.lookup();
                HEAD = lookup.findVarHandle(PromiseImpl.class, "head", CompletionAction.class);
                VALUE = lookup.findVarHandle(PromiseImpl.class, "value", Result.class);
            } catch (final ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private PromiseImpl(Result<T> value) {
            this.value = value;
            this.processed = value == null ? null : this.head;
        }

        @Override
        public <U> Promise<U> map(Fn1<U, ? super T> mapper) {
            if (value != null) {
                return new PromiseImpl<>(value.map(mapper));
            }

            var result = new PromiseImpl<U>(null);

            push(new CompletionAction<>(value -> result.resolve(value.map(mapper)), result));

            return result;
        }

        @Override
        public Promise<T> mapError(Fn1<Cause, Cause> mapper) {
            if (value != null) {
                return new PromiseImpl<>(value.mapError(mapper));
            }

            var result = new PromiseImpl<T>(null);

            push(new CompletionAction<>(value -> result.resolve(value.mapError(mapper)), result));

            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> Promise<U> flatMap(Fn1<Promise<U>, ? super T> mapper) {
            if (value != null) {
                return value.fold(_ -> new PromiseImpl<>((Result<U>) value), mapper);
            }

            var result = new PromiseImpl<U>(null);

            push(new CompletionAction<>(value -> value.fold(_ -> new PromiseImpl<>((Result<U>) value), mapper)
                                                      .onResult(result::resolve),
                                        result));

            return result;
        }

        @Override
        public <U> Promise<U> fold(Fn1<Promise<U>, Result<T>> mapper) {
            if (value != null) {
                return mapper.apply(value);
            }

            var result = new PromiseImpl<U>(null);

            push(new CompletionAction<>(value -> mapper.apply(value)
                                                       .onResult(result::resolve),
                                        result));
            return result;
        }

        @Override
        public Promise<T> onResult(Consumer<Result<T>> action) {
            if (value != null) {
                action.accept(value);
            } else {
                push(new CompletionAction<>(action, null));
            }

            return this;
        }

        @Override
        public Promise<T> resolve(Result<T> value) {
            if (VALUE.compareAndSet(this, null, value)) {
                runAsync(() -> runActions(value));
            }

            return this;
        }

        @Override
        public boolean isResolved() {
            return value != null;
        }

        public Promise<T> async(Consumer<Promise<T>> action) {
            runAsync(() -> action.accept(this));
            return this;
        }

        public Promise<T> async(Timeout timeout, Consumer<Promise<T>> action) {
            runAsync(timeout, () -> action.accept(this));
            return this;
        }


        @Override
        public Result<T> await() {
            CompletionAction<T> action;

            while ((action = processed) == null) {
                Thread.onSpinWait();
                Thread.yield();
            }

            while (action != NOP) {
                action.dependency.await();
                action = action.next;
            }

            return value;
        }

        @Override
        public Result<T> await(Timeout timeout) {
            return await(timeout.nanoseconds());
        }

        @Override
        public String toString() {
            var string = value == null ? "<>"
                                       : value.toString();
            return STR."Promise(\{string}\{')'}";
        }

        @SuppressWarnings("unchecked")
        private void runActions(Result<T> value) {
            CompletionAction<T> processed = NOP;
            CompletionAction<T> head;

            while ((head = swapHead()) != null) {
                while (head != null) {
                    head.action.accept(value);
                    var current = head;
                    head = head.next;

                    if (current.dependency != null) {
                        current.next = processed;
                        processed = current;
                    }
                }
            }

            this.processed = processed;
        }

        private Result<T> await(long delayNanos) {
            var start = System.nanoTime();

            CompletionAction<T> action;

            while ((action = processed) == null) {
                Thread.onSpinWait();

                if (System.nanoTime() - start > delayNanos) {
                    return new CoreError.Timeout("Result.join(timeout) expired while waiting for completion of current instance").result();
                }
            }

            while (action != NOP) {
                var currentNanoTime = System.nanoTime();

                if (currentNanoTime - start > delayNanos) {
                    return new CoreError.Timeout("Result.join(timeout) expired while waiting for completion of dependencies").result();
                }

                action.dependency.await(currentNanoTime - start);
                action = action.next;
            }

            return value;
        }

        private void push(CompletionAction<T> newHead) {
            CompletionAction<T> oldHead;

            do {
                oldHead = head;
                newHead.next = oldHead;
            } while (!HEAD.compareAndSet(this, oldHead, newHead));
        }

        private CompletionAction<T> swapHead() {
            CompletionAction<T> head;

            do {
                head = this.head;
            } while (!HEAD.compareAndSet(this, head, NOP));

            CompletionAction<T> current = head;
            CompletionAction<T> prev = null;
            CompletionAction<T> next;

            //Reverse list
            while (current != NOP) {
                next = current.next;
                current.next = prev;
                prev = current;
                current = next;
            }

            return prev;
        }
    }
}

enum AsyncExecutor {
    INSTANCE;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void runAsync(Runnable runnable) {
        executor.submit(runnable);
    }
}
