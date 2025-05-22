package org.pragmatica.lang;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    }
}