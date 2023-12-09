package com.github.pgasync.conversion;

import com.github.pgasync.net.SqlException;
import com.github.pgasync.Oid;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * @author Antti Laisi
 * <p>
 * TODO: Add support for Java 8 temporal types.
 */
class TemporalConversions {

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
        switch (oid) {
            case UNSPECIFIED: // fallthrough
            case DATE:
                try {
                    return LocalDate.parse(value, ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    throw new SqlException("Invalid date: " + value);
                }
            default:
                throw new SqlException("Unsupported conversion " + oid.name() + " -> Date");
        }
    }

    static Time toTime(Oid oid, String value) {
        try {
            switch (oid) {
                case UNSPECIFIED: // fallthrough
                case TIME:
                    return Time.valueOf(LocalTime.parse(value, ISO_LOCAL_TIME));
                case TIMETZ:
                    return Time.valueOf(OffsetTime.parse(value, TIMEZ_FORMAT).toLocalTime());
                default:
                    throw new SqlException("Unsupported conversion " + oid.name() + " -> Time");
            }
        } catch (DateTimeParseException e) {
            throw new SqlException("Invalid time: " + value);
        }
    }

    static Timestamp toTimestamp(Oid oid, String value) {
        try {
            switch (oid) {
                case UNSPECIFIED: // fallthrough
                case TIMESTAMP:
                    return Timestamp.from(LocalDateTime.parse(value, TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC));
                case TIMESTAMPTZ:
                    return Timestamp.from(OffsetDateTime.parse(value, TIMESTAMPTZ_FORMAT).toInstant());
                default:
                    throw new SqlException("Unsupported conversion " + oid.name() + " -> Time");
            }
        } catch (DateTimeParseException e) {
            throw new SqlException("Invalid time: " + value);
        }
    }

    static Date toDate(Oid oid, String value) {
        try {
            switch (oid) {
                case UNSPECIFIED: // fallthrough
                case TIMESTAMP:
                    return new Date(LocalDateTime.parse(value, TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC).toEpochMilli());
                case TIMESTAMPTZ:
                    return new Date(OffsetDateTime.parse(value, TIMESTAMPTZ_FORMAT).toInstant().toEpochMilli());
                default:
                    throw new SqlException("Unsupported conversion " + oid.name() + " -> Time");
            }
        } catch (DateTimeParseException e) {
            throw new SqlException("Invalid time: " + value);
        }
    }

    static Instant toInstant(Oid oid, String value) {
        try {
            switch (oid) {
                case UNSPECIFIED: // fallthrough
                case TIMESTAMP:
                    return LocalDateTime.parse(value, TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC);
                case TIMESTAMPTZ:
                    return OffsetDateTime.parse(value, TIMESTAMPTZ_FORMAT).toInstant();
                default:
                    throw new SqlException("Unsupported conversion " + oid.name() + " -> Time");
            }
        } catch (DateTimeParseException e) {
            throw new SqlException("Invalid time: " + value);
        }
    }

    static String fromLocalDate(LocalDate localDate) {
        return ISO_LOCAL_DATE.format(localDate);
    }

    static String fromTime(Time time) {
        return ISO_LOCAL_TIME.format(time.toLocalTime());
    }

    static String fromInstant(Instant instant) {
        return TIMESTAMP_FORMAT.format(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    static String fromDate(Date date) {
        return fromInstant(Instant.ofEpochMilli(date.getTime()));
    }

    static String fromTimestamp(Timestamp ts) {
        return fromInstant(Instant.ofEpochMilli(ts.getTime()));
    }
}
