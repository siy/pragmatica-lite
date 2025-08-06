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
import org.pragmatica.lang.Option.None;
import org.pragmatica.lang.Option.Some;
import org.pragmatica.lang.utils.Causes;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OptionTest {
    @SuppressWarnings("EqualsWithItself")   // Intentional test of equals method.
    @Test
    void emptyOptionsAreEqual() {
        assertEquals(Option.empty(), Option.empty());
        assertEquals("None()", Option.empty().toString());
    }

    @Test
    void patternMatchingIsSupported() {
        var optionValue = Option.present(123);

        switch (optionValue) {
            case Some<Integer> some -> assertEquals(123, some.value());
            case None<Integer> none -> fail("Unexpected value: " + none);
        }
    }

    @Test
    void presentOptionsAreEqualIfContentEqual() {
        assertEquals(Option.present(123), Option.present(123));
        assertNotEquals(Option.present(321), Option.present(123));
        assertNotEquals(Option.empty(), Option.present(1));
        assertNotEquals(Option.present(1), Option.empty());
        assertEquals("Some(1)", Option.present(1).toString());
    }

    @Test
    void presentOptionCanBeTransformed() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .map(Object::toString)
              .onPresent(value -> assertEquals("123", value))
              .onEmptyRun(Assertions::fail);
    }

    @Test
    void presentOptionCanBeTransformed2() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .map(() -> 321)
              .onPresent(value -> assertEquals(321, value))
              .onEmptyRun(Assertions::fail);
    }

    @Test
    void emptyOptionRemainsEmptyAfterTransformation() {
        Option.empty()
              .onPresentRun(Assertions::fail)
              .map(Object::toString)
              .onPresentRun(Assertions::fail);
    }

    @Test
    void emptyOptionRemainsEmptyAfterTransformation2() {
        Option.empty()
              .onPresentRun(Assertions::fail)
              .map(() -> 321)
              .onPresentRun(Assertions::fail);
    }

    @Test
    void presentOptionCanBeFlatMapped() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .flatMap(value -> Option.present(value.toString()))
              .onPresent(value -> assertEquals("123", value))
              .onEmpty(Assertions::fail);
    }

    @Test
    void presentOptionCanBeFlatMappedIntoEmptyOption() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .flatMap(_ -> Option.empty())
              .onPresentRun(Assertions::fail);
    }

    @Test
    void emptyOptionRemainsEmptyAfterFlatMap() {
        Option.empty()
              .onPresentRun(Assertions::fail)
              .flatMap(value -> Option.present(value.toString()))
              .onPresentRun(Assertions::fail);
    }

    @Test
    void presentOptionCanBeFlatMapped2() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .flatMap(() -> Option.present(321))
              .onPresent(value -> assertEquals(321, value))
              .onEmpty(Assertions::fail);
    }

    @Test
    void presentOptionCanBeFlatMappedIntoEmptyOption2() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .flatMap(Option::empty)
              .onPresentRun(Assertions::fail);
    }

    @Test
    void emptyOptionRemainsEmptyAfterFlatMap2() {
        Option.empty()
              .onPresentRun(Assertions::fail)
              .flatMap(() -> Option.present(1))
              .onPresentRun(Assertions::fail);
    }

    @Test
    void presentOptionCanBeFilteredToPresentOption() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .filter(value -> value > 120L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail);

    }

    @Test
    void presentOptionCanBeFilteredToEmptyOption() {
        Option.present(123L)
              .onPresent(value -> assertEquals(123L, value))
              .onEmpty(Assertions::fail)
              .filter(value -> value < 120L)
              .onPresentRun(Assertions::fail);
    }

    @Test
    void whenOptionPresentThenPresentSideEffectIsTriggered() {
        var flag = new AtomicBoolean(false);

        Option.present(123L)
              .onPresent(_ -> flag.set(true));

        assertTrue(flag.get());
    }

    @Test
    void whenOptionEmptyThenPresentSideEffectIsNotTriggered() {
        var flag = new AtomicBoolean(false);

        Option.empty()
              .onPresent(_ -> flag.set(true));

        assertFalse(flag.get());
    }

    @Test
    void whenOptionEmptyThenEmptySideEffectIsTriggered() {
        var flag = new AtomicBoolean(false);

        Option.empty()
              .onEmpty(() -> flag.set(true));

        assertTrue(flag.get());
    }

    @Test
    void whenOptionPresentThenEmptySideEffectIsNotTriggered() {
        var flag = new AtomicBoolean(false);

        Option.present(123L)
              .onEmpty(() -> flag.set(true));

        assertFalse(flag.get());
    }

    @Test
    void presentSideEffectIsInvokedForPresentOption() {
        var flagPresent = new AtomicLong(0L);
        var flagEmpty = new AtomicBoolean(false);

        Option.present(123L)
              .apply(() -> flagEmpty.set(true), flagPresent::set);

        assertEquals(123L, flagPresent.get());
        assertFalse(flagEmpty.get());
    }

    @Test
    void emptySideEffectIsInvokedForEmptyOption() {
        var flagPresent = new AtomicLong(0L);
        var flagEmpty = new AtomicBoolean(false);

        Option.<Long>empty()
              .apply(() -> flagEmpty.set(true), flagPresent::set);

        assertEquals(0L, flagPresent.get());
        assertTrue(flagEmpty.get());
    }

    @Test
    void emptyOptionCanBeReplacedWithOtherOption() {
        Option.empty()
              .orElse(Option.present(123L))
              .onEmptyRun(Assertions::fail)
              .onPresent(value -> assertEquals(123L, value));
    }

    @Test
    void emptyOptionCanBeReplacedWithOptionProvidedBySupplier() {
        Option.empty()
              .orElse(() -> Option.present(123L))
              .onEmptyRun(Assertions::fail)
              .onPresent(value -> assertEquals(123L, value));
    }

    @Test
    void valueCanBeObtainedFromOption() {
        assertEquals(321L, Option.present(321L).or(123L));
        assertEquals(123L, Option.empty().or(123L));
    }

    @Test
    void valueCanBeLazilyObtainedFromOption() {
        var flag = new AtomicBoolean(false);
        assertEquals(321L, Option.present(321L).or(() -> {
            flag.set(true);
            return 123L;
        }));
        assertFalse(flag.get());

        assertEquals(123L, Option.empty().or(() -> {
            flag.set(true);
            return 123L;
        }));
        assertTrue(flag.get());
    }

    @Test
    void presentOptionCanBeStreamed() {
        assertEquals(1L, Option.present(1).stream().collect(Collectors.summarizingInt(Integer::intValue)).getSum());
    }

    @Test
    void emptyOptionCanBeStreamedToEmptyStream() {
        assertEquals(0L, Option.empty().stream().count());
    }

    @Test
    void presentOptionCanBeConvertedToSuccessResult() {
        Option.present(1).toResult(Causes.cause("Not expected"))
              .onSuccess(value -> assertEquals(1, value))
              .onFailureRun(Assertions::fail);
    }

    @Test
    void emptyOptionCanBeConvertedToSuppliedResult() {
        Option.empty().toResult(() -> Result.success(123))
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));
    }

    @Test
    void presentOptionCanBeConvertedWithoutSupplyingResult() {
        Option.present(1).toResult(() -> Result.success(321))
              .onSuccess(value -> assertEquals(1, value))
              .onFailureRun(Assertions::fail);
    }

    @Test
    void emptyOptionCanBeConvertedToFailureResult() {
        Option.empty().toResult(Causes.cause("Expected"))
              .onSuccess(_ -> fail("Should not be a success"))
              .onFailure(cause -> assertEquals("Expected", cause.message()));
    }

    @Test
    void presentOptionCanBeConvertedToSuccessPromise() {
        Option<Integer> integerOption = Option.present(1);
        integerOption.<Promise<Integer>>fold(Causes.cause("Not expected")::promise, Promise::success)
              .await() // Not strictly necessary, as Promise is created resolved.
              .onSuccess(value -> assertEquals(1, value))
              .onFailureRun(Assertions::fail);
    }

    @Test
    void emptyOptionCanBeConvertedToSuppliedPromise() {
        Option.empty().<Promise<Object>>fold(() -> Promise.success(123), Promise::success)
              .await() // Not strictly necessary, as Promise is created resolved.
              .onFailureRun(Assertions::fail)
              .onSuccess(value -> assertEquals(123, value));
    }

    @Test
    void presentOptionCanBeConvertedWithoutSupplyingPromise() {
        Option<Integer> integerOption = Option.present(1);
        integerOption.fold(() -> Promise.success(321), Promise::success)
              .await() // Not strictly necessary, as Promise is created resolved.
              .onSuccess(value -> assertEquals(1, value))
              .onFailureRun(Assertions::fail);
    }

    @Test
    void emptyOptionCanBeConvertedToFailurePromise() {
        Cause cause1 = Causes.cause("Expected");
        Option.empty().fold(cause1::promise, Promise::success)
              .await() // Not strictly necessary, as Promise is created resolved.
              .onSuccess(_ -> fail("Should not be a success"))
              .onFailure(cause -> assertEquals("Expected", cause.message()));
    }


    @Test
    void optionCanBeConvertedToOptional() {
        assertEquals(Optional.of(321), Option.present(321).toOptional());
        assertEquals(Optional.empty(), Option.empty().toOptional());
    }

    @Test
    void optionalCanBeConvertedToOption() {
        assertEquals(Option.option(123), Option.from(Optional.of(123)));
        assertEquals(Option.empty(), Option.from(Optional.empty()));
    }

    @Test
    void anyReturnsFirstPresentOption() {
        Option.any(Option.present(1), Option.present(2))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(1, value));

        Option.any(Option.empty(), Option.present(2))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(2, value));

        Option.any(Option.empty(), Option.empty())
              .onPresentRun(Assertions::fail);
    }

    @Test
    void allOfReturnsListIfAllValuesPresent() {
        Option.allOf(Option.present(1), Option.present(2), Option.present(3))
              .onEmpty(Assertions::fail)
              .onPresent(values -> {
                  assertEquals(3, values.size());
                  assertEquals(1, values.get(0));
                  assertEquals(2, values.get(1));
                  assertEquals(3, values.get(2));
              });
    }

    @SuppressWarnings("deprecation")
    @Test
    void unwrapReturnsValueIfPresent() {
        assertEquals(123, Option.present(123).unwrap());
    }

    @SuppressWarnings("deprecation")
    @Test
    void unwrapThrowsExceptionIfEmpty() {
        Assertions.assertThrows(IllegalStateException.class, () -> Option.empty().unwrap());
    }

    @Test
    void allOfReturnsEmptyOneValueIsMissing() {
        Option.allOf(Option.present(1), Option.empty(), Option.present(3))
              .onPresentRun(Assertions::fail);
    }

    @Test
    void anyReturnsFirstPresentOption2() {
        Option.any(Option.present(1), () -> Option.present(2))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(1, value));

        Option.any(Option.empty(), () -> Option.present(2))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(2, value));

        Option.any(Option.empty(), Option::empty)
              .onPresentRun(Assertions::fail);
    }

    @Test
    void liftFnAndLiftMethodsWrapNullableValues() {
        // Test liftFn with non-null value
        Option.lift1(String::toUpperCase, "hello")
              .onEmpty(() -> fail("Should be present"))
              .onPresent(result -> assertEquals("HELLO", result));

        // Test liftFn with null input returns empty
        Option.lift1((String input) -> input.toUpperCase(), null)
              .onPresentRun(() -> fail("Should be empty"));

        // Test liftFn with function returning null
        Option.lift1((String input) -> null, "hello")
              .onPresentRun(() -> fail("Should be empty"));

        // Test lift with non-null supplier result
        Option.lift(() -> "success")
              .onEmpty(() -> fail("Should be present"))
              .onPresent(result -> assertEquals("success", result));

        // Test lift with supplier returning null
        Option.lift(() -> null)
              .onPresentRun(() -> fail("Should be empty"));

        // Test complex transformation chain
        Option.lift1((Integer input) -> input * 2, 5)
              .flatMap(doubled -> Option.lift1((Integer input) -> "Result: " + input, doubled))
              .onEmpty(() -> fail("Should be present"))
              .onPresent(result -> assertEquals("Result: 10", result));
    }

    @Test
    void anyLazilyEvaluatesOtherOptions() {
        var flag = new AtomicBoolean(false);

        Option.any(Option.present(1),
                   () -> {
                       flag.set(true);
                       return Option.present(2);
                   })
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(1, value));

        assertFalse(flag.get());

        Option.any(Option.empty(),
                   () -> {
                       flag.set(true);
                       return Option.present(2);
                   })
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(2, value));

        assertTrue(flag.get());
    }

    @Test
    void anyFindsFirstNonEmptyOption() {
        Option.any(Option.empty(), Option.present(2))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(2, value));

        Option.any(Option.empty(), Option.empty(), Option.present(3))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(3, value));

        Option.any(Option.empty(), Option.empty(), Option.empty(), Option.present(4))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(4, value));

        Option.any(Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.present(5))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(5, value));

        Option.any(Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.present(6))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(6, value));

        Option.any(Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.present(7))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(7, value));

        Option.any(Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.present(8))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(8, value));

        Option.any(Option.empty(),
                   Option.empty(),
                   Option.empty(),
                   Option.empty(),
                   Option.empty(),
                   Option.empty(),
                   Option.empty(),
                   Option.empty(),
                   Option.present(9))
              .onEmpty(Assertions::fail)
              .onPresent(value -> assertEquals(9, value));
    }

    @Test
    void allIsPresentIfAllInputsArePresent() {
        Option.all(Option.present(1))
              .map(v1 -> v1)
              .onPresent(value -> assertEquals(1, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1))
              .map(Integer::sum)
              .onPresent(value -> assertEquals(2, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1))
              .map((v1, v2, v3) -> v1 + v2 + v3)
              .onPresent(value -> assertEquals(3, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1))
              .map((v1, v2, v3, v4) -> v1 + v2 + v3 + v4)
              .onPresent(value -> assertEquals(4, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1))
              .map((v1, v2, v3, v4, v5) -> v1 + v2 + v3 + v4 + v5)
              .onPresent(value -> assertEquals(5, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1))
              .map((v1, v2, v3, v4, v5, v6) -> v1 + v2 + v3 + v4 + v5 + v6)
              .onPresent(value -> assertEquals(6, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1))
              .map((v1, v2, v3, v4, v5, v6, v7) -> v1 + v2 + v3 + v4 + v5 + v6 + v7)
              .onPresent(value -> assertEquals(7, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1))
              .map((v1, v2, v3, v4, v5, v6, v7, v8) -> v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8)
              .onPresent(value -> assertEquals(8, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1))
              .map((v1, v2, v3, v4, v5, v6, v7, v8, v9) -> v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9)
              .onPresent(value -> assertEquals(9, value))
              .onEmpty(Assertions::fail);
    }

    @Test
    void allCanBeFlatMappedIfAllInputsArePresent() {
        Option.all(Option.present(1))
              .flatMap(Option::present)
              .onPresent(value -> assertEquals(1, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1))
              .flatMap((v1, v2) -> Option.present(v1 + v2))
              .onPresent(value -> assertEquals(2, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1))
              .flatMap((v1, v2, v3) -> Option.present(v1 + v2 + v3))
              .onPresent(value -> assertEquals(3, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1))
              .flatMap((v1, v2, v3, v4) -> Option.present(v1 + v2 + v3 + v4))
              .onPresent(value -> assertEquals(4, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1))
              .flatMap((v1, v2, v3, v4, v5) -> Option.present(v1 + v2 + v3 + v4 + v5))
              .onPresent(value -> assertEquals(5, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1))
              .flatMap((v1, v2, v3, v4, v5, v6) -> Option.present(v1 + v2 + v3 + v4 + v5 + v6))
              .onPresent(value -> assertEquals(6, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1))
              .flatMap((v1, v2, v3, v4, v5, v6, v7) -> Option.present(v1 + v2 + v3 + v4 + v5 + v6 + v7))
              .onPresent(value -> assertEquals(7, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1))
              .flatMap((v1, v2, v3, v4, v5, v6, v7, v8) -> Option.present(v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8))
              .onPresent(value -> assertEquals(8, value))
              .onEmpty(Assertions::fail);

        Option.all(Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1),
                   Option.present(1))
              .flatMap((v1, v2, v3, v4, v5, v6, v7, v8, v9) -> Option.present(v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8 + v9))
              .onPresent(value -> assertEquals(9, value))
              .onEmpty(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing1() {
        Option.all(Option.empty()).id().onPresentRun(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing2() {
        Option.all(Option.empty(), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.empty()).id().onPresentRun(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing3() {
        Option.all(Option.empty(), Option.present(1), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.empty(), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.empty()).id().onPresentRun(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing4() {
        Option.all(Option.empty(), Option.present(1), Option.present(1), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.empty(), Option.present(1), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.empty(), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.empty()).id().onPresentRun(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing5() {
        Option.all(Option.empty(), Option.present(1), Option.present(1), Option.present(1), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.empty(), Option.present(1), Option.present(1), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.empty(), Option.present(1), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.empty(), Option.present(1)).id().onPresentRun(Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.empty()).id().onPresentRun(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing6() {
        Option.all(Option.empty(), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1)).id().onPresentRun(
            Assertions::fail);
        Option.all(Option.present(1), Option.empty(), Option.present(1), Option.present(1), Option.present(1), Option.present(1)).id().onPresentRun(
            Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.empty(), Option.present(1), Option.present(1), Option.present(1)).id().onPresentRun(
            Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.empty(), Option.present(1), Option.present(1)).id().onPresentRun(
            Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.empty(), Option.present(1)).id().onPresentRun(
            Assertions::fail);
        Option.all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.empty()).id().onPresentRun(
            Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing7() {
        Option
            .all(Option.empty(), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1))
            .id()
            .onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1), Option.empty(), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1))
            .id()
            .onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1), Option.present(1), Option.empty(), Option.present(1), Option.present(1), Option.present(1), Option.present(1))
            .id()
            .onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1), Option.present(1), Option.present(1), Option.empty(), Option.present(1), Option.present(1), Option.present(1))
            .id()
            .onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.empty(), Option.present(1), Option.present(1))
            .id()
            .onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.empty(), Option.present(1))
            .id()
            .onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.present(1), Option.empty())
            .id()
            .onPresentRun(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing8() {
        Option
            .all(Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty()).id().onPresentRun(Assertions::fail);
    }

    @Test
    void allIsMissingIfAnyInputIsMissing9() {
        Option
            .all(Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty(),
                 Option.present(1)).id().onPresentRun(Assertions::fail);
        Option
            .all(Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.present(1),
                 Option.empty()).id().onPresentRun(Assertions::fail);
    }
}