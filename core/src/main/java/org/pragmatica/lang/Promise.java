/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.Tuple.*;
import org.pragmatica.lang.io.CoreError;
import org.pragmatica.lang.io.Timeout;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.utils.ResultCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.pragmatica.lang.Result.unitResult;
import static org.pragmatica.lang.utils.ActionableThreshold.threshold;
import static org.pragmatica.lang.utils.ResultCollector.resultCollector;

/**
 * This is a simple implementation of Promise monad. Promise is a one of the three {@code Core Monads} (along with {@link Option} and {@link Result})
 * which are used to represent special variable states. Promise is a representation of the {@code eventually available value}.
 * <p>
 * This implementation serves three purposes:
 * <ul>
 * <li>- Asynchronous version of {@link Result} monad - {@code Promise} can be resolved to successful and failed states</li>
 * <li>- A basic building block of asynchronous processing.</li>
 * <li>- Resolution event "broker", which implements "exactly once" delivery semantics.</li>
 * </ul>
 * Last two purposes are closely related - they both react on resolution event. But the semantics and requirements to the
 * event handling behavior is quite different.
 * <p>Promise-based asynchronous processing mental model built around representing processing
 * as sequence of transformations which are applied to the same value. Each transformation is applied to input values exactly once, executed
 * with exactly one thread, in the order in which transformations are applied in the code. This mental model is easy to write and reason about
 * and has very low "asynchronous" mental overhead. Since each promise in the chain "depends" on the previous one, actions (transformations)
 * attached to the promise are called "dependent actions".
 * <p>Resolution event broker has no such limitations. It starts actions as asynchronous tasks but does not wait for their completions. Obviously,
 * such actions are called "independent actions". They are harder to reason about because their execution may take arbitrary
 * amount of time.
 */
/* Implementation notes: this version of the implementation is heavily inspired by the implementation of
the CompletableFuture. There are several differences though:
- Method naming consistent with widely used Optional and Streams.
- More orthogonal API. No duplicates with "Async" suffix, for example. Instead, provided "async" method, which accepts promise consumer lambda.
- No methods which do a complex combination of futures. Instead, provided type-safe "all" and "any" predicates for up to 9 promises.
This makes synchronization points in code more explicit, easier to write and reason about.
- No exceptions, no nulls. This makes implementation exceptionally simple comparing to the CompletableFuture.
- Interface-heavy implementation, with only minimal number of methods to implement.
  Only two base methods are used to implement all dependent and independent action methods. All remaining transformations
  and event handling methods are implemented in terms of these two methods.
 */
@SuppressWarnings("unused")
public interface Promise<T> {
    /**
     * Underlying method for all dependent actions. It applies provided action to the result of the promise and returns new promise.
     *
     * @param action Function to be applied to the result of the promise.
     *
     * @return New promise instance.
     */
    <U> Promise<U> fold(Fn1<Promise<U>, Result<T>> action);

    /**
     * Underlying method for all independent actions. It asynchronously runs consumer with the promise result once it is available.
     *
     * @param action Consumer to be executed with the result of the promise.
     *
     * @return Current promise instance.
     */
    Promise<T> onResult(Consumer<Result<T>> action);

    /**
     * Transform the success value of the promise once promise is resolved.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @param transformation Function to be applied to the success value of the promise.
     *
     * @return New promise instance.
     */
    default <U> Promise<U> map(Fn1<U, ? super T> transformation) {
        return replaceResult(result -> result.map(transformation));
    }

    /**
     * Replace the value of the promise with the provided value once promise is resolved into success result.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @param supplier New value supplier.
     *
     * @return New promise instance.
     */
    default <U> Promise<U> map(Supplier<U> supplier) {
        return map(_ -> supplier.get());
    }

    /**
     * Transform the success value of the promise once promise is resolved. The transformation function returns a new promise.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @param transformation Function to be applied to the success value of the promise.
     *
     * @return New promise instance.
     */
    default <U> Promise<U> flatMap(Fn1<Promise<U>, ? super T> transformation) {
        return fold(result -> result.fold(Promise::<U>failure, transformation));
    }

