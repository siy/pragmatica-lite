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

    /// Ensures that a value satisfies a given predicate.
    ///
    /// This method evaluates the provided value against the given predicate. If the predicate returns
    /// true, a success result containing the original value is returned. Otherwise, a failure result
    /// is returned with an appropriate error message.
    ///
    /// @param <T> the type of the value being verified
    /// @param value the value to verify
    /// @param predicate the predicate to test the value against
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result if the predicate is not satisfied
    static <T> Result<T> ensure(T value, Predicate<T> predicate) {
        return ensure(Causes.forValue("Value {0} does not satisfy the predicate"), value, predicate);
    }

    /// Ensures that a value satisfies a given predicate.
    ///
    /// This method allows specifying a custom function to generate error causes based on the input value,
    /// providing more context-aware error messages than the standard ensure method.
    ///
    /// @param causeProvider Function that creates a Cause based on the input value when validation fails
    /// @param value         The value to be validated
    /// @param predicate     The predicate to test the value against
    /// @param <T>           The type of the value being verified
    ///
    /// @return Success result containing the value if predicate returns true, failure result otherwise
    static <T> Result<T> ensure(Fn1<Cause, T> causeProvider, T value, Predicate<T> predicate) {
        if (predicate.test(value)) {
            return Result.success(value);
        }

        return causeProvider.apply(value).result();
    }

    /// Creates a reusable validation function that performs a given check with custom cause provider.
    /// This is a function factory that creates validation functions that can be reused across
    /// different validation scenarios with consistent error messaging.
    ///
    /// @param causeProvider Function that creates a Cause based on the input value when validation fails
    /// @param predicate     The predicate to test values against
    /// @param <T>           The type of the values being verified
    ///
    /// @return A function that takes a value and returns a Result containing the value or failure
    static <T> Fn1<Result<T>, T> ensureFn(Fn1<Cause, T> causeProvider, Predicate<T> predicate) {
        return value -> ensure(causeProvider, value, predicate);
    }

    /// Ensures that a value satisfies a binary predicate with one additional parameter.
    ///
    /// This method is a convenience wrapper around the single-parameter `ensure` method,
    /// allowing the use of a predicate that takes two parameters: the value being tested
    /// and an additional parameter.
    ///
    /// @param <T> the type of the value being verified
    /// @param <P1> the type of the additional parameter
    /// @param value the value to verify
    /// @param predicate the binary predicate to test the value against
    /// @param param1 the additional parameter to pass to the predicate
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result if the predicate is not satisfied
    static <T, P1> Result<T> ensure(T value, Fn2<Boolean, T, P1> predicate, P1 param1) {
        return ensure(value, v -> predicate.apply(v, param1));
    }

    /// Create a function which will perform given check and return [Result]
    static <T, P1> Fn1<Result<T>, T> ensureFn(Fn2<Boolean, T, P1> predicate, P1 param1) {
        return value -> ensure(value, predicate, param1);
    }

    /// Ensures that a value satisfies a binary predicate with one additional parameter.
    ///
    /// This method allows specifying a custom function to generate error causes, providing more
    /// context-aware error messages than the standard binary ensure method.
    ///
    /// @param causeProvider Function that creates a Cause based on the input value when validation fails
    /// @param value         The value to be validated
    /// @param predicate     The binary predicate to test the value against
    /// @param param1        The additional parameter to pass to the predicate
    /// @param <T>           The type of the value being verified
    /// @param <P1>          The type of the additional parameter
    ///
    /// @return Success result containing the value if predicate returns true, failure result otherwise
    static <T, P1> Result<T> ensure(Fn1<Cause, T> causeProvider, T value, Fn2<Boolean, T, P1> predicate, P1 param1) {
        return ensure(causeProvider, value, v -> predicate.apply(v, param1));
    }

    /// Creates a reusable validation function that performs a binary check with custom cause provider.
    /// This is a function factory that creates validation functions with one additional parameter,
    /// useful for creating reusable range checks, comparisons, etc.
    ///
    /// @param causeProvider Function that creates a Cause based on the input value when validation fails
    /// @param predicate     The binary predicate to test values against
    /// @param param1        The additional parameter to pass to the predicate
    /// @param <T>           The type of the values being verified
    /// @param <P1>          The type of the additional parameter
    ///
    /// @return A function that takes a value and returns a Result containing the value or failure
    static <T, P1> Fn1<Result<T>, T> ensureFn(Fn1<Cause, T> causeProvider, Fn2<Boolean, T, P1> predicate, P1 param1) {
        return value -> ensure(causeProvider, value, predicate, param1);
    }

    /// Ensures that a value satisfies a ternary predicate with two additional parameters.
    ///
    /// This method is a convenience wrapper around the single-parameter `ensure` method,
    /// allowing the use of a predicate that takes three parameters: the value being tested
    /// and two additional parameters.
    ///
    /// @param <T> the type of the value being verified
    /// @param <P1> the type of the first additional parameter
    /// @param <P2> the type of the second additional parameter
    /// @param value the value to verify
    /// @param predicate the ternary predicate to test the value against
    /// @param param1 the first additional parameter to pass to the predicate
    /// @param param2 the second additional parameter to pass to the predicate
    /// @return a success result containing the value if the predicate is satisfied,
    ///         or a failure result if the predicate is not satisfied
    static <T, P1, P2> Result<T> ensure(T value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2) {
        return ensure(value, v -> predicate.apply(v, param1, param2));
    }

    /// Create a function which will perform given check and return [Result]
    static <T, P1, P2> Fn1<Result<T>, T> ensureFn(Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2) {
        return value -> ensure(value, predicate, param1, param2);
    }

    /// Ensures that a value satisfies a ternary predicate with two additional parameters.
    ///
    /// This method allows specifying a custom function to generate error causes, providing more
    /// context-aware error messages than the standard ternary ensure method.
    ///
    /// @param causeProvider Function that creates a Cause based on the input value when validation fails
    /// @param value         The value to be validated
    /// @param predicate     The ternary predicate to test the value against
    /// @param param1        The first additional parameter to pass to the predicate
    /// @param param2        The second additional parameter to pass to the predicate
    /// @param <T>           The type of the value being verified
    /// @param <P1>          The type of the first additional parameter
    /// @param <P2>          The type of the second additional parameter
    ///
    /// @return Success result containing the value if predicate returns true, failure result otherwise
    static <T, P1, P2> Result<T> ensure(Fn1<Cause, T> causeProvider, T value, Fn3<Boolean, T, P1, P2> predicate, P1 param1, P2 param2) {
        return ensure(causeProvider, value, v -> predicate.apply(v, param1, param2));
    }

    /// Creates a reusable validation function that performs a ternary check with custom cause provider.
    /// This is a function factory that creates validation functions with two additional parameters,
    /// useful for creating reusable range checks between bounds, pattern matching, etc.
    ///
    /// @param causeProvider Function that creates a Cause based on the input value when validation fails
    /// @param predicate     The ternary predicate to test values against
    /// @param param1        The first additional parameter to pass to the predicate
    /// @param param2        The second additional parameter to pass to the predicate
    /// @param <T>           The type of the values being verified
    /// @param <P1>          The type of the first additional parameter
    /// @param <P2>          The type of the second additional parameter
    ///
    /// @return A function that takes a value and returns a Result containing the value or failure
    static <T, P1, P2> Fn1<Result<T>, T> ensureFn(Fn1<Cause, T> causeProvider, Fn3<Boolean, T, P1, P2> predicate, P1 param1,  P2 param2) {
        return value -> ensure(causeProvider, value, predicate, param1, param2);
    }


    /// Combines multiple individual validation checks into a single validation function.
    /// The returned function will apply all checks in sequence, returning the first failure
    /// encountered or success if all checks pass. This is useful for creating composite
    /// validation rules that must all be satisfied.
    ///
    /// @param checks Variable number of validation functions to combine
    /// @param <T>    The type of the values being verified
    ///
    /// @return A single validation function that applies all checks in sequence
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

    /// A collection of predicate functions that can be used with the `ensure` methods.
    ///
    /// This interface provides a comprehensive set of predicate functions for common validation
    /// scenarios, including:
    /// - Option validation (some/none)
    /// - Numeric comparisons (greater than, less than, etc.)
    /// - String validation (empty, blank, contains, matches, etc.)
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
