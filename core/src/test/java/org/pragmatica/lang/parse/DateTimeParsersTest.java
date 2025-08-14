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

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Result;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeParsersTest {

    @Test
    void testParseLocalDateSuccess() {
        Result<LocalDate> result = DateTimeParsers.parseLocalDate("2023-12-25");
        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2023, 12, 25), result.unwrap());
    }

    @Test
    void testParseLocalDateFailure() {
        Result<LocalDate> result = DateTimeParsers.parseLocalDate("invalid-date");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseLocalDateWithFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Result<LocalDate> result = DateTimeParsers.parseLocalDate("25/12/2023", formatter);
        assertTrue(result.isSuccess());
        assertEquals(LocalDate.of(2023, 12, 25), result.unwrap());
    }

    @Test
    void testParseLocalTimeSuccess() {
        Result<LocalTime> result = DateTimeParsers.parseLocalTime("14:30:15");
        assertTrue(result.isSuccess());
        assertEquals(LocalTime.of(14, 30, 15), result.unwrap());
    }

    @Test
    void testParseLocalTimeFailure() {
        Result<LocalTime> result = DateTimeParsers.parseLocalTime("invalid-time");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseLocalTimeWithFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        Result<LocalTime> result = DateTimeParsers.parseLocalTime("14:30", formatter);
        assertTrue(result.isSuccess());
        assertEquals(LocalTime.of(14, 30), result.unwrap());
    }

    @Test
    void testParseLocalDateTimeSuccess() {
        Result<LocalDateTime> result = DateTimeParsers.parseLocalDateTime("2023-12-25T14:30:15");
        assertTrue(result.isSuccess());
        assertEquals(LocalDateTime.of(2023, 12, 25, 14, 30, 15), result.unwrap());
    }

    @Test
    void testParseLocalDateTimeFailure() {
        Result<LocalDateTime> result = DateTimeParsers.parseLocalDateTime("invalid-datetime");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseLocalDateTimeWithFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Result<LocalDateTime> result = DateTimeParsers.parseLocalDateTime("25/12/2023 14:30:15", formatter);
        assertTrue(result.isSuccess());
        assertEquals(LocalDateTime.of(2023, 12, 25, 14, 30, 15), result.unwrap());
    }

    @Test
    void testParseZonedDateTimeSuccess() {
        Result<ZonedDateTime> result = DateTimeParsers.parseZonedDateTime("2023-12-25T14:30:15+01:00[Europe/Paris]");
        assertTrue(result.isSuccess());
        ZonedDateTime expected = ZonedDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneId.of("Europe/Paris"));
        assertEquals(expected, result.unwrap());
    }

    @Test
    void testParseZonedDateTimeFailure() {
        Result<ZonedDateTime> result = DateTimeParsers.parseZonedDateTime("invalid-zoneddatetime");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseOffsetDateTimeSuccess() {
        Result<OffsetDateTime> result = DateTimeParsers.parseOffsetDateTime("2023-12-25T14:30:15+01:00");
        assertTrue(result.isSuccess());
        OffsetDateTime expected = OffsetDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneOffset.ofHours(1));
        assertEquals(expected, result.unwrap());
    }

    @Test
    void testParseOffsetDateTimeFailure() {
        Result<OffsetDateTime> result = DateTimeParsers.parseOffsetDateTime("invalid-offsetdatetime");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseOffsetTimeSuccess() {
        Result<OffsetTime> result = DateTimeParsers.parseOffsetTime("14:30:15+01:00");
        assertTrue(result.isSuccess());
        OffsetTime expected = OffsetTime.of(14, 30, 15, 0, ZoneOffset.ofHours(1));
        assertEquals(expected, result.unwrap());
    }

    @Test
    void testParseOffsetTimeFailure() {
        Result<OffsetTime> result = DateTimeParsers.parseOffsetTime("invalid-offsettime");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseInstantSuccess() {
        Result<Instant> result = DateTimeParsers.parseInstant("2023-12-25T14:30:15.123Z");
        assertTrue(result.isSuccess());
        Instant expected = Instant.parse("2023-12-25T14:30:15.123Z");
        assertEquals(expected, result.unwrap());
    }

    @Test
    void testParseInstantFailure() {
        Result<Instant> result = DateTimeParsers.parseInstant("invalid-instant");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseDurationSuccess() {
        Result<Duration> result = DateTimeParsers.parseDuration("PT1H30M");
        assertTrue(result.isSuccess());
        Duration expected = Duration.ofHours(1).plusMinutes(30);
        assertEquals(expected, result.unwrap());
    }

    @Test
    void testParseDurationFailure() {
        Result<Duration> result = DateTimeParsers.parseDuration("invalid-duration");
        assertTrue(result.isFailure());
    }

    @Test
    void testParsePeriodSuccess() {
        Result<Period> result = DateTimeParsers.parsePeriod("P1Y2M3D");
        assertTrue(result.isSuccess());
        Period expected = Period.of(1, 2, 3);
        assertEquals(expected, result.unwrap());
    }

    @Test
    void testParsePeriodFailure() {
        Result<Period> result = DateTimeParsers.parsePeriod("invalid-period");
        assertTrue(result.isFailure());
    }

    @Test
    void testNullInputs() {
        assertTrue(DateTimeParsers.parseLocalDate(null).isFailure());
        assertTrue(DateTimeParsers.parseLocalTime(null).isFailure());
        assertTrue(DateTimeParsers.parseLocalDateTime(null).isFailure());
        assertTrue(DateTimeParsers.parseZonedDateTime(null).isFailure());
        assertTrue(DateTimeParsers.parseOffsetDateTime(null).isFailure());
        assertTrue(DateTimeParsers.parseOffsetTime(null).isFailure());
        assertTrue(DateTimeParsers.parseInstant(null).isFailure());
        assertTrue(DateTimeParsers.parseDuration(null).isFailure());
        assertTrue(DateTimeParsers.parsePeriod(null).isFailure());
    }

    @Test
    void testEmptyStringInputs() {
        assertTrue(DateTimeParsers.parseLocalDate("").isFailure());
        assertTrue(DateTimeParsers.parseLocalTime("").isFailure());
        assertTrue(DateTimeParsers.parseLocalDateTime("").isFailure());
        assertTrue(DateTimeParsers.parseZonedDateTime("").isFailure());
        assertTrue(DateTimeParsers.parseOffsetDateTime("").isFailure());
        assertTrue(DateTimeParsers.parseOffsetTime("").isFailure());
        assertTrue(DateTimeParsers.parseInstant("").isFailure());
        assertTrue(DateTimeParsers.parseDuration("").isFailure());
        assertTrue(DateTimeParsers.parsePeriod("").isFailure());
    }
}