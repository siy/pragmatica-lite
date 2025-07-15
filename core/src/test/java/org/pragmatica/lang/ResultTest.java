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
                     Result.success(123).filter(Causes.forValue("%d"), v -> v < 0));
        assertNotEquals(Result.failure(Causes.cause("321")),
                        Result.success(123).filter(Causes.forValue("%d"), v -> v < 0));
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
              .filter(Causes.forValue("Value %d is below threshold"), value -> value > 321)
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

        Result.lift(Causes::fromThrowable, () -> {})
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
              .filter(Causes.forValue("Value %d is below threshold"), value -> value > 123)
              .onSuccess(value -> assertEquals(321, value))
              .onFailureRun(Assertions::fail);

        Result.success(321)
              .filter(Causes.forValue("Value %d is below threshold"), value -> value > 321)
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
              .toPromise()
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
              .toPromise()
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
              .toPromise()
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
              .toPromise()
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
              .toPromise()
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
              .toPromise()
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
              .toPromise()
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
              .toPromise()
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
              .toPromise()
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

    static String throwingFunction(int i) {
        if (i == 3) {
            throw new IllegalStateException("Just throw exception " + i);
        }

        return "Input:" + i;
    }
}
