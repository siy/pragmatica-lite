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
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeTest {

    @Test
    void testParseLocalDateSuccess() {
        var expected = LocalDate.of(2023, 12, 25);
        DateTime.parseLocalDate("2023-12-25")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalDateFailure() {
        DateTime.parseLocalDate("invalid-date")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseLocalDateWithFormatter() {
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var expected = LocalDate.of(2023, 12, 25);
        DateTime.parseLocalDate("25/12/2023", formatter)
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalTimeSuccess() {
        var expected = LocalTime.of(14, 30, 15);
        DateTime.parseLocalTime("14:30:15")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalTimeFailure() {
        DateTime.parseLocalTime("invalid-time")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseLocalTimeWithFormatter() {
        var formatter = DateTimeFormatter.ofPattern("HH:mm");
        var expected = LocalTime.of(14, 30);
        DateTime.parseLocalTime("14:30", formatter)
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalDateTimeSuccess() {
        var expected = LocalDateTime.of(2023, 12, 25, 14, 30, 15);
        DateTime.parseLocalDateTime("2023-12-25T14:30:15")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalDateTimeFailure() {
        DateTime.parseLocalDateTime("invalid-datetime")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseLocalDateTimeWithFormatter() {
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        var expected = LocalDateTime.of(2023, 12, 25, 14, 30, 15);
        DateTime.parseLocalDateTime("25/12/2023 14:30:15", formatter)
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseZonedDateTimeSuccess() {
        var expected = ZonedDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneId.of("Europe/Paris"));
        DateTime.parseZonedDateTime("2023-12-25T14:30:15+01:00[Europe/Paris]")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseZonedDateTimeFailure() {
        DateTime.parseZonedDateTime("invalid-zoneddatetime")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseOffsetDateTimeSuccess() {
        var expected = OffsetDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneOffset.ofHours(1));
        DateTime.parseOffsetDateTime("2023-12-25T14:30:15+01:00")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseOffsetDateTimeFailure() {
        DateTime.parseOffsetDateTime("invalid-offsetdatetime")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseOffsetTimeSuccess() {
        var expected = OffsetTime.of(14, 30, 15, 0, ZoneOffset.ofHours(1));
        DateTime.parseOffsetTime("14:30:15+01:00")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseOffsetTimeFailure() {
        DateTime.parseOffsetTime("invalid-offsettime")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseInstantSuccess() {
        var expected = Instant.parse("2023-12-25T14:30:15.123Z");
        DateTime.parseInstant("2023-12-25T14:30:15.123Z")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseInstantFailure() {
        DateTime.parseInstant("invalid-instant")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseDurationSuccess() {
        var expected = Duration.ofHours(1).plusMinutes(30);
        DateTime.parseDuration("PT1H30M")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseDurationFailure() {
        DateTime.parseDuration("invalid-duration")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParsePeriodSuccess() {
        var expected = Period.of(1, 2, 3);
        DateTime.parsePeriod("P1Y2M3D")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParsePeriodFailure() {
        DateTime.parsePeriod("invalid-period")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testNullInputs() {
        DateTime.parseLocalDate(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseLocalTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseLocalDateTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseZonedDateTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseOffsetDateTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseOffsetTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseInstant(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseDuration(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parsePeriod(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testEmptyStringInputs() {
        DateTime.parseLocalDate("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseLocalTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseLocalDateTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseZonedDateTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseOffsetDateTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseOffsetTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseInstant("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parseDuration("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));

        DateTime.parsePeriod("")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    // Edge cases tests

    @Test
    void testLeapYearDateValid() {
        DateTime.parseLocalDate("2024-02-29")
                .onFailureRun(Assertions::fail)
                .onSuccess(date -> assertEquals(29, date.getDayOfMonth()));
    }

    @Test
    void testNonLeapYearFeb29Invalid() {
        DateTime.parseLocalDate("2023-02-29")
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testTimeBoundaries() {
        DateTime.parseLocalTime("23:59:59")
                .onFailureRun(Assertions::fail)
                .onSuccess(time -> assertEquals(LocalTime.of(23, 59, 59), time));

        DateTime.parseLocalTime("00:00:00")
                .onFailureRun(Assertions::fail)
                .onSuccess(time -> assertEquals(LocalTime.of(0, 0, 0), time));
    }

    @Test
    void testNegativeDuration() {
        DateTime.parseDuration("PT-1H")
                .onFailureRun(Assertions::fail)
                .onSuccess(duration -> assertEquals(Duration.ofHours(-1), duration));
    }

    @Test
    void testZeroDuration() {
        DateTime.parseDuration("PT0S")
                .onFailureRun(Assertions::fail)
                .onSuccess(duration -> assertEquals(Duration.ZERO, duration));
    }

    @Test
    void testFormatterFailureLocalDate() {
        var formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTime.parseLocalDate("25/12/2023", formatter)  // DD/MM format input with MM/DD formatter
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testFormatterFailureLocalTime() {
        var formatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTime.parseLocalTime("25:99", formatter)  // Invalid time
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testFormatterFailureLocalDateTime() {
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        DateTime.parseLocalDateTime("2023-12-25T14:30:15", formatter)  // ISO format with custom formatter
                .onSuccessRun(Assertions::fail)
                .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testZonedDateTimeWithFormatter() {
        var formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        var expected = ZonedDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneId.of("Europe/Paris"));
        DateTime.parseZonedDateTime("2023-12-25T14:30:15+01:00[Europe/Paris]", formatter)
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testOffsetDateTimeWithFormatter() {
        var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        var expected = OffsetDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneOffset.ofHours(1));
        DateTime.parseOffsetDateTime("2023-12-25T14:30:15+01:00", formatter)
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testOffsetTimeWithFormatter() {
        var formatter = DateTimeFormatter.ISO_OFFSET_TIME;
        var expected = OffsetTime.of(14, 30, 15, 0, ZoneOffset.ofHours(1));
        DateTime.parseOffsetTime("14:30:15+01:00", formatter)
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(expected, value));
    }
}