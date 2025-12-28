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

import org.pragmatica.lang.Result;

import java.time.*;
import java.time.format.DateTimeFormatter;

/// Functional wrappers for Java Time API parsing methods that return Result<T> instead of throwing exceptions
public sealed interface DateTime {
    /// Parse a string as a LocalDate value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed LocalDate or parsing error
    static Result<LocalDate> parseLocalDate(String text) {
        return Result.lift1(LocalDate::parse, text);
    }

    /// Parse a string as a LocalDate value using specified formatter
    ///
    /// @param text String to parse
    /// @param formatter DateTimeFormatter to use for parsing
    ///
    /// @return Result containing parsed LocalDate or parsing error
    static Result<LocalDate> parseLocalDate(String text, DateTimeFormatter formatter) {
        return Result.lift2(LocalDate::parse, text, formatter);
    }

    /// Parse a string as a LocalTime value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed LocalTime or parsing error
    static Result<LocalTime> parseLocalTime(String text) {
        return Result.lift1(LocalTime::parse, text);
    }

    /// Parse a string as a LocalTime value using specified formatter
    ///
    /// @param text String to parse
    /// @param formatter DateTimeFormatter to use for parsing
    ///
    /// @return Result containing parsed LocalTime or parsing error
    static Result<LocalTime> parseLocalTime(String text, DateTimeFormatter formatter) {
        return Result.lift2(LocalTime::parse, text, formatter);
    }

    /// Parse a string as a LocalDateTime value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed LocalDateTime or parsing error
    static Result<LocalDateTime> parseLocalDateTime(String text) {
        return Result.lift1(LocalDateTime::parse, text);
    }

    /// Parse a string as a LocalDateTime value using specified formatter
    ///
    /// @param text String to parse
    /// @param formatter DateTimeFormatter to use for parsing
    ///
    /// @return Result containing parsed LocalDateTime or parsing error
    static Result<LocalDateTime> parseLocalDateTime(String text, DateTimeFormatter formatter) {
        return Result.lift2(LocalDateTime::parse, text, formatter);
    }

    /// Parse a string as a ZonedDateTime value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed ZonedDateTime or parsing error
    static Result<ZonedDateTime> parseZonedDateTime(String text) {
        return Result.lift1(ZonedDateTime::parse, text);
    }

    /// Parse a string as a ZonedDateTime value using specified formatter
    ///
    /// @param text String to parse
    /// @param formatter DateTimeFormatter to use for parsing
    ///
    /// @return Result containing parsed ZonedDateTime or parsing error
    static Result<ZonedDateTime> parseZonedDateTime(String text, DateTimeFormatter formatter) {
        return Result.lift2(ZonedDateTime::parse, text, formatter);
    }

    /// Parse a string as an OffsetDateTime value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed OffsetDateTime or parsing error
    static Result<OffsetDateTime> parseOffsetDateTime(String text) {
        return Result.lift1(OffsetDateTime::parse, text);
    }

    /// Parse a string as an OffsetDateTime value using specified formatter
    ///
    /// @param text String to parse
    /// @param formatter DateTimeFormatter to use for parsing
    ///
    /// @return Result containing parsed OffsetDateTime or parsing error
    static Result<OffsetDateTime> parseOffsetDateTime(String text, DateTimeFormatter formatter) {
        return Result.lift2(OffsetDateTime::parse, text, formatter);
    }

    /// Parse a string as an OffsetTime value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed OffsetTime or parsing error
    static Result<OffsetTime> parseOffsetTime(String text) {
        return Result.lift1(OffsetTime::parse, text);
    }

    /// Parse a string as an OffsetTime value using specified formatter
    ///
    /// @param text String to parse
    /// @param formatter DateTimeFormatter to use for parsing
    ///
    /// @return Result containing parsed OffsetTime or parsing error
    static Result<OffsetTime> parseOffsetTime(String text, DateTimeFormatter formatter) {
        return Result.lift2(OffsetTime::parse, text, formatter);
    }

    /// Parse a string as an Instant value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed Instant or parsing error
    static Result<Instant> parseInstant(String text) {
        return Result.lift1(Instant::parse, text);
    }

    /// Parse a string as a Duration value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed Duration or parsing error
    static Result<Duration> parseDuration(String text) {
        return Result.lift1(Duration::parse, text);
    }

    /// Parse a string as a Period value using ISO format
    ///
    /// @param text String to parse
    ///
    /// @return Result containing parsed Period or parsing error
    static Result<Period> parsePeriod(String text) {
        return Result.lift1(Period::parse, text);
    }

    record unused() implements DateTime {}
}