    /**
     * Replace the success value of the promise once promise is resolved with the promise obtained from the provided supplier.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @param supplier Supplier of the new promise.
     *
     * @return New promise instance.
     */
    default <U> Promise<U> flatMap(Supplier<Promise<U>> supplier) {
        return flatMap(_ -> supplier.get());
    }

    /**
     * Transform the failure value of the promise once promise is resolved.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @param transformation Function to be applied to the failure value of the promise.
     *
     * @return New promise instance.
     */
    default Promise<T> mapError(Fn1<Cause, ? super Cause> transformation) {
        return replaceResult(result -> result.mapError(transformation));
    }

    /**
     * Add tracing information to the failure value of the promise once promise is resolved.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @return New promise instance.
     */
    default Promise<T> trace() {
        return mapError(Causes::trace);
    }

    /**
     * Recover from failure by transforming failure cause into new value.
     *
     * @param mapper Function to transform failure cause
     *
     * @return current instance (in case of success) or transformed instance (in case of failure)
     */
    default Promise<T> recover(Fn1<T, ? super Cause> mapper) {
        return replaceResult(result -> result.recover(mapper));
    }

    /**
     * Transform the result of the promise once promise is resolved.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @param transformation Function to be applied to the result of the promise.
     *
     * @return New promise instance.
     */
    default <U> Promise<U> mapResult(Fn1<Result<U>, ? super T> transformation) {
        return replaceResult(result -> result.flatMap(transformation));
    }

    /**
     * Replace the result of the promise with the transformed result once promise is resolved.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @param transformation Function to be applied to the result of the promise.
     *
     * @return New promise instance.
     */
    default <U> Promise<U> replaceResult(Fn1<Result<U>, Result<T>> transformation) {
        return fold(result -> Promise.resolved(transformation.apply(result)));
    }

    /**
     * Run an asynchronous action once promise is resolved.
     * <br>
     * This method is an independent action and executed asynchronously.
     *
     * @param action Action to be executed once promise is resolved.
     *
     * @return Current promise instance.
     */
    default Promise<T> onResultRun(Runnable action) {
        return onResult(_ -> action.run());
    }

    /**
     * Run an action once promise is resolved. The action is executed in the order in which transformations
     *
     * @param consumer Action to be executed once promise is resolved.
     *
     * @return New promise instance.
     */
    default Promise<T> withResult(Consumer<Result<? super T>> consumer) {
        return fold(result -> Promise.resolved(result.onResultRun(() -> consumer.accept(result))));
    }

    /**
     * Run an action once promise is resolved with success. The action is executed asynchronously.
     *
     * @param action Action to be executed once promise is resolved with success.
     *
     * @return Current promise instance.
     */
    default Promise<T> onSuccess(Consumer<T> action) {
        return onResult(result -> result.onSuccess(action));
    }

    /**
     * Run an action once promise is resolved with success. The action is executed asynchronously.
     *
     * @param action Action to be executed once promise is resolved with success.
     *
     * @return Current promise instance.
     */
    default Promise<T> onSuccessRun(Runnable action) {
        return onResult(result -> result.onSuccessRun(action));
    }

    /**
     * Run an action once promise is resolved with success. The action is executed in the order in which transformations are written in the code.
     *
     * @param consumer Action to be executed once promise is resolved with success.
     *
     * @return New promise instance.
     */
    default Promise<T> withSuccess(Consumer<T> consumer) {
        return fold(result -> Promise.resolved(result.onSuccess(consumer)));
    }

    /**
     * Run an action once promise is resolved with success. The action is executed asynchronously.
     *
     * @param action Action to be executed once promise is resolved with success.
     *
     * @return Current promise instance.
     */
    default Promise<T> onFailure(Consumer<Cause> action) {
        return onResult(result -> result.onFailure(action));
    }

    /**
     * Run an action once promise is resolved with success. The action is executed asynchronously.
     *
     * @param action Action to be executed
     */
    default Promise<T> onFailureRun(Runnable action) {
        return onResult(result -> result.onFailureRun(action));
    }

