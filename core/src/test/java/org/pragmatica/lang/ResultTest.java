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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Result.Failure;
import org.pragmatica.lang.Result.Success;
import org.pragmatica.lang.utils.Causes;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    @Test
    void successResultsAreEqualIfValueEqual() {
        assertEquals(Result.success("123"), Result.success(123).map(Objects::toString));
        assertNotEquals(Result.success("321"), Result.success(123).map(Objects::toString));
        assertEquals(Result.success(List.of("123")), Result.success(List.of("123")));
    }

    @Test
    void failureResultsAreEqualIfFailureIsEqual() {
        assertEquals(Result.failure(Causes.cause("123")),
                     Result.success(123).filter(Causes.forOneValue("%d"), v -> v < 0));
        assertNotEquals(Result.failure(Causes.cause("321")),
                        Result.success(123).filter(Causes.forOneValue("%d"), v -> v < 0));
    }

    @Test
    void patterMatchingIsSupportedForSuccess() {
        var resultValue = Result.success(123);

        switch (resultValue) {
            case Success<?> success -> assertEquals(123, success.value());
            case Failure<?> failure -> Assertions.fail("Unexpected failure: " + failure.cause());
        }
    }

    @Test
    void patterMatchingIsSupportedForFailure() {
        var resultValue = Result.failure(Causes.cause("123"));

        switch (resultValue) {
            case Success<?> success -> Assertions.fail("Unexpected success: " + success.value());
            case Failure<?> failure -> assertEquals(Causes.cause("123"), failure.cause());
        }
    }

    @Test
    void successResultCanBeTransformedWithMap() {
        Result.success(123)
              .map(Objects::toString)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals("123", value));
    }

    @Test
    void successResultCanBeTransformedWithMap2() {
        Result.success(123)
              .map(() -> 321)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(321, value));
    }

    @Test
    void successResultCanBeTransformedWithFlatMap() {
        Result.success(123)
              .flatMap(v -> Result.success(v.toString()))
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals("123", value));
    }

    @Test
    void successResultCanBeTransformedWithFlatMap2() {
        Result.success(123)
              .flatMap(() -> Result.success(321))
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(321, value));
    }

    @Test
    void successResultCanBeTransformedWithFlatMapIntoFailure() {
        Result.success(123)
              .flatMap(_ -> Result.failure(Causes.cause("123")))
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void successResultCanBeTransformedWithFlatMapIntoFailure2() {
        Result.success(123)
              .flatMap(() -> Result.failure(Causes.cause("123")))
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void failureResultRemainsUnchangedAfterMap() {
        Result.<Integer>failure(Causes.cause("Some error"))
              .map(Objects::toString)
              .onFailure(cause -> assertEquals("Some error", cause.message()))
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void failureResultRemainsUnchangedAfterMap2() {
        Result.<Integer>failure(Causes.cause("Some error"))
              .map(() -> 321)
              .onFailure(cause -> assertEquals("Some error", cause.message()))
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void failureResultRemainsUnchangedAfterFlatMap() {
        Result.<Integer>failure(Causes.cause("Some error"))
              .flatMap(v -> Result.success(v.toString()))
              .onFailure(cause -> assertEquals("Some error", cause.message()))
              .onSuccessRun(Assertions::fail);

    }

    @Test
    void resultCanBeTraced() {
        Result.<Integer>failure(Causes.cause("Some error"))
              .trace()
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertTrue(cause.message().contains("ResultTest")));
    }

    @Test
    void failureCanBeRecovered() {
        Result.<Integer>failure(Causes.cause("Some error"))
              .recover(_ -> 321)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(321, value));
    }

    @Test
    void successRemainUnchangedAfterRecover() {
        Result.success(123)
              .recover(_ -> 321)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));
    }

    @Test
    void failureResultRemainsUnchangedAfterFlatMap2() {
        Result.<Integer>failure(Causes.cause("Some error"))
              .flatMap(() -> Result.success(321))
              .onFailure(cause -> assertEquals("Some error", cause.message()))
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void onlyOneMethodIsInvokedOnApply() {
        Result.success(321).apply(
                failure -> fail(failure.message()),
                Functions::unitFn
        );

        Result.failure(Causes.cause("Some error")).apply(
                Functions::unitFn,
                value -> fail(value.toString())
        );
    }

    @Test
    void onSuccessIsInvokedForSuccessResult() {
        Result.success(123)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));
        Result.<Integer>failure(Causes.cause("123"))
              .onFailure(cause -> assertEquals("123", cause.message()))
              .onSuccess(value -> fail(value.toString()));
    }

    @Test
    void onSuccessRunIsInvokedForSuccessResult() {
        var flag1 = new AtomicBoolean(false);

        Result.success(123)
              .onFailureRun(Assertions::fail)
              .onSuccessRun(() -> flag1.set(true));

        assertTrue(flag1.get());

        var flag2 = new AtomicBoolean(false);

        Result.<Integer>failure(Causes.cause("123"))
              .onFailureRun(() -> flag2.set(true))
              .onSuccessRun(Assertions::fail);

        assertTrue(flag2.get());
    }

    @Test
    void onFailureIsInvokedForFailure() {
        Result.success(123)
              .onFailure(cause -> fail(cause.message()))
              .onSuccess(value -> assertEquals(123, value));
        Result.<Integer>failure(Causes.cause("123"))
              .onFailure(cause -> assertEquals("123", cause.message()))
              .onSuccess(value -> fail(value.toString()));
    }

    @Test
    void onFailureRunIsInvokedForFailureResult() {
        var flag1 = new AtomicBoolean(false);

        Result.success(123)
              .onFailureRun(Assertions::fail)
              .onSuccessRun(() -> flag1.set(true));

        assertTrue(flag1.get());

        var flag2 = new AtomicBoolean(false);

        Result.<Integer>failure(Causes.cause("123"))
              .onFailureRun(() -> flag2.set(true))
              .onSuccessRun(Assertions::fail);

        assertTrue(flag2.get());
    }

    @Test
    void resultCanBeConvertedToOption() {
        Result.success(123).option()
              .onPresent(value -> assertEquals(123, value))
              .onEmpty(Assertions::fail);

        var flag1 = new AtomicBoolean(false);

        Result.<Integer>failure(Causes.cause("123")).option()
              .onPresent(_ -> fail("Should not happen"))
              .onEmpty(() -> flag1.set(true));

        assertTrue(flag1.get());
    }

    @Test
    void resultStatusCanBeChecked() {
        assertTrue(Result.success(321).isSuccess());
        assertFalse(Result.success(321).isFailure());
        assertFalse(Result.failure(Causes.cause("321")).isSuccess());
        assertTrue(Result.failure(Causes.cause("321")).isFailure());
    }

    @Test
    void successResultCanBeFiltered() {
        Result.success(231)
              .onSuccess(value -> assertEquals(231, value))
              .onFailureRun(Assertions::fail)
              .filter(Causes.forOneValue("Value %d is below threshold"), value -> value > 321)
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertEquals("Value 231 is below threshold", cause.message()));
    }

    @Test
    void liftWrapsCodeWhichCanThrowExceptions() {
        Result.lift(Causes::fromThrowable, () -> throwingFunction(3))
              .onFailure(cause -> assertTrue(cause.message()
                                                  .startsWith("java.lang.IllegalStateException: Just throw exception 3")))
              .onSuccess(_ -> fail("Expecting failure"));

        Result.lift(Causes::fromThrowable, () -> throwingFunction(4))
              .onFailure(cause -> fail(cause.message()))
              .onSuccess(value -> assertEquals("Input:4", value));

        Result.lift(Causes::fromThrowable, () -> {
              })
              .onFailure(cause -> fail(cause.message()))
              .onSuccess(value -> assertEquals(Unit.unit(), value));

        Result.lift(Causes.cause("Oops!"), () -> throwingFunction(3))
              .onFailure(cause -> assertEquals("Oops!", cause.message()))
              .onSuccess(_ -> fail("Expecting failure"));

        Result.lift(Causes.cause("Oops!"), () -> throwingFunction(4))
              .onFailure(cause -> fail(cause.message()))
              .onSuccess(value -> assertEquals("Input:4", value));

        Result.lift(Causes.cause("Oops!"), (Functions.ThrowingRunnable) () -> {
                  throw new IllegalStateException();
              })
              .onSuccess(_ -> fail("Expecting failure"));
    }

    @Test
    void flatMap2AllowsConvenientParameterMixing() {
        // Test successful flatMap2
        Result.success(10)
              .flatMap2((value, multiplier) -> Result.success(value * multiplier), 3)
              .onSuccess(result -> assertEquals(30, result))
              .onFailureRun(() -> fail("Should succeed"));

        // Test flatMap2 with failure in original result
        Result.<Integer>failure(Causes.cause("Original failure"))
              .flatMap2((value, multiplier) -> Result.success(value * multiplier), 3)
              .onSuccessRun(() -> fail("Should fail"))
              .onFailure(cause -> assertEquals("Original failure", cause.message()));

        // Test flatMap2 with failure in mapper
        Result.success(10)
              .flatMap2((_, _) -> Result.failure(Causes.cause("Mapper failure")), 3)
              .onSuccessRun(() -> fail("Should fail"))
              .onFailure(cause -> assertEquals("Mapper failure", cause.message()));
    }

    @Test
    void liftXMethodsWrapThrowingFunctions() {
        // Test liftFn1 with custom exception mapper
        var fn1WithMapper = Result.liftFn1(Causes::fromThrowable, (Integer input) -> {
            if (input < 0) {
                throw new IllegalArgumentException("Negative input");
            }
            return "Value: " + input;
        });

        fn1WithMapper.apply(5)
                     .onSuccess(result -> assertEquals("Value: 5", result))
                     .onFailureRun(() -> fail("Should succeed"));

        fn1WithMapper.apply(-1)
                     .onFailure(cause -> assertTrue(cause.message().contains("IllegalArgumentException")))
                     .onSuccessRun(() -> fail("Should fail"));

        // Test liftFn1 with default exception mapper
        Result.lift1(String::toUpperCase, "hello")
              .onSuccess(result -> assertEquals("HELLO", result))
              .onFailureRun(() -> fail("Should succeed"));

        // Test liftFn2
        Result.lift2(Integer::sum, 3, 4)
              .onSuccess(result -> assertEquals(7, result))
              .onFailureRun(() -> fail("Should succeed"));

        // Test liftFn3  
        Result.lift3((Integer a, Integer b, Integer c) -> a + b + c, 1, 2, 3)
              .onSuccess(result -> assertEquals(6, result))
              .onFailureRun(() -> fail("Should succeed"));

        // Test liftFn2 function factory
        var fn2Factory = Result.liftFn2(Causes::fromThrowable, (Integer a, Integer b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Negative input");
            }
            return a + b;
        });

        fn2Factory.apply(3, 4)
                  .onSuccess(result -> assertEquals(7, result))
                  .onFailureRun(() -> fail("Should succeed"));

        fn2Factory.apply(-1, 4)
                  .onFailure(cause -> assertTrue(cause.message().contains("IllegalArgumentException")))
                  .onSuccessRun(() -> fail("Should fail"));

        // Test liftFn3 function factory
        var fn3Factory = Result.liftFn3(Causes::fromThrowable, (Integer a, Integer b, Integer c) -> {
            if (a < 0 || b < 0 || c < 0) {
                throw new IllegalArgumentException("Negative input");
            }
            return a + b + c;
        });

        fn3Factory.apply(1, 2, 3)
                  .onSuccess(result -> assertEquals(6, result))
                  .onFailureRun(() -> fail("Should succeed"));

        fn3Factory.apply(-1, 2, 3)
                  .onFailure(cause -> assertTrue(cause.message().contains("IllegalArgumentException")))
                  .onSuccessRun(() -> fail("Should fail"));
    }

    @Test
    void resultCanBeConvertedToStream() {
        assertEquals(321, Result.success(321).stream().findFirst().orElseThrow());
        assertTrue(Result.failure(Causes.cause("Some error")).stream().findFirst().isEmpty());
    }

    @Test
    void resultCanBeFiltered() {
        Result.success(321)
              .filter(Causes.cause("Value is below threshold"), value -> value > 123)
              .onSuccess(value -> assertEquals(321, value))
              .onFailureRun(Assertions::fail);

        Result.success(321)
              .filter(Causes.cause("Value is below threshold"), value -> value > 321)
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertEquals("Value is below threshold", cause.message()));
    }

    @Test
    void resultCanBeFiltered2() {
        Result.success(321)
              .filter(Causes.forOneValue("Value %d is below threshold"), value -> value > 123)
              .onSuccess(value -> assertEquals(321, value))
              .onFailureRun(Assertions::fail);

        Result.success(321)
              .filter(Causes.forOneValue("Value %d is below threshold"), value -> value > 321)
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertEquals("Value 321 is below threshold", cause.message()));
    }

    @Test
    void onResultIsInvokedForSuccess() {
        Result.success(321)
              .onResult(result -> assertEquals(Result.success(321), result));
    }

    @Test
    void onResultIsInvokedForFailure() {
        Result.failure(Causes.cause("Some error"))
              .onResult(result -> assertEquals(Result.failure(Causes.cause("Some error")), result));
    }

    @SuppressWarnings("deprecation")
    @Test
    void onResultRunIsInvokedForSuccess() {
        var flag = new AtomicBoolean(false);

        var result = Result.success(321)
                           .onResultRun(() -> flag.set(true));

        assertTrue(flag.get());
        assertEquals(321, result.unwrap());
    }

    @Test
    void onResultRunIsInvokedForFailure() {
        var flag = new AtomicBoolean(false);

        Result.failure(Causes.cause("Some error"))
              .onResultRun(() -> flag.set(true));

        assertTrue(flag.get());
    }

    @SuppressWarnings("deprecation")
    @Test
    void successCanBeUnwrapped() {
        assertEquals(321, Result.success(321).unwrap());
    }

    @SuppressWarnings("deprecation")
    @Test
    void failureCanBeUnwrapped() {
        assertThrows(IllegalStateException.class, () -> Result.failure(Causes.cause("Some error")).unwrap());
    }

    @Test
    void resultCanBeMappedToUnit() {
        Result.success(321)
              .mapToUnit()
              .onSuccess(value -> assertEquals(Unit.unit(), value))
              .onFailureRun(Assertions::fail);

        Result.failure(Causes.cause("Some error"))
              .mapToUnit()
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertEquals(Causes.cause("Some error"), cause));
    }

    @Test
    void resultValueCanBeRetrievedWithOr() {
        assertEquals(321, Result.success(321).or(123));
        assertEquals(123, Result.failure(Causes.cause("Some error")).or(123));
    }

    @Test
    void resultValueCanBeRetrievedWithOr2() {
        assertEquals(321, Result.success(321).or(() -> 123));
        assertEquals(123, Result.failure(Causes.cause("Some error")).or(() -> 123));
    }

    @Test
    void failureResultCanBeReplacedWithOrElse() {
        Result.success(321)
              .orElse(Result.success(123))
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(321, value));
        Result.<Integer>failure(Causes.cause("Some error"))
              .orElse(Result.success(123))
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));
    }

    @Test
    void failureResultCanBeReplacedWithOrElse2() {
        Result.ok(321)
              .orElse(() -> Result.success(123))
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(321, value));
        Result.<Integer>err(Causes.cause("Some error"))
              .orElse(() -> Result.success(123))
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));
    }

    @Test
    void resultCanBeConvertedToPromise() {
        Promise.resolved(Result.success(321))
               .await()
               .onSuccess(value -> assertEquals(321, value))
               .onFailureRun(Assertions::fail);

        Promise.resolved(Result.failure(Causes.cause("Some error")))
               .await()
               .onFailure(cause -> assertEquals(Causes.cause("Some error"), cause))
               .onSuccessRun(Assertions::fail);
    }

    @Test
    void anyReturnsFirstSuccess0() {
        Result.any(Result.success(321),
                   Result.failure(Causes.cause("Another error")),
                   Result.success(123))
              .onSuccess(value -> assertEquals(321, value))
              .onFailureRun(Assertions::fail)
              .onResult(System.out::println);
    }

    @Test
    void anyReturnsFirstSuccess1() {
        Result.any(Result.failure(Causes.cause("Some error")),
                   Result.success(321),
                   Result.failure(Causes.cause("Another error")),
                   Result.success(123))
              .onSuccess(value -> assertEquals(321, value))
              .onFailureRun(Assertions::fail);
    }

    @Test
    void anyReturnsFirstSuccess2() {
        var flag = new AtomicBoolean(false);

        Result.any(Result.failure(Causes.cause("Some error")),
                   () -> Result.success(321),
                   () -> {
                       flag.set(true);
                       return Result.failure(Causes.cause("Another error"));
                   },
                   () -> Result.success(123))
              .onSuccess(value -> assertEquals(321, value))
              .onFailureRun(Assertions::fail);

        assertFalse(flag.get());
    }

    @Test
    void anyReturnsFirstSuccess3() {
        var flag = new AtomicBoolean(false);

        Result.any(Result.success(321),
                   () -> {
                       flag.set(true);
                       return Result.failure(Causes.cause("Another error"));
                   },
                   () -> Result.success(123))
              .onSuccess(value -> assertEquals(321, value))
              .onFailureRun(Assertions::fail);

        assertFalse(flag.get());
    }

    @Test
    void anyReturnsFailureForAllFailures() {
        Result.any(Result.failure(Causes.cause("Some error")),
                   Result.failure(Causes.cause("Another error")),
                   Result.failure(Causes.cause("One more error")),
                   Result.failure(Causes.cause("Yet another error")))
              .onSuccessRun(Assertions::fail)
              .onResult(System.out::println);
    }

    @Test
    void anyReturnsFailureForAllFailures2() {
        Result.any(Result.failure(Causes.cause("Some error")),
                   () -> Result.failure(Causes.cause("Another error")),
                   () -> Result.failure(Causes.cause("One more error")),
                   () -> Result.failure(Causes.cause("Yet another error")))
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allOfReturnsListOfValuesForSuccessInputs() {
        Result.allOf(Result.success(321),
                     Result.success(123))
              .onSuccess(values -> {
                  assertEquals(2, values.size());
                  assertEquals(321, values.get(0));
                  assertEquals(123, values.get(1));
              })
              .onFailureRun(Assertions::fail);

        Result.allOf(List.of(Result.success(321),
                             Result.success(123)))
              .onSuccess(values -> {
                  assertEquals(2, values.size());
                  assertEquals(321, values.get(0));
                  assertEquals(123, values.get(1));
              })
              .onFailureRun(Assertions::fail);

        Result.allOf(Stream.of(Result.success(321),
                               Result.success(123)))
              .onSuccess(values -> {
                  assertEquals(2, values.size());
                  assertEquals(321, values.get(0));
                  assertEquals(123, values.get(1));
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allOfReturnsFailureIfAnyInputIsFailure() {
        Result.allOf(Result.success(321),
                     Result.failure(Causes.cause("Some error 1")),
                     Result.failure(Causes.cause("Some error 2")))
              .onFailure(cause -> assertEquals(2, cause.stream().count()))
              .onSuccessRun(Assertions::fail);

        Result.allOf(List.of(Result.success(321),
                             Result.failure(Causes.cause("Some error")),
                             Result.failure(Causes.cause("Some error 2"))))
              .onFailure(cause -> assertEquals(2, cause.stream().count()))
              .onSuccessRun(Assertions::fail);

        Result.allOf(Stream.of(Result.success(321),
                               Result.failure(Causes.cause("Some error")),
                               Result.failure(Causes.cause("Some error 2"))))
              .onFailure(cause -> assertEquals(2, cause.stream().count()))
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor1SuccessInput() {
        Result.all(Result.success(321))
              .map(value -> {
                  assertEquals(321, value);
                  return value;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor2SuccessInputs() {
        Result.all(Result.success(321), Result.success(123))
              .map((value1, value2) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  return value1 + value2;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor3SuccessInputs() {
        Result.all(Result.success(321), Result.success(123), Result.success(456))
              .map((value1, value2, value3) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  return value1 + value2 + value3;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor4SuccessInputs() {
        Result.all(Result.success(321), Result.success(123), Result.success(456), Result.success(789))
              .map((value1, value2, value3, value4) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  return value1 + value2 + value3 + value4;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor5SuccessInputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654))
              .map((value1, value2, value3, value4, value5) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  return value1 + value2 + value3 + value4 + value5;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor6SuccessInputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987))
              .map((value1, value2, value3, value4, value5, value6) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  return value1 + value2 + value3 + value4 + value5 + value6;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor7SuccessInputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321))
              .map((value1, value2, value3, value4, value5, value6, value7) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  return value1 + value2 + value3 + value4 + value5 + value6 + value7;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor8SuccessInputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.success(123))
              .map((value1, value2, value3, value4, value5, value6, value7, value8) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  assertEquals(123, value8);
                  return value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsSuccessFor9SuccessInputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.success(123),
                   Result.success(456))
              .map((value1, value2, value3, value4, value5, value6, value7, value8, value9) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  assertEquals(123, value8);
                  assertEquals(456, value9);
                  return value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8 + value9;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor1SuccessInputCanFlatMap() {
        Result.all(Result.success(321))
              .flatMap(value -> {
                  assertEquals(321, value);
                  return Result.success(value);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor2SuccessInputsCanFlatMap() {
        Result.all(Result.success(321), Result.success(123))
              .flatMap((value1, value2) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  return Result.success(value1 + value2);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor3SuccessInputsCanFlatMap() {
        Result.all(Result.success(321), Result.success(123), Result.success(456))
              .flatMap((value1, value2, value3) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  return Result.success(value1 + value2 + value3);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor4SuccessInputsCanFlatMap() {
        Result.all(Result.success(321), Result.success(123), Result.success(456), Result.success(789))
              .flatMap((value1, value2, value3, value4) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  return Result.success(value1 + value2 + value3 + value4);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor5SuccessInputsCanFlatMap() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654))
              .flatMap((value1, value2, value3, value4, value5) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  return Result.success(value1 + value2 + value3 + value4 + value5);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor6SuccessInputsCanFlatMap() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987))
              .flatMap((value1, value2, value3, value4, value5, value6) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  return Result.success(value1 + value2 + value3 + value4 + value5 + value6);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor7SuccessInputsCanFlatMap() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321))
              .flatMap((value1, value2, value3, value4, value5, value6, value7) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  return Result.success(value1 + value2 + value3 + value4 + value5 + value6 + value7);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor8SuccessInputsCanFlatMap() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.success(123))
              .flatMap((value1, value2, value3, value4, value5, value6, value7, value8) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  assertEquals(123, value8);
                  return Result.success(value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor9SuccessInputsCanFlatMap() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.success(123),
                   Result.success(456))
              .flatMap((value1, value2, value3, value4, value5, value6, value7, value8, value9) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  assertEquals(123, value8);
                  assertEquals(456, value9);
                  return Result.success(value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8 + value9);
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor1SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321))
              .async()
              .map(value -> {
                  assertEquals(321, value);
                  return value;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor2SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321), Result.success(123))
              .async()
              .map((value1, value2) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  return value1 + value2;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor3SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321), Result.success(123), Result.success(456))
              .async()
              .map((value1, value2, value3) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  return value1 + value2 + value3;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor4SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321), Result.success(123), Result.success(456), Result.success(789))
              .async()
              .map((value1, value2, value3, value4) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  return value1 + value2 + value3 + value4;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor5SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654))
              .async()
              .map((value1, value2, value3, value4, value5) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  return value1 + value2 + value3 + value4 + value5;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor6SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987))
              .async()
              .map((value1, value2, value3, value4, value5, value6) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  return value1 + value2 + value3 + value4 + value5 + value6;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor7SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321))
              .async()
              .map((value1, value2, value3, value4, value5, value6, value7) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  return value1 + value2 + value3 + value4 + value5 + value6 + value7;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor8SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.success(123))
              .async()
              .map((value1, value2, value3, value4, value5, value6, value7, value8) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  assertEquals(123, value8);
                  return value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allFor9SuccessInputsConvertibleToPromise() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.success(123),
                   Result.success(456))
              .async()
              .map((value1, value2, value3, value4, value5, value6, value7, value8, value9) -> {
                  assertEquals(321, value1);
                  assertEquals(123, value2);
                  assertEquals(456, value3);
                  assertEquals(789, value4);
                  assertEquals(654, value5);
                  assertEquals(987, value6);
                  assertEquals(321, value7);
                  assertEquals(123, value8);
                  assertEquals(456, value9);
                  return value1 + value2 + value3 + value4 + value5 + value6 + value7 + value8 + value9;
              })
              .await()
              .onFailureRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn1Input() {
        Result.all(Result.failure(Causes.cause("Some error")))
              .map(value -> value)
              .onSuccessRun(Assertions::fail);
    }

    @SuppressWarnings("Convert2MethodRef") // Explicit sum is more consistent with other tests
    @Test
    void allReturnsFailureForAnyFailureIn2Inputs() {
        Result.all(Result.success(321), Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2) ->
                           value1 + value2)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn3Inputs() {
        Result.all(Result.success(321), Result.success(123), Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2, value3) ->
                           value1 + value2 + value3)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn4Inputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2, value3, value4) ->
                           value1 + value2 + value3 + value4)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn5Inputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2, value3, value4, value5) ->
                           value1 + value2 + value3 + value4 + value5)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn6Inputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2, value3, value4, value5, value6) ->
                           value1 + value2 + value3 + value4 + value5 + value6)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn7Inputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2, value3, value4, value5, value6, value7) ->
                           value1 + value2 + value3 + value4 + value5 + value6 + value7)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn8Inputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2, value3, value4, value5, value6, value7, value8) ->
                           value1
                                   + value2
                                   + value3
                                   + value4
                                   + value5
                                   + value6
                                   + value7
                                   + value8)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsFailureForAnyFailureIn9Inputs() {
        Result.all(Result.success(321),
                   Result.success(123),
                   Result.success(456),
                   Result.success(789),
                   Result.success(654),
                   Result.success(987),
                   Result.success(321),
                   Result.success(123),
                   Result.<Integer>failure(Causes.cause("Some error")))
              .map((value1, value2, value3, value4, value5, value6, value7, value8, value9) ->
                           value1
                                   + value2
                                   + value3
                                   + value4
                                   + value5
                                   + value6
                                   + value7
                                   + value8
                                   + value9)
              .onSuccessRun(Assertions::fail);
    }

    @Test
    void allReturnsCompositeCauseWithAllFailuresIncluded1() {
        var failure = Result.all(Causes.cause("Cause 1").result())
                            .map((_) -> Assertions.fail("Should not be called"));

        failure.onFailure(cause -> {
            assertInstanceOf(Causes.CompositeCause.class, cause);
            assertTrue(cause.message().startsWith("Composite:"));
            cause.stream().forEach(innerCause -> assertInstanceOf(Causes.SimpleCause.class, innerCause));
            assertEquals(1L, cause.stream().count());
        });
    }

    @Test
    void allReturnsCompositeCauseWithAllFailuresIncluded2() {
        var failure = Result.all(Causes.cause("Cause 1").result(),
                                 Causes.cause("Cause 2").result())
                            .map((_, _) -> Assertions.fail("Should not be called"));

        failure.onFailure(cause -> {
            assertInstanceOf(Causes.CompositeCause.class, cause);
            assertTrue(cause.message().startsWith("Composite:"));
            cause.stream().forEach(innerCause -> assertInstanceOf(Causes.SimpleCause.class, innerCause));
            assertEquals(2L, cause.stream().count());
        });
    }

    static String throwingFunction(int i) {
        if (i == 3) {
            throw new IllegalStateException("Just throw exception " + i);
        }

        return "Input:" + i;
    }

    // Tests for Result aliases

    @Test
    void onErrIsAliasForOnFailure() {
        var flag = new AtomicBoolean(false);
        Result.failure(Causes.cause("error"))
              .onErr(cause -> {
                  assertEquals("error", cause.message());
                  flag.set(true);
              });
        assertTrue(flag.get());
    }

    @Test
    void onOkIsAliasForOnSuccess() {
        var flag = new AtomicBoolean(false);
        Result.success(123)
              .onOk(value -> {
                  assertEquals(123, value);
                  flag.set(true);
              });
        assertTrue(flag.get());
    }

    @Test
    void runIsAliasForApply() {
        var successFlag = new AtomicBoolean(false);
        var failureFlag = new AtomicBoolean(false);

        Result.success(123).run(
                _ -> failureFlag.set(true),
                value -> {
                    assertEquals(123, value);
                    successFlag.set(true);
                }
        );
        assertTrue(successFlag.get());
        assertFalse(failureFlag.get());

        successFlag.set(false);
        Result.failure(Causes.cause("error")).run(
                cause -> {
                    assertEquals("error", cause.message());
                    failureFlag.set(true);
                },
                _ -> successFlag.set(true)
        );
        assertTrue(failureFlag.get());
        assertFalse(successFlag.get());
    }

    @Test
    void tryOfWithNoExceptionMapperUsesDefault() {
        Result.tryOf(() -> 123)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));

        Result.tryOf(() -> {
                  throw new RuntimeException("test error");
              })
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertTrue(cause.message().contains("test error")));
    }

    @Test
    void tryOfWithFixedCauseUsesProvidedCause() {
        var fixedCause = Causes.cause("fixed error");

        Result.tryOf(() -> 123, fixedCause)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));

        Result.tryOf(() -> {
                  throw new RuntimeException("original error");
              }, fixedCause)
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertEquals("fixed error", cause.message()));
    }

    @Test
    void tryOfWithExceptionMapperUsesMapper() {
        Result.tryOf(() -> 123, Causes::fromThrowable)
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));

        Result.tryOf(
                      () -> {
                          throw new RuntimeException("mapped error");
                      },
                      ex -> Causes.cause("Custom: " + ex.getMessage()))
              .onSuccessRun(Assertions::fail)
              .onFailure(cause -> assertEquals("Custom: mapped error", cause.message()));
    }

    // Tests for instance all() methods

    @Test
    void instanceAllWith1FunctionChainsDependentOperation() {
        Result.success("base")
              .all(base -> Result.success(base + "-derived"))
              .map(derived -> {
                  assertEquals("base-derived", derived);
                  return derived;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void instanceAllWith2FunctionsChainsDependentOperations() {
        Result.success("token")
              .all(
                      token -> Result.success(token + "-parsed"),
                      token -> Result.success(token.length())
              )
              .map((parsed, length) -> {
                  assertEquals("token-parsed", parsed);
                  assertEquals(5, length);
                  return parsed + ":" + length;
              })
              .onFailureRun(Assertions::fail);
    }

    @Test
    void instanceAllWith3FunctionsChainsDependentOperations() {
        Result.success(10)
              .all(
                      n -> Result.success(n * 2),
                      n -> Result.success(n * 3),
                      n -> Result.success(n * 4)
              )
              .map((r1, r2, r3) -> {
                  assertEquals(20, r1);
                  assertEquals(30, r2);
                  assertEquals(40, r3);
                  return r1 + r2 + r3;
              })
              .onSuccess(sum -> assertEquals(90, sum))
              .onFailureRun(Assertions::fail);
    }

    @Test
    void instanceAllReturnsFailureIfAnyFunctionFails() {
        Result.success("base")
              .all(
                      base -> Result.success(base + "-first"),
                      _ -> Result.failure(Causes.cause("second failed")),
                      base -> Result.success(base + "-third")
              )
              .map((_, _, _) -> Assertions.fail("Should not be called"))
              .onFailure(cause -> assertEquals("second failed", cause.message()));
    }

    @Test
    void instanceAllReturnsFailureIfSourceResultIsFailed() {
        Result.<String>failure(Causes.cause("source failed"))
              .all(
                      base -> Result.success(base + "-derived")
              )
              .map(_ -> Assertions.fail("Should not be called"))
              .onFailure(cause -> assertEquals("source failed", cause.message()));
    }

    @Test
    void instanceAllWithForComprehensionStyleUsage() {
        // Simulates parsing a token and extracting components
        Result.success("jwt-token-value")
              .all(
                      jwt -> Result.success(jwt), // pass through JWT
                      jwt -> Result.success(jwt.split("-")[0]) // extract issuer part
              )
              .map((jwt, issuer) -> new TokenBearer(jwt, issuer))
              .onFailureRun(Assertions::fail)
              .onSuccess(bearer -> {
                  assertEquals("jwt-token-value", bearer.jwt);
                  assertEquals("jwt", bearer.issuer);
              });
    }

    record TokenBearer(String jwt, String issuer) {}

    // ==================== Result.sequence() tests ====================

    @Test
    void sequenceWithSingleSupplierReturnsSuccessWhenSupplierSucceeds() {
        var result = Result.sequence(
                () -> Result.success("value1")
        ).map(v1 -> v1);

        assertTrue(result.isSuccess());
        result.onSuccess(v -> assertEquals("value1", v));
    }

    @Test
    void sequenceWithSingleSupplierReturnsFailureWhenSupplierFails() {
        var result = Result.sequence(
                () -> Result.<String>failure(Causes.cause("error1"))
        ).map(v1 -> v1);

        assertTrue(result.isFailure());
    }

    @Test
    void sequenceWithTwoSuppliersShortCircuitsOnFirstFailure() {
        var secondSupplierCalled = new AtomicBoolean(false);

        var result = Result.sequence(
                () -> Result.<String>failure(Causes.cause("error1")),
                () -> {
                    secondSupplierCalled.set(true);
                    return Result.success(42);
                }
        ).map((v1, v2) -> v1 + v2);

        assertTrue(result.isFailure());
        assertFalse(secondSupplierCalled.get(), "Second supplier should not be called when first fails");
    }

    @Test
    void sequenceWithTwoSuppliersReturnsSuccessWhenBothSucceed() {
        var result = Result.sequence(
                () -> Result.success("hello"),
                () -> Result.success(42)
        ).map((v1, v2) -> v1 + "-" + v2);

        assertTrue(result.isSuccess());
        result.onSuccess(v -> assertEquals("hello-42", v));
    }

    @Test
    void sequenceWithThreeSuppliersShortCircuitsOnSecondFailure() {
        var thirdSupplierCalled = new AtomicBoolean(false);

        var result = Result.sequence(
                () -> Result.success("first"),
                () -> Result.<Integer>failure(Causes.cause("error2")),
                () -> {
                    thirdSupplierCalled.set(true);
                    return Result.success(true);
                }
        ).map((v1, v2, v3) -> v1 + v2 + v3);

        assertTrue(result.isFailure());
        assertFalse(thirdSupplierCalled.get(), "Third supplier should not be called when second fails");
    }

    @Test
    void sequenceWithThreeSuppliersReturnsSuccessWhenAllSucceed() {
        var result = Result.sequence(
                () -> Result.success("a"),
                () -> Result.success("b"),
                () -> Result.success("c")
        ).map((v1, v2, v3) -> v1 + v2 + v3);

        assertTrue(result.isSuccess());
        result.onSuccess(v -> assertEquals("abc", v));
    }

    @Test
    void sequenceIsLazyAndDoesNotEvaluateSuppliersUntilTerminalOperation() {
        var suppliersCalled = new AtomicBoolean(false);

        // Just creating the mapper should not call suppliers
        var mapper = Result.sequence(
                () -> {
                    suppliersCalled.set(true);
                    return Result.success("value");
                }
        );

        assertFalse(suppliersCalled.get(), "Suppliers should not be called until terminal operation");

        // Terminal operation triggers evaluation
        mapper.map(v -> v);

        assertTrue(suppliersCalled.get(), "Suppliers should be called after terminal operation");
    }

    @Test
    void sequenceWithFiveSuppliersReturnsCorrectTuple() {
        var result = Result.sequence(
                () -> Result.success(1),
                () -> Result.success(2),
                () -> Result.success(3),
                () -> Result.success(4),
                () -> Result.success(5)
        ).map((v1, v2, v3, v4, v5) -> v1 + v2 + v3 + v4 + v5);

        assertTrue(result.isSuccess());
        result.onSuccess(v -> assertEquals(15, v));
    }

    @Test
    void sequenceWithNineSuppliersReturnsCorrectTuple() {
        var result = Result.sequence(
                () -> Result.success(1),
                () -> Result.success(2),
                () -> Result.success(3),
                () -> Result.success(4),
                () -> Result.success(5),
                () -> Result.success(6),
                () -> Result.success(7),
                () -> Result.success(8),
                () -> Result.success(9)
        ).map((v1, v2, v3, v4, v5, v6, v7, v8, v9) -> v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9);

        assertTrue(result.isSuccess());
        result.onSuccess(v -> assertEquals(45, v));
    }

    @Test
    void sequenceShortCircuitsAtEighthSupplier() {
        var ninthSupplierCalled = new AtomicBoolean(false);

        var result = Result.sequence(
                () -> Result.success(1),
                () -> Result.success(2),
                () -> Result.success(3),
                () -> Result.success(4),
                () -> Result.success(5),
                () -> Result.success(6),
                () -> Result.success(7),
                () -> Result.<Integer>failure(Causes.cause("error8")),
                () -> {
                    ninthSupplierCalled.set(true);
                    return Result.success(9);
                }
        ).map((v1, v2, v3, v4, v5, v6, v7, v8, v9) -> v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9);

        assertTrue(result.isFailure());
        assertFalse(ninthSupplierCalled.get(), "Ninth supplier should not be called when eighth fails");
    }
}
