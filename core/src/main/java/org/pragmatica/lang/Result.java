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
 */

package org.pragmatica.lang;

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.Result.Failure;
import org.pragmatica.lang.Result.Success;
import org.pragmatica.lang.utils.Causes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.pragmatica.lang.Result.unitResult;
import static org.pragmatica.lang.Tuple.*;
import static org.pragmatica.lang.Unit.unit;


/// Representation of the operation result. The result can be either success or failure. In case of success it holds value returned by the operation.
/// In case of failure it holds a failure description.
///
/// @param <T> Type of value in case of success.
@SuppressWarnings("unused")
public sealed interface Result<T> permits Success, Failure {
    /// Transform the operation result value into a value of another type and wrap the new value into [Result]. Transformation takes place if the current
    /// instance (this) contains a successful result, otherwise the current instance remains unchanged and the transformation function is not invoked.
    ///
    /// @param mapper Function to transform successful value
    ///
    /// @return transformed value (in case of success) or current instance (in case of failure)
    @SuppressWarnings("unchecked")
    default <U> Result<U> map(Fn1<U, ? super T> mapper) {
        return fold(_ -> (Result<U>) this, r -> success(mapper.apply(r)));
    }

    /// Replace a successful result with the value obtained from the provided supplier.
    ///
    /// @param supplier Source of the replacement value
    ///
    /// @return replaced value (in case of success) or current instance (in case of failure)
    default <U> Result<U> map(Supplier<U> supplier) {
        return map(_ -> supplier.get());
    }

    /// Transform an operation result into another operation result. In case if the current instance (this) is an error, the transformation function is not
    /// invoked and the value remains the same.
    ///
    /// @param mapper Function to apply to result
    ///
    /// @return transformed value (in case of success) or current instance (in case of failure)
    @SuppressWarnings("unchecked")
    default <U> Result<U> flatMap(Fn1<Result<U>, ? super T> mapper) {
        return fold(_ -> (Result<U>) this, mapper);
    }

    /// Version of the [#flatMap(Fn1)] which allows convenient "mixing in" additional parameter without the need to revert
    /// to traditional lambda.
    ///
    /// @param mapper     Mapping function that takes the success value and an additional parameter
    /// @param parameter2 Additional parameter to pass to the mapping function
    /// @param <U>        The type of the new result value
    /// @param <I>        The type of the additional parameter
    ///
    /// @return New result instance
    default <U, I> Result<U> flatMap2(Fn2<Result<U>, ? super T, ? super I> mapper, I parameter2) {
        return flatMap(value -> mapper.apply(value, parameter2));
    }

    /// Replace the current instance with the instance returned by provided [Supplier]. The replacement happens only if the current instance contains
    /// a successful result, otherwise the current instance remains unchanged.
    ///
    /// @param mapper Source of the replacement result.
    ///
    /// @return replacement result (in case of success) or current instance (in case of failure)
    @SuppressWarnings("unchecked")
    default <U> Result<U> flatMap(Supplier<Result<U>> mapper) {
        return fold(_ -> (Result<U>) this, _ -> mapper.get());
    }

    /// Transform the cause of the failure.
    ///
    /// @param mapper Function to transform failure cause
    ///
    /// @return current instance (in case of success) or transformed instance (in case of failure)
    default Result<T> mapError(Fn1<Cause, ? super Cause> mapper) {
        return fold(cause -> mapper.apply(cause).result(), _ -> this);
    }

    /// Add tracing information to the failure cause.
    ///
    /// @return current instance (in case of success) or instance with tracing information (in case of failure)
    Result<T> trace();

    /// Recover from failure by transforming failure cause into new value.
    ///
    /// @param mapper Function to transform failure cause
    ///
    /// @return current instance (in case of success) or transformed instance (in case of failure)
    default Result<T> recover(Fn1<T, ? super Cause> mapper) {
        return fold(cause -> success(mapper.apply(cause)), _ -> this);
    }

