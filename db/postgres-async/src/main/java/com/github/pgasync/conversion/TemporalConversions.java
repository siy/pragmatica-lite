package com.github.pgasync.conversion;

import com.github.pgasync.Oid;
import com.github.pgasync.net.SqlException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import static com.github.pgasync.conversion.Common.returnError;
import static java.time.format.DateTimeFormatter.*;

/**
 * @author Antti Laisi
 * <p>
 * TODO: Add support for Java 8 temporal types.
 */
final class TemporalConversions {
    private TemporalConversions() {}

    private static final DateTimeFormatter TIMESTAMP_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter();

    private static final DateTimeFormatter TIMESTAMPTZ_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .appendOffset("+HH:mm", "+00")
            .toFormatter();

    private static final DateTimeFormatter TIMEZ_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_TIME)
            .appendOffset("+HH:mm", "+00")
            .toFormatter();

    static LocalDate toLocalDate(Oid oid, String value) {
        try {
            return switch (oid) {
                case UNSPECIFIED, DATE -> LocalDate.parse(value, ISO_LOCAL_DATE);
                default -> returnError(oid, "LocalDate");
            };
        } catch (DateTimeParseException e) {
            throw new SqlException(STR."Invalid date: \{value}");
        }
    }

    static LocalTime toTime(Oid oid, String value) {
        try {
            return switch (oid) {
                case UNSPECIFIED, TIME -> LocalTime.parse(value, ISO_LOCAL_TIME);
                case TIMETZ -> OffsetTime.parse(value, TIMEZ_FORMAT).toLocalTime();
                default -> returnError(oid, "Time");
            };
        } catch (DateTimeParseException e) {
            throw new SqlException(STR."Invalid time: \{value}");
        }
    }

    static LocalDateTime toDate(Oid oid, String value) {
        try {
            return switch (oid) {
                case UNSPECIFIED, TIMESTAMP -> LocalDateTime.parse(value, TIMESTAMP_FORMAT);
                case TIMESTAMPTZ -> OffsetDateTime.parse(value, TIMESTAMPTZ_FORMAT).toLocalDateTime();
                default -> returnError(oid, "Date");
            };
        } catch (DateTimeParseException e) {
            throw new SqlException(STR."Invalid time: \{value}");
        }
    }

    static Instant toInstant(Oid oid, String value) {
        try {
            return switch (oid) {
                case UNSPECIFIED, TIMESTAMP -> LocalDateTime.parse(value, TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC);
                case TIMESTAMPTZ -> OffsetDateTime.parse(value, TIMESTAMPTZ_FORMAT).toInstant();
                default -> returnError(oid, "Instant");
            };
        } catch (DateTimeParseException e) {
            throw new SqlException(STR."Invalid time: \{value}");
        }
    }

    static String fromLocalDate(LocalDate localDate) {
        return ISO_LOCAL_DATE.format(localDate);
    }

    static String fromLocalTime(LocalTime localTime) {
        return ISO_LOCAL_TIME.format(localTime);
    }

    static String fromLocalDateTime(LocalDateTime localDate) {
        return TIMESTAMP_FORMAT.format(localDate);
    }

    static String fromZoneDateTime(ZonedDateTime zonedDateTime) {
        return TIMESTAMPTZ_FORMAT.format(zonedDateTime);
    }

    static String fromInstant(Instant instant) {
        return TIMESTAMP_FORMAT.format(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
