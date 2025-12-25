package org.pragmatica.lang;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn2;
import org.pragmatica.lang.Functions.Fn3;
import org.pragmatica.lang.utils.Causes;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/// A utility interface that provides validation functionality to ensure values meet specific criteria.
///
/// The `Verify` interface encapsulates methods for validating values against predicates and
/// returning appropriate `Result` objects that indicate success or failure. This interface works
/// in conjunction with the nested `Is` interface that provides a rich set of predicate functions.
///
/// Example usage:
/// ```java
/// // Simple predicate validation
/// Result<Integer> result = Verify.ensure(42, value -> value > 0);
/// // Using Verify.Is predicates
/// Result<Integer> result = Verify.ensure(42, Verify.Is::greaterThan, 0);
/// // Complex condition with multiple parameters
/// Result<Integer> result = Verify.ensure(42, Verify.Is::between, 0, 100);
/// ```
public sealed interface Verify {

    //------------------------------------------------------------------------------------------------------------------
    // Unary predicate variants
    //------------------------------------------------------------------------------------------------------------------

    /// Ensures that a value satisfies a given predicate.
    ///
    /// This method evaluates the provided value against the given predicate. If the predicate returns
    /// true, a success result containing the original value is returned. Otherwise, a failure result
    /// is returned with a generic error message.
    ///
    /// @param value     the value to verify
    /// @param predicate the predicate to test the value against
    /// @param <T>       the type of the value being verified
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result if the predicate is not satisfied
    static <T> Result<T> ensure(T value, Predicate<T> predicate) {
        return ensure(value, predicate, Causes.forOneValue("Value %s does not satisfy the predicate"));
    }

    /// Ensures that a value satisfies a given predicate, with a fixed cause on failure.
    ///
    /// @param value     the value to verify
    /// @param predicate the predicate to test the value against
    /// @param cause     the cause to use if validation fails
    /// @param <T>       the type of the value being verified
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the specified cause if not
    static <T> Result<T> ensure(T value, Predicate<T> predicate, Cause cause) {
        return ensure(value, predicate, _ -> cause);
    }

    /// Ensures that a value satisfies a given predicate, with a custom cause provider.
    ///
    /// This method allows specifying a custom function to generate error causes based on the input value,
    /// providing more context-aware error messages.
    ///
    /// @param value         the value to verify
    /// @param predicate     the predicate to test the value against
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param <T>           the type of the value being verified
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the generated cause if not
    static <T> Result<T> ensure(T value, Predicate<T> predicate, Fn1<Cause, T> causeProvider) {
        if (predicate.test(value)) {
            return Result.success(value);
        }

        return causeProvider.apply(value).result();
    }

    /// Ensures that a value satisfies a given predicate, with a fixed cause on failure.
    ///
    /// @param cause     the cause to use if validation fails
    /// @param value     the value to verify
    /// @param predicate the predicate to test the value against
    /// @param <T>       the type of the value being verified
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the specified cause if not
    ///
    /// @deprecated Use {@link #ensure(Object, Predicate, Cause)} instead for more natural parameter order.
    @Deprecated(forRemoval = true)
    static <T> Result<T> ensure(Cause cause, T value, Predicate<T> predicate) {
        return ensure(value, predicate, cause);
    }

    /// Ensures that a value satisfies a given predicate, with a custom cause provider.
    ///
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param value         the value to verify
    /// @param predicate     the predicate to test the value against
    /// @param <T>           the type of the value being verified
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the generated cause if not
    ///
    /// @deprecated Use {@link #ensure(Object, Predicate, Fn1)} instead for more natural parameter order.
    @Deprecated(forRemoval = true)
    static <T> Result<T> ensure(Fn1<Cause, T> causeProvider, T value, Predicate<T> predicate) {
        return ensure(value, predicate, causeProvider);
    }

    //------------------------------------------------------------------------------------------------------------------
    // Binary predicate variants (predicate with one additional parameter)
    //------------------------------------------------------------------------------------------------------------------