    /**
     * Run an action once promise is resolved with failure. The action is executed in the order in which transformations are written in the code.
     *
     * @param consumer Action to be executed once promise is resolved with failure.
     *
     * @return New promise instance.
     */
    default Promise<T> withFailure(Consumer<Cause> consumer) {
        return fold(result -> Promise.resolved(result.onFailure(consumer)));
    }

    /**
     * Replace current instance with provided promise if current instance is resolved with failure.
     *
     * @param promise Promise to replace current instance with.
     *
     * @return New promise instance.
     */
    default Promise<T> orElse(Promise<T> promise) {
        return fold(result -> result.fold(_ -> promise, _ -> this));
    }

    /**
     * Replace current instance with the promise obtained from provided supplier if current instance is resolved with failure.
     *
     * @param supplier Supplier of the promise to replace current instance with.
     *
     * @return New promise instance.
     */
    default Promise<T> orElse(Supplier<Promise<T>> supplier) {
        return fold(result -> result.fold(_ -> supplier.get(), _ -> this));
    }

    /**
     * Check if the promise is resolved.
     *
     * @return {@code true} if the promise is resolved, {@code false} otherwise.
     */
    boolean isResolved();

    /**
     * Resolve the promise with the provided result.
     *
     * @param value Value to resolve the promise with.
     *
     * @return Current promise instance.
     */
    Promise<T> resolve(Result<T> value);

    /**
     * Resolve the promise to success with the provided value.
     *
     * @param value Value to resolve the promise with.
     *
     * @return Current promise instance.
     */
    @SuppressWarnings("UnusedReturnValue")
    default Promise<T> succeed(T value) {
        return resolve(Result.success(value));
    }

    /**
     * Resolve the promise to failure with the provided cause.
     *
     * @param cause Cause to resolve the promise with.
     *
     * @return Current promise instance.
     */
    default Promise<T> fail(Cause cause) {
        return resolve(Result.failure(cause));
    }

    /**
     * Asynchronously resolve the promise to success with the provided value.
     *
     * @param supplier Supplier of the value to resolve the promise with.
     *
     * @return Current promise instance.
     */
    default Promise<T> succeedAsync(Supplier<? extends T> supplier) {
        return async(promise -> promise.succeed(supplier.get()));
    }

    /**
     * Asynchronously resolve the promise to failure with the provided cause.
     *
     * @param supplier Supplier of the cause to resolve the promise with.
     *
     * @return Current promise instance.
     */
    default Promise<T> failAsync(Supplier<Cause> supplier) {
        return async(promise -> promise.fail(supplier.get()));
    }

    /**
     * Cancel the promise.
     *
     * @return Current promise instance.
     */
    default Promise<T> cancel() {
        return fail(PROMISE_CANCELLED);
    }

    /**
     * Await the resolution of the promise.
     *
     * @return Result of the promise resolution.
     */
    Result<T> await();

    /**
     * Await the resolution of the promise with the provided timeout.
     *
     * @param timeout Timeout to wait for the resolution.
     *
     * @return Result of the promise resolution.
     */
    Result<T> await(Timeout timeout);

    /**
     * Run the provided consumer asynchronously and pass current instance as a parameter.
     *
     * @param consumer Consumer to execute asynchronously.
     *
     * @return Current promise instance.
     */
    default Promise<T> async(Consumer<Promise<T>> consumer) {
        AsyncExecutor.INSTANCE.runAsync(() -> consumer.accept(this));
        return this;
    }

    /**
     * Run the provided consumer asynchronously and pass current instance as a parameter. The consumer is executed after the specified timeout.
     *
     * @param timeout Timeout to wait before executing the consumer.
     * @param action  Consumer to execute asynchronously.
     *
     * @return Current promise instance.
     */
    default Promise<T> async(Timeout timeout, Consumer<Promise<T>> action) {
        AsyncExecutor.INSTANCE.runAsync(timeout, () -> action.accept(this));
        return this;
    }

    /**
     * Transform the promise into a promise resolved to {@link Unit}. This is useful when promise is used in a "event broker" and actual value does
     * not matter.
     * <br>
     * This method is a dependent action and executed in the order in which transformations are written in the code.
     *
     * @return New promise instance.
     */
    default Promise<Unit> mapToUnit() {
        return map(Unit::toUnit);
    }

