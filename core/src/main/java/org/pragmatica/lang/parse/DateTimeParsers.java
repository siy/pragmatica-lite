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
public final class DateTimeParsers {
    private DateTimeParsers() {}

    /// Parse a string as a LocalDate value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed LocalDate or parsing error
    public static Result<LocalDate> parseLocalDate(String text) {
        return Result.lift(() -> LocalDate.parse(text));
    }

    /// Parse a string as a LocalDate value using specified formatter
    /// - **text**: String to parse
    /// - **formatter**: DateTimeFormatter to use for parsing
    /// - **Returns**: Result containing parsed LocalDate or parsing error
    public static Result<LocalDate> parseLocalDate(String text, DateTimeFormatter formatter) {
        return Result.lift(() -> LocalDate.parse(text, formatter));
    }

    /// Parse a string as a LocalTime value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed LocalTime or parsing error
    public static Result<LocalTime> parseLocalTime(String text) {
        return Result.lift(() -> LocalTime.parse(text));
    }

    /// Parse a string as a LocalTime value using specified formatter
    /// - **text**: String to parse
    /// - **formatter**: DateTimeFormatter to use for parsing
    /// - **Returns**: Result containing parsed LocalTime or parsing error
    public static Result<LocalTime> parseLocalTime(String text, DateTimeFormatter formatter) {
        return Result.lift(() -> LocalTime.parse(text, formatter));
    }

    /// Parse a string as a LocalDateTime value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed LocalDateTime or parsing error
    public static Result<LocalDateTime> parseLocalDateTime(String text) {
        return Result.lift(() -> LocalDateTime.parse(text));
    }

    /// Parse a string as a LocalDateTime value using specified formatter
    /// - **text**: String to parse
    /// - **formatter**: DateTimeFormatter to use for parsing
    /// - **Returns**: Result containing parsed LocalDateTime or parsing error
    public static Result<LocalDateTime> parseLocalDateTime(String text, DateTimeFormatter formatter) {
        return Result.lift(() -> LocalDateTime.parse(text, formatter));
    }

    /// Parse a string as a ZonedDateTime value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed ZonedDateTime or parsing error
    public static Result<ZonedDateTime> parseZonedDateTime(String text) {
        return Result.lift(() -> ZonedDateTime.parse(text));
    }

    /// Parse a string as a ZonedDateTime value using specified formatter
    /// - **text**: String to parse
    /// - **formatter**: DateTimeFormatter to use for parsing
    /// - **Returns**: Result containing parsed ZonedDateTime or parsing error
    public static Result<ZonedDateTime> parseZonedDateTime(String text, DateTimeFormatter formatter) {
        return Result.lift(() -> ZonedDateTime.parse(text, formatter));
    }

    /// Parse a string as an OffsetDateTime value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed OffsetDateTime or parsing error
    public static Result<OffsetDateTime> parseOffsetDateTime(String text) {
        return Result.lift(() -> OffsetDateTime.parse(text));
    }

    /// Parse a string as an OffsetDateTime value using specified formatter
    /// - **text**: String to parse
    /// - **formatter**: DateTimeFormatter to use for parsing
    /// - **Returns**: Result containing parsed OffsetDateTime or parsing error
    public static Result<OffsetDateTime> parseOffsetDateTime(String text, DateTimeFormatter formatter) {
        return Result.lift(() -> OffsetDateTime.parse(text, formatter));
    }

    /// Parse a string as an OffsetTime value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed OffsetTime or parsing error
    public static Result<OffsetTime> parseOffsetTime(String text) {
        return Result.lift(() -> OffsetTime.parse(text));
    }

    /// Parse a string as an OffsetTime value using specified formatter
    /// - **text**: String to parse
    /// - **formatter**: DateTimeFormatter to use for parsing
    /// - **Returns**: Result containing parsed OffsetTime or parsing error
    public static Result<OffsetTime> parseOffsetTime(String text, DateTimeFormatter formatter) {
        return Result.lift(() -> OffsetTime.parse(text, formatter));
    }

    /// Parse a string as an Instant value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed Instant or parsing error
    public static Result<Instant> parseInstant(String text) {
        return Result.lift(() -> Instant.parse(text));
    }

    /// Parse a string as a Duration value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed Duration or parsing error
    public static Result<Duration> parseDuration(String text) {
        return Result.lift(() -> Duration.parse(text));
    }

    /// Parse a string as a Period value using ISO format
    /// - **text**: String to parse
    /// - **Returns**: Result containing parsed Period or parsing error
    public static Result<Period> parsePeriod(String text) {
        return Result.lift(() -> Period.parse(text));
    }
}