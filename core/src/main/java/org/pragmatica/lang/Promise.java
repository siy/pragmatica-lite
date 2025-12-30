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
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.utils.ResultCollector;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.Result.unitResult;
import static org.pragmatica.lang.utils.ActionableThreshold.threshold;
import static org.pragmatica.lang.utils.ResultCollector.resultCollector;

/// This is a simple implementation of Promise monad. Promise is one of the three `Core Monads` (along with [Option] and [Result])
/// which are used to represent special variable states. Promise is a representation of the `eventually available value`.
///
/// This implementation serves three purposes:
///
///   - - Asynchronous version of [Result] monad - `Promise` can be resolved to successful and failed states
///   - - A basic building block of asynchronous processing.
///   - - Resolution event "broker", which implements "exactly once" delivery semantics.
///
/// The last two purposes are closely related - they both react on resolution event.
/// But the semantics and requirements to the event handling behavior are quite different.
///
/// Promise-based asynchronous processing mental model built around representing processing
/// as the sequence of transformations which are applied to the same value. Each transformation is applied to input values exactly once, executed
/// with exactly one thread, in the order in which transformations are applied in the code. This mental model is straightforward to write and reason about
/// and has very low "asynchronous" mental overhead. Since each promise in the chain "depends" on the previous one, actions (transformations)
/// attached to the promise are called "dependent actions".
///
/// Resolution event broker has no such limitations. It starts actions as asynchronous tasks but does not wait for their completion.
/// Such actions are called "independent actions". They are harder to reason about because their execution may take an arbitrary
/// amount of time.
/* Implementation notes: this version of the implementation is heavily inspired by the implementation of
the CompletableFuture. There are several differences, though:
- Method naming consistent with widely used Optional and Streams.
- More orthogonal API. No duplicates with "Async" suffix, for example. Instead, provided "async" method, which accepts promise consumer lambda.
- No methods that do a complex combination of futures. Instead, provided type-safe "all" and "any" predicates for up to 9 promises.
This makes synchronization points in code more explicit, easier to write and reason about.
- No exceptions, no nulls. This makes implementation exceptionally simple compared to the CompletableFuture.
- Interface-heavy implementation, with only a minimal number of methods to implement.
  Only two base methods are used to implement all dependent and independent action methods. All remaining transformations
  and event handling methods are implemented in terms of these two methods.
 */
@SuppressWarnings("unused")
public interface Promise<T> {
    /// Underlying method for all dependent actions. It applies provided action to the result of the promise and returns new promise.
    ///
    /// @param action Function to be applied to the result of the promise.
    ///
    /// @return New promise instance.
    <U> Promise<U> fold(Fn1<Promise<U>, Result<T>> action);

    /// Underlying method for all independent actions. It asynchronously runs consumer with the promise result once it is available.
    ///
    /// @param action Consumer to be executed with the result of the promise.
    ///
    /// @return Current promise instance.
    Promise<T> onResult(Consumer<Result<T>> action);

    /// Transform the success value of the promise once the promise is resolved.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @param transformation Function to be applied to the success value of the promise.
    ///
    /// @return New promise instance.
    default <U> Promise<U> map(Fn1<U, ? super T> transformation) {
        return replaceResult(result -> result.map(transformation));
    }

    /// Replace the value of the promise with the provided value once the promise is resolved into a success result.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @param supplier New value supplier.
    ///
    /// @return New promise instance.
    default <U> Promise<U> map(Supplier<U> supplier) {
        return map(_ -> supplier.get());
    }

    /// Transform the success value of the promise once the promise is resolved. The transformation function returns a new promise.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @param transformation Function to be applied to the success value of the promise.
    ///
    /// @return New promise instance.
    default <U> Promise<U> flatMap(Fn1<Promise<U>, ? super T> transformation) {
        return fold(result -> result.fold(Promise:: <U>failure, transformation));
    }

    /// Version of the [#flatMap(Fn1)] which allows convenient "mixing in" additional parameter without the need to revert
    /// to traditional lambda.
    default <U, I> Promise<U> flatMap2(Fn2<Promise<U>, ? super T, ? super I> mapper, I parameter2) {
        return flatMap(value -> mapper.apply(value, parameter2));
    }

    /// Replace the success value of the promise once the promise is resolved with the promise obtained from the provided supplier.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @param supplier Supplier of the new promise.
    ///
    /// @return New promise instance.
    default <U> Promise<U> flatMap(Supplier<Promise<U>> supplier) {
        return flatMap(_ -> supplier.get());
    }

    /// Transform the failure value of the promise once the promise is resolved.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @param transformation Function to be applied to the failure value of the promise.
    ///
    /// @return New promise instance.
    default Promise<T> mapError(Fn1<Cause, ? super Cause> transformation) {
        return replaceResult(result -> result.mapError(transformation));
    }

    /// Add tracing information to the failure value of the promise once the promise is resolved.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @return New promise instance.
    default Promise<T> trace() {
        var text = Thread.currentThread()
                         .getStackTrace() [2]
                         .toString();
        return mapError(cause -> Causes.CompositeCause.toComposite(text, cause));
    }

    /// Recover from failure by transforming failure cause into new value.
    ///
    /// @param mapper Function to transform failure cause
    ///
    /// @return current instance (in case of success) or transformed instance (in case of failure)
    default Promise<T> recover(Fn1<T, ? super Cause> mapper) {
        return replaceResult(result -> result.recover(mapper));
    }

    /// Transform the result of the promise once the promise is resolved.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @param transformation Function to be applied to the result of the promise.
    ///
    /// @return New promise instance.
    default <U> Promise<U> mapResult(Fn1<Result<U>, ? super T> transformation) {
        return replaceResult(result -> result.flatMap(transformation));
    }

    /// Replace the result of the promise with the transformed result once the promise is resolved.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @param transformation Function to be applied to the result of the promise.
    ///
    /// @return New promise instance.
    default <U> Promise<U> replaceResult(Fn1<Result<U>, Result<T>> transformation) {
        return fold(result -> Promise.resolved(transformation.apply(result)));
    }

    /// Run an asynchronous action once the promise is resolved.
    ///
    /// This method is an independent action and executed asynchronously.
    ///
    /// @param action Action to be executed once a promise is resolved.
    ///
    /// @return Current promise instance.
    default Promise<T> onResultAsync(Consumer<Result<T>> action) {
        return onResult(action);
    }

    /// Run the provided action once the promise is resolved, regardless of success or failure.
    ///
    /// @param action Action to execute when promise resolves
    ///
    /// @return current promise instance
    default Promise<T> onResultRun(Runnable action) {
        return onResult(_ -> action.run());
    }

    /// Run the provided action asynchronously once the promise is resolved, regardless of success or failure.
    ///
    /// @param action Action to execute when promise resolves
    ///
    /// @return current promise instance
    default Promise<T> onResultRunAsync(Runnable action) {
        return onResult(_ -> action.run());
    }

    /// Run an action once the promise is resolved. The action is executed in the order in which transformations
    ///
    /// @param consumer Action to be executed once the promise is resolved.
    ///
    /// @return New promise instance.
    default Promise<T> withResult(Consumer<Result<T>> consumer) {
        return fold(result -> {
            result.onResult(consumer);
            return this;
        });
    }

    /// Run an action once the promise is resolved with success. The action is executed asynchronously.
    ///
    /// @param action Action to be executed once the promise is resolved with success.
    ///
    /// @return Current promise instance.
    default Promise<T> onSuccess(Consumer<T> action) {
        return onResult(result -> result.onSuccess(action));
    }

    /// Run an action asynchronously once the promise is resolved with success. This is an alias for [#onSuccess(Consumer)].
    ///
    /// @param action Action to be executed once the promise is resolved with success
    ///
    /// @return current promise instance
    default Promise<T> onSuccessAsync(Consumer<T> action) {
        return onSuccess(action);
    }

    /// Run an action once the promise is resolved with success. The action is executed asynchronously.
    ///
    /// @param action Action to be executed once the promise is resolved with success.
    ///
    /// @return Current promise instance.
    default Promise<T> onSuccessRun(Runnable action) {
        return onResult(result -> result.onSuccessRun(action));
    }

    /// Run an action asynchronously once the promise is resolved with success. This is an alias for [#onSuccessRun(Runnable)].
    ///
    /// @param action Action to be executed once the promise is resolved with success
    ///
    /// @return current promise instance
    default Promise<T> onSuccessRunAsync(Runnable action) {
        return onSuccessRun(action);
    }

    /// Run an action once the promise is resolved with success. The action is executed in the order in which transformations are written in the code.
    ///
    /// @param consumer Action to be executed once the promise is resolved with success.
    ///
    /// @return New promise instance.
    default Promise<T> withSuccess(Consumer<T> consumer) {
        return fold(result -> Promise.resolved(result.onSuccess(consumer)));
    }

    /// Run an action once the promise is resolved with success. The action is executed asynchronously.
    ///
    /// @param action Action to be executed once the promise is resolved with success.
    ///
    /// @return Current promise instance.
    default Promise<T> onFailure(Consumer<Cause> action) {
        return onResult(result -> result.onFailure(action));
    }

