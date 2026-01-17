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

package org.pragmatica.lang.parse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.parse.TimeSpan.TimeSpanError;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TimeSpanTest {

    @Nested
    class SuccessfulParsing {

        @Test
        void timeSpan_succeeds_forMilliseconds() {
            TimeSpan.timeSpan("100ms")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(100, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_forSeconds() {
            TimeSpan.timeSpan("5s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(5000, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_forMinutes() {
            TimeSpan.timeSpan("2m")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(120, ts.toSeconds()));
        }

        @Test
        void timeSpan_succeeds_forHours() {
            TimeSpan.timeSpan("3h")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(3 * 60 * 60, ts.toSeconds()));
        }

        @Test
        void timeSpan_succeeds_forDays() {
            TimeSpan.timeSpan("2d")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(2 * 24 * 60 * 60, ts.toSeconds()));
        }

        @Test
        void timeSpan_succeeds_forMicroseconds() {
            TimeSpan.timeSpan("500us")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(500_000, ts.toNanos()));
        }

        @Test
        void timeSpan_succeeds_forNanoseconds() {
            TimeSpan.timeSpan("1000ns")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(1000, ts.toNanos()));
        }

        @Test
        void timeSpan_succeeds_forComplexDuration() {
            // 1d16h10m18s = 1 day + 16 hours + 10 minutes + 18 seconds
            var expected = Duration.ofDays(1)
                                   .plusHours(16)
                                   .plusMinutes(10)
                                   .plusSeconds(18);

            TimeSpan.timeSpan("1d16h10m18s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expected, ts.duration()));
        }

        @Test
        void timeSpan_succeeds_forComplexDurationWithWhitespace() {
            var expected = Duration.ofDays(1)
                                   .plusHours(16)
                                   .plusMinutes(10)
                                   .plusSeconds(18);

            TimeSpan.timeSpan("1d 16h 10m 18s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expected, ts.duration()));
        }

        @Test
        void timeSpan_succeeds_forMixedUnits() {
            // 1s500ms = 1500 milliseconds
            TimeSpan.timeSpan("1s500ms")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(1500, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_forZeroValues() {
            TimeSpan.timeSpan("0s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(0, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_forLargeValues() {
            TimeSpan.timeSpan("365d")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(365L * 24 * 60 * 60, ts.toSeconds()));
        }

        @Test
        void timeSpan_succeeds_withLeadingWhitespace() {
            TimeSpan.timeSpan("  100ms")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(100, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_withTrailingWhitespace() {
            TimeSpan.timeSpan("100ms  ")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(100, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_forEquivalentDurations() {
            var expectedMillis = 86400000L;

            TimeSpan.timeSpan("1d")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expectedMillis, ts.toMillis()));

            TimeSpan.timeSpan("24h")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expectedMillis, ts.toMillis()));

            TimeSpan.timeSpan("1440m")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expectedMillis, ts.toMillis()));

            TimeSpan.timeSpan("86400s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expectedMillis, ts.toMillis()));

            TimeSpan.timeSpan("86400000ms")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expectedMillis, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_forAllUnitsCombo() {
            var expected = Duration.ofDays(1)
                                   .plusHours(2)
                                   .plusMinutes(3)
                                   .plusSeconds(4)
                                   .plusMillis(5)
                                   .plusNanos(6_007L); // 6us + 7ns

            TimeSpan.timeSpan("1d2h3m4s5ms6us7ns")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expected, ts.duration()));
        }
    }

    @Nested
    class FailureCases {

        @Test
        void timeSpan_fails_forNullInput() {
            TimeSpan.timeSpan(null)
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertInstanceOf(TimeSpanError.class, cause));
        }

        @Test
        void timeSpan_fails_forEmptyInput() {
            TimeSpan.timeSpan("")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertEquals(TimeSpanError.EMPTY_INPUT, cause));
        }

        @Test
        void timeSpan_fails_forWhitespaceOnlyInput() {
            TimeSpan.timeSpan("   ")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertEquals(TimeSpanError.EMPTY_INPUT, cause));
        }

        @Test
        void timeSpan_fails_forInvalidUnit() {
            TimeSpan.timeSpan("100x")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertInstanceOf(TimeSpanError.InvalidComponent.class, cause));
        }

        @Test
        void timeSpan_fails_forNoUnit() {
            TimeSpan.timeSpan("100")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertInstanceOf(TimeSpanError.InvalidComponent.class, cause));
        }

        @Test
        void timeSpan_fails_forUnitOnly() {
            TimeSpan.timeSpan("ms")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertInstanceOf(TimeSpanError.class, cause));
        }

        @Test
        void timeSpan_fails_forNegativeValue() {
            TimeSpan.timeSpan("-100ms")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertInstanceOf(TimeSpanError.class, cause));
        }

        @Test
        void timeSpan_fails_forDecimalValue() {
            TimeSpan.timeSpan("1.5s")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertInstanceOf(TimeSpanError.class, cause));
        }

        @Test
        void timeSpan_fails_forInvalidFormats() {
            TimeSpan.timeSpan("abc")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(Assertions::assertNotNull);

            TimeSpan.timeSpan("hello")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(Assertions::assertNotNull);

            TimeSpan.timeSpan("one second")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(Assertions::assertNotNull);

            TimeSpan.timeSpan("1 second")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(Assertions::assertNotNull);
        }

        @Test
        void timeSpan_fails_forMixedValidAndInvalid() {
            TimeSpan.timeSpan("1s2x")
                    .onSuccessRun(Assertions::fail)
                    .onFailure(cause -> assertInstanceOf(TimeSpanError.InvalidComponent.class, cause));
        }
    }

    @Nested
    class DurationAccess {

        @Test
        void duration_returnsCorrectValue() {
            var expected = Duration.ofMillis(100);

            TimeSpan.timeSpan("100ms")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(expected, ts.duration()));
        }

        @Test
        void toNanos_returnsCorrectValue() {
            TimeSpan.timeSpan("1ms")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(1_000_000, ts.toNanos()));
        }

        @Test
        void toMillis_returnsCorrectValue() {
            TimeSpan.timeSpan("1s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(1000, ts.toMillis()));
        }

        @Test
        void toSeconds_returnsCorrectValue() {
            TimeSpan.timeSpan("1m")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(60, ts.toSeconds()));
        }
    }

    @Nested
    class ToStringFormatting {

        @Test
        void toString_formatsSimpleDuration() {
            TimeSpan.timeSpan("100ms")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals("100ms", ts.toString()));
        }

        @Test
        void toString_formatsComplexDuration() {
            TimeSpan.timeSpan("1d 2h 3m 4s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals("1d2h3m4s", ts.toString()));
        }

        @Test
        void toString_formatsZeroDuration() {
            TimeSpan.timeSpan("0s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals("0s", ts.toString()));
        }

        @Test
        void toString_omitsZeroComponents() {
            TimeSpan.timeSpan("1h30m")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals("1h30m", ts.toString()));
        }

        @Test
        void toString_includesSubsecondUnits() {
            TimeSpan.timeSpan("1s500ms250us125ns")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals("1s500ms250us125ns", ts.toString()));
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void timeSpan_succeeds_forRepeatedUnits() {
            // 1s1s = 2s (accumulates)
            TimeSpan.timeSpan("1s1s")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(2000, ts.toMillis()));
        }

        @Test
        void timeSpan_succeeds_forVeryLargeDuration() {
            TimeSpan.timeSpan("9999999999ns")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> assertEquals(9999999999L, ts.toNanos()));
        }

        @Test
        void timeSpan_succeeds_forMultipleWhitespace() {
            TimeSpan.timeSpan("1d   2h      3m")
                    .onFailureRun(Assertions::fail)
                    .onSuccess(ts -> {
                        var expected = Duration.ofDays(1).plusHours(2).plusMinutes(3);
                        assertEquals(expected, ts.duration());
                    });
        }
    }
}