    /// Ensures that a value satisfies a binary predicate with one additional parameter.
    ///
    /// This method is a convenience wrapper allowing the use of a predicate that takes two parameters:
    /// the value being tested and an additional parameter.
    ///
    /// @param value     the value to verify
    /// @param predicate the binary predicate to test the value against
    /// @param param1    the additional parameter to pass to the predicate
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result if the predicate is not satisfied
    static <T, P1> Result<T> ensure(T value, Fn2<Boolean, T, P1> predicate, P1 param1) {
        return ensure(value, v -> predicate.apply(v, param1));
    }

    /// Ensures that a value satisfies a binary predicate, with a fixed cause on failure.
    /// This is an alias with cause at the end of the parameter list for more natural reading.
    ///
    /// @param value     the value to verify
    /// @param predicate the binary predicate to test the value against
    /// @param param1    the additional parameter to pass to the predicate
    /// @param cause     the cause to use if validation fails
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the specified cause if not
    static <T, P1> Result<T> ensure(T value, Fn2<Boolean, T, P1> predicate, P1 param1, Cause cause) {
        return ensure(value, predicate, param1, _ -> cause);
    }

    /// Ensures that a value satisfies a binary predicate, with a custom cause provider.
    /// This is an alias with cause provider at the end of the parameter list for more natural reading.
    ///
    /// @param value         the value to verify
    /// @param predicate     the binary predicate to test the value against
    /// @param param1        the additional parameter to pass to the predicate
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param <T>           the type of the value being verified
    /// @param <P1>          the type of the additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the generated cause if not
    static <T, P1> Result<T> ensure(T value, Fn2<Boolean, T, P1> predicate, P1 param1, Fn1<Cause, T> causeProvider) {
        return ensure(value, v -> predicate.apply(v, param1), causeProvider);
    }

    /// Ensures that a value satisfies a binary predicate, with a fixed cause on failure.
    ///
    /// @param cause     the cause to use if validation fails
    /// @param value     the value to verify
    /// @param predicate the binary predicate to test the value against
    /// @param param1    the additional parameter to pass to the predicate
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the specified cause if not
    ///
    /// @deprecated Use {@link #ensure(Object, Fn2, Object, Cause)} instead for more natural parameter order.
    @Deprecated(forRemoval = true)
    static <T, P1> Result<T> ensure(Cause cause, T value, Fn2<Boolean, T, P1> predicate, P1 param1) {
        return ensure(value, predicate, param1, cause);
    }

    /// Ensures that a value satisfies a binary predicate, with a custom cause provider.
    ///
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param value         the value to verify
    /// @param predicate     the binary predicate to test the value against
    /// @param param1        the additional parameter to pass to the predicate
    /// @param <T>           the type of the value being verified
    /// @param <P1>          the type of the additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the generated cause if not
    ///
    /// @deprecated Use {@link #ensure(Object, Fn2, Object, Fn1)} instead for more natural parameter order.
    @Deprecated(forRemoval = true)
    static <T, P1> Result<T> ensure(Fn1<Cause, T> causeProvider, T value, Fn2<Boolean, T, P1> predicate, P1 param1) {
        return ensure(value, predicate, param1, causeProvider);
    }

    //------------------------------------------------------------------------------------------------------------------
    // Ternary predicate variants (predicate with two additional parameters)
    //------------------------------------------------------------------------------------------------------------------

    /// Ensures that a value satisfies a ternary predicate with two additional parameters.
    ///
    /// This method is a convenience wrapper allowing the use of a predicate that takes three parameters:
    /// the value being tested and two additional parameters.
    ///
    /// @param value     the value to verify
    /// @param predicate the ternary predicate to test the value against
    /// @param param1    the first additional parameter to pass to the predicate
    /// @param param2    the second additional parameter to pass to the predicate
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the first additional parameter
    /// @param <P2>      the type of the second additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result if the predicate is not satisfied
    static <T, P1, P2> Result<T> ensure(T value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2) {
        return ensure(value, v -> predicate.apply(v, param1, param2));
    }

    /// Ensures that a value satisfies a ternary predicate, with a fixed cause on failure.
    /// This is an alias with cause at the end of the parameter list for more natural reading.
    ///
    /// @param value     the value to verify
    /// @param predicate the ternary predicate to test the value against
    /// @param param1    the first additional parameter to pass to the predicate
    /// @param param2    the second additional parameter to pass to the predicate
    /// @param cause     the cause to use if validation fails
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the first additional parameter
    /// @param <P2>      the type of the second additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the specified cause if not
    static <T, P1, P2> Result<T> ensure(T value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2, Cause cause) {
        return ensure(value, predicate, param1, param2, _ -> cause);
    }