    /// Run an action asynchronously once the promise is resolved with failure. This is an alias for [#onFailure(Consumer)].
    ///
    /// @param action Action to be executed once the promise is resolved with failure
    ///
    /// @return current promise instance
    default Promise<T> onFailureAsync(Consumer<Cause> action) {
        return onFailure(action);
    }

    /// Run an action once the promise is resolved with success. The action is executed asynchronously.
    ///
    /// @param action Action to be executed
    default Promise<T> onFailureRun(Runnable action) {
        return onResult(result -> result.onFailureRun(action));
    }

    /// Run an action asynchronously once the promise is resolved with failure. This is an alias for [#onFailureRun(Runnable)].
    ///
    /// @param action Action to be executed once the promise is resolved with failure
    ///
    /// @return current promise instance
    default Promise<T> onFailureRunAsync(Runnable action) {
        return onFailureRun(action);
    }

    /// Filter instance against provided predicate. If the predicate returns `true`, then the instance remains unchanged. If the predicate returns
    /// `false`, then a failure instance is created using given [Cause].
    ///
    /// @param cause     failure to use in case if predicate returns `false`
    /// @param predicate predicate to invoke
    ///
    /// @return current instance if predicate returns `true` or failure instance if predicate returns `false`
    default Promise<T> filter(Cause cause, Predicate<T> predicate) {
        return fold(result -> result.filter(cause, predicate)
                                    .async());
    }

    /// Asynchronous version of the filtering
    default Promise<T> filter(Cause cause, Promise<Boolean> predicate) {
        return filter(_ -> cause, predicate);
    }

    /// Filter instance against provided predicate. If the predicate returns `true`, then the instance remains unchanged. If the predicate returns
    /// `false`, then a failure instance is created using [Cause] created by the provided function.
    ///
    /// @param causeMapper function which transforms the tested value into an instance of [Cause] if predicate returns `false`
    /// @param predicate   predicate to invoke
    ///
    /// @return current instance if predicate returns `true` or failure instance if predicate returns `false`
    default Promise<T> filter(Fn1<Cause, T> causeMapper, Predicate<T> predicate) {
        return fold(result -> result.filter(causeMapper, predicate)
                                    .async());
    }

    default Promise<T> filter(Fn1<Cause, T> causeMapper, Promise<Boolean> predicate) {
        return fold(result -> result.fold(Promise::failure,
                                          value -> predicate.flatMap(decision -> decision
                                                                                 ? Promise.this
                                                                                 : causeMapper.apply(value)
                                                                                              .promise())));
    }

    /// Run an action once the promise is resolved with failure. The action is executed in the order in which transformations are written in the code.
    ///
    /// @param consumer Action to be executed once the promise is resolved with failure.
    ///
    /// @return New promise instance.
    default Promise<T> withFailure(Consumer<Cause> consumer) {
        return fold(result -> Promise.resolved(result.onFailure(consumer)));
    }

    /// Replace current instance with provided promise if current instance is resolved with failure.
    ///
    /// @param promise Promise to replace the current instance with.
    ///
    /// @return New promise instance.
    default Promise<T> orElse(Promise<T> promise) {
        return fold(result -> result.fold(_ -> promise, _ -> this));
    }

    /// Replace the current instance with the promise obtained from provided supplier if current instance is resolved with failure.
    ///
    /// @param supplier Supplier of the promise to replace the current instance with.
    ///
    /// @return New promise instance.
    default Promise<T> orElse(Supplier<Promise<T>> supplier) {
        return fold(result -> result.fold(_ -> supplier.get(), _ -> this));
    }

    /// Check if the promise is resolved.
    ///
    /// @return `true` if the promise is resolved, `false` otherwise.
    boolean isResolved();

    /// Resolve the promise with the provided result.
    ///
    /// @param value Value to resolve the promise with.
    ///
    /// @return Current promise instance.
    Promise<T> resolve(Result<T> value);

    /// Resolve the promise to success with the provided value.
    ///
    /// @param value Value to resolve the promise with.
    ///
    /// @return Current promise instance.
    @SuppressWarnings("UnusedReturnValue")
    default Promise<T> succeed(T value) {
        return resolve(Result.success(value));
    }

    /// Resolve the promise to failure with the provided cause.
    ///
    /// @param cause Cause to resolve the promise with.
    ///
    /// @return Current promise instance.
    default Promise<T> fail(Cause cause) {
        return resolve(Result.failure(cause));
    }

    /// Asynchronously resolve the promise to success with the provided value.
    ///
    /// @param supplier Supplier of the value to resolve the promise with.
    ///
    /// @return Current promise instance.
    default Promise<T> succeedAsync(Supplier< ? extends T> supplier) {
        return async(promise -> promise.succeed(supplier.get()));
    }

    /// Asynchronously resolve the promise to failure with the provided cause.
    ///
    /// @param supplier Supplier of the cause to resolve the promise with.
    ///
    /// @return Current promise instance.
    default Promise<T> failAsync(Supplier<Cause> supplier) {
        return async(promise -> promise.fail(supplier.get()));
    }

    /// Set timeout for the promise. If promise will remain unresolved after timeout, it will
    /// be forcefully resolved with the [CoreError.Timeout] failure.
    ///
    /// **WARNING!!!**
    /// Timeouts should be inserted as close to actual operations as possible.
    /// Otherwise, they don't cancel the operation itself, but subsequent transformations.
    /// This may result in incorrect handling of subsequent operations as they will
    /// be executed only when the original operation is completed.
    /// For example:
    ///
    /// Incorrect:
    /// ```java
    ///     return lowLevelCall()
    ///             .map(this::transformationStep1)     // <-- This transformation will wait for lowLevelCall() to resolve
    ///             .flatMap(this::transformationStep3) // <-- This transformation will see timeout error and will be skipped
    ///             .timeout(timeSpan(10).seconds());
    /// ```
    /// If after timeout `lowLevelCall()` succeeds, the `transformationStep1()` will be executed. If it has side effects,
    /// this may result in a subtle, hard to nail down bug. To avoid this, place `timeout()` call as early
    /// as possible in the transformation chain.
    ///
    /// Correct:
    /// ```java
    ///     return lowLevelCall()
    ///             .timeout(timeSpan(10).seconds())    // <-- The promise returned by lowLevelCall() will be cancelled by timeout
    ///             .map(this::transformationStep1)     // <-- This transformation will see Timeout error and will be skipped
    ///             .flatMap(this::transformationStep3);// <-- This transformation will see Timeout error and will be skipped
    /// ```
    default Promise<T> timeout(TimeSpan timeout) {
        return async(timeout,
                     promise -> promise.fail(new CoreError.Timeout("Promise timed out after " + timeout.millis() + "ms")));
    }

    /// Cancel the promise.
    ///
    /// @return Current promise instance.
    default Promise<T> cancel() {
        return fail(PROMISE_CANCELLED);
    }

    /// Await the resolution of the promise.
    ///
    /// @return Result of the promise resolution.
    Result<T> await();

    /// Await the resolution of the promise with the provided timeout.
    ///
    /// @param timeout Timeout to wait for the resolution.
    ///
    /// @return Result of the promise resolution.
    Result<T> await(TimeSpan timeout);

    /// This method is necessary to make [Result] and [Promise] API consistent.
    ///
    /// @return current instance
    default Promise<T> async() {
        return this;
    }

    /// Run the provided consumer asynchronously and pass the current instance as a parameter.
    ///
    /// @param consumer Consumer to execute asynchronously.
    ///
    /// @return Current promise instance.
    default Promise<T> async(Consumer<Promise<T>> consumer) {
        AsyncExecutor.INSTANCE.runAsync(() -> consumer.accept(this));
        return this;
    }

    /// Executes the given supplier asynchronously and returns a promise that resolves when the supplier's result is obtained.
    ///
    /// @param supplier a supplier function that provides a result of type Result<T> to be resolved asynchronously
    ///
    /// @return a Promise of type T that resolves with the result of the supplier execution
    default Promise<T> async(Supplier<Result<T>> supplier) {
        return async(promise -> promise.resolve(supplier.get()));
    }

    /// Run the provided consumer asynchronously and pass the current instance as a parameter. The consumer is executed after the specified timeout.
    ///
    /// @param delay  Time to wait before executing the consumer.
    /// @param action Consumer to execute asynchronously.
    ///
    /// @return Current promise instance.
    default Promise<T> async(TimeSpan delay, Consumer<Promise<T>> action) {
        AsyncExecutor.INSTANCE.runAsync(delay, () -> action.accept(this));
        return this;
    }

    /// Transform the promise into a promise resolved to [Unit]. This is useful when the promise is used in an "event broker" and the actual value does
    /// not matter.
    ///
    /// This method is a dependent action and executed in the order in which transformations are written in the code.
    ///
    /// @return New promise instance.
    default Promise<Unit> mapToUnit() {
        return map(Unit::toUnit);
    }