    /// Apply consumers to result value. Note that depending on the result, success or failure, only one consumer will be applied at a time.
    ///
    /// @param failureConsumer Consumer for failure result
    /// @param successConsumer Consumer for success result
    ///
    /// @return current instance
    default Result<T> apply(Consumer<? super Cause> failureConsumer, Consumer<? super T> successConsumer) {
        return fold(t -> {
            failureConsumer.accept(t);
            return this;
        }, t -> {
            successConsumer.accept(t);
            return this;
        });
    }

    /// Pass successful operation result value into provided consumer.
    ///
    /// @param consumer Consumer to pass value to
    ///
    /// @return current instance for fluent call chaining
    default Result<T> onSuccess(Consumer<T> consumer) {
        fold(Functions::toNull, v -> {
            consumer.accept(v);
            return null;
        });
        return this;
    }

    /// Run provided action in case of success.
    ///
    /// @return current instance for fluent call chaining
    default Result<T> onSuccessRun(Runnable action) {
        fold(Functions::toNull, _ -> {
            action.run();
            return null;
        });
        return this;
    }

    /// Pass failure operation result value into provided consumer.
    ///
    /// @param consumer Consumer to pass value to
    ///
    /// @return current instance for fluent call chaining
    default Result<T> onFailure(Consumer<? super Cause> consumer) {
        fold(v -> {
            consumer.accept(v);
            return null;
        }, Functions::toNull);
        return this;
    }

    /// Run provided action in case of failure.
    ///
    /// @return current instance for fluent call chaining
    default Result<T> onFailureRun(Runnable action) {
        fold(_ -> {
            action.run();
            return null;
        }, Functions::toNull);
        return this;
    }

    /// Convert an instance into [Option] of the same value type. Successful instance is converted into present [Option] and failure - into
    /// empty [Option]. Note that during such a conversion error information is getting lost.
    ///
    /// @return [Option] instance which is present in case of success and missing in case of failure.
    default Option<T> option() {
        return fold(_ -> Option.empty(), Option::option);
    }

    /// Check if instance is success.
    ///
    /// @return `true` if instance is success and `false` otherwise
    default boolean isSuccess() {
        return fold(Functions::toFalse, Functions::toTrue);
    }

    /// Check if an instance is failure.
    ///
    /// @return `true` if instance is failure and `false` otherwise
    default boolean isFailure() {
        return fold(Functions::toTrue, Functions::toFalse);
    }

    /// Stream current instance. For failure instance, an empty stream is created. For success instance,
    /// the stream with a single element is returned. The element is the value stored in
    /// the current instance.
    ///
    /// @return created stream
    default Stream<T> stream() {
        return fold(_ -> Stream.empty(), Stream::of);
    }

    /// Filter instance against provided predicate. If the predicate returns `true`, then the instance remains unchanged. If the predicate returns
    /// `false`, then a failure instance is created using given [Cause].
    ///
    /// @param cause     failure to use in case if predicate returns `false`
    /// @param predicate predicate to invoke
    ///
    /// @return current instance if predicate returns `true` or [Failure] instance if predicate returns `false`
    default Result<T> filter(Cause cause, Predicate<T> predicate) {
        return fold(_ -> this, v -> predicate.test(v) ? this : failure(cause));
    }

    /// Filter instance against provided predicate. If the predicate returns `true`, then the instance remains unchanged. If the predicate returns
    /// `false`, then a failure instance is created using [Cause] created by the provided function.
    ///
    /// @param causeMapper function which transforms the tested value into an instance of [Cause] if predicate returns `false`
    /// @param predicate   predicate to invoke
    ///
    /// @return current instance if predicate returns `true` or [Failure] instance if predicate returns `false`
    default Result<T> filter(Fn1<Cause, T> causeMapper, Predicate<T> predicate) {
        return fold(_ -> this, v -> predicate.test(v) ? this : failure(causeMapper.apply(v)));
    }

    /// Return value store in the current instance (if this instance represents a successful result) or provided replacement value.
    ///
    /// @param replacement replacement value returned if current instance represents failure.
    ///
    /// @return value stored in the current instance (in case of success) or replacement value.
    default T or(T replacement) {
        return fold(_ -> replacement, Functions::id);
    }