    /// Ensures that a value satisfies a ternary predicate, with a custom cause provider.
    /// This is an alias with cause provider at the end of the parameter list for more natural reading.
    ///
    /// @param value         the value to verify
    /// @param predicate     the ternary predicate to test the value against
    /// @param param1        the first additional parameter to pass to the predicate
    /// @param param2        the second additional parameter to pass to the predicate
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param <T>           the type of the value being verified
    /// @param <P1>          the type of the first additional parameter
    /// @param <P2>          the type of the second additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the generated cause if not
    static <T, P1, P2> Result<T> ensure(T value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2, Fn1<Cause, T> causeProvider) {
        return ensure(value, v -> predicate.apply(v, param1, param2), causeProvider);
    }

    /// Ensures that a value satisfies a ternary predicate, with a fixed cause on failure.
    ///
    /// @param cause     the cause to use if validation fails
    /// @param value     the value to verify
    /// @param predicate the ternary predicate to test the value against
    /// @param param1    the first additional parameter to pass to the predicate
    /// @param param2    the second additional parameter to pass to the predicate
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the first additional parameter
    /// @param <P2>      the type of the second additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the specified cause if not
    ///
    /// @deprecated Use {@link #ensure(Object, Fn3, Object, Object, Cause)} instead for more natural parameter order.
    @Deprecated(forRemoval = true)
    static <T, P1, P2> Result<T> ensure(Cause cause, T value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2) {
        return ensure(value, predicate, param1, param2, cause);
    }

    /// Ensures that a value satisfies a ternary predicate, with a custom cause provider.
    ///
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param value         the value to verify
    /// @param predicate     the ternary predicate to test the value against
    /// @param param1        the first additional parameter to pass to the predicate
    /// @param param2        the second additional parameter to pass to the predicate
    /// @param <T>           the type of the value being verified
    /// @param <P1>          the type of the first additional parameter
    /// @param <P2>          the type of the second additional parameter
    ///
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result with the generated cause if not
    ///
    /// @deprecated Use {@link #ensure(Object, Fn3, Object, Object, Fn1)} instead for more natural parameter order.
    @Deprecated(forRemoval = true)
    static <T, P1, P2> Result<T> ensure(Fn1<Cause, T> causeProvider, T value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2) {
        return ensure(value, predicate, param1, param2, causeProvider);
    }

    //------------------------------------------------------------------------------------------------------------------
    // Combining validators
    //------------------------------------------------------------------------------------------------------------------

    /// Combines multiple individual validation checks into a single validation function.
    ///
    /// The returned function will apply all checks in sequence, returning the first failure
    /// encountered or success if all checks pass. This is useful for creating composite
    /// validation rules that must all be satisfied.
    ///
    /// @param checks variable number of validation functions to combine
    /// @param <T>    the type of the values being verified
    ///
    /// @return a single validation function that applies all checks in sequence
    @SafeVarargs
    static <T> Fn1<Result<T>, T> combine(Fn1<Result<T>, T> ... checks) {
        return value -> {
            for (var check : checks) {
                var result = check.apply(value);

                if (result.isSuccess()) {
                    continue;
                }
                return result;
            }
            return Result.success(value);
        };
    }

    //------------------------------------------------------------------------------------------------------------------
    // Optional value validation (ensureOption)
    //------------------------------------------------------------------------------------------------------------------

