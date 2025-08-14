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

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeParsersTest {

    @Test
    void testParseLocalDateSuccess() {
        LocalDate expected = LocalDate.of(2023, 12, 25);
        DateTimeParsers.parseLocalDate("2023-12-25")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalDateFailure() {
        DateTimeParsers.parseLocalDate("invalid-date")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParseLocalDateWithFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate expected = LocalDate.of(2023, 12, 25);
        DateTimeParsers.parseLocalDate("25/12/2023", formatter)
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalTimeSuccess() {
        LocalTime expected = LocalTime.of(14, 30, 15);
        DateTimeParsers.parseLocalTime("14:30:15")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalTimeFailure() {
        DateTimeParsers.parseLocalTime("invalid-time")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParseLocalTimeWithFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime expected = LocalTime.of(14, 30);
        DateTimeParsers.parseLocalTime("14:30", formatter)
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalDateTimeSuccess() {
        LocalDateTime expected = LocalDateTime.of(2023, 12, 25, 14, 30, 15);
        DateTimeParsers.parseLocalDateTime("2023-12-25T14:30:15")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseLocalDateTimeFailure() {
        DateTimeParsers.parseLocalDateTime("invalid-datetime")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParseLocalDateTimeWithFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime expected = LocalDateTime.of(2023, 12, 25, 14, 30, 15);
        DateTimeParsers.parseLocalDateTime("25/12/2023 14:30:15", formatter)
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseZonedDateTimeSuccess() {
        ZonedDateTime expected = ZonedDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneId.of("Europe/Paris"));
        DateTimeParsers.parseZonedDateTime("2023-12-25T14:30:15+01:00[Europe/Paris]")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseZonedDateTimeFailure() {
        DateTimeParsers.parseZonedDateTime("invalid-zoneddatetime")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParseOffsetDateTimeSuccess() {
        OffsetDateTime expected = OffsetDateTime.of(2023, 12, 25, 14, 30, 15, 0, ZoneOffset.ofHours(1));
        DateTimeParsers.parseOffsetDateTime("2023-12-25T14:30:15+01:00")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseOffsetDateTimeFailure() {
        DateTimeParsers.parseOffsetDateTime("invalid-offsetdatetime")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParseOffsetTimeSuccess() {
        OffsetTime expected = OffsetTime.of(14, 30, 15, 0, ZoneOffset.ofHours(1));
        DateTimeParsers.parseOffsetTime("14:30:15+01:00")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseOffsetTimeFailure() {
        DateTimeParsers.parseOffsetTime("invalid-offsettime")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParseInstantSuccess() {
        Instant expected = Instant.parse("2023-12-25T14:30:15.123Z");
        DateTimeParsers.parseInstant("2023-12-25T14:30:15.123Z")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseInstantFailure() {
        DateTimeParsers.parseInstant("invalid-instant")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParseDurationSuccess() {
        Duration expected = Duration.ofHours(1).plusMinutes(30);
        DateTimeParsers.parseDuration("PT1H30M")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseDurationFailure() {
        DateTimeParsers.parseDuration("invalid-duration")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testParsePeriodSuccess() {
        Period expected = Period.of(1, 2, 3);
        DateTimeParsers.parsePeriod("P1Y2M3D")
                       .onFailureRun(() -> fail("Expected successful parsing"))
                       .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParsePeriodFailure() {
        DateTimeParsers.parsePeriod("invalid-period")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testNullInputs() {
        DateTimeParsers.parseLocalDate(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseLocalTime(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseLocalDateTime(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseZonedDateTime(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseOffsetDateTime(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseOffsetTime(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseInstant(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseDuration(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parsePeriod(null)
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }

    @Test
    void testEmptyStringInputs() {
        DateTimeParsers.parseLocalDate("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseLocalTime("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseLocalDateTime("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseZonedDateTime("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseOffsetDateTime("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseOffsetTime("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseInstant("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parseDuration("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
                       
        DateTimeParsers.parsePeriod("")
                       .onSuccessRun(() -> fail("Expected parsing failure"))
                       .onFailure(cause -> assertNotNull(cause));
    }
}