    /// Return value store in the current instance (if this instance represents a successful result) or value returned by the provided supplier.
    ///
    /// @param supplier source of replacement value returned if current instance represents failure.
    ///
    /// @return value stored in the current instance (in case of success) or replacement value.
    default T or(Supplier<T> supplier) {
        return fold(_ -> supplier.get(), Functions::id);
    }

    /// Return current instance if this instance represents successful result or replacement instance if current instance represents a failure.
    ///
    /// @param replacement replacement instance returned if current instance represents failure.
    ///
    /// @return current instance (in case of success) or replacement instance.
    default Result<T> orElse(Result<T> replacement) {
        return fold(_ -> replacement, _ -> this);
    }

    /// Return the current instance if this instance represents a successful result or instance returned by provided supplier if current instance represents
    /// a failure.
    ///
    /// @param supplier source of replacement instance returned if current instance represents failure.
    ///
    /// @return current instance (in case of success) or replacement instance.
    default Result<T> orElse(Supplier<Result<T>> supplier) {
        return fold(_ -> supplier.get(), _ -> this);
    }

    default Result<T> onResult(Consumer<Result<T>> consumer) {
        consumer.accept(this);
        return this;
    }

    default Result<T> onResultRun(Runnable runnable) {
        return onResult(_ -> runnable.run());
    }

    /// This method allows "unwrapping" the value stored inside the Result instance. If a value is missing, then [IllegalStateException] is thrown.
    ///
    /// WARNING!!!
    /// This method should be avoided in the production code. Its main intended use case - simplification of the tests. For this reason
    ///  the method is marked as [Deprecated]. This generates a warning at compile time.
    ///
    /// @return value stored inside present instance.
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    default T unwrap() {
        return fold(v -> {throw new IllegalStateException("Unwrap error: " + v.message());}, Functions::id);
    }

    /// Handle both possible states (success/failure) and produce a single value from it.
    ///
    /// @param failureMapper function to transform failure into value
    /// @param successMapper function to transform success into value
    ///
    /// @return the result transformed by the one of the mappers.
    <U> U fold(Fn1<? extends U, ? super Cause> failureMapper, Fn1<? extends U, ? super T> successMapper);

    default Result<Unit> mapToUnit() {
        return map(Unit::toUnit);
    }

    /// Convert the current instance into [Promise] instance resolved with the current instance.
    ///
    /// @return created promise
    default Promise<T> async() {
        return Promise.resolved(this);
    }

    Result<Unit> UNIT_RESULT = success(unit());

    /// A constant instance of a successful instance holding [Unit] value.
    ///
    /// @return instance of a successful result with [Unit] value
    static Result<Unit> unitResult() {
        return UNIT_RESULT;
    }

    /// Create an instance of a successful operation result.
    ///
    /// @param value Operation result
    ///
    /// @return created instance
    static <U> Result<U> success(U value) {
        return new Success<>(value);
    }

    static <U> Result<U> ok(U value) {
        return success(value);
    }

    record Success<T>(T value) implements Result<T> {
        @Override
        public <U> U fold(Fn1<? extends U, ? super Cause> failureMapper, Fn1<? extends U, ? super T> successMapper) {
            return successMapper.apply(value);
        }

        @Override
        public String toString() {
            return "Success(" + value.toString() + ")";
        }

        @Override
        public Result<T> trace() {
            return this;
        }
    }

    /// Create an instance of a failure result.
    ///
    /// @param value Operation error value
    ///
    /// @return created instance
    static <U> Result<U> failure(Cause value) {
        return new Failure<>(value);
    }

    static <U> Result<U> err(Cause value) {
        return new Failure<>(value);
    }

    record Failure<T>(Cause cause) implements Result<T> {
        private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

        @Override
        public <U> U fold(Fn1<? extends U, ? super Cause> failureMapper, Fn1<? extends U, ? super T> successMapper) {
            return failureMapper.apply(cause);
        }

        @Override
        public String toString() {
            return "Failure(" + cause.toString() + ")";
        }