    //------------------------------------------------------------------------------------------------------------------
    // Instance all() methods - for-comprehension style composition
    //------------------------------------------------------------------------------------------------------------------
    /// Chain a dependent operation with access to this Promise's value.
    /// Enables for-comprehension style composition without nested flatMaps.
    ///
    /// @param fn1 Function that takes the current value and returns a Promise
    /// @param <T1> Type of the result from fn1
    ///
    /// @return Mapper1 for further transformation
    default <T1> Mapper1<T1> all(Fn1<Promise<T1>, T> fn1) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .map(Tuple::tuple));
    }

    /// Chain two dependent operations with access to this Promise's value.
    ///
    /// @param fn1 First function that takes the current value and returns a Promise
    /// @param fn2 Second function that takes the current value and returns a Promise
    /// @param <T1> Type of the result from fn1
    /// @param <T2> Type of the result from fn2
    ///
    /// @return Mapper2 for further transformation
    default <T1, T2> Mapper2<T1, T2> all(Fn1<Promise<T1>, T> fn1,
                                         Fn1<Promise<T2>, T> fn2) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .map(v2 -> Tuple.tuple(v1, v2))));
    }

    /// Chain three dependent operations with access to this Promise's value.
    ///
    /// @param fn1 First function that takes the current value and returns a Promise
    /// @param fn2 Second function that takes the current value and returns a Promise
    /// @param fn3 Third function that takes the current value and returns a Promise
    /// @param <T1> Type of the result from fn1
    /// @param <T2> Type of the result from fn2
    /// @param <T3> Type of the result from fn3
    ///
    /// @return Mapper3 for further transformation
    default <T1, T2, T3> Mapper3<T1, T2, T3> all(Fn1<Promise<T1>, T> fn1,
                                                 Fn1<Promise<T2>, T> fn2,
                                                 Fn1<Promise<T3>, T> fn3) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .map(v3 -> Tuple.tuple(v1, v2, v3)))));
    }

    /// Chain four dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4> Mapper4<T1, T2, T3, T4> all(Fn1<Promise<T1>, T> fn1,
                                                         Fn1<Promise<T2>, T> fn2,
                                                         Fn1<Promise<T3>, T> fn3,
                                                         Fn1<Promise<T4>, T> fn4) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .map(v4 -> Tuple.tuple(v1,
                                                                                                                  v2,
                                                                                                                  v3,
                                                                                                                  v4))))));
    }

    /// Chain five dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5> Mapper5<T1, T2, T3, T4, T5> all(Fn1<Promise<T1>, T> fn1,
                                                                 Fn1<Promise<T2>, T> fn2,
                                                                 Fn1<Promise<T3>, T> fn3,
                                                                 Fn1<Promise<T4>, T> fn4,
                                                                 Fn1<Promise<T5>, T> fn5) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .map(v5 -> Tuple.tuple(v1,
                                                                                                                                    v2,
                                                                                                                                    v3,
                                                                                                                                    v4,
                                                                                                                                    v5)))))));
    }

    /// Chain six dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6> Mapper6<T1, T2, T3, T4, T5, T6> all(Fn1<Promise<T1>, T> fn1,
                                                                         Fn1<Promise<T2>, T> fn2,
                                                                         Fn1<Promise<T3>, T> fn3,
                                                                         Fn1<Promise<T4>, T> fn4,
                                                                         Fn1<Promise<T5>, T> fn5,
                                                                         Fn1<Promise<T6>, T> fn6) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .map(v6 -> Tuple.tuple(v1,
                                                                                                                                                      v2,
                                                                                                                                                      v3,
                                                                                                                                                      v4,
                                                                                                                                                      v5,
                                                                                                                                                      v6))))))));
    }

    /// Chain seven dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7> Mapper7<T1, T2, T3, T4, T5, T6, T7> all(Fn1<Promise<T1>, T> fn1,
                                                                                 Fn1<Promise<T2>, T> fn2,
                                                                                 Fn1<Promise<T3>, T> fn3,
                                                                                 Fn1<Promise<T4>, T> fn4,
                                                                                 Fn1<Promise<T5>, T> fn5,
                                                                                 Fn1<Promise<T6>, T> fn6,
                                                                                 Fn1<Promise<T7>, T> fn7) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .map(v7 -> Tuple.tuple(v1,
                                                                                                                                                                        v2,
                                                                                                                                                                        v3,
                                                                                                                                                                        v4,
                                                                                                                                                                        v5,
                                                                                                                                                                        v6,
                                                                                                                                                                        v7)))))))));
    }

    /// Chain eight dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8> Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> all(Fn1<Promise<T1>, T> fn1,
                                                                                         Fn1<Promise<T2>, T> fn2,
                                                                                         Fn1<Promise<T3>, T> fn3,
                                                                                         Fn1<Promise<T4>, T> fn4,
                                                                                         Fn1<Promise<T5>, T> fn5,
                                                                                         Fn1<Promise<T6>, T> fn6,
                                                                                         Fn1<Promise<T7>, T> fn7,
                                                                                         Fn1<Promise<T8>, T> fn8) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .map(v8 -> Tuple.tuple(v1,
                                                                                                                                                                                          v2,
                                                                                                                                                                                          v3,
                                                                                                                                                                                          v4,
                                                                                                                                                                                          v5,
                                                                                                                                                                                          v6,
                                                                                                                                                                                          v7,
                                                                                                                                                                                          v8))))))))));
    }

    /// Chain nine dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8, T9> Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> all(Fn1<Promise<T1>, T> fn1,
                                                                                                 Fn1<Promise<T2>, T> fn2,
                                                                                                 Fn1<Promise<T3>, T> fn3,
                                                                                                 Fn1<Promise<T4>, T> fn4,
                                                                                                 Fn1<Promise<T5>, T> fn5,
                                                                                                 Fn1<Promise<T6>, T> fn6,
                                                                                                 Fn1<Promise<T7>, T> fn7,
                                                                                                 Fn1<Promise<T8>, T> fn8,
                                                                                                 Fn1<Promise<T9>, T> fn9) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .flatMap(v8 -> fn9.apply(v)
                                                                                                                                                                                     .map(v9 -> Tuple.tuple(v1,
                                                                                                                                                                                                            v2,
                                                                                                                                                                                                            v3,
                                                                                                                                                                                                            v4,
                                                                                                                                                                                                            v5,
                                                                                                                                                                                                            v6,
                                                                                                                                                                                                            v7,
                                                                                                                                                                                                            v8,
                                                                                                                                                                                                            v9)))))))))));
    }

    /// Chain ten dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Mapper10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> all(Fn1<Promise<T1>, T> fn1,
                                                                                                            Fn1<Promise<T2>, T> fn2,
                                                                                                            Fn1<Promise<T3>, T> fn3,
                                                                                                            Fn1<Promise<T4>, T> fn4,
                                                                                                            Fn1<Promise<T5>, T> fn5,
                                                                                                            Fn1<Promise<T6>, T> fn6,
                                                                                                            Fn1<Promise<T7>, T> fn7,
                                                                                                            Fn1<Promise<T8>, T> fn8,
                                                                                                            Fn1<Promise<T9>, T> fn9,
                                                                                                            Fn1<Promise<T10>, T> fn10) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .flatMap(v8 -> fn9.apply(v)
                                                                                                                                                                                     .flatMap(v9 -> fn10.apply(v)
                                                                                                                                                                                                        .map(v10 -> Tuple.tuple(v1,
                                                                                                                                                                                                                                v2,
                                                                                                                                                                                                                                v3,
                                                                                                                                                                                                                                v4,
                                                                                                                                                                                                                                v5,
                                                                                                                                                                                                                                v6,
                                                                                                                                                                                                                                v7,
                                                                                                                                                                                                                                v8,
                                                                                                                                                                                                                                v9,
                                                                                                                                                                                                                                v10))))))))))));
    }

    /// Chain eleven dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Mapper11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> all(Fn1<Promise<T1>, T> fn1,
                                                                                                                      Fn1<Promise<T2>, T> fn2,
                                                                                                                      Fn1<Promise<T3>, T> fn3,
                                                                                                                      Fn1<Promise<T4>, T> fn4,
                                                                                                                      Fn1<Promise<T5>, T> fn5,
                                                                                                                      Fn1<Promise<T6>, T> fn6,
                                                                                                                      Fn1<Promise<T7>, T> fn7,
                                                                                                                      Fn1<Promise<T8>, T> fn8,
                                                                                                                      Fn1<Promise<T9>, T> fn9,
                                                                                                                      Fn1<Promise<T10>, T> fn10,
                                                                                                                      Fn1<Promise<T11>, T> fn11) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .flatMap(v8 -> fn9.apply(v)
                                                                                                                                                                                     .flatMap(v9 -> fn10.apply(v)
                                                                                                                                                                                                        .flatMap(v10 -> fn11.apply(v)
                                                                                                                                                                                                                            .map(v11 -> Tuple.tuple(v1,
                                                                                                                                                                                                                                                    v2,
                                                                                                                                                                                                                                                    v3,
                                                                                                                                                                                                                                                    v4,
                                                                                                                                                                                                                                                    v5,
                                                                                                                                                                                                                                                    v6,
                                                                                                                                                                                                                                                    v7,
                                                                                                                                                                                                                                                    v8,
                                                                                                                                                                                                                                                    v9,
                                                                                                                                                                                                                                                    v10,
                                                                                                                                                                                                                                                    v11)))))))))))));
    }

    /// Chain twelve dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Mapper12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> all(Fn1<Promise<T1>, T> fn1,
                                                                                                                                Fn1<Promise<T2>, T> fn2,
                                                                                                                                Fn1<Promise<T3>, T> fn3,
                                                                                                                                Fn1<Promise<T4>, T> fn4,
                                                                                                                                Fn1<Promise<T5>, T> fn5,
                                                                                                                                Fn1<Promise<T6>, T> fn6,
                                                                                                                                Fn1<Promise<T7>, T> fn7,
                                                                                                                                Fn1<Promise<T8>, T> fn8,
                                                                                                                                Fn1<Promise<T9>, T> fn9,
                                                                                                                                Fn1<Promise<T10>, T> fn10,
                                                                                                                                Fn1<Promise<T11>, T> fn11,
                                                                                                                                Fn1<Promise<T12>, T> fn12) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .flatMap(v8 -> fn9.apply(v)
                                                                                                                                                                                     .flatMap(v9 -> fn10.apply(v)
                                                                                                                                                                                                        .flatMap(v10 -> fn11.apply(v)
                                                                                                                                                                                                                            .flatMap(v11 -> fn12.apply(v)
                                                                                                                                                                                                                                                .map(v12 -> Tuple.tuple(v1,
                                                                                                                                                                                                                                                                        v2,
                                                                                                                                                                                                                                                                        v3,
                                                                                                                                                                                                                                                                        v4,
                                                                                                                                                                                                                                                                        v5,
                                                                                                                                                                                                                                                                        v6,
                                                                                                                                                                                                                                                                        v7,
                                                                                                                                                                                                                                                                        v8,
                                                                                                                                                                                                                                                                        v9,
                                                                                                                                                                                                                                                                        v10,
                                                                                                                                                                                                                                                                        v11,
                                                                                                                                                                                                                                                                        v12))))))))))))));
    }

    /// Chain thirteen dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Mapper13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> all(Fn1<Promise<T1>, T> fn1,
                                                                                                                                          Fn1<Promise<T2>, T> fn2,
                                                                                                                                          Fn1<Promise<T3>, T> fn3,
                                                                                                                                          Fn1<Promise<T4>, T> fn4,
                                                                                                                                          Fn1<Promise<T5>, T> fn5,
                                                                                                                                          Fn1<Promise<T6>, T> fn6,
                                                                                                                                          Fn1<Promise<T7>, T> fn7,
                                                                                                                                          Fn1<Promise<T8>, T> fn8,
                                                                                                                                          Fn1<Promise<T9>, T> fn9,
                                                                                                                                          Fn1<Promise<T10>, T> fn10,
                                                                                                                                          Fn1<Promise<T11>, T> fn11,
                                                                                                                                          Fn1<Promise<T12>, T> fn12,
                                                                                                                                          Fn1<Promise<T13>, T> fn13) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .flatMap(v8 -> fn9.apply(v)
                                                                                                                                                                                     .flatMap(v9 -> fn10.apply(v)
                                                                                                                                                                                                        .flatMap(v10 -> fn11.apply(v)
                                                                                                                                                                                                                            .flatMap(v11 -> fn12.apply(v)
                                                                                                                                                                                                                                                .flatMap(v12 -> fn13.apply(v)
                                                                                                                                                                                                                                                                    .map(v13 -> Tuple.tuple(v1,
                                                                                                                                                                                                                                                                                            v2,
                                                                                                                                                                                                                                                                                            v3,
                                                                                                                                                                                                                                                                                            v4,
                                                                                                                                                                                                                                                                                            v5,
                                                                                                                                                                                                                                                                                            v6,
                                                                                                                                                                                                                                                                                            v7,
                                                                                                                                                                                                                                                                                            v8,
                                                                                                                                                                                                                                                                                            v9,
                                                                                                                                                                                                                                                                                            v10,
                                                                                                                                                                                                                                                                                            v11,
                                                                                                                                                                                                                                                                                            v12,
                                                                                                                                                                                                                                                                                            v13)))))))))))))));
    }

    /// Chain fourteen dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Mapper14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> all(Fn1<Promise<T1>, T> fn1,
                                                                                                                                                    Fn1<Promise<T2>, T> fn2,
                                                                                                                                                    Fn1<Promise<T3>, T> fn3,
                                                                                                                                                    Fn1<Promise<T4>, T> fn4,
                                                                                                                                                    Fn1<Promise<T5>, T> fn5,
                                                                                                                                                    Fn1<Promise<T6>, T> fn6,
                                                                                                                                                    Fn1<Promise<T7>, T> fn7,
                                                                                                                                                    Fn1<Promise<T8>, T> fn8,
                                                                                                                                                    Fn1<Promise<T9>, T> fn9,
                                                                                                                                                    Fn1<Promise<T10>, T> fn10,
                                                                                                                                                    Fn1<Promise<T11>, T> fn11,
                                                                                                                                                    Fn1<Promise<T12>, T> fn12,
                                                                                                                                                    Fn1<Promise<T13>, T> fn13,
                                                                                                                                                    Fn1<Promise<T14>, T> fn14) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .flatMap(v8 -> fn9.apply(v)
                                                                                                                                                                                     .flatMap(v9 -> fn10.apply(v)
                                                                                                                                                                                                        .flatMap(v10 -> fn11.apply(v)
                                                                                                                                                                                                                            .flatMap(v11 -> fn12.apply(v)
                                                                                                                                                                                                                                                .flatMap(v12 -> fn13.apply(v)
                                                                                                                                                                                                                                                                    .flatMap(v13 -> fn14.apply(v)
                                                                                                                                                                                                                                                                                        .map(v14 -> Tuple.tuple(v1,
                                                                                                                                                                                                                                                                                                                v2,
                                                                                                                                                                                                                                                                                                                v3,
                                                                                                                                                                                                                                                                                                                v4,
                                                                                                                                                                                                                                                                                                                v5,
                                                                                                                                                                                                                                                                                                                v6,
                                                                                                                                                                                                                                                                                                                v7,
                                                                                                                                                                                                                                                                                                                v8,
                                                                                                                                                                                                                                                                                                                v9,
                                                                                                                                                                                                                                                                                                                v10,
                                                                                                                                                                                                                                                                                                                v11,
                                                                                                                                                                                                                                                                                                                v12,
                                                                                                                                                                                                                                                                                                                v13,
                                                                                                                                                                                                                                                                                                                v14))))))))))))))));
    }

    /// Chain fifteen dependent operations with access to this Promise's value.
    default <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Mapper15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> all(Fn1<Promise<T1>, T> fn1,
                                                                                                                                                              Fn1<Promise<T2>, T> fn2,
                                                                                                                                                              Fn1<Promise<T3>, T> fn3,
                                                                                                                                                              Fn1<Promise<T4>, T> fn4,
                                                                                                                                                              Fn1<Promise<T5>, T> fn5,
                                                                                                                                                              Fn1<Promise<T6>, T> fn6,
                                                                                                                                                              Fn1<Promise<T7>, T> fn7,
                                                                                                                                                              Fn1<Promise<T8>, T> fn8,
                                                                                                                                                              Fn1<Promise<T9>, T> fn9,
                                                                                                                                                              Fn1<Promise<T10>, T> fn10,
                                                                                                                                                              Fn1<Promise<T11>, T> fn11,
                                                                                                                                                              Fn1<Promise<T12>, T> fn12,
                                                                                                                                                              Fn1<Promise<T13>, T> fn13,
                                                                                                                                                              Fn1<Promise<T14>, T> fn14,
                                                                                                                                                              Fn1<Promise<T15>, T> fn15) {
        return () -> flatMap(v -> fn1.apply(v)
                                     .flatMap(v1 -> fn2.apply(v)
                                                       .flatMap(v2 -> fn3.apply(v)
                                                                         .flatMap(v3 -> fn4.apply(v)
                                                                                           .flatMap(v4 -> fn5.apply(v)
                                                                                                             .flatMap(v5 -> fn6.apply(v)
                                                                                                                               .flatMap(v6 -> fn7.apply(v)
                                                                                                                                                 .flatMap(v7 -> fn8.apply(v)
                                                                                                                                                                   .flatMap(v8 -> fn9.apply(v)
                                                                                                                                                                                     .flatMap(v9 -> fn10.apply(v)
                                                                                                                                                                                                        .flatMap(v10 -> fn11.apply(v)
                                                                                                                                                                                                                            .flatMap(v11 -> fn12.apply(v)
                                                                                                                                                                                                                                                .flatMap(v12 -> fn13.apply(v)
                                                                                                                                                                                                                                                                    .flatMap(v13 -> fn14.apply(v)
                                                                                                                                                                                                                                                                                        .flatMap(v14 -> fn15.apply(v)
                                                                                                                                                                                                                                                                                                            .map(v15 -> Tuple.tuple(v1,
                                                                                                                                                                                                                                                                                                                                    v2,
                                                                                                                                                                                                                                                                                                                                    v3,
                                                                                                                                                                                                                                                                                                                                    v4,
                                                                                                                                                                                                                                                                                                                                    v5,
                                                                                                                                                                                                                                                                                                                                    v6,
                                                                                                                                                                                                                                                                                                                                    v7,
                                                                                                                                                                                                                                                                                                                                    v8,
                                                                                                                                                                                                                                                                                                                                    v9,
                                                                                                                                                                                                                                                                                                                                    v10,
                                                                                                                                                                                                                                                                                                                                    v11,
                                                                                                                                                                                                                                                                                                                                    v12,
                                                                                                                                                                                                                                                                                                                                    v13,
                                                                                                                                                                                                                                                                                                                                    v14,
                                                                                                                                                                                                                                                                                                                                    v15)))))))))))))))));
    }

    /// Create a new unresolved promise instance.
    ///
    /// @return New promise instance.
    static <T> Promise<T> promise() {
        return new PromiseImpl<>(null);
    }

    /// Create a new resolved promise instance resolved with the provided value.
    ///
    /// @param value Value to resolve the promise with.
    ///
    /// @return New resolved promise instance.
    static <T> Promise<T> resolved(Result<T> value) {
        return new PromiseImpl<>(value);
    }

    /// Create a new resolved promise instance resolved into success with the provided value.
    ///
    /// @param value Value to resolve the promise with.
    ///
    /// @return New resolved promise instance.
    static <T> Promise<T> success(T value) {
        return new PromiseImpl<>(Result.success(value));
    }

    /// Create a new resolved promise instance resolved into success with the provided value.
    ///
    /// @param value Value to resolve the promise with.
    ///
    /// @return New resolved promise instance.
    static <T> Promise<T> ok(T value) {
        return success(value);
    }

    /// Create a new resolved promise instance resolved into failure with the provided cause.
    ///
    /// @param cause Cause to resolve the promise with.
    ///
    /// @return New resolved promise instance.
    static <T> Promise<T> failure(Cause cause) {
        return new PromiseImpl<>(Result.failure(cause));
    }

    /// Create a new resolved promise instance resolved into failure with the provided cause.
    ///
    /// @param cause Cause to resolve the promise with.
    ///
    /// @return New resolved promise instance.
    static <T> Promise<T> err(Cause cause) {
        return failure(cause);
    }

    /// Create a new unresolved promise instance and run the provided consumer asynchronously with the newly created instance.
    ///
    /// @param consumer Consumer to execute asynchronously with the created instance.
    ///
    /// @return Created instance.
    static <T> Promise<T> promise(Consumer<Promise<T>> consumer) {
        return Promise.<T> promise()
               .async(consumer);
    }

    /// Creates a promise that resolves with the result provided by the given supplier.
    ///
    /// @param supplier a supplier that provides a result to resolve the promise
    ///
    /// @return a promise that asynchronously resolves with the result provided by the supplier
    static <T> Promise<T> promise(Supplier<Result<T>> supplier) {
        return promise(promise -> promise.resolve(supplier.get()));
    }

    /// Create a new unresolved promise instance and run the provided consumer asynchronously with the newly created instance after the specified timeout.
    ///
    /// @param delay    delay before execution starts
    /// @param consumer Consumer to execute asynchronously with the created instance.
    ///
    /// @return Created instance.
    static <T> Promise<T> promise(TimeSpan delay, Consumer<Promise<T>> consumer) {
        return Promise.<T> promise()
               .async(delay, consumer);
    }

    /// Create a new unresolved promise instance and run the provided supplier asynchronously.
    /// Result returned by the supplier is then used to resolve the promise.
    ///
    /// @param delay    delay before execution starts
    /// @param supplier Supplier to execute asynchronously
    ///
    /// @return Created instance.
    static <T> Promise<T> promise(TimeSpan delay, Supplier<Result<T>> supplier) {
        return promise(delay,
                       promise -> promise.resolve(supplier.get()));
    }

    /// Asynchronously run the provided lambda and eventually resolve returned [Promise] with the value returned by lambda if the call succeeds or with
    /// the failure if call throws exception.
    ///
    /// @param exceptionMapper the function which will transform exception into instance of [Cause]
    /// @param supplier        the call to wrap
    ///
    /// @return the [Promise] instance, which eventually will be resolved with the output of the provided lambda
    static <U> Promise<U> lift(Fn1< ? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn0<U> supplier) {
        return Promise.promise(() -> Result.lift(exceptionMapper, supplier));
    }

    /// Wrap the call to the provided function into success [Result] if the call succeeds of into failure [Result] if call throws exception.
    ///
    /// @param exceptionMapper the function which will transform exception into instance of [Cause]
    /// @param function        the function to wrap
    ///
    /// @return the [Promise] instance, which eventually will be resolved with the output of the provided lambda
    static <U, T1> Fn1<Promise<U>, T1> liftFn1(Fn1< ? extends Cause, ? super Throwable> exceptionMapper,
                                               ThrowingFn1<U, T1> function) {
        return value -> Promise.promise(() -> Result.lift(exceptionMapper, () -> function.apply(value)));
    }

    /// Convenience method for creating a binary function that wraps a throwing function and returns a Promise.
    /// This is a function factory that creates reusable binary functions for promise-based operations.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing binary function to wrap
    /// @param <U>             The return type of the function
    /// @param <T1>            The type of the first parameter
    /// @param <T2>            The type of the second parameter
    ///
    /// @return A binary function that takes two parameters and returns a Promise
    static <U, T1, T2> Fn2<Promise<U>, T1, T2> liftFn2(Fn1< ? extends Cause, ? super Throwable> exceptionMapper,
                                                       ThrowingFn2<U, T1, T2> function) {
        return (value1, value2) -> Promise.promise(() -> Result.lift(exceptionMapper,
                                                                     () -> function.apply(value1, value2)));
    }

    /// Convenience method for creating a ternary function that wraps a throwing function and returns a Promise.
    /// This is a function factory that creates reusable ternary functions for promise-based operations.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing ternary function to wrap
    /// @param <U>             The return type of the function
    /// @param <T1>            The type of the first parameter
    /// @param <T2>            The type of the second parameter
    /// @param <T3>            The type of the third parameter
    ///
    /// @return A ternary function that takes three parameters and returns a Promise
    static <U, T1, T2, T3> Fn3<Promise<U>, T1, T2, T3> liftFn3(Fn1< ? extends Cause, ? super Throwable> exceptionMapper,
                                                               ThrowingFn3<U, T1, T2, T3> function) {
        return (value1, value2, value3) -> Promise.promise(() -> Result.lift(exceptionMapper,
                                                                             () -> function.apply(value1, value2, value3)));
    }

    /// Same as [#liftFn1(Fn1, ThrowingFn1)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing unary function to wrap
    /// @param <U>      The return type of the function
    /// @param <T1>     The type of the parameter
    ///
    /// @return A unary function that takes one parameter and returns a Promise
    static <U, T1> Fn1<Promise<U>, T1> liftFn1(ThrowingFn1<U, T1> function) {
        return liftFn1(Causes::fromThrowable, function);
    }

    /// Same as [#liftFn2(Fn1, ThrowingFn2)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing binary function to wrap
    /// @param <U>      The return type of the function
    /// @param <T1>     The type of the first parameter
    /// @param <T2>     The type of the second parameter
    ///
    /// @return A binary function that takes two parameters and returns a Promise
    static <U, T1, T2> Fn2<Promise<U>, T1, T2> liftFn2(ThrowingFn2<U, T1, T2> function) {
        return liftFn2(Causes::fromThrowable, function);
    }

    /// Same as [#liftFn3(Fn1, ThrowingFn3)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing ternary function to wrap
    /// @param <U>      The return type of the function
    /// @param <T1>     The type of the first parameter
    /// @param <T2>     The type of the second parameter
    /// @param <T3>     The type of the third parameter
    ///
    /// @return A ternary function that takes three parameters and returns a Promise
    static <U, T1, T2, T3> Fn3<Promise<U>, T1, T2, T3> liftFn3(ThrowingFn3<U, T1, T2, T3> function) {
        return liftFn3(Causes::fromThrowable, function);
    }

    /// Convenience method for directly invoking a throwing unary function and wrapping the result in a Promise.
    /// This method provides immediate invocation rather than returning a function factory.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing unary function to invoke
    /// @param value1          The parameter value to pass to the function
    /// @param <U>             The return type of the function
    /// @param <T1>            The type of the parameter
    ///
    /// @return A Promise that will be resolved with the function result or failure
    static <U, T1> Promise<U> lift1(Fn1< ? extends Cause, ? super Throwable> exceptionMapper,
                                    ThrowingFn1<U, T1> function,
                                    T1 value1) {
        return Promise.promise(() -> Result.lift(exceptionMapper, () -> function.apply(value1)));
    }

    /// Same as [#lift1(Fn1, ThrowingFn1, Object)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing unary function to invoke
    /// @param value1   The parameter value to pass to the function
    /// @param <U>      The return type of the function
    /// @param <T1>     The type of the parameter
    ///
    /// @return A Promise that will be resolved with the function result or failure
    static <U, T1> Promise<U> lift1(ThrowingFn1<U, T1> function, T1 value1) {
        return lift1(Causes::fromThrowable, function, value1);
    }

    /// Convenience method for directly invoking a throwing binary function and wrapping the result in a Promise.
    /// This method provides immediate invocation rather than returning a function factory.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing binary function to invoke
    /// @param value1          The first parameter value to pass to the function
    /// @param value2          The second parameter value to pass to the function
    /// @param <U>             The return type of the function
    /// @param <T1>            The type of the first parameter
    /// @param <T2>            The type of the second parameter
    ///
    /// @return A Promise that will be resolved with the function result or failure
    static <U, T1, T2> Promise<U> lift2(Fn1< ? extends Cause, ? super Throwable> exceptionMapper,
                                        ThrowingFn2<U, T1, T2> function,
                                        T1 value1,
                                        T2 value2) {
        return Promise.promise(() -> Result.lift(exceptionMapper, () -> function.apply(value1, value2)));
    }

    /// Same as [#lift2(Fn1, ThrowingFn2, Object, Object)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing binary function to invoke
    /// @param value1   The first parameter value to pass to the function
    /// @param value2   The second parameter value to pass to the function
    /// @param <U>      The return type of the function
    /// @param <T1>     The type of the first parameter
    /// @param <T2>     The type of the second parameter
    ///
    /// @return A Promise that will be resolved with the function result or failure
    static <U, T1, T2> Promise<U> lift2(ThrowingFn2<U, T1, T2> function, T1 value1, T2 value2) {
        return lift2(Causes::fromThrowable, function, value1, value2);
    }

    /// Convenience method for directly invoking a throwing ternary function and wrapping the result in a Promise.
    /// This method provides immediate invocation rather than returning a function factory.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing ternary function to invoke
    /// @param value1          The first parameter value to pass to the function
    /// @param value2          The second parameter value to pass to the function
    /// @param value3          The third parameter value to pass to the function
    /// @param <U>             The return type of the function
    /// @param <T1>            The type of the first parameter
    /// @param <T2>            The type of the second parameter
    /// @param <T3>            The type of the third parameter
    ///
    /// @return A Promise that will be resolved with the function result or failure
    static <U, T1, T2, T3> Promise<U> lift3(Fn1< ? extends Cause, ? super Throwable> exceptionMapper,
                                            ThrowingFn3<U, T1, T2, T3> function,
                                            T1 value1,
                                            T2 value2,
                                            T3 value3) {
        return Promise.promise(() -> Result.lift(exceptionMapper, () -> function.apply(value1, value2, value3)));
    }

    /// Same as [#lift3(Fn1, ThrowingFn3, Object, Object, Object)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing ternary function to invoke
    /// @param value1   The first parameter value to pass to the function
    /// @param value2   The second parameter value to pass to the function
    /// @param value3   The third parameter value to pass to the function
    /// @param <U>      The return type of the function
    /// @param <T1>     The type of the first parameter
    /// @param <T2>     The type of the second parameter
    /// @param <T3>     The type of the third parameter
    ///
    /// @return A Promise that will be resolved with the function result or failure
    static <U, T1, T2, T3> Promise<U> lift3(ThrowingFn3<U, T1, T2, T3> function, T1 value1, T2 value2, T3 value3) {
        return lift3(Causes::fromThrowable, function, value1, value2, value3);
    }

    /// Asynchronously run the provided lambda and eventually resolve returned [Promise] with the [Unit] if the call succeeds or with the failure
    /// if call throws exception.
    ///
    /// @param exceptionMapper the function which will transform exception into instance of [Cause]
    /// @param runnable        the call to wrap
    ///
    /// @return the [Promise] instance which eventually will be resolved with the [Unit] or with the failure with the provided cause.
    static Promise<Unit> lift(Fn1< ? extends Cause, ? super Throwable> exceptionMapper, ThrowingRunnable runnable) {
        return Promise.promise(() -> Result.lift(exceptionMapper, runnable));
    }

    /// Asynchronously run the provided lambda and eventually resolve returned [Promise] with the [Unit] if the call succeeds or with the failure
    /// if call throws exception.
    ///
    /// @param cause    the cause, which will be used to create a failure result
    /// @param supplier the call to wrap
    ///
    /// @return the [Promise] instance, which eventually will be resolved with the output of the provided lambda
    static <U> Promise<U> lift(Cause cause, ThrowingFn0<U> supplier) {
        return Promise.promise(() -> Result.lift(cause, supplier));
    }

    /// Similar to [#lift(Fn1,ThrowingFn0)] but with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param supplier the call to wrap
    ///
    /// @return the [Promise] instance, which eventually will be resolved with the output of the provided lambda
    static <U> Promise<U> lift(ThrowingFn0<U> supplier) {
        return Promise.promise(() -> Result.lift(supplier));
    }

    /// Asynchronously run the provided lambda and eventually resolve returned [Promise] with the [Unit] if the call succeeds or with the failure
    /// if call throws exception.
    ///
    /// @param cause    the cause, which will be used to create a failure result
    /// @param runnable the call to wrap
    ///
    /// @return the [Promise] instance which eventually will be resolved with the [Unit] or with the failure with the provided cause.
    static Promise<Unit> lift(Cause cause, ThrowingRunnable runnable) {
        return Promise.promise(() -> Result.lift(cause, runnable));
    }

    /// Similar to [#lift(Fn1,ThrowingRunnable)] but with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param runnable the call to wrap
    ///
    /// @return Unit result which is success if no exceptions were thrown or failure otherwise
    static Promise<Unit> lift(ThrowingRunnable runnable) {
        return Promise.promise(() -> Result.lift(runnable));
    }

    /// Asynchronously run the provided lambda and eventually resolve returned [Promise] with the [Unit] if the call succeeds or with the failure
    ///
    /// @param action the action to run
    ///
    /// @return the [Promise] instance which eventually will be resolved with the [Unit] or with the failure with the provided cause.
    static Promise<Unit> async(Runnable action) {
        return Promise.lift(CoreError::exception, action::run);
    }

    /// Fail all provided promises with the provided cause.
    ///
    /// @param cause    Cause to fail the promises with.
    /// @param promises Promises to fail.
    @SafeVarargs
    static <T> void failAll(Cause cause, Promise<T>... promises) {
        failAll(cause, List.of(promises));
    }

    /// Fail all provided promises with the provided cause.
    ///
    /// @param cause    Cause to fail the promises with.
    /// @param promises Promises to fail.
    static <T> void failAll(Cause cause, List<Promise<T>> promises) {
        promises.forEach(promise -> promise.fail(cause));
    }

    /// Instance of the [Promise] resolved into success with [Unit].
    ///
    /// @return The singleton instance of the [Promise] resolved into success with [Unit].
    static Promise<Unit> unitPromise() {
        return UNIT;
    }

    /// Return promise which will be resolved once any of the promises provided as parameters are resolved with success. If none of the promises
    /// are resolved with success, then the created instance will be resolved with the provided `failureResult`.
    ///
    /// @param failureResult Result in case if no instances were resolved with success
    /// @param promises      Input promises
    ///
    /// @return Created instance
    @SafeVarargs
    static <T> Promise<T> any(Result<T> failureResult, Promise<T>... promises) {
        return Promise.promise(anySuccess -> threshold(promises.length,
                                                       () -> anySuccess.resolve(failureResult))
                                             .apply(at -> List.of(promises)
                                                              .forEach(promise -> promise.withResult(result -> result.onSuccess(anySuccess::succeed)
                                                                                                                     .onSuccessRun(() -> cancelAll(promises))
                                                                                                                     .onResultRun(at::registerEvent)))));
    }

    /// Return promise which will be resolved once any of the promises provided as parameters are resolved with success. If none of the promises
    /// are resolved with success, then the created instance will be resolved with the provided `failureResult`.
    ///
    /// @param failureResult Result in case if no instances were resolved with success
    /// @param promises      Input promises
    ///
    /// @return Created instance
    static <T> Promise<T> any(Result<T> failureResult, List<Promise<T>> promises) {
        return Promise.promise(anySuccess -> threshold(promises.size(),
                                                       () -> anySuccess.resolve(failureResult))
                                             .apply(at -> promises.forEach(
        promise -> promise.withResult(result -> result.onSuccess(anySuccess::succeed)
                                                      .onSuccessRun(() -> cancelAll(promises))
                                                      .onResultRun(at::registerEvent)))));
    }

    /// Return promise which will be resolved once any of the promises provided as parameters are resolved with success. If none of the promises
    /// are resolved with success, then the created instance will be resolved with [CoreError.Cancelled].
    ///
    /// @param promises Input promises
    ///
    /// @return Created instance
    @SuppressWarnings("unchecked")
    @SafeVarargs
    static <T> Promise<T> any(Promise<T>... promises) {
        return any((Result<T>) OTHER_SUCCEEDED, promises);
    }

    /// Return promise which will be resolved once any of the promises provided as parameters are resolved with success. If none of the promises
    /// are resolved with success, then the created instance will be resolved with [CoreError.Cancelled].
    ///
    /// @param promises Input promises
    ///
    /// @return Created instance
    @SuppressWarnings("unchecked")
    static <T> Promise<T> any(List<Promise<T>> promises) {
        return any((Result<T>) OTHER_SUCCEEDED, promises);
    }

    /// Cancel all provided promises.
    ///
    /// @param promises Input promises.
    @SafeVarargs
    static <T> void cancelAll(Promise<T>... promises) {
        cancelAll(List.of(promises));
    }

    /// Cancel all provided promises.
    ///
    /// @param promises Input promises.
    static <T> void cancelAll(List<Promise<T>> promises) {
        failAll(PROMISE_CANCELLED, promises);
    }

    /// Return a promise which will be resolved with the list containing results from all passed promises.
    ///
    /// @param promises Collection of promises to be resolved.
    ///
    /// @return Promise instance, which will be resolved with the list of results from resolved promises.
    @SuppressWarnings("unchecked")
    static <T> Promise<List<Result<T>>> allOf(Collection<Promise<T>> promises) {
        if (promises.isEmpty()) {
            return Promise.success(List.of());
        }
        var array = promises.toArray(new Promise[0]);
        var promise = Promise.promise();
        var collector = ResultCollector.resultCollector(promises.size(),
                                                        values -> promise.succeed(List.of(values)));
        IntStream.range(0,
                        promises.size())
                 .forEach(index -> array[index].withResult(result -> collector.registerEvent(index, result)));
        return promise.map(list -> (List<Result<T>>) list);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// The function for a single input promise is provided for completeness.
    ///
    /// @param promise1 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    static <T1> Mapper1<T1> all(Promise<T1> promise1) {
        var causes = Causes.composite();
        return () -> promise1.map(Tuple::tuple)
                             .mapError(causes::append);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2> Mapper2<T1, T2> all(Promise<T1> promise1, Promise<T2> promise2) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1])
                                                 .id(),
                                 promise1,
                                 promise2);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    /// @param promise3 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2, T3> Mapper3<T1, T2, T3> all(Promise<T1> promise1, Promise<T2> promise2, Promise<T3> promise3) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    /// @param promise3 Input promise
    /// @param promise4 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4> Mapper4<T1, T2, T3, T4> all(Promise<T1> promise1,
                                                        Promise<T2> promise2,
                                                        Promise<T3> promise3,
                                                        Promise<T4> promise4) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    /// @param promise3 Input promise
    /// @param promise4 Input promise
    /// @param promise5 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5> Mapper5<T1, T2, T3, T4, T5> all(Promise<T1> promise1,
                                                                Promise<T2> promise2,
                                                                Promise<T3> promise3,
                                                                Promise<T4> promise4,
                                                                Promise<T5> promise5) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    /// @param promise3 Input promise
    /// @param promise4 Input promise
    /// @param promise5 Input promise
    /// @param promise6 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6> Mapper6<T1, T2, T3, T4, T5, T6> all(Promise<T1> promise1,
                                                                        Promise<T2> promise2,
                                                                        Promise<T3> promise3,
                                                                        Promise<T4> promise4,
                                                                        Promise<T5> promise5,
                                                                        Promise<T6> promise6) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    /// @param promise3 Input promise
    /// @param promise4 Input promise
    /// @param promise5 Input promise
    /// @param promise6 Input promise
    /// @param promise7 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7> Mapper7<T1, T2, T3, T4, T5, T6, T7> all(Promise<T1> promise1,
                                                                                Promise<T2> promise2,
                                                                                Promise<T3> promise3,
                                                                                Promise<T4> promise4,
                                                                                Promise<T5> promise5,
                                                                                Promise<T6> promise6,
                                                                                Promise<T7> promise7) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    /// @param promise3 Input promise
    /// @param promise4 Input promise
    /// @param promise5 Input promise
    /// @param promise6 Input promise
    /// @param promise7 Input promise
    /// @param promise8 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8> Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> all(Promise<T1> promise1,
                                                                                        Promise<T2> promise2,
                                                                                        Promise<T3> promise3,
                                                                                        Promise<T4> promise4,
                                                                                        Promise<T5> promise5,
                                                                                        Promise<T6> promise6,
                                                                                        Promise<T7> promise7,
                                                                                        Promise<T8> promise8) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8);
    }

    /// Return a promise which will be resolved when all promises passed as a parameter are resolved. If any of the provided promises are
    /// resolved with error, then the resulting promise will be also resolved with error.
    ///
    /// @param promise1 Input promise
    /// @param promise2 Input promise
    /// @param promise3 Input promise
    /// @param promise4 Input promise
    /// @param promise5 Input promise
    /// @param promise6 Input promise
    /// @param promise7 Input promise
    /// @param promise8 Input promise
    /// @param promise9 Input promise
    ///
    /// @return Promise instance, which will be resolved with all collected results.
    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> all(Promise<T1> promise1,
                                                                                                Promise<T2> promise2,
                                                                                                Promise<T3> promise3,
                                                                                                Promise<T4> promise4,
                                                                                                Promise<T5> promise5,
                                                                                                Promise<T6> promise6,
                                                                                                Promise<T7> promise7,
                                                                                                Promise<T8> promise8,
                                                                                                Promise<T9> promise9) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7],
                                                      (Result<T9>) values[8])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8,
                                 promise9);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Mapper10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> all(Promise<T1> promise1,
                                                                                                           Promise<T2> promise2,
                                                                                                           Promise<T3> promise3,
                                                                                                           Promise<T4> promise4,
                                                                                                           Promise<T5> promise5,
                                                                                                           Promise<T6> promise6,
                                                                                                           Promise<T7> promise7,
                                                                                                           Promise<T8> promise8,
                                                                                                           Promise<T9> promise9,
                                                                                                           Promise<T10> promise10) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7],
                                                      (Result<T9>) values[8],
                                                      (Result<T10>) values[9])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8,
                                 promise9,
                                 promise10);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Mapper11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> all(Promise<T1> promise1,
                                                                                                                     Promise<T2> promise2,
                                                                                                                     Promise<T3> promise3,
                                                                                                                     Promise<T4> promise4,
                                                                                                                     Promise<T5> promise5,
                                                                                                                     Promise<T6> promise6,
                                                                                                                     Promise<T7> promise7,
                                                                                                                     Promise<T8> promise8,
                                                                                                                     Promise<T9> promise9,
                                                                                                                     Promise<T10> promise10,
                                                                                                                     Promise<T11> promise11) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7],
                                                      (Result<T9>) values[8],
                                                      (Result<T10>) values[9],
                                                      (Result<T11>) values[10])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8,
                                 promise9,
                                 promise10,
                                 promise11);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Mapper12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> all(Promise<T1> promise1,
                                                                                                                               Promise<T2> promise2,
                                                                                                                               Promise<T3> promise3,
                                                                                                                               Promise<T4> promise4,
                                                                                                                               Promise<T5> promise5,
                                                                                                                               Promise<T6> promise6,
                                                                                                                               Promise<T7> promise7,
                                                                                                                               Promise<T8> promise8,
                                                                                                                               Promise<T9> promise9,
                                                                                                                               Promise<T10> promise10,
                                                                                                                               Promise<T11> promise11,
                                                                                                                               Promise<T12> promise12) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7],
                                                      (Result<T9>) values[8],
                                                      (Result<T10>) values[9],
                                                      (Result<T11>) values[10],
                                                      (Result<T12>) values[11])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8,
                                 promise9,
                                 promise10,
                                 promise11,
                                 promise12);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Mapper13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> all(Promise<T1> promise1,
                                                                                                                                         Promise<T2> promise2,
                                                                                                                                         Promise<T3> promise3,
                                                                                                                                         Promise<T4> promise4,
                                                                                                                                         Promise<T5> promise5,
                                                                                                                                         Promise<T6> promise6,
                                                                                                                                         Promise<T7> promise7,
                                                                                                                                         Promise<T8> promise8,
                                                                                                                                         Promise<T9> promise9,
                                                                                                                                         Promise<T10> promise10,
                                                                                                                                         Promise<T11> promise11,
                                                                                                                                         Promise<T12> promise12,
                                                                                                                                         Promise<T13> promise13) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7],
                                                      (Result<T9>) values[8],
                                                      (Result<T10>) values[9],
                                                      (Result<T11>) values[10],
                                                      (Result<T12>) values[11],
                                                      (Result<T13>) values[12])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8,
                                 promise9,
                                 promise10,
                                 promise11,
                                 promise12,
                                 promise13);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Mapper14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> all(Promise<T1> promise1,
                                                                                                                                                   Promise<T2> promise2,
                                                                                                                                                   Promise<T3> promise3,
                                                                                                                                                   Promise<T4> promise4,
                                                                                                                                                   Promise<T5> promise5,
                                                                                                                                                   Promise<T6> promise6,
                                                                                                                                                   Promise<T7> promise7,
                                                                                                                                                   Promise<T8> promise8,
                                                                                                                                                   Promise<T9> promise9,
                                                                                                                                                   Promise<T10> promise10,
                                                                                                                                                   Promise<T11> promise11,
                                                                                                                                                   Promise<T12> promise12,
                                                                                                                                                   Promise<T13> promise13,
                                                                                                                                                   Promise<T14> promise14) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7],
                                                      (Result<T9>) values[8],
                                                      (Result<T10>) values[9],
                                                      (Result<T11>) values[10],
                                                      (Result<T12>) values[11],
                                                      (Result<T13>) values[12],
                                                      (Result<T14>) values[13])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8,
                                 promise9,
                                 promise10,
                                 promise11,
                                 promise12,
                                 promise13,
                                 promise14);
    }

    @SuppressWarnings("unchecked")
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Mapper15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> all(Promise<T1> promise1,
                                                                                                                                                             Promise<T2> promise2,
                                                                                                                                                             Promise<T3> promise3,
                                                                                                                                                             Promise<T4> promise4,
                                                                                                                                                             Promise<T5> promise5,
                                                                                                                                                             Promise<T6> promise6,
                                                                                                                                                             Promise<T7> promise7,
                                                                                                                                                             Promise<T8> promise8,
                                                                                                                                                             Promise<T9> promise9,
                                                                                                                                                             Promise<T10> promise10,
                                                                                                                                                             Promise<T11> promise11,
                                                                                                                                                             Promise<T12> promise12,
                                                                                                                                                             Promise<T13> promise13,
                                                                                                                                                             Promise<T14> promise14,
                                                                                                                                                             Promise<T15> promise15) {
        return () -> setupResult(values -> Result.all((Result<T1>) values[0],
                                                      (Result<T2>) values[1],
                                                      (Result<T3>) values[2],
                                                      (Result<T4>) values[3],
                                                      (Result<T5>) values[4],
                                                      (Result<T6>) values[5],
                                                      (Result<T7>) values[6],
                                                      (Result<T8>) values[7],
                                                      (Result<T9>) values[8],
                                                      (Result<T10>) values[9],
                                                      (Result<T11>) values[10],
                                                      (Result<T12>) values[11],
                                                      (Result<T13>) values[12],
                                                      (Result<T14>) values[13],
                                                      (Result<T15>) values[14])
                                                 .id(),
                                 promise1,
                                 promise2,
                                 promise3,
                                 promise4,
                                 promise5,
                                 promise6,
                                 promise7,
                                 promise8,
                                 promise9,
                                 promise10,
                                 promise11,
                                 promise12,
                                 promise13,
                                 promise14,
                                 promise15);
    }

    Promise<Unit>UNIT = Promise.resolved(unitResult());

    Result< ? >OTHER_SUCCEEDED = new CoreError.Cancelled("Cancelled because other Promise instance succeeded").result();

    CoreError.Cancelled PROMISE_CANCELLED = new CoreError.Cancelled("Promise cancelled");

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper1
    interface Mapper1<T1> {
        Promise<Tuple1<T1>> id();

        default <R> Promise<R> map(Fn1<R, T1> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn1<Promise<R>, T1> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper2
    interface Mapper2<T1, T2> {
        Promise<Tuple2<T1, T2>> id();

        default <R> Promise<R> map(Fn2<R, T1, T2> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn2<Promise<R>, T1, T2> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper3
    interface Mapper3<T1, T2, T3> {
        Promise<Tuple3<T1, T2, T3>> id();

        default <R> Promise<R> map(Fn3<R, T1, T2, T3> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn3<Promise<R>, T1, T2, T3> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper4
    interface Mapper4<T1, T2, T3, T4> {
        Promise<Tuple4<T1, T2, T3, T4>> id();

        default <R> Promise<R> map(Fn4<R, T1, T2, T3, T4> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn4<Promise<R>, T1, T2, T3, T4> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper5
    interface Mapper5<T1, T2, T3, T4, T5> {
        Promise<Tuple5<T1, T2, T3, T4, T5>> id();

        default <R> Promise<R> map(Fn5<R, T1, T2, T3, T4, T5> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn5<Promise<R>, T1, T2, T3, T4, T5> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper6
    interface Mapper6<T1, T2, T3, T4, T5, T6> {
        Promise<Tuple6<T1, T2, T3, T4, T5, T6>> id();

        default <R> Promise<R> map(Fn6<R, T1, T2, T3, T4, T5, T6> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn6<Promise<R>, T1, T2, T3, T4, T5, T6> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper7
    interface Mapper7<T1, T2, T3, T4, T5, T6, T7> {
        Promise<Tuple7<T1, T2, T3, T4, T5, T6, T7>> id();

        default <R> Promise<R> map(Fn7<R, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn7<Promise<R>, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper8
    interface Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> {
        Promise<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> id();

        default <R> Promise<R> map(Fn8<R, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper9
    interface Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        Promise<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> id();

        default <R> Promise<R> map(Fn9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper10
    interface Mapper10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> {
        Promise<Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> id();

        default <R> Promise<R> map(Fn10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn10<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper11
    interface Mapper11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> {
        Promise<Tuple11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> id();

        default <R> Promise<R> map(Fn11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn11<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper12
    interface Mapper12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> {
        Promise<Tuple12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>> id();

        default <R> Promise<R> map(Fn12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn12<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper13
    interface Mapper13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> {
        Promise<Tuple13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>> id();

        default <R> Promise<R> map(Fn13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn13<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper14
    interface Mapper14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> {
        Promise<Tuple14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>> id();

        default <R> Promise<R> map(Fn14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn14<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    /// Helper interface for convenient tuple transformation.
    ///
    /// @see Result.Mapper15
    interface Mapper15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> {
        Promise<Tuple15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>> id();

        default <R> Promise<R> map(Fn15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> mapper) {
            return id()
                   .map(tuple -> tuple.map(mapper));
        }

        default <R> Promise<R> flatMap(Fn15<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> mapper) {
            return id()
                   .flatMap(tuple -> tuple.map(mapper));
        }
    }

    private static <R> Promise<R> setupResult(FnX<Result<R>> transformer, Promise<?>... promises) {
        var promise = Promise.<R>promise();
        var collector = resultCollector(promises.length,
                                        values -> promise.resolve(transformer.apply(values)));
        int count = 0;
        for (var p : promises) {
            final var index = count++ ;
            p.withResult(result -> collector.registerEvent(index, result));
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
    void runAsync(TimeSpan delay, Runnable runnable) {
        runAsync(() -> {
            try{
                Thread.sleep(delay.duration());
            } catch (InterruptedException e) {}
            runnable.run();
        });
    }
}

final class PromiseImpl<T> implements Promise<T> {
    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private static final Logger log = LoggerFactory.getLogger(Promise.class);

    volatile Result<T> result;
    volatile Completion<T> stack;

    // Rely on default initialization to null
    PromiseImpl(Result<T> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return result == null
               ? "Promise<>"
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
        }else {
            push(new CompletionOnResult<>(action));
        }
        return this;
    }

    @Override
    public <U> Promise<U> fold(Fn1<Promise<U>, Result<T>> action) {
        if (result != null) {
            return action.apply(result);
        }else {
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
            var stackTraceElement = thread.getStackTrace() [2];
            log.trace("Thread {} ({}) is waiting for resolution of Promise {} at {}:{}",
                      thread.threadId(),
                      thread.getName(),
                      this,
                      stackTraceElement.getFileName(),
                      stackTraceElement.getLineNumber());
        }
        push(new CompletionJoin<>(thread));
        while (result == null) {
            LockSupport.park();
        }
        return result;
    }

    /// Await for resolution of current instance with specified timeout. Note that if timeout is expired, then the current instance remains unresolved.
    ///
    /// @param timeout Timeout to wait for resolution
    ///
    /// @return If the instance is resolved while waiting, then the result of resolution is returned. Otherwise, the Timeout error is returned.
    @Override
    public Result<T> await(TimeSpan timeout) {
        if (result != null) {
            return result;
        }
        var thread = Thread.currentThread();
        if (log.isTraceEnabled()) {
            var stackTraceElement = thread.getStackTrace() [2];
            log.trace("Thread {} ({}) is waiting for resolution of Promise {} at {}:{} for {}ns",
                      thread.threadId(),
                      thread.getName(),
                      this,
                      stackTraceElement.getFileName(),
                      stackTraceElement.getLineNumber(),
                      timeout.nanos());
        }
        push(new CompletionJoin<>(thread));
        var deadline = System.nanoTime() + timeout.nanos();
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
            do{
                processActions();
            }while (this.stack != null);
        }
        return this;
    }

    @SuppressWarnings("rawtypes")
    private void processActions() {
        Completion head;
        do{
            head = this.stack;
        }while (!STACK.compareAndSet(this, head, null));
        // Split all completions into three lists - joins, regular completions (dependent transformations), and
        // event handlers.
        // Regular completions are executed immediately, event processors executed asynchronously.
        // Joins are executed after all regular completions are done.
        CompletionJoin joins = null;
        CompletionOnResult events = null;
        CompletionFold actions = null;
        Completion current = head;
        Completion tmp;
        // Split and reverse the list in one pass
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
        do{
            if (result != null) {
                // In rare circumstances, when one thread resolves the instance while other tries to
                // add new independent completion, we resolve completion here. There might be chances that this might
                // lead to race condition if actions done by completions attached to this Promise have shared data.
                // Otherwise, the dependency chain is still maintained properly even with this invocation.
                completion.complete(result);
                return;
            }
            prevStack = stack;
            completion.next = prevStack;
        }while (!STACK.compareAndSet(this, prevStack, completion));
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
                var stackTraceElement = thread.getStackTrace() [2];
                log.trace("Unblocking thread {} ({}) after resolution of Promise with {} at {}:{}",
                          thread.threadId(),
                          thread.getName(),
                          value,
                          stackTraceElement.getFileName(),
                          stackTraceElement.getLineNumber());
            }
            LockSupport.unpark(thread);
        }
    }

    private static final VarHandle RESULT;
    private static final VarHandle STACK;

    static {
        try{
            var lookup = MethodHandles.lookup();
            RESULT = lookup.findVarHandle(PromiseImpl.class, "result", Result.class);
            STACK = lookup.findVarHandle(PromiseImpl.class, "stack", PromiseImpl.Completion.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        // Reduce the risk of rare disastrous classloading in the first call to
        // LockSupport.park: https://bugs.openjdk.org/browse/JDK-8074773
        @SuppressWarnings("unused")
        Class< ? > ensureLoaded = LockSupport.class;
    }
}
