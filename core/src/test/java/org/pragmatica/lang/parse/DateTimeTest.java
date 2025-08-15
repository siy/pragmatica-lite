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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
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
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testNullInputs() {
        DateTime.parseLocalDate(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseLocalTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseLocalDateTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseZonedDateTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseOffsetDateTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseOffsetTime(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseInstant(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseDuration(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parsePeriod(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testEmptyStringInputs() {
        DateTime.parseLocalDate("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseLocalTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseLocalDateTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseZonedDateTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseOffsetDateTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseOffsetTime("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseInstant("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parseDuration("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);

        DateTime.parsePeriod("")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }
}