        @Override
        public Result<T> trace() {
            var text = WALKER.walk(frames ->
                                           frames.skip(1)
                                                 .findFirst()
                                                 .map(StackWalker.StackFrame::toString)
                                                 .orElse("<unknown>"));

            return mapError(cause -> Causes.CompositeCause.toComposite(text, cause));
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // Interaction with legacy code
    //------------------------------------------------------------------------------------------------------------------

    /// Wrap value returned by provided lambda into success [Result] if the call succeeds or into failure [Result] if call throws exception.
    ///
    /// @param exceptionMapper the function which will transform exception into instance of [Cause]
    /// @param supplier        the call to wrap
    ///
    /// @return result transformed by the provided lambda and wrapped into [Result]
    static <U> Result<U> lift(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn0<U> supplier) {
        try {
            return success(supplier.apply());
        } catch (Throwable e) {
            return failure(exceptionMapper.apply(e));
        }
    }

    /// Convenience method for directly invoking a throwing unary function and wrapping the result in a Result.
    /// This method provides immediate invocation rather than returning a function factory.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing unary function to invoke
    /// @param value1          The parameter value to pass to the function
    /// @param <U>             The return type of the function
    /// @param <T1>            The type of the parameter
    ///
    /// @return A Result that contains either the function result or failure
    static <U, T1> Result<U> lift1(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn1<U, T1> function, T1 value1) {
        return lift(exceptionMapper, () -> function.apply(value1));
    }

    /// Same as [#lift1(Fn1, ThrowingFn1, Object)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function   The throwing unary function to invoke
    /// @param inputValue The parameter value to pass to the function
    /// @param <R>        The return type of the function
    /// @param <T>        The type of the parameter
    ///
    /// @return A Result that contains either the function result or failure
    static <R, T> Result<R> lift1(ThrowingFn1<R, T> function, T inputValue) {
        return lift1(Causes::fromThrowable, function, inputValue);
    }

    /// Convenience method for directly invoking a throwing binary function and wrapping the result in a Result.
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
    /// @return A Result that contains either the function result or failure
    static <U, T1, T2> Result<U> lift2(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn2<U, T1, T2> function, T1 value1, T2 value2) {
        return lift(exceptionMapper, () -> function.apply(value1, value2));
    }

    /// Same as [#lift2(Fn1, ThrowingFn2, Object, Object)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function     The throwing binary function to invoke
    /// @param inputValue1  The first parameter value to pass to the function
    /// @param inputValue2  The second parameter value to pass to the function
    /// @param <R>          The return type of the function
    /// @param <T1>         The type of the first parameter
    /// @param <T2>         The type of the second parameter
    ///
    /// @return A Result that contains either the function result or failure
    static <R, T1, T2> Result<R> lift2(ThrowingFn2<R, T1, T2> function, T1 inputValue1, T2 inputValue2) {
        return lift2(Causes::fromThrowable, function, inputValue1, inputValue2);
    }

    /// Convenience method for directly invoking a throwing ternary function and wrapping the result in a Result.
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
    /// @return A Result that contains either the function result or failure
    static <U, T1, T2, T3> Result<U> lift3(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn3<U, T1, T2, T3> function, T1 value1, T2 value2, T3 value3) {
        return lift(exceptionMapper, () -> function.apply(value1, value2, value3));
    }

    /// Same as [#lift3(Fn1, ThrowingFn3, Object, Object, Object)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function     The throwing ternary function to invoke
    /// @param inputValue1  The first parameter value to pass to the function
    /// @param inputValue2  The second parameter value to pass to the function
    /// @param inputValue3  The third parameter value to pass to the function
    /// @param <R>          The return type of the function
    /// @param <T1>         The type of the first parameter
    /// @param <T2>         The type of the second parameter
    /// @param <T3>         The type of the third parameter
    ///
    /// @return A Result that contains either the function result or failure
    static <R, T1, T2, T3> Result<R> lift3(ThrowingFn3<R, T1, T2, T3> function, T1 inputValue1, T2 inputValue2, T3 inputValue3) {
        return lift3(Causes::fromThrowable, function, inputValue1, inputValue2, inputValue3);
    }

    /// Wrap the call to the provided lambda into success [Result] if the call succeeds or into failure [Result] if call throws exception.
    ///
    /// @param exceptionMapper the function which will transform exception into instance of [Cause]
    /// @param runnable        the call to wrap
    ///
    /// @return Unit result which is success if no exceptions were thrown or failure otherwise
    static Result<Unit> lift(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingRunnable runnable) {
        try {
            runnable.run();
            return unitResult();
        } catch (Throwable e) {
            return failure(exceptionMapper.apply(e));
        }
    }

    /// Similar to [#lift(Fn1,ThrowingFn0)] but with a fixed [Cause] instance.
    ///
    /// @param cause    the cause to use in case of failure
    /// @param supplier the call to wrap
    ///
    /// @return result transformed by the provided lambda and wrapped into [Result]
    static <U> Result<U> lift(Cause cause, ThrowingFn0<U> supplier) {
        return lift(_ -> cause, supplier);
    }

    /// Similar to [#lift(Fn1,ThrowingFn0)] but with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param supplier the call to wrap
    ///
    /// @return result transformed by the provided lambda and wrapped into [Result]
    static <U> Result<U> lift(ThrowingFn0<U> supplier) {
        return lift(Causes::fromThrowable, supplier);
    }

    /// Similar to [#lift(Fn1,ThrowingRunnable)] but with a fixed [Cause] instance.
    ///
    /// @param cause    the cause to use in case of failure
    /// @param runnable the call to wrap
    ///
    /// @return Unit result which is success if no exceptions were thrown or failure otherwise
    static Result<Unit> lift(Cause cause, ThrowingRunnable runnable) {
        return lift(_ -> cause, runnable);
    }

    /// Similar to [#lift(Fn1,ThrowingRunnable)] but with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param runnable the call to wrap
    ///
    /// @return Unit result which is success if no exceptions were thrown or failure otherwise
    static Result<Unit> lift(ThrowingRunnable runnable) {
        return lift(Causes::fromThrowable, runnable);
    }

    /// Wrap the call to the provided function into success [Result] if the call succeeds of into failure [Result] if call throws exception.
    ///
    /// @param exceptionMapper the function which will transform exception into instance of [Cause]
    /// @param function        the function to call
    ///
    /// @return invocation outcome wrapped into [Result]
    /// Convenience method for creating a unary function that wraps a throwing function and returns a Result.
    /// This is a function factory that creates reusable unary functions for result-based operations.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing unary function to wrap
    /// @param <R>             The return type of the function
    /// @param <T1>            The type of the parameter
    ///
    /// @return A unary function that takes one parameter and returns a Result
    static <R, T1> Fn1<Result<R>, T1> liftFn1(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn1<R, T1> function) {
        return input -> lift(exceptionMapper, () -> function.apply(input));
    }

    /// Convenience method for creating a binary function that wraps a throwing function and returns a Result.
    /// This is a function factory that creates reusable binary functions for result-based operations.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing binary function to wrap
    /// @param <R>             The return type of the function
    /// @param <T1>            The type of the first parameter
    /// @param <T2>            The type of the second parameter
    ///
    /// @return A binary function that takes two parameters and returns a Result
    static <R, T1, T2> Fn2<Result<R>, T1, T2> liftFn2(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn2<R, T1, T2> function) {
        return (inputValue1, inputValue2) -> lift(exceptionMapper, () -> function.apply(inputValue1, inputValue2));
    }

    /// Convenience method for creating a ternary function that wraps a throwing function and returns a Result.
    /// This is a function factory that creates reusable ternary functions for result-based operations.
    ///
    /// @param exceptionMapper Function to convert exceptions to Cause instances
    /// @param function        The throwing ternary function to wrap
    /// @param <R>             The return type of the function
    /// @param <T1>            The type of the first parameter
    /// @param <T2>            The type of the second parameter
    /// @param <T3>            The type of the third parameter
    ///
    /// @return A ternary function that takes three parameters and returns a Result
    static <R, T1, T2, T3> Fn3<Result<R>, T1, T2, T3> liftFn3(Fn1<? extends Cause, ? super Throwable> exceptionMapper, ThrowingFn3<R, T1, T2, T3> function) {
        return (inputValue1, inputValue2, inputValue3) -> lift(exceptionMapper, () -> function.apply(inputValue1, inputValue2, inputValue3));
    }

    /// Same as [#liftFn2(Fn1, ThrowingFn2)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing binary function to wrap
    /// @param <R>      The return type of the function
    /// @param <T1>     The type of the first parameter
    /// @param <T2>     The type of the second parameter
    ///
    /// @return A binary function that takes two parameters and returns a Result
    static <R, T1, T2> Fn2<Result<R>, T1, T2> liftFn2(ThrowingFn2<R, T1, T2> function) {
        return liftFn2(Causes::fromThrowable, function);
    }

    /// Same as [#liftFn3(Fn1, ThrowingFn3)] with [Causes#fromThrowable(Throwable)] used for exception mapping.
    ///
    /// @param function The throwing ternary function to wrap
    /// @param <R>      The return type of the function
    /// @param <T1>     The type of the first parameter
    /// @param <T2>     The type of the second parameter
    /// @param <T3>     The type of the third parameter
    ///
    /// @return A ternary function that takes three parameters and returns a Result
    static <R, T1, T2, T3> Fn3<Result<R>, T1, T2, T3> liftFn3(ThrowingFn3<R, T1, T2, T3> function) {
        return liftFn3(Causes::fromThrowable, function);
    }

    //------------------------------------------------------------------------------------------------------------------
    // Predicates
    //------------------------------------------------------------------------------------------------------------------

    /// Find and return the first success instance among provided.
    ///
    /// @param first   first input result
    /// @param results remaining input results
    ///
    /// @return first success instance among provided
    @SafeVarargs
    static <T> Result<T> any(Result<T> first, Result<T>... results) {
        if (first.isSuccess()) {
            return first;
        }

        for (var result : results) {
            if (result.isSuccess()) {
                return result;
            }
        }

        return first;
    }

    /// Lazy version of the [#any(Result, Result\[\])].
    ///
    /// @param first     first instance to check
    /// @param suppliers suppliers which provide remaining instances for check
    ///
    /// @return first success instance among provided
    @SafeVarargs
    static <T> Result<T> any(Result<T> first, Supplier<Result<T>>... suppliers) {
        if (first.isSuccess()) {
            return first;
        }

        for (var supplier : suppliers) {
            var result = supplier.get();

            if (result.isSuccess()) {
                return result;
            }
        }

        return first;
    }

    /// Transform the stream of [Result] instances into [Result] with the list of values.
    ///
    /// @param results input stream of [Result] instances
    ///
    /// @return success instance if all [Result] instances in the list are successes or failure instance if any instance in this list is a failure
    static <T> Result<List<T>> allOf(Stream<Result<T>> results) {
        var causes = Causes.composite();
        var values = new ArrayList<T>();

        results.forEach(val -> val.fold(causes::append, values::add));

        return causes.isEmpty() ? success(values) : failure(causes);
    }

    /// Transform provided [Result] instances into [Result] with the list of values.
    ///
    /// @param results input stream of [Result] instances
    ///
    /// @return success instance if all [Result] instances in this list are successes or failure instance if any instance in this list is a failure
    @SafeVarargs
    static <T> Result<List<T>> allOf(Result<T>... results) {
        return allOf(List.of(results));
    }

    /// Transform the list of [Result] instances into [Result] with the list of values.
    ///
    /// @param results input list of [Result] instances
    ///
    /// @return success instance if all [Result] instances in the list are successes or failure instance if any instance in this list is a failure
    static <T> Result<List<T>> allOf(List<Result<T>> results) {
        var causes = Causes.composite();
        var values = new ArrayList<T>();

        for (var value : results) {
            value.onFailure(causes::append)
                 .onSuccess(values::add);
        }

        return causes.isEmpty() ? success(values) : failure(causes);
    }

    /// Transform provided results into a single result containing the tuple of values.
    /// The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper1] prepared for further transformation.
    static <T1> Mapper1<T1> all(Result<T1> value) {
        var causes = Causes.composite();

        return () -> value.flatMap(vv1 -> success(tuple(vv1)))
                          .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper2] prepared for further transformation.
    static <T1, T2> Mapper2<T1, T2> all(Result<T1> value1, Result<T2> value2) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> success(tuple(vv1, vv2)))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper3] prepared for further transformation.
    static <T1, T2, T3> Mapper3<T1, T2, T3> all(Result<T1> value1, Result<T2> value2, Result<T3> value3) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> value3.flatMap(
                                                                 vv3 -> success(tuple(vv1, vv2, vv3)))
                                                             .mapError(causes::append))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper4] prepared for further transformation.
    static <T1, T2, T3, T4> Mapper4<T1, T2, T3, T4> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3, Result<T4> value4
    ) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> value3.flatMap(
                                                                 vv3 -> value4.flatMap(
                                                                                  vv4 -> success(tuple(vv1, vv2, vv3, vv4)))
                                                                              .mapError(causes::append))
                                                             .mapError(causes::append))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper5] prepared for further transformation.
    static <T1, T2, T3, T4, T5> Mapper5<T1, T2, T3, T4, T5> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3, Result<T4> value4, Result<T5> value5
    ) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> value3.flatMap(
                                                                 vv3 -> value4.flatMap(
                                                                                  vv4 -> value5.flatMap(
                                                                                                   vv5 -> success(tuple(vv1, vv2, vv3, vv4, vv5)))
                                                                                               .mapError(causes::append))
                                                                              .mapError(causes::append))
                                                             .mapError(causes::append))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper6] prepared for further transformation.
    static <T1, T2, T3, T4, T5, T6> Mapper6<T1, T2, T3, T4, T5, T6> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6
    ) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> value3.flatMap(
                                                                 vv3 -> value4.flatMap(
                                                                                  vv4 -> value5.flatMap(
                                                                                                   vv5 -> value6.flatMap(
                                                                                                                    vv6 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6)))
                                                                                                                .mapError(causes::append))
                                                                                               .mapError(causes::append))
                                                                              .mapError(causes::append))
                                                             .mapError(causes::append))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper7] prepared for further transformation.
    static <T1, T2, T3, T4, T5, T6, T7> Mapper7<T1, T2, T3, T4, T5, T6, T7> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6,
        Result<T7> value7
    ) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> value3.flatMap(
                                                                 vv3 -> value4.flatMap(
                                                                                  vv4 -> value5.flatMap(
                                                                                                   vv5 -> value6.flatMap(
                                                                                                                    vv6 -> value7.flatMap(
                                                                                                                                     vv7 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7)))
                                                                                                                                 .mapError(causes::append))
                                                                                                                .mapError(causes::append))
                                                                                               .mapError(causes::append))
                                                                              .mapError(causes::append))
                                                             .mapError(causes::append))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper8] prepared for further transformation.
    static <T1, T2, T3, T4, T5, T6, T7, T8> Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6,
        Result<T7> value7, Result<T8> value8
    ) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> value3.flatMap(
                                                                 vv3 -> value4.flatMap(
                                                                                  vv4 -> value5.flatMap(
                                                                                                   vv5 -> value6.flatMap(
                                                                                                                    vv6 -> value7.flatMap(
                                                                                                                                     vv7 -> value8.flatMap(
                                                                                                                                                      vv8 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7, vv8)))
                                                                                                                                                  .mapError(causes::append))
                                                                                                                                 .mapError(causes::append))
                                                                                                                .mapError(causes::append))
                                                                                               .mapError(causes::append))
                                                                              .mapError(causes::append))
                                                             .mapError(causes::append))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Transform provided results into the single result containing a tuple of values. The result is failure if any input result is failure. Otherwise,
    /// the returned instance contains a tuple with values from input results.
    ///
    /// @return [Mapper9] prepared for further transformation.
    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> all(
        Result<T1> value1, Result<T2> value2, Result<T3> value3,
        Result<T4> value4, Result<T5> value5, Result<T6> value6,
        Result<T7> value7, Result<T8> value8, Result<T9> value9
    ) {
        var causes = Causes.composite();

        return () -> value1.flatMap(
                               vv1 -> value2.flatMap(
                                                vv2 -> value3.flatMap(
                                                                 vv3 -> value4.flatMap(
                                                                                  vv4 -> value5.flatMap(
                                                                                                   vv5 -> value6.flatMap(
                                                                                                                    vv6 -> value7.flatMap(
                                                                                                                                     vv7 -> value8.flatMap(
                                                                                                                                                      vv8 -> value9.flatMap(
                                                                                                                                                                       vv9 -> success(tuple(vv1, vv2, vv3, vv4, vv5, vv6, vv7, vv8, vv9)))
                                                                                                                                                                   .mapError(causes::append))
                                                                                                                                                  .mapError(causes::append))
                                                                                                                                 .mapError(causes::append))
                                                                                                                .mapError(causes::append))
                                                                                               .mapError(causes::append))
                                                                              .mapError(causes::append))
                                                             .mapError(causes::append))
                                            .mapError(causes::append))
                           .mapError(causes::append);
    }

    /// Helper interface for convenient [Tuple1] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper1<T1> {
        Result<Tuple1<T1>> id();

        default <R> Result<R> map(Fn1<R, T1> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn1<Result<R>, T1> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper1<T1> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple2] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper2<T1, T2> {
        Result<Tuple2<T1, T2>> id();

        default <R> Result<R> map(Fn2<R, T1, T2> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn2<Result<R>, T1, T2> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper2<T1, T2> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple3] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper3<T1, T2, T3> {
        Result<Tuple3<T1, T2, T3>> id();

        default <R> Result<R> map(Fn3<R, T1, T2, T3> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn3<Result<R>, T1, T2, T3> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper3<T1, T2, T3> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple4] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper4<T1, T2, T3, T4> {
        Result<Tuple4<T1, T2, T3, T4>> id();

        default <R> Result<R> map(Fn4<R, T1, T2, T3, T4> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn4<Result<R>, T1, T2, T3, T4> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper4<T1, T2, T3, T4> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple5] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper5<T1, T2, T3, T4, T5> {
        Result<Tuple5<T1, T2, T3, T4, T5>> id();

        default <R> Result<R> map(Fn5<R, T1, T2, T3, T4, T5> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn5<Result<R>, T1, T2, T3, T4, T5> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper5<T1, T2, T3, T4, T5> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple6] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper6<T1, T2, T3, T4, T5, T6> {
        Result<Tuple6<T1, T2, T3, T4, T5, T6>> id();

        default <R> Result<R> map(Fn6<R, T1, T2, T3, T4, T5, T6> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn6<Result<R>, T1, T2, T3, T4, T5, T6> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper6<T1, T2, T3, T4, T5, T6> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple7] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper7<T1, T2, T3, T4, T5, T6, T7> {
        Result<Tuple7<T1, T2, T3, T4, T5, T6, T7>> id();

        default <R> Result<R> map(Fn7<R, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn7<Result<R>, T1, T2, T3, T4, T5, T6, T7> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper7<T1, T2, T3, T4, T5, T6, T7> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple8] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> {
        Result<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> id();

        default <R> Result<R> map(Fn8<R, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn8<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper8<T1, T2, T3, T4, T5, T6, T7, T8> async() {
            return () -> Promise.resolved(id());
        }
    }

    /// Helper interface for convenient [Tuple9] transformation. In case if you need to return a tuple, it might be more convenient to return
    /// this interface instead. For example, instead of this:
    /// <blockquote><pre>
    ///     return tuple(value, ...);
    /// </pre></blockquote>
    /// return this:
    /// <blockquote><pre>
    ///     return () -> tuple(value, ...);
    /// </pre></blockquote>
    interface Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        Result<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> id();

        default <R> Result<R> map(Fn9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().map(tuple -> tuple.map(mapper));
        }

        default <R> Result<R> flatMap(Fn9<Result<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> mapper) {
            return id().flatMap(tuple -> tuple.map(mapper));
        }

        default Promise.Mapper9<T1, T2, T3, T4, T5, T6, T7, T8, T9> async() {
            return () -> Promise.resolved(id());
        }
    }
}
