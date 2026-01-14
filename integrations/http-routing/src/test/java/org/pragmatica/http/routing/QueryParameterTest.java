package org.pragmatica.http.routing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.http.routing.QueryParameter.*;

class QueryParameterTest {

    @Nested
    class StringParameter {
        @Test
        void parse_succeeds_forValidString() {
            var param = aString("name");

            param.parse(List.of("hello"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals("hello", value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aString("name");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aString("name");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aString("name");

            param.parse(List.of("first", "second", "third"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals("first", value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aString("myParam");

            assertEquals("myParam", param.name());
        }
    }

    @Nested
    class IntegerParameter {
        @Test
        void parse_succeeds_forValidInteger() {
            var param = aInteger("count");

            param.parse(List.of("42"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(42, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forNegativeInteger() {
            var param = aInteger("count");

            param.parse(List.of("-100"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(-100, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aInteger("count");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aInteger("count");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_fails_forInvalidFormat() {
            var param = aInteger("count");

            param.parse(List.of("not-a-number"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_fails_forFloatValue() {
            var param = aInteger("count");

            param.parse(List.of("42.5"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aInteger("count");

            param.parse(List.of("10", "20", "30"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(10, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aInteger("myCount");

            assertEquals("myCount", param.name());
        }
    }

    @Nested
    class LongParameter {
        @Test
        void parse_succeeds_forValidLong() {
            var param = aLong("id");

            param.parse(List.of("9223372036854775807"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(9223372036854775807L, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forNegativeLong() {
            var param = aLong("id");

            param.parse(List.of("-9223372036854775808"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(-9223372036854775808L, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aLong("id");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aLong("id");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_fails_forInvalidFormat() {
            var param = aLong("id");

            param.parse(List.of("abc"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aLong("id");

            param.parse(List.of("100", "200"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(100L, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aLong("userId");

            assertEquals("userId", param.name());
        }
    }

    @Nested
    class BooleanParameter {
        @Test
        void parse_succeeds_forTrue() {
            var param = aBoolean("active");

            param.parse(List.of("true"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(true, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forFalse() {
            var param = aBoolean("active");

            param.parse(List.of("false"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(false, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forYes() {
            var param = aBoolean("active");

            param.parse(List.of("yes"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(true, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forNo() {
            var param = aBoolean("active");

            param.parse(List.of("no"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(false, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_caseInsensitive() {
            var param = aBoolean("active");

            param.parse(List.of("TRUE"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(true, value))
                                      .onEmpty(() -> fail()));

            param.parse(List.of("FALSE"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(false, value))
                                      .onEmpty(() -> fail()));

            param.parse(List.of("YES"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(true, value))
                                      .onEmpty(() -> fail()));

            param.parse(List.of("NO"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(false, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aBoolean("active");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aBoolean("active");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_fails_forInvalidValue() {
            var param = aBoolean("active");

            param.parse(List.of("maybe"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_fails_forNumericValue() {
            var param = aBoolean("active");

            param.parse(List.of("1"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aBoolean("active");

            param.parse(List.of("true", "false"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(true, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aBoolean("isActive");

            assertEquals("isActive", param.name());
        }
    }

    @Nested
    class DoubleParameter {
        @Test
        void parse_succeeds_forValidDouble() {
            var param = aDouble("price");

            param.parse(List.of("19.99"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(19.99, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forNegativeDouble() {
            var param = aDouble("price");

            param.parse(List.of("-5.5"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(-5.5, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forIntegerValue() {
            var param = aDouble("price");

            param.parse(List.of("100"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(100.0, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forScientificNotation() {
            var param = aDouble("value");

            param.parse(List.of("1.5e10"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(1.5e10, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aDouble("price");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aDouble("price");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_fails_forInvalidFormat() {
            var param = aDouble("price");

            param.parse(List.of("abc"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aDouble("price");

            param.parse(List.of("10.5", "20.5"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(10.5, value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aDouble("minPrice");

            assertEquals("minPrice", param.name());
        }
    }

    @Nested
    class DecimalParameter {
        @Test
        void parse_succeeds_forValidDecimal() {
            var param = aDecimal("amount");

            param.parse(List.of("123.456"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(new BigDecimal("123.456"), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forNegativeDecimal() {
            var param = aDecimal("amount");

            param.parse(List.of("-999.99"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(new BigDecimal("-999.99"), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forLargeDecimal() {
            var param = aDecimal("amount");

            param.parse(List.of("12345678901234567890.12345678901234567890"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value ->
                     assertEquals(new BigDecimal("12345678901234567890.12345678901234567890"), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aDecimal("amount");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aDecimal("amount");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_fails_forInvalidFormat() {
            var param = aDecimal("amount");

            param.parse(List.of("not-a-decimal"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aDecimal("amount");

            param.parse(List.of("100.00", "200.00"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(new BigDecimal("100.00"), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aDecimal("totalAmount");

            assertEquals("totalAmount", param.name());
        }
    }

    @Nested
    class LocalDateParameter {
        @Test
        void parse_succeeds_forValidDate() {
            var param = aLocalDate("date");

            param.parse(List.of("2023-12-15"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(LocalDate.of(2023, 12, 15), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forLeapYearDate() {
            var param = aLocalDate("date");

            param.parse(List.of("2024-02-29"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(LocalDate.of(2024, 2, 29), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aLocalDate("date");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aLocalDate("date");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_fails_forInvalidFormat() {
            var param = aLocalDate("date");

            param.parse(List.of("12/15/2023"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_fails_forInvalidDate() {
            var param = aLocalDate("date");

            param.parse(List.of("2023-02-30"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_fails_forDateTimeValue() {
            var param = aLocalDate("date");

            param.parse(List.of("2023-12-15T10:30:00"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aLocalDate("date");

            param.parse(List.of("2023-01-01", "2023-12-31"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value -> assertEquals(LocalDate.of(2023, 1, 1), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aLocalDate("startDate");

            assertEquals("startDate", param.name());
        }
    }

    @Nested
    class LocalDateTimeParameter {
        @Test
        void parse_succeeds_forValidDateTime() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of("2023-12-15T10:30:00"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value ->
                     assertEquals(LocalDateTime.of(2023, 12, 15, 10, 30, 0), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forDateTimeWithSeconds() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of("2023-12-15T10:30:45"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value ->
                     assertEquals(LocalDateTime.of(2023, 12, 15, 10, 30, 45), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_succeeds_forDateTimeWithNanos() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of("2023-12-15T10:30:45.123456789"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value ->
                     assertEquals(LocalDateTime.of(2023, 12, 15, 10, 30, 45, 123456789), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void parse_returnsNone_forNullList() {
            var param = aLocalDateTime("timestamp");

            param.parse(null)
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_returnsNone_forEmptyList() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of())
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(_ -> fail()));
        }

        @Test
        void parse_fails_forInvalidFormat() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of("12/15/2023 10:30:00"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_fails_forDateOnlyValue() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of("2023-12-15"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_fails_forInvalidTime() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of("2023-12-15T25:00:00"))
                 .onSuccess(_ -> fail());
        }

        @Test
        void parse_usesFirstValue_forMultipleValues() {
            var param = aLocalDateTime("timestamp");

            param.parse(List.of("2023-01-01T00:00:00", "2023-12-31T23:59:59"))
                 .onFailure(_ -> fail())
                 .onSuccess(opt -> opt.onPresent(value ->
                     assertEquals(LocalDateTime.of(2023, 1, 1, 0, 0, 0), value))
                                      .onEmpty(() -> fail()));
        }

        @Test
        void name_returnsParameterName() {
            var param = aLocalDateTime("createdAt");

            assertEquals("createdAt", param.name());
        }
    }

    @Nested
    class ErrorMessages {
        @Test
        void integerError_containsParameterNameAndValue() {
            var param = aInteger("count");

            param.parse(List.of("invalid"))
                 .onSuccess(_ -> fail())
                 .onFailure(cause -> {
                     assertInstanceOf(ParameterError.InvalidParameter.class, cause);
                     var msg = cause.message();
                     assert msg.contains("count") : "Error should contain param name";
                     assert msg.contains("invalid") : "Error should contain invalid value";
                 });
        }

        @Test
        void booleanError_indicatesExpectedValues() {
            var param = aBoolean("active");

            param.parse(List.of("maybe"))
                 .onSuccess(_ -> fail())
                 .onFailure(cause -> {
                     var msg = cause.message();
                     assert msg.contains("true/false") || msg.contains("yes/no")
                         : "Error should indicate expected values";
                 });
        }
    }
}
