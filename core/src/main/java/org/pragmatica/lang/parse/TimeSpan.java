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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

import java.time.Duration;
import java.util.regex.Pattern;

import static org.pragmatica.lang.Result.success;

/// Human-friendly duration representation with parsing support.
///
/// Supports parsing duration strings with the following units:
/// - `d` - days
/// - `h` - hours
/// - `m` - minutes
/// - `s` - seconds
/// - `ms` - milliseconds
/// - `us` - microseconds
/// - `ns` - nanoseconds
///
/// Examples:
/// - "100ms" - 100 milliseconds
/// - "1d16h10m18s" - 1 day, 16 hours, 10 minutes, 18 seconds
/// - "1d 16h 10m 18s" - same as above (whitespace allowed between components)
/// - "500us" - 500 microseconds
/// - "1s500ms" - 1.5 seconds
public sealed interface TimeSpan {
    /// Parse a human-friendly duration string.
    ///
    /// @param text the duration string to parse
    /// @return Result containing parsed TimeSpan or parsing error
    static Result<TimeSpan> timeSpan(String text) {
        if (text == null) {
            return TimeSpanError.NULL_INPUT.result();
        }
        var normalized = text.strip();
        if (normalized.isEmpty()) {
            return TimeSpanError.EMPTY_INPUT.result();
        }
        return parseComponents(normalized);
    }

    /// Get the underlying Duration value.
    Duration duration();

    /// Get the total duration in nanoseconds.
    default long toNanos() {
        return duration().toNanos();
    }

    /// Get the total duration in milliseconds.
    default long toMillis() {
        return duration().toMillis();
    }

    /// Get the total duration in seconds.
    default long toSeconds() {
        return duration().toSeconds();
    }

    /// Sealed interface for TimeSpan parsing errors.
    sealed interface TimeSpanError extends Cause {
        enum General implements TimeSpanError {
            NULL_INPUT("Input cannot be null"),
            EMPTY_INPUT("Input cannot be empty"),
            NO_VALID_COMPONENTS("No valid duration components found"),
            INVALID_FORMAT("Invalid duration format");
            private final String message;
            General(String message) {
                this.message = message;
            }
            @Override
            public String message() {
                return message;
            }
        }

        TimeSpanError NULL_INPUT = General.NULL_INPUT;
        TimeSpanError EMPTY_INPUT = General.EMPTY_INPUT;
        TimeSpanError NO_VALID_COMPONENTS = General.NO_VALID_COMPONENTS;
        TimeSpanError INVALID_FORMAT = General.INVALID_FORMAT;

        record InvalidComponent(String component) implements TimeSpanError {
            @Override
            public String message() {
                return "Invalid duration component: " + component;
            }
        }

        record InvalidValue(String value, String unit) implements TimeSpanError {
            @Override
            public String message() {
                return "Invalid value '" + value + "' for unit '" + unit + "'";
            }
        }
    }

    // Pattern for individual duration components: number followed by unit
    // Units: d (days), h (hours), m (minutes), s (seconds), ms (milliseconds), us (microseconds), ns (nanoseconds)
    // Order matters: ms/us/ns must be checked before m/s to avoid partial matches
    Pattern COMPONENT_PATTERN = Pattern.compile("(\\d+)(ns|us|ms|d|h|m|s)");

    // Pattern to validate entire input (only digits, units, and whitespace)
    Pattern VALID_INPUT_PATTERN = Pattern.compile("^[\\d\\s]*(ns|us|ms|d|h|m|s|[\\d\\s])*$", Pattern.CASE_INSENSITIVE);

    private static Result<TimeSpan> parseComponents(String input) {
        // Remove all whitespace for parsing
        var compacted = input.replaceAll("\\s+", "");
        if (compacted.isEmpty()) {
            return TimeSpanError.EMPTY_INPUT.result();
        }
        var matcher = COMPONENT_PATTERN.matcher(compacted);
        var duration = Duration.ZERO;
        var lastEnd = 0;
        var foundAny = false;
        while (matcher.find()) {
            // Check for invalid characters between components
            if (matcher.start() != lastEnd) {
                var invalid = compacted.substring(lastEnd, matcher.start());
                return new TimeSpanError.InvalidComponent(invalid).result();
            }
            var valueStr = matcher.group(1);
            var unit = matcher.group(2);
            try{
                var value = Long.parseLong(valueStr);
                duration = addDuration(duration, value, unit);
                foundAny = true;
            } catch (NumberFormatException e) {
                return new TimeSpanError.InvalidValue(valueStr, unit).result();
            }
            lastEnd = matcher.end();
        }
        // Check for trailing invalid characters
        if (lastEnd != compacted.length()) {
            var invalid = compacted.substring(lastEnd);
            return new TimeSpanError.InvalidComponent(invalid).result();
        }
        if (!foundAny) {
            return TimeSpanError.NO_VALID_COMPONENTS.result();
        }
        return success(new TimeSpanValue(duration));
    }

    private static Duration addDuration(Duration base, long value, String unit) {
        return switch (unit) {
            case "d" -> base.plusDays(value);
            case "h" -> base.plusHours(value);
            case "m" -> base.plusMinutes(value);
            case "s" -> base.plusSeconds(value);
            case "ms" -> base.plusMillis(value);
            case "us" -> base.plusNanos(value * 1000);
            case "ns" -> base.plusNanos(value);
            default -> base;
        };
    }

    record TimeSpanValue(Duration duration) implements TimeSpan {
        @Override
        public String toString() {
            return formatDuration(duration);
        }

        private static String formatDuration(Duration duration) {
            if (duration.isZero()) {
                return "0s";
            }
            var builder = new StringBuilder();
            var nanos = duration.toNanos();
            var days = nanos / (24L * 60 * 60 * 1_000_000_000L);
            nanos %= (24L * 60 * 60 * 1_000_000_000L);
            var hours = nanos / (60L * 60 * 1_000_000_000L);
            nanos %= (60L * 60 * 1_000_000_000L);
            var minutes = nanos / (60L * 1_000_000_000L);
            nanos %= (60L * 1_000_000_000L);
            var seconds = nanos / 1_000_000_000L;
            nanos %= 1_000_000_000L;
            var millis = nanos / 1_000_000L;
            nanos %= 1_000_000L;
            var micros = nanos / 1_000L;
            nanos %= 1_000L;
            if (days > 0) builder.append(days)
                                 .append("d");
            if (hours > 0) builder.append(hours)
                                  .append("h");
            if (minutes > 0) builder.append(minutes)
                                    .append("m");
            if (seconds > 0) builder.append(seconds)
                                    .append("s");
            if (millis > 0) builder.append(millis)
                                   .append("ms");
            if (micros > 0) builder.append(micros)
                                   .append("us");
            if (nanos > 0) builder.append(nanos)
                                  .append("ns");
            return builder.isEmpty()
                   ? "0s"
                   : builder.toString();
        }
    }

    record unused() implements TimeSpan {
        @Override
        public Duration duration() {
            throw new UnsupportedOperationException();
        }
    }
}