    /// Validates an optional value against a predicate if present, succeeds with empty Option if absent.
    ///
    /// @param value     the optional value to verify
    /// @param predicate the predicate to test the value against if present
    /// @param <T>       the type of the value being verified
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure if present and invalid
    static <T> Result<Option<T>> ensureOption(Option<T> value, Predicate<T> predicate) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate).map(Option::some)
        );
    }

    /// Validates an optional value against a predicate if present, with a fixed cause on failure.
    ///
    /// @param value     the optional value to verify
    /// @param predicate the predicate to test the value against if present
    /// @param cause     the cause to use if validation fails
    /// @param <T>       the type of the value being verified
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure with the specified cause if present and invalid
    static <T> Result<Option<T>> ensureOption(Option<T> value, Predicate<T> predicate, Cause cause) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, cause).map(Option::some)
        );
    }

    /// Validates an optional value against a predicate if present, with a custom cause provider.
    ///
    /// @param value         the optional value to verify
    /// @param predicate     the predicate to test the value against if present
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param <T>           the type of the value being verified
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure with the generated cause if present and invalid
    static <T> Result<Option<T>> ensureOption(Option<T> value, Predicate<T> predicate, Fn1<Cause, T> causeProvider) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, causeProvider).map(Option::some)
        );
    }

    /// Validates an optional value against a binary predicate if present.
    ///
    /// @param value     the optional value to verify
    /// @param predicate the binary predicate to test the value against if present
    /// @param param1    the additional parameter to pass to the predicate
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the additional parameter
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure if present and invalid
    static <T, P1> Result<Option<T>> ensureOption(Option<T> value, Fn2<Boolean, T, P1> predicate, P1 param1) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, param1).map(Option::some)
        );
    }

    /// Validates an optional value against a binary predicate if present, with a fixed cause on failure.
    ///
    /// @param value     the optional value to verify
    /// @param predicate the binary predicate to test the value against if present
    /// @param param1    the additional parameter to pass to the predicate
    /// @param cause     the cause to use if validation fails
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the additional parameter
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure with the specified cause if present and invalid
    static <T, P1> Result<Option<T>> ensureOption(Option<T> value, Fn2<Boolean, T, P1> predicate, P1 param1, Cause cause) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, param1, cause).map(Option::some)
        );
    }

    /// Validates an optional value against a binary predicate if present, with a custom cause provider.
    ///
    /// @param value         the optional value to verify
    /// @param predicate     the binary predicate to test the value against if present
    /// @param param1        the additional parameter to pass to the predicate
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param <T>           the type of the value being verified
    /// @param <P1>          the type of the additional parameter
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure with the generated cause if present and invalid
    static <T, P1> Result<Option<T>> ensureOption(Option<T> value, Fn2<Boolean, T, P1> predicate, P1 param1, Fn1<Cause, T> causeProvider) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, param1, causeProvider).map(Option::some)
        );
    }

    /// Validates an optional value against a ternary predicate if present.
    ///
    /// @param value     the optional value to verify
    /// @param predicate the ternary predicate to test the value against if present
    /// @param param1    the first additional parameter to pass to the predicate
    /// @param param2    the second additional parameter to pass to the predicate
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the first additional parameter
    /// @param <P2>      the type of the second additional parameter
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure if present and invalid
    static <T, P1, P2> Result<Option<T>> ensureOption(Option<T> value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, param1, param2).map(Option::some)
        );
    }

    /// Validates an optional value against a ternary predicate if present, with a fixed cause on failure.
    ///
    /// @param value     the optional value to verify
    /// @param predicate the ternary predicate to test the value against if present
    /// @param param1    the first additional parameter to pass to the predicate
    /// @param param2    the second additional parameter to pass to the predicate
    /// @param cause     the cause to use if validation fails
    /// @param <T>       the type of the value being verified
    /// @param <P1>      the type of the first additional parameter
    /// @param <P2>      the type of the second additional parameter
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure with the specified cause if present and invalid
    static <T, P1, P2> Result<Option<T>> ensureOption(Option<T> value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2, Cause cause) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, param1, param2, cause).map(Option::some)
        );
    }

    /// Validates an optional value against a ternary predicate if present, with a custom cause provider.
    ///
    /// @param value         the optional value to verify
    /// @param predicate     the ternary predicate to test the value against if present
    /// @param param1        the first additional parameter to pass to the predicate
    /// @param param2        the second additional parameter to pass to the predicate
    /// @param causeProvider function that creates a Cause based on the input value when validation fails
    /// @param <T>           the type of the value being verified
    /// @param <P1>          the type of the first additional parameter
    /// @param <P2>          the type of the second additional parameter
    ///
    /// @return success with Option.none() if empty, success with Option.some(value) if present and valid,
    ///         or failure with the generated cause if present and invalid
    static <T, P1, P2> Result<Option<T>> ensureOption(Option<T> value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2, Fn1<Cause, T> causeProvider) {
        return value.fold(
            () -> Result.success(Option.none()),
            v -> ensure(v, predicate, param1, param2, causeProvider).map(Option::some)
        );
    }

    /// A collection of predicate functions that can be used with the `ensure` methods.
    ///
    /// This interface provides a comprehensive set of predicate functions for common validation
    /// scenarios, including Option validation (some/none), numeric comparisons (greater than, less than, etc.),
    /// and string validation (empty, blank, contains, matches, etc.).
    ///
    /// These predicates are designed to be used with the `ensure` methods of the `Verify` interface,
    /// but they can also be used independently as standard predicate functions.
    interface Is {
        /// Checks if an Option contains a value (is present).
        ///
        /// @param <T> the type of the value in the Option
        /// @param option the Option to check
        /// @return true if the Option is present, false otherwise
        static <T> boolean some(Option<T> option) {
            return option.isPresent();
        }
        
        /// Checks if an Option is empty (contains no value).
        ///
        /// @param <T> the type of the value in the Option
        /// @param option the Option to check
        /// @return true if the Option is empty, false otherwise
        static <T> boolean none(Option<T> option) {
            return option.isEmpty();
        }

        /// Checks if a number is positive (greater than zero).
        ///
        /// @param <T>   the type of number being checked, must extend Number
        /// @param value the value to check
        ///
        /// @return true if the value is positive, false otherwise
        static <T extends Number> boolean positive(T value) {
            return value.doubleValue() > 0;
        }


        /// Checks if a number is negative (less than zero).
        ///
        /// @param <T>   the type of number being checked, must extend Number
        /// @param value the value to check
        ///
        /// @return true if the value is negative, false otherwise
        static <T extends Number> boolean negative(T value) {
            return value.doubleValue() < 0;
        }
        
        /// Checks if a number is non-negative (greater than or equal to zero).
        ///
        /// @param <T>   the type of number being checked, must extend Number
        /// @param value the value to check
        ///
        /// @return true if the value is non-negative, false otherwise
        static <T extends Number> boolean nonNegative(T value) {
            return value.doubleValue() >= 0;
        }

        /// Checks if a number is non-positive (less than or equal to zero).
        ///
        /// @param <T>   the type of number being checked, must extend Number
        /// @param value the value to check
        ///
        /// @return true if the value is non-positive, false otherwise
        static <T extends Number> boolean nonPositive(T value) {
            return value.doubleValue() <= 0;
        }

        /// Checks if a value is greater than a boundary.
        ///
        /// @param <T> the type of values being compared, must extend Comparable
        /// @param value the value to check
        /// @param boundary the boundary to compare against
        /// @return true if value is greater than boundary, false otherwise
        static <T extends Comparable<T>> boolean greaterThan(T value, T boundary) {
            return value.compareTo(boundary) > 0;
        }

        /// Checks if a value is greater than or equal to a boundary.
        ///
        /// @param <T> the type of values being compared, must extend Comparable
        /// @param value the value to check
        /// @param boundary the boundary to compare against
        /// @return true if value is greater than or equal to boundary, false otherwise
        static <T extends Comparable<T>> boolean greaterThanOrEqualTo(T value, T boundary) {
            return value.compareTo(boundary) >= 0;
        }

        /// Checks if a value is less than a boundary.
        ///
        /// @param <T> the type of values being compared, must extend Comparable
        /// @param value the value to check
        /// @param boundary the boundary to compare against
        /// @return true if value is less than boundary, false otherwise
        static <T extends Comparable<T>> boolean lessThan(T value, T boundary) {
            return value.compareTo(boundary) < 0;
        }

        /// Checks if a value is less than or equal to a boundary.
        ///
        /// @param <T> the type of values being compared, must extend Comparable
        /// @param value the value to check
        /// @param boundary the boundary to compare against
        /// @return true if value is less than or equal to boundary, false otherwise
        static <T extends Comparable<T>> boolean lessThanOrEqualTo(T value, T boundary) {
            return value.compareTo(boundary) <= 0;
        }

        /// Checks if a value is equal to a boundary using compareTo.
        ///
        /// @param <T> the type of values being compared, must extend Comparable
        /// @param value the value to check
        /// @param boundary the boundary to compare against
        /// @return true if value is equal to boundary, false otherwise
        static <T extends Comparable<T>> boolean equalTo(T value, T boundary) {
            return value.compareTo(boundary) == 0;
        }

        /// Checks if a value is not equal to a boundary using compareTo.
        ///
        /// @param <T> the type of values being compared, must extend Comparable
        /// @param value the value to check
        /// @param boundary the boundary to compare against
        /// @return true if value is not equal to boundary, false otherwise
        static <T extends Comparable<T>> boolean notEqualTo(T value, T boundary) {
            return value.compareTo(boundary) != 0;
        }

        /// Checks if a value is between min and max (inclusive).
        ///
        /// @param <T> the type of values being compared, must extend Comparable
        /// @param value the value to check
        /// @param min the minimum boundary (inclusive)
        /// @param max the maximum boundary (inclusive)
        /// @return true if the value is between min and max (inclusive), false otherwise
        static <T extends Comparable<T>> boolean between(T value, T min, T max) {
            return greaterThanOrEqualTo(value, min) && lessThanOrEqualTo(value, max);
        }

        /// Check if a value is not null
        ///
        /// This predicate is useful for ensuring that required values are present.
        ///
        /// @param value The value to check
        /// @param <T>   The type of the value
        /// @return true if the value is not null, false otherwise
        static <T> boolean notNull(T value) {
            return value != null;
        }

        /// Checks if a char sequence is empty.
        ///
        /// @param <T> the type of char sequence being checked
        /// @param value the char sequence to check
        /// @return true if the char sequence is empty, false otherwise
        static <T extends CharSequence> boolean empty(T value) {
            return value.isEmpty();
        }

        /// Checks if a char sequence is not empty.
        ///
        /// @param <T> the type of char sequence being checked
        /// @param value the char sequence to check
        /// @return true if the char sequence is not empty, false otherwise
        static <T extends CharSequence> boolean notEmpty(T value) {
            return !empty(value);
        }

        /// Checks if a char sequence is blank (empty or contains only whitespace).
        /// @param <T> the type of char sequence being checked
        /// @param value the char sequence to check
        /// @return true if the char sequence is blank, false otherwise
        static <T extends CharSequence> boolean blank(T value) {
            return value.chars().allMatch(Character::isWhitespace);
        }

        /// Checks if a char sequence is not blank (contains at least one non-whitespace character).
        ///
        /// @param <T> the type of char sequence being checked
        /// @param value the char sequence to check
        /// @return true if the char sequence is not blank, false otherwise
        static <T extends CharSequence> boolean notBlank(T value) {
            return !blank(value);
        }

        /// Checks if a character sequence length is within specified bounds (inclusive).
        ///
        /// This predicate is useful for validating string lengths, input field constraints,
        /// password requirements, etc.
        ///
        /// @param value  The character sequence to check
        /// @param minLen The minimum allowed length (inclusive)
        /// @param maxLen The maximum allowed length (inclusive)
        /// @param <T>    The type of character sequence
        /// @return true if the length is between minLen and maxLen (inclusive), false otherwise
        static <T extends CharSequence> boolean lenBetween(T value, int minLen, int maxLen) {
            return value.length() >= minLen && value.length() <= maxLen;
        }

        /// Checks if a String contains a substring.
        ///
        /// @param value the String to check
        /// @param substring the substring to look for
        /// @return true if the String contains the substring, false otherwise
        static boolean contains(String value, CharSequence substring) {
            return value.contains(substring.toString());
        }

        /// Checks if a String does not contain a substring.
        ///
        /// @param value the String to check
        /// @param substring the substring to look for
        /// @return true if the String does not contain the substring, false otherwise
        static boolean notContains(String value, CharSequence substring) {
            return !contains(value, substring);
        }

        /// Checks if a String matches a regular expression pattern.
        ///
        /// @param value the String to check
        /// @param regex the regular expression pattern to match against
        /// @return true if the String matches the pattern, false otherwise
        static boolean matches(String value, String regex) {
            return value.matches(regex);
        }

        /// Checks if a String matches a compiled regular expression pattern.
        ///
        /// @param value the String to check
        /// @param regex the compiled Pattern to match against
        /// @return true if the String matches the pattern, false otherwise
        static boolean matches(String value, Pattern regex) {
            return regex.matcher(value).matches();
        }
    }

    @SuppressWarnings("unused")
    record unused() implements Verify {}
}