    /**
     * Create new unresolved promise instance.
     *
     * @return New promise instance.
     */
    static <T> Promise<T> promise() {
        return new PromiseImpl<>(null);
    }

    /**
     * Create new resolved promise instance resolved with the provided value.
     *
     * @param value Value to resolve the promise with.
     *
     * @return New resolved promise instance.
     */
    static <T> Promise<T> resolved(Result<T> value) {
        return new PromiseImpl<>(value);
    }

    /**
     * Create new resolved promise instance resolved into success with the provided value.
     *
     * @param value Value to resolve the promise with.
     *
     * @return New resolved promise instance.
     */
    static <T> Promise<T> success(T value) {
        return new PromiseImpl<>(Result.success(value));
    }

    /**
     * Create new resolved promise instance resolved into success with the provided value.
     *
     * @param value Value to resolve the promise with.
     *
     * @return New resolved promise instance.
     */
    static <T> Promise<T> ok(T value) {
        return success(value);
    }

    /**
     * Create new resolved promise instance resolved into failure with the provided cause.
     *
     * @param cause Cause to resolve the promise with.
     *
     * @return New resolved promise instance.
     */
    static <T> Promise<T> failure(Cause cause) {
        return new PromiseImpl<>(Result.failure(cause));
    }

    /**
     * Create new resolved promise instance resolved into failure with the provided cause.
     *
     * @param cause Cause to resolve the promise with.
     *
     * @return New resolved promise instance.
     */
    static <T> Promise<T> err(Cause cause) {
        return failure(cause);
    }

    /**
     * Create new unresolved promise instance and run the provided consumer asynchronously with the newly created instance.
     *
     * @param consumer Consumer to execute asynchronously with the created instance.
     *
     * @return Created instance.
     */
    static <T> Promise<T> promise(Consumer<Promise<T>> consumer) {
        return Promise.<T>promise().async(consumer);
    }

    /**
     * Create new unresolved promise instance and run the provided consumer asynchronously with the newly created instance after specified timeout.
     *
     * @param consumer Consumer to execute asynchronously with the created instance.
     *
     * @return Created instance.
     */
    static <T> Promise<T> promise(Timeout timeout, Consumer<Promise<T>> consumer) {
        return Promise.<T>promise().async(timeout, consumer);
    }

    /**
     * Use underlying executor to run provided runnable asynchronously.
     *
     * @param runnable Runnable to run asynchronously.
     */
    static void async(Runnable runnable) {
        AsyncExecutor.INSTANCE.runAsync(runnable);
    }

    /**
     * Fail all provided promises with the provided cause.
     *
     * @param cause    Cause to fail the promises with.
     * @param promises Promises to fail.
     */
    @SafeVarargs
    static <T> void failAll(Cause cause, Promise<T>... promises) {
        failAll(cause, List.of(promises));
    }

    /**
     * Fail all provided promises with the provided cause.
     *
     * @param cause    Cause to fail the promises with.
     * @param promises Promises to fail.
     */
    static <T> void failAll(Cause cause, List<Promise<T>> promises) {
        promises.forEach(promise -> promise.fail(cause));
    }

