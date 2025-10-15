package org.pragmatica.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.utils.Causes;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VerifyTest {

    @SuppressWarnings("deprecation")
    @Nested
    @DisplayName("Verify class tests")
    class VerifyMethodsTest {
        
        @Test
        @DisplayName("ensure with predicate should return success for valid value")
        void ensureWithPredicateReturnsSuccessForValidValue() {
            var result = Verify.ensure(5, value -> Verify.Is.greaterThan(value, 0));
            
            assertTrue(result.isSuccess());
            assertEquals(5, result.unwrap());
        }
        
        @Test
        @DisplayName("ensure with predicate should return failure for invalid value")
        void ensureWithPredicateReturnsFailureForInvalidValue() {
            var result = Verify.ensure(-5, value -> Verify.Is.greaterThan(value, 0));
            
            assertTrue(result.isFailure());
        }
        
        @Test
        @DisplayName("ensure with Fn2 should return success for valid value")
        void ensureWithFn2ReturnsSuccessForValidValue() {
            var result = Verify.ensure(10, Verify.Is::greaterThan, 5);
            
            assertTrue(result.isSuccess());
            assertEquals(10, result.unwrap());
        }
        
        @Test
        @DisplayName("ensure with Fn2 should return failure for invalid value")
        void ensureWithFn2ReturnsFailureForInvalidValue() {
            var result = Verify.ensure(3, Verify.Is::greaterThan, 5);
            
            assertTrue(result.isFailure());
        }
        
        @Test
        @DisplayName("ensure with Fn3 should return success for valid value")
        void ensureWithFn3ReturnsSuccessForValidValue() {
            var result = Verify.ensure(10, Verify.Is::between, 5, 15);
            
            assertTrue(result.isSuccess());
            assertEquals(10, result.unwrap());
        }
        
        @Test
        @DisplayName("ensure with Fn3 should return failure for invalid value")
        void ensureWithFn3ReturnsFailureForInvalidValue() {
            var result = Verify.ensure(20, Verify.Is::between, 5, 15);
            
            assertTrue(result.isFailure());
        }
    }
    
    @Nested
    @DisplayName("Verify.Is class tests")
    class VerifyIsMethodsTest {
        
        @Test
        @DisplayName("some should return true for non-empty option")
        void someShouldReturnTrueForNonEmptyOption() {
            var option = Option.some("test");
            assertTrue(Verify.Is.some(option));
        }
        
        @Test
        @DisplayName("some should return false for empty option")
        void someShouldReturnFalseForEmptyOption() {
            var option = Option.none();
            assertFalse(Verify.Is.some(option));
        }
        
        @Test
        @DisplayName("none should return true for empty option")
        void noneShouldReturnTrueForEmptyOption() {
            var option = Option.none();
            assertTrue(Verify.Is.none(option));
        }
        
        @Test
        @DisplayName("none should return false for non-empty option")
        void noneShouldReturnFalseForNonEmptyOption() {
            var option = Option.some("test");
            assertFalse(Verify.Is.none(option));
        }
        
        @Test
        @DisplayName("greaterThan should return true when value is greater than boundary")
        void greaterThanShouldReturnTrueWhenValueIsGreaterThanBoundary() {
            assertTrue(Verify.Is.greaterThan(10, 5));
            assertFalse(Verify.Is.greaterThan(5, 5));
            assertFalse(Verify.Is.greaterThan(3, 5));
        }
        
        @Test
        @DisplayName("greaterThanOrEqualTo should return true when value is greater than or equal to boundary")
        void greaterThanOrEqualToShouldReturnTrueWhenValueIsGreaterThanOrEqualToBoundary() {
            assertTrue(Verify.Is.greaterThanOrEqualTo(10, 5));
            assertTrue(Verify.Is.greaterThanOrEqualTo(5, 5));
            assertFalse(Verify.Is.greaterThanOrEqualTo(3, 5));
        }
        
        @Test
        @DisplayName("lessThan should return true when value is less than boundary")
        void lessThanShouldReturnTrueWhenValueIsLessThanBoundary() {
            assertTrue(Verify.Is.lessThan(3, 5));
            assertFalse(Verify.Is.lessThan(5, 5));
            assertFalse(Verify.Is.lessThan(10, 5));
        }
        
        @Test
        @DisplayName("lessThanOrEqualTo should return true when value is less than or equal to boundary")
        void lessThanOrEqualToShouldReturnTrueWhenValueIsLessThanOrEqualToBoundary() {
            assertTrue(Verify.Is.lessThanOrEqualTo(3, 5));
            assertTrue(Verify.Is.lessThanOrEqualTo(5, 5));
            assertFalse(Verify.Is.lessThanOrEqualTo(10, 5));
        }
        
        @Test
        @DisplayName("equalTo should return true when value is equal to boundary")
        void equalToShouldReturnTrueWhenValueIsEqualToBoundary() {
            assertTrue(Verify.Is.equalTo(5, 5));
            assertFalse(Verify.Is.equalTo(3, 5));
            assertFalse(Verify.Is.equalTo(10, 5));
        }
        
        @Test
        @DisplayName("notEqualTo should return true when value is not equal to boundary")
        void notEqualToShouldReturnTrueWhenValueIsNotEqualToBoundary() {
            assertTrue(Verify.Is.notEqualTo(3, 5));
            assertTrue(Verify.Is.notEqualTo(10, 5));
            assertFalse(Verify.Is.notEqualTo(5, 5));
        }
        
        @Test
        @DisplayName("between should return true when value is between min and max")
        void betweenShouldReturnTrueWhenValueIsBetweenMinAndMax() {
            assertTrue(Verify.Is.between(5, 1, 10));
            assertTrue(Verify.Is.between(1, 1, 10));
            assertTrue(Verify.Is.between(10, 1, 10));
            assertFalse(Verify.Is.between(0, 1, 10));
            assertFalse(Verify.Is.between(11, 1, 10));
        }
        
        @Test
        @DisplayName("empty should return true for empty CharSequence")
        void emptyShouldReturnTrueForEmptyCharSequence() {
            assertTrue(Verify.Is.empty(""));
            assertFalse(Verify.Is.empty("test"));
            assertFalse(Verify.Is.empty(" "));
        }
        
        @Test
        @DisplayName("notEmpty should return true for non-empty CharSequence")
        void notEmptyShouldReturnTrueForNonEmptyCharSequence() {
            assertTrue(Verify.Is.notEmpty("test"));
            assertTrue(Verify.Is.notEmpty(" "));
            assertFalse(Verify.Is.notEmpty(""));
        }
        
        @Test
        @DisplayName("blank should return true for blank CharSequence")
        void blankShouldReturnTrueForBlankCharSequence() {
            assertTrue(Verify.Is.blank(""));
            assertTrue(Verify.Is.blank(" "));
            assertTrue(Verify.Is.blank("\t\n"));
            assertFalse(Verify.Is.blank("test"));
        }

        @Test
        @DisplayName("blank should handle null and edge cases correctly")
        void blankShouldHandleNullAndEdgeCases() {
            assertTrue(Verify.Is.blank(""));
            assertTrue(Verify.Is.blank(" \t\n\r"));
            assertFalse(Verify.Is.blank("a"));
            assertFalse(Verify.Is.blank(" a "));
        }
        
        @Test
        @DisplayName("notBlank should return true for non-blank CharSequence")
        void notBlankShouldReturnTrueForNonBlankCharSequence() {
            assertTrue(Verify.Is.notBlank("test"));
            assertTrue(Verify.Is.notBlank(" test "));
            assertFalse(Verify.Is.notBlank(""));
            assertFalse(Verify.Is.notBlank(" "));
            assertFalse(Verify.Is.notBlank("\t\n"));
        }
        
        @Test
        @DisplayName("contains should return true when string contains substring")
        void containsShouldReturnTrueWhenStringContainsSubstring() {
            assertTrue(Verify.Is.contains("test string", "test"));
            assertTrue(Verify.Is.contains("test string", "string"));
            assertTrue(Verify.Is.contains("test string", " "));
            assertFalse(Verify.Is.contains("test string", "xyz"));
        }
        
        @Test
        @DisplayName("notContains should return true when string doesn't contain substring")
        void notContainsShouldReturnTrueWhenStringDoesNotContainSubstring() {
            assertTrue(Verify.Is.notContains("test string", "xyz"));
            assertFalse(Verify.Is.notContains("test string", "test"));
            assertFalse(Verify.Is.notContains("test string", "string"));
            assertFalse(Verify.Is.notContains("test string", " "));
        }
        
        @Test
        @DisplayName("matches with String regex should return true for matching string")
        void matchesWithStringRegexShouldReturnTrueForMatchingString() {
            assertTrue(Verify.Is.matches("12345", "\\d+"));
            assertTrue(Verify.Is.matches("abc", "[a-z]+"));
            assertFalse(Verify.Is.matches("abc123", "\\d+"));
            assertFalse(Verify.Is.matches("ABC", "[a-z]+"));
        }

        @Test
        @DisplayName("matches should handle special regex patterns correctly")
        void matchesShouldHandleSpecialRegexPatterns() {
            // Test with complex regex patterns
            assertTrue(Verify.Is.matches("abc123", "[a-z]+\\d+"));
            assertTrue(Verify.Is.matches("john.doe@example.com", "^[\\w.-]+@[\\w.-]+\\.\\w+$"));
            assertFalse(Verify.Is.matches("invalid@email", "^[\\w.-]+@[\\w.-]+\\.\\w+$"));
        }
        
        @Test
        @DisplayName("matches with Pattern should return true for matching string")
        void matchesWithPatternShouldReturnTrueForMatchingString() {
            var digitPattern = Pattern.compile("\\d+");
            var lowerCasePattern = Pattern.compile("[a-z]+");
            
            assertTrue(Verify.Is.matches("12345", digitPattern));
            assertTrue(Verify.Is.matches("abc", lowerCasePattern));
            assertFalse(Verify.Is.matches("abc123", digitPattern));
            assertFalse(Verify.Is.matches("ABC", lowerCasePattern));
        }
        
        @Test
        @DisplayName("ensure with String validation")
        void ensureWithStringValidation() {
            var validString = "Hello, world!";
            var emptyString = "";
            var blankString = "   ";
            
            var notEmptyResult = Verify.ensure(validString, Verify.Is::notEmpty);
            var notBlankResult = Verify.ensure(validString, Verify.Is::notBlank);
            var containsResult = Verify.ensure(validString, Verify.Is::contains, "world");
            var matchesResult = Verify.ensure(validString, Verify.Is::matches, ".*world.*");
            
            var emptyFailResult = Verify.ensure(emptyString, Verify.Is::notEmpty);
            var blankFailResult = Verify.ensure(blankString, Verify.Is::notBlank);
            var containsFailResult = Verify.ensure(validString, Verify.Is::contains, "missing");
            
            assertTrue(notEmptyResult.isSuccess());
            assertTrue(notBlankResult.isSuccess());
            assertTrue(containsResult.isSuccess());
            assertTrue(matchesResult.isSuccess());
            
            assertTrue(emptyFailResult.isFailure());
            assertTrue(blankFailResult.isFailure());
            assertTrue(containsFailResult.isFailure());
        }
        
        @Test
        @DisplayName("ensure with Option validation")
        void ensureWithOptionValidation() {
            var someOption = Option.some("test");
            var noneOption = Option.none();
            
            var someResult = Verify.ensure(someOption, Verify.Is::some);
            var noneResult = Verify.ensure(noneOption, Verify.Is::none);
            
            var someFailResult = Verify.ensure(someOption, Verify.Is::none);
            var noneFailResult = Verify.ensure(noneOption, Verify.Is::some);
            
            assertTrue(someResult.isSuccess());
            assertTrue(noneResult.isSuccess());
            
            assertTrue(someFailResult.isFailure());
            assertTrue(noneFailResult.isFailure());
        }
        
        @Test
        @DisplayName("ensure with numeric comparisons")
        void ensureWithNumericComparisons() {
            var number = 42;
            
            var greaterThanResult = Verify.ensure(number, Verify.Is::greaterThan, 20);
            var lessThanResult = Verify.ensure(number, Verify.Is::lessThan, 100);
            var betweenResult = Verify.ensure(number, Verify.Is::between, 40, 50);
            var equalToResult = Verify.ensure(number, Verify.Is::equalTo, 42);
            var notEqualToResult = Verify.ensure(number, Verify.Is::notEqualTo, 100);
            
            var greaterThanFailResult = Verify.ensure(number, Verify.Is::greaterThan, 50);
            var betweenFailResult = Verify.ensure(number, Verify.Is::between, 100, 200);
            
            assertTrue(greaterThanResult.isSuccess());
            assertTrue(lessThanResult.isSuccess());
            assertTrue(betweenResult.isSuccess());
            assertTrue(equalToResult.isSuccess());
            assertTrue(notEqualToResult.isSuccess());
            
            assertTrue(greaterThanFailResult.isFailure());
            assertTrue(betweenFailResult.isFailure());
        }

        @Test
        @DisplayName("ensure with combined validations")
        void ensureWithCombinedValidations() {
            var validString = "Hello123";
            
            // Using multiple predicates in combination
            var result = Verify.ensure(validString, value -> 
                Verify.Is.notBlank(value) && 
                Verify.Is.contains(value, "Hello") &&
                Verify.Is.matches(value, ".*\\d+.*"));
            
            assertTrue(result.isSuccess());
            
            // Fail case
            var invalidString = "Hello";
            var failResult = Verify.ensure(invalidString, value -> 
                Verify.Is.notBlank(value) && 
                Verify.Is.contains(value, "Hello") &&
                Verify.Is.matches(value, ".*\\d+.*"));
            
            assertTrue(failResult.isFailure());
        }

        @Test
        @DisplayName("positive should return true for positive numbers")
        void positiveShouldReturnTrueForPositiveNumbers() {
            assertTrue(Verify.Is.positive(10));
            assertTrue(Verify.Is.positive(0.1));
            assertFalse(Verify.Is.positive(0));
            assertFalse(Verify.Is.positive(-5));
        }

        @Test
        @DisplayName("negative should return true for negative numbers")
        void negativeShouldReturnTrueForNegativeNumbers() {
            assertTrue(Verify.Is.negative(-10));
            assertTrue(Verify.Is.negative(-0.1));
            assertFalse(Verify.Is.negative(0));
            assertFalse(Verify.Is.negative(5));
        }

        @Test
        @DisplayName("nonNegative should return true for non-negative numbers")
        void nonNegativeShouldReturnTrueForNonNegativeNumbers() {
            assertTrue(Verify.Is.nonNegative(10));
            assertTrue(Verify.Is.nonNegative(0));
            assertFalse(Verify.Is.nonNegative(-0.1));
            assertFalse(Verify.Is.nonNegative(-5));
        }

        @Test
        @DisplayName("nonPositive should return true for non-positive numbers")
        void nonPositiveShouldReturnTrueForNonPositiveNumbers() {
            assertTrue(Verify.Is.nonPositive(-10));
            assertTrue(Verify.Is.nonPositive(0));
            assertFalse(Verify.Is.nonPositive(0.1));
            assertFalse(Verify.Is.nonPositive(5));
        }

        @Test
        @DisplayName("notNull should return true for non-null values")
        void notNullShouldReturnTrueForNonNullValues() {
            assertTrue(Verify.Is.notNull("test"));
            assertTrue(Verify.Is.notNull(123));
            assertTrue(Verify.Is.notNull(new Object()));
            assertFalse(Verify.Is.notNull(null));
        }

        @Test
        @DisplayName("lenBetween should return true for char sequences with length in range")
        void lenBetweenShouldReturnTrueForCharSequencesWithLengthInRange() {
            assertTrue(Verify.Is.lenBetween("hello", 3, 10));
            assertTrue(Verify.Is.lenBetween("test", 4, 4));
            assertTrue(Verify.Is.lenBetween("", 0, 5));
            assertFalse(Verify.Is.lenBetween("hello", 6, 10));
            assertFalse(Verify.Is.lenBetween("test", 1, 3));
        }
    }

    @Nested
    @DisplayName("Verify enhanced methods tests")
    class VerifyEnhancedMethodsTest {

        @Test
        @DisplayName("ensure with custom cause provider should use custom messages")
        void ensureWithCustomCauseProviderShouldUseCustomMessages() {
            Fn1<Cause, Integer> customCauseProvider = value -> Causes.cause("Custom error for value: " + value);

            // Success case
            Verify.ensure(customCauseProvider, 10, value -> value > 5)
                  .onSuccess(value -> assertEquals(10, value))
                  .onFailureRun(() -> fail("Should succeed"));

            // Failure case with custom cause
            Verify.ensure(customCauseProvider, 3, value -> value > 5)
                  .onSuccessRun(() -> fail("Should fail"))
                  .onFailure(cause -> assertEquals("Custom error for value: 3", cause.message()));
        }

        @Test
        @DisplayName("ensureFn should create reusable validation functions")
        void ensureFnShouldCreateReusableValidationFunctions() {
            Fn1<Cause, String> customCauseProvider = value -> Causes.cause("String '" + value + "' is invalid");
            var validationFn = Verify.ensureFn(customCauseProvider, Verify.Is::notBlank);

            // Test success case
            validationFn.apply("hello")
                        .onSuccess(value -> assertEquals("hello", value))
                        .onFailureRun(() -> fail("Should succeed"));

            // Test failure case
            validationFn.apply("   ")
                        .onSuccessRun(() -> fail("Should fail"))
                        .onFailure(cause -> assertEquals("String '   ' is invalid", cause.message()));
        }

        @Test
        @DisplayName("ensureFn with binary predicate should work correctly")
        void ensureFnWithBinaryPredicateShouldWorkCorrectly() {
            Fn1<Cause, Integer> causeProvider = value -> Causes.cause("Value " + value + " failed range check");
            var rangeCheck = Verify.ensureFn(causeProvider, Verify.Is::greaterThan, 5);

            rangeCheck.apply(10)
                      .onSuccess(value -> assertEquals(10, value))
                      .onFailureRun(() -> fail("Should succeed"));

            rangeCheck.apply(3)
                      .onSuccessRun(() -> fail("Should fail"))
                      .onFailure(cause -> assertEquals("Value 3 failed range check", cause.message()));
        }

        @Test
        @DisplayName("ensureFn with ternary predicate should work correctly")
        void ensureFnWithTernaryPredicateShouldWorkCorrectly() {
            Fn1<Cause, Integer> causeProvider = value -> Causes.cause("Value " + value + " is not in range");
            var betweenCheck = Verify.ensureFn(causeProvider, Verify.Is::between, 5, 10);

            betweenCheck.apply(7)
                        .onSuccess(value -> assertEquals(7, value))
                        .onFailureRun(() -> fail("Should succeed"));

            betweenCheck.apply(12)
                        .onSuccessRun(() -> fail("Should fail"))
                        .onFailure(cause -> assertEquals("Value 12 is not in range", cause.message()));
        }

        @Test
        @DisplayName("combine should merge multiple validation functions")
        void combineShouldMergeMultipleValidationFunctions() {
            Fn1<Result<String>, String> notNullCheck = Verify.ensureFn(value -> Causes.cause("Value is null"), Verify.Is::notNull);
            Fn1<Result<String>, String> notBlankCheck = Verify.ensureFn(value -> Causes.cause("Value is blank"), Verify.Is::notBlank);
            Fn1<Result<String>, String> lengthCheck = Verify.ensureFn(value -> Causes.cause("Value length is invalid"), 
                                              Verify.Is::lenBetween, 3, 10);

            var combinedCheck = Verify.combine(notNullCheck, notBlankCheck, lengthCheck);

            // Test success case - all validations pass
            combinedCheck.apply("hello")
                         .onSuccess(value -> assertEquals("hello", value))
                         .onFailureRun(() -> fail("Should succeed"));

            // Test failure on first check (null)
            combinedCheck.apply(null)
                         .onSuccessRun(() -> fail("Should fail"))
                         .onFailure(cause -> assertEquals("Value is null", cause.message()));

            // Test failure on second check (blank)
            combinedCheck.apply("   ")
                         .onSuccessRun(() -> fail("Should fail"))
                         .onFailure(cause -> assertEquals("Value is blank", cause.message()));

            // Test failure on third check (length)
            combinedCheck.apply("hi")
                         .onSuccessRun(() -> fail("Should fail"))
                         .onFailure(cause -> assertEquals("Value length is invalid", cause.message()));
        }

        @Test
        @DisplayName("ensure with binary predicate and custom cause provider should work")
        void ensureWithBinaryPredicateAndCustomCauseProviderShouldWork() {
            Fn1<Cause, Integer> causeProvider = value -> Causes.cause("Custom: " + value + " failed check");

            Verify.ensure(causeProvider, 10, Verify.Is::greaterThan, 5)
                  .onSuccess(value -> assertEquals(10, value))
                  .onFailureRun(() -> fail("Should succeed"));

            Verify.ensure(causeProvider, 3, Verify.Is::greaterThan, 5)
                  .onSuccessRun(() -> fail("Should fail"))
                  .onFailure(cause -> assertEquals("Custom: 3 failed check", cause.message()));
        }

        @Test
        @DisplayName("ensure with ternary predicate and custom cause provider should work")
        void ensureWithTernaryPredicateAndCustomCauseProviderShouldWork() {
            Fn1<Cause, Integer> causeProvider = value -> Causes.cause("Custom: " + value + " out of range");

            Verify.ensure(causeProvider, 7, Verify.Is::between, 5, 10)
                  .onSuccess(value -> assertEquals(7, value))
                  .onFailureRun(() -> fail("Should succeed"));

            Verify.ensure(causeProvider, 12, Verify.Is::between, 5, 10)
                  .onSuccessRun(() -> fail("Should fail"))
                  .onFailure(cause -> assertEquals("Custom: 12 out of range", cause.message()));
        }

        @Test
        @DisplayName("ensureFn with fixed Cause for unary predicate should work")
        void ensureFnWithFixedCauseForUnaryPredicateShouldWork() {
            var fixedCause = Causes.cause("Fixed error: value is invalid");
            var validationFn = Verify.<String>ensureFn(fixedCause, value -> Verify.Is.notBlank(value));

            // Success case
            validationFn.apply("valid")
                        .onSuccess(value -> assertEquals("valid", value))
                        .onFailureRun(() -> fail("Should succeed"));

            // Failure case - should use fixed cause
            validationFn.apply("   ")
                        .onSuccessRun(() -> fail("Should fail"))
                        .onFailure(cause -> assertEquals("Fixed error: value is invalid", cause.message()));
        }

        @Test
        @DisplayName("ensureFn with fixed Cause for binary predicate should work")
        void ensureFnWithFixedCauseForBinaryPredicateShouldWork() {
            var fixedCause = Causes.cause("Fixed error: range check failed");
            var rangeCheck = Verify.<Integer, Integer>ensureFn(fixedCause,
                (value, boundary) -> Verify.Is.greaterThan(value, boundary), 10);

            // Success case
            rangeCheck.apply(15)
                      .onSuccess(value -> assertEquals(15, value))
                      .onFailureRun(() -> fail("Should succeed"));

            // Failure case - should use fixed cause regardless of value
            rangeCheck.apply(5)
                      .onSuccessRun(() -> fail("Should fail"))
                      .onFailure(cause -> assertEquals("Fixed error: range check failed", cause.message()));

            // Another failure with different value - same fixed cause
            rangeCheck.apply(0)
                      .onSuccessRun(() -> fail("Should fail"))
                      .onFailure(cause -> assertEquals("Fixed error: range check failed", cause.message()));
        }

        @Test
        @DisplayName("ensureFn with fixed Cause for ternary predicate should work")
        void ensureFnWithFixedCauseForTernaryPredicateShouldWork() {
            var fixedCause = Causes.cause("Fixed error: value out of bounds");
            var betweenCheck = Verify.<Integer, Integer, Integer>ensureFn(fixedCause,
                (value, min, max) -> Verify.Is.between(value, min, max), 10, 20);

            // Success case
            betweenCheck.apply(15)
                        .onSuccess(value -> assertEquals(15, value))
                        .onFailureRun(() -> fail("Should succeed"));

            // Failure case - below range
            betweenCheck.apply(5)
                        .onSuccessRun(() -> fail("Should fail"))
                        .onFailure(cause -> assertEquals("Fixed error: value out of bounds", cause.message()));

            // Failure case - above range, same fixed cause
            betweenCheck.apply(25)
                        .onSuccessRun(() -> fail("Should fail"))
                        .onFailure(cause -> assertEquals("Fixed error: value out of bounds", cause.message()));
        }

        @Test
        @DisplayName("ensureFn with fixed Cause should be reusable across different values")
        void ensureFnWithFixedCauseShouldBeReusableAcrossDifferentValues() {
            var fixedCause = Causes.cause("Password too short");
            var passwordValidator = Verify.<String, Integer, Integer>ensureFn(fixedCause,
                (value, min, max) -> Verify.Is.lenBetween(value, min, max), 8, 20);

            // Multiple validations with same function
            passwordValidator.apply("password123")
                             .onSuccess(value -> assertEquals("password123", value))
                             .onFailureRun(() -> fail("Should succeed"));

            passwordValidator.apply("short")
                             .onSuccessRun(() -> fail("Should fail"))
                             .onFailure(cause -> assertEquals("Password too short", cause.message()));

            passwordValidator.apply("pw")
                             .onSuccessRun(() -> fail("Should fail"))
                             .onFailure(cause -> assertEquals("Password too short", cause.message()));
        }
    }
}