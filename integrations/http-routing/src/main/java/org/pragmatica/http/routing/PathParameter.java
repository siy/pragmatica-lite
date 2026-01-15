package org.pragmatica.http.routing;

import org.pragmatica.lang.Result;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Locale;

import static org.pragmatica.http.routing.ParameterError.InvalidParameter;
import static org.pragmatica.http.routing.ParameterError.PathMismatch;

/**
 * Type-safe path parameter parser.
 * <p>
 * Provides factory methods for common parameter types following JBCT naming conventions.
 * All parsers return {@link Result} - they never throw exceptions.
 *
 * @param <T> the type of the parsed parameter value
 */
@SuppressWarnings("unused")
public interface PathParameter<T> {
    /**
     * Parse a string value into the target type.
     *
     * @param value the raw string value from the path
     * @return success with parsed value, or failure with descriptive error
     */
    Result<T> parse(String value);

    /**
     * Creates a spacer that matches a literal path segment.
     * Used for fixed path components like "/api" or "/users".
     *
     * @param text the expected literal value
     * @return parser that succeeds only if value matches exactly
     */
    static PathParameter<String> spacer(String text) {
        return value -> text.equals(value)
                        ? Result.success(value)
                        : new PathMismatch(text, value).result();
    }

    /**
     * String parameter - accepts any string value.
     */
    static PathParameter<String> aString() {
        return Result::success;
    }

    /**
     * Byte parameter - parses signed 8-bit integer.
     */
    static PathParameter<Byte> aByte() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid byte value: " + value),
                                    () -> Byte.parseByte(value));
    }

    /**
     * Short parameter - parses signed 16-bit integer.
     */
    static PathParameter<Short> aShort() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid short value: " + value),
                                    () -> Short.parseShort(value));
    }

    /**
     * Integer parameter - parses signed 32-bit integer.
     */
    static PathParameter<Integer> aInteger() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid integer value: " + value),
                                    () -> Integer.parseInt(value));
    }

    /**
     * Long parameter - parses signed 64-bit integer.
     */
    static PathParameter<Long> aLong() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid long value: " + value),
                                    () -> Long.parseLong(value));
    }

    /**
     * Double parameter - parses 64-bit floating point number.
     */
    static PathParameter<Double> aDouble() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid double value: " + value),
                                    () -> Double.parseDouble(value));
    }

    /**
     * Float parameter - parses 32-bit floating point number.
     */
    static PathParameter<Float> aFloat() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid float value: " + value),
                                    () -> Float.parseFloat(value));
    }

    /**
     * BigDecimal parameter - parses arbitrary precision decimal.
     */
    static PathParameter<BigDecimal> aDecimal() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid decimal value: " + value),
                                    () -> new BigDecimal(value));
    }

    /**
     * Boolean parameter - parses boolean value.
     * Accepts "true"/"false" and "yes"/"no" (case-insensitive).
     */
    static PathParameter<Boolean> aBoolean() {
        return value -> {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
                return Result.success(true);
            } else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) {
                return Result.success(false);
            }
            return new InvalidParameter("Invalid boolean value: " + value + " (expected true/false or yes/no)").result();
        };
    }

    /**
     * OffsetDateTime parameter - parses ISO-8601 date-time with offset.
     * Example: "2023-12-15T10:30:00+01:00"
     */
    static PathParameter<OffsetDateTime> aOffsetDateTime() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid offset date-time value: " + value),
                                    () -> OffsetDateTime.parse(value));
    }

    /**
     * LocalDateTime parameter - parses ISO-8601 date-time without offset.
     * Example: "2023-12-15T10:30:00"
     */
    static PathParameter<LocalDateTime> aLocalDateTime() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid local date-time value: " + value),
                                    () -> LocalDateTime.parse(value));
    }

    /**
     * LocalDate parameter - parses ISO-8601 date.
     * Example: "2023-12-15"
     */
    static PathParameter<LocalDate> aLocalDate() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid local date value: " + value),
                                    () -> LocalDate.parse(value));
    }

    /**
     * LocalTime parameter - parses ISO-8601 time.
     * Example: "10:30:00"
     */
    static PathParameter<LocalTime> aLocalTime() {
        return value -> Result.lift(_ -> new InvalidParameter("Invalid local time value: " + value),
                                    () -> LocalTime.parse(value));
    }

    /**
     * Duration parameter - parses ISO-8601 duration.
     * Accepts both "PT1H30M" and "1H30M" formats.
     */
    static PathParameter<Duration> aDuration() {
        return value -> {
            var upperCase = value.toUpperCase(Locale.ROOT);
            return tryParseDuration(upperCase)
                                   .orElse(() -> tryParseDuration("PT" + upperCase));
        };
    }

    private static Result<Duration> tryParseDuration(String value) {
        return Result.lift(_ -> new InvalidParameter("Invalid duration value: " + value), () -> Duration.parse(value));
    }
}