    /**
     * Instance of the {@link Promise} resolved into success with {@link Unit}.
     *
     * @return The singleton instance of the {@link Promise} resolved into success with {@link Unit}.
     */
    @SuppressWarnings("unchecked")
    static <U> Promise<U> unitPromise() {
        return (Promise<U>) UNIT;
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
                             .forEach(promise -> promise.onResult(result -> result.onSuccess(anySuccess::succeed)
                                                                                  .onSuccessRun(() -> cancelAll(promises)))
                                                        .onResultRun(at::registerEvent))));
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
    static <T> Promise<T> anySuccess(Result<T> failureResult, List<Promise<T>> promises) {
        return Promise.promise(anySuccess -> threshold(promises.size(), () -> anySuccess.resolve(failureResult))
            .apply(at -> promises.forEach(promise -> promise.onResult(result -> result.onSuccess(anySuccess::succeed)
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
    @SuppressWarnings("unchecked")
    @SafeVarargs
    static <T> Promise<T> anySuccess(Promise<T>... promises) {
        return anySuccess((Result<T>) OTHER_SUCCEEDED, promises);
    }

    /**
     * Return promise which will be resolved once any of the promises provided as a parameters will be resolved with success. If none of the promises
     * will be resolved with success, then created instance will be resolved with {@link CoreError.Cancelled}.
     *
     * @param promises Input promises
     *
     * @return Created instance
     */
    @SuppressWarnings("unchecked")
    static <T> Promise<T> anySuccess(List<Promise<T>> promises) {
        return anySuccess((Result<T>) OTHER_SUCCEEDED, promises);
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
        failAll(PROMISE_CANCELLED, promises);
    }

    /**
     * Return a promise which will be resolved with the list of results of execution of all passed promises.
     *
     * @param promises Collection of promises to be resolved.
     *
     * @return Promise instance, which will be resolved with the list of results of resolved promises.
     */
    @SuppressWarnings("unchecked")
    static <T> Promise<List<Result<T>>> allOf(Collection<Promise<T>> promises) {
        if (promises.isEmpty()) {
            return Promise.success(List.of());
        }

        var array = promises.toArray(new Promise[0]);
        var promise = Promise.promise();
        var collector = ResultCollector.resultCollector(promises.size(),
                                                        values -> promise.succeed(List.of(values)));
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
        var causes = Causes.composite();

        return () -> promise1.map(Tuple::tuple)
                             .mapError(causes::append);
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
        return () -> setupResult(values -> Result.all((Result<T1>) values[0], (Result<T2>) values[1]).id(), promise1, promise2);
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

        return () -> setupResult(values -> Result.all((Result<T1>) values[0], (Result<T2>) values[1], (Result<T3>) values[2]).id(),
                                 promise1, promise2, promise3);
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

        return () -> setupResult(values -> Result.all(
                                     (Result<T1>) values[0], (Result<T2>) values[1],
                                     (Result<T3>) values[2], (Result<T4>) values[3]).id(),
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

        return () -> setupResult(values -> Result.all(
                                     (Result<T1>) values[0], (Result<T2>) values[1], (Result<T3>) values[2],
                                     (Result<T4>) values[3], (Result<T5>) values[4]).id(),
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

        return () -> setupResult(values -> Result.all(
                                     (Result<T1>) values[0], (Result<T2>) values[1], (Result<T3>) values[2],
                                     (Result<T4>) values[3], (Result<T5>) values[4], (Result<T6>) values[5]).id(),
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

        return () -> setupResult(values -> Result.all(
                                     (Result<T1>) values[0], (Result<T2>) values[1], (Result<T3>) values[2],
                                     (Result<T4>) values[3], (Result<T5>) values[4], (Result<T6>) values[5], (Result<T7>) values[6]).id(),
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

        return () -> setupResult(values -> Result.all(
                                     (Result<T1>) values[0], (Result<T2>) values[1], (Result<T3>) values[2], (Result<T4>) values[3],
                                     (Result<T5>) values[4], (Result<T6>) values[5], (Result<T7>) values[6], (Result<T8>) values[7]).id(),
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

        return () -> setupResult(values -> Result.all(
                                     (Result<T1>) values[0], (Result<T2>) values[1], (Result<T3>) values[2], (Result<T4>) values[3], (Result<T5>) values[4],
                                     (Result<T6>) values[5], (Result<T7>) values[6], (Result<T8>) values[7], (Result<T9>) values[8]).id(),
                                 promise1, promise2, promise3, promise4, promise5, promise6, promise7, promise8, promise9);
    }

    Promise<?> UNIT = Promise.resolved(unitResult());

    Result<?> OTHER_SUCCEEDED = Result.failure(new CoreError.Cancelled("Cancelled because other Promise instance succeeded"));

    CoreError.Cancelled PROMISE_CANCELLED = new CoreError.Cancelled("Promise cancelled");

    /**
     * Helper interface for convenient tuple transformation.
     *
     * @see Result.Mapper1
     */
    interface Mapper1<T1> {
        Promise<Tuple1<T1>> id();

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
        Promise<Tuple2<T1, T2>> id();

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
        Promise<Tuple3<T1, T2, T3>> id();

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
        Promise<Tuple4<T1, T2, T3, T4>> id();

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
        Promise<Tuple5<T1, T2, T3, T4, T5>> id();

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
        Promise<Tuple6<T1, T2, T3, T4, T5, T6>> id();

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
        Promise<Tuple7<T1, T2, T3, T4, T5, T6, T7>> id();

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
        Promise<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> id();

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
        Promise<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> id();

        default <R> Promise<R> map(Fn9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }
    }

    private static <R> Promise<R> setupResult(FnX<Result<R>> transformer, Promise<?>... promises) {
        var promise = Promise.<R>promise();
        var collector = resultCollector(promises.length, values -> promise.resolve(transformer.apply(values)));

        int count = 0;
        for (var p : promises) {
            final var index = count++;
            p.onResult(result -> collector.registerEvent(index, result));
        }

        return promise;
    }
}

enum AsyncExecutor {
    INSTANCE;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    void runAsync(Runnable runnable) {
        executor.submit(runnable);
    }

    void runAsync(Timeout timeout, Runnable runnable) {
        runAsync(() -> {
            try {
                Thread.sleep(timeout.duration());
            } catch (InterruptedException e) {
                // ignore
            }
            runnable.run();
        });
    }
}

final class PromiseImpl<T> implements Promise<T> {
    private static final Logger log = LoggerFactory.getLogger(Promise.class);

    volatile Result<T> result;
    volatile Completion<T> stack; // Rely on default initialization to null

    PromiseImpl(Result<T> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return result == null ? "Promise<>"
                              : "Promise<" + result + '>';
    }

    @Override
    public boolean isResolved() {
        return result != null;
    }

    @Override
    public Promise<T> onResult(Consumer<Result<T>> action) {
        if (result != null) {
            action.accept(result);
            return this;
        } else {
            push(new CompletionOnResult<>(action));
        }

        return this;
    }

    @Override
    public <U> Promise<U> fold(Fn1<Promise<U>, Result<T>> action) {
        if (result != null) {
            return action.apply(result);
        } else {
            return chain(action);
        }
    }

    @Override
    public Result<T> await() {
        if (result != null) {
            return result;
        }

        var thread = Thread.currentThread();

        if (log.isTraceEnabled()) {
            var stackTraceElement = thread.getStackTrace()[2];

            log.trace("Thread {} ({}) is waiting for resolution of Promise {} at {}:{}",
                      thread.threadId(), thread.getName(), this,
                      stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
        }

        push(new CompletionJoin<>(thread));

        while (result == null) {
            LockSupport.park();
        }

        return result;
    }

    /**
     * Await for resolution of current instance with specified timeout. Note that if timeout is expired, then current instance remains unresolved.
     *
     * @param timeout Timeout to wait for resolution
     *
     * @return If instance is resolved while waiting, then the result of resolution is returned. Otherwise, Timeout error is returned.
     */
    @Override
    public Result<T> await(Timeout timeout) {
        if (result != null) {
            return result;
        }

        var thread = Thread.currentThread();

        if (log.isTraceEnabled()) {
            var stackTraceElement = thread.getStackTrace()[2];

            log.trace("Thread {} ({}) is waiting for resolution of Promise {} at {}:{} for {}ns",
                      thread.threadId(), thread.getName(), this,
                      stackTraceElement.getFileName(), stackTraceElement.getLineNumber(),
                      timeout.nanoseconds());
        }

        push(new CompletionJoin<>(thread));

        var deadline = System.nanoTime() + timeout.nanoseconds();

        while (result == null && System.nanoTime() < deadline) {
            LockSupport.parkNanos(deadline - System.nanoTime());
        }

        if (result == null) {
            return new CoreError.Timeout("Promise is not resolved within specified timeout").result();
        }

        return result;
    }

    @Override
    public Promise<T> resolve(Result<T> value) {
        if (RESULT.compareAndSet(this, null, value)) {
            do {
                processActions();
            } while (this.stack != null);
        }
        return this;
    }

    @SuppressWarnings("rawtypes")
    private void processActions() {
        Completion head;

        do {
            head = this.stack;
        } while (!STACK.compareAndSet(this, head, null));

        // Split all completions into three lists - joins, regular completions (dependent transformations), and
        // event handlers.
        // Regular completions are executed immediately, event processors executed asynchronously.
        // Joins are executed after all regular completions are done.
        CompletionJoin joins = null;
        CompletionOnResult events = null;
        CompletionFold actions = null;

        Completion current = head;
        Completion tmp;

        // Split and reverse list in one pass
        while (current != null) {
            tmp = current.next;

            switch ((CompletionMarker) current) {
                case CompletionJoin join -> {
                    join.next = joins;
                    joins = join;
                }

                case CompletionOnResult event -> {
                    event.next = events;
                    events = event;
                }

                case CompletionFold action -> {
                    action.next = actions;
                    actions = action;
                }
            }
            current = tmp;
        }

        runEventHandlers(events);
        runSequentialActions(actions);
        runJoins(joins);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void runJoins(Completion current) {
        while (current != null) {
            current.complete(result);
            current = current.next;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void runSequentialActions(Completion current) {
        while (current != null) {
            current.complete(result);
            current = current.next;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void runEventHandlers(Completion asyncEvents) {
        AsyncExecutor.INSTANCE.runAsync(() -> {
            var current = asyncEvents;

            while (current != null) {
                current.complete(result);
                current = current.next;
            }
        });
    }

    private <U> Promise<U> chain(Fn1<Promise<U>, Result<T>> transformer) {
        var dependency = new PromiseImpl<U>(null);

        push(new CompletionFold<>(dependency, transformer));

        return dependency;
    }

    private void push(Completion<T> completion) {
        Completion<T> prevStack;
        do {
            if (result != null) {
                // In rare circumstances, when one thread is resolved instance while other tries to
                // add new independent completion, we resolve completion here. There might be chances, that this might
                // lead to race condition if actions done by completions attached to this Promise have shared data.
                // Otherwise, dependency chain is still maintained properly even with this invocation.
                completion.complete(result);
                return;
            }
            prevStack = stack;
            completion.next = prevStack;
        } while (!STACK.compareAndSet(this, prevStack, completion));
    }

    sealed interface CompletionMarker permits CompletionOnResult, CompletionFold, CompletionJoin {}

    abstract static class Completion<T> {
        volatile Completion<T> next;

        abstract public void complete(Result<T> value);
    }

    final static class CompletionFold<U, T> extends Completion<T> implements CompletionMarker {
        private final Promise<U> dependency;
        private final Fn1<Promise<U>, Result<T>> transformer;

        CompletionFold(Promise<U> dependency, Fn1<Promise<U>, Result<T>> transformer) {
            this.dependency = dependency;
            this.transformer = transformer;
        }

        @Override
        public void complete(Result<T> value) {
            transformer.apply(value)
                       .onResult(dependency::resolve);
        }
    }

    final static class CompletionOnResult<T> extends Completion<T> implements CompletionMarker {
        private final Consumer<Result<T>> consumer;

        CompletionOnResult(Consumer<Result<T>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void complete(Result<T> value) {
            consumer.accept(value);
        }
    }

    final static class CompletionJoin<T> extends Completion<T> implements CompletionMarker {
        private final Thread thread;

        CompletionJoin(Thread thread) {
            this.thread = thread;
        }

        @Override
        public void complete(Result<T> value) {
            if (log.isTraceEnabled()) {
                var stackTraceElement = thread.getStackTrace()[2];

                log.trace("Unblocking thread {} ({}) after resolution of Promise with {} at {}:{}",
                          thread.threadId(), thread.getName(), value,
                          stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
            }

            LockSupport.unpark(thread);
        }
    }

    private static final VarHandle RESULT;
    private static final VarHandle STACK;

    static {
        try {
            var lookup = MethodHandles.lookup();
            RESULT = lookup.findVarHandle(PromiseImpl.class, "result", Result.class);
            STACK = lookup.findVarHandle(PromiseImpl.class, "stack", PromiseImpl.Completion.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.org/browse/JDK-8074773
        @SuppressWarnings("unused")
        Class<?> ensureLoaded = LockSupport.class;
    }
}