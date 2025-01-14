package org.pragmatica.config.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.config.api.ParameterType.*;
import org.pragmatica.lang.type.TypeToken;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.lang.Result.success;

class ParameterTypeTest {
    public static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.of(2007, 12, 3, 10, 15, 30, 0, ZoneOffset.ofHours(1));
    public static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2007, 12, 3, 10, 15, 30);
    public static final LocalDate LOCAL_DATE = LocalDate.of(2007, 12, 3);
    public static final LocalTime LOCAL_TIME = LocalTime.of(10, 15, 30);

    @Test
    void tokensAreOfCorrectTypeForPlainTypes() {
        assertEquals(new TypeToken<String>() {}, StringParameter.INSTANCE.token());
        assertEquals(new TypeToken<Long>() {}, LongParameter.INSTANCE.token());
        assertEquals(new TypeToken<BigDecimal>() {}, DecimalParameter.INSTANCE.token());
        assertEquals(new TypeToken<Boolean>() {}, BooleanParameter.INSTANCE.token());
        assertEquals(new TypeToken<OffsetDateTime>() {}, OffsetDateTimeParameter.INSTANCE.token());
        assertEquals(new TypeToken<LocalDateTime>() {}, LocalDateTimeParameter.INSTANCE.token());
        assertEquals(new TypeToken<LocalDate>() {}, LocalDateParameter.INSTANCE.token());
        assertEquals(new TypeToken<LocalTime>() {}, LocalTimeParameter.INSTANCE.token());
        assertEquals(new TypeToken<Duration>() {}, DurationParameter.INSTANCE.token());
    }

    @Test
    void tokensAreOfCorrectTypeForArrayTypes() {
        assertEquals(new TypeToken<List<String>>() {}, StringArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<Long>>() {}, LongArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<BigDecimal>>() {}, DecimalArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<Boolean>>() {}, BooleanArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<OffsetDateTime>>() {}, OffsetDateTimeArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<LocalDateTime>>() {}, LocalDateTimeArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<LocalDate>>() {}, LocalDateArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<LocalTime>>() {}, LocalTimeArrayParameter.INSTANCE.token());
        assertEquals(new TypeToken<List<Duration>>() {}, DurationArrayParameter.INSTANCE.token());
    }

    //TODO: negative tests
    @Test
    void parsingOfCorrectDataWorksAsExpected() {
        assertEquals(success("test"), StringParameter.INSTANCE.apply("test"));
        assertEquals(success("test"), StringParameter.INSTANCE.apply("\"test\""));
        assertEquals(success("te\"st"), StringParameter.INSTANCE.apply("\"te\\\"st\""));
        assertEquals(success("test"), StringParameter.INSTANCE.apply("'test'"));
        assertEquals(success("te'st"), StringParameter.INSTANCE.apply("'te\\'st'"));
        assertEquals(success("test,  test"), StringParameter.INSTANCE.apply(" test,  test "));
        assertEquals(success("test,"), StringParameter.INSTANCE.apply("test, "));
        assertEquals(success(123456789L), LongParameter.INSTANCE.apply("123456789"));
        assertEquals(success(BigDecimal.valueOf(-3.1415925e-4)), DecimalParameter.INSTANCE.apply("-3.1415925e-4"));
        assertEquals(success(true), BooleanParameter.INSTANCE.apply("true"));
        assertEquals(success(false), BooleanParameter.INSTANCE.apply("false"));
        assertEquals(success(OFFSET_DATE_TIME), OffsetDateTimeParameter.INSTANCE.apply("2007-12-03T10:15:30+01:00"));
        assertEquals(success(LOCAL_DATE_TIME), LocalDateTimeParameter.INSTANCE.apply("2007-12-03T10:15:30"));
        assertEquals(success(LOCAL_DATE), LocalDateParameter.INSTANCE.apply("2007-12-03"));
        assertEquals(success(LOCAL_TIME), LocalTimeParameter.INSTANCE.apply("10:15:30"));
        assertEquals(success(Duration.ofSeconds(3456)), DurationParameter.INSTANCE.apply("PT3456S"));
        // without PT prefix
        assertEquals(success(Duration.ofSeconds(3456)), DurationParameter.INSTANCE.apply("3456s"));
    }

    @Test
    void emptyArraysParsedIntoEmptyResults() {
        StringArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        StringArrayParameter.INSTANCE.apply("[,]").onFailureRun(Assertions::fail).onSuccess(list -> assertEquals(2, list.size()));
        StringArrayParameter.INSTANCE.apply("[,,]").onFailureRun(Assertions::fail).onSuccess(list -> assertEquals(3, list.size()));

        LongArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        DecimalArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        BooleanArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        OffsetDateTimeArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        LocalDateTimeArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        LocalDateArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        LocalTimeArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
        DurationArrayParameter.INSTANCE.apply("[]").onFailureRun(Assertions::fail).onSuccess(list -> assertTrue(list.isEmpty()));
    }

    @Test
    void parsingOfCorrectArrayDataWorksAsExpected() {
        assertEquals(success(List.of("one")), StringArrayParameter.INSTANCE.apply("[ one ]"));
        assertEquals(success(List.of("one", "two")), StringArrayParameter.INSTANCE.apply("[ one, two ]"));
        assertEquals(success(List.of("one", "two")), StringArrayParameter.INSTANCE.apply("[  'one' ,  'two'   ]"));
        assertEquals(success(List.of("one", "two")), StringArrayParameter.INSTANCE.apply("['one','two']"));
        assertEquals(success(List.of("o\ne", "two")), StringArrayParameter.INSTANCE.apply("['o\\ne','t\\wo']"));
        assertEquals(success(List.of("'one", "two'")), StringArrayParameter.INSTANCE.apply("['\\'one','two\\'']"));

        assertEquals(success(List.of(123L)), LongArrayParameter.INSTANCE.apply("[123]"));
        assertEquals(success(List.of(123L, -456L)), LongArrayParameter.INSTANCE.apply("[ 123, -456]"));

        assertEquals(success(List.of(BigDecimal.valueOf(1.23))), DecimalArrayParameter.INSTANCE.apply("[ 1.23]"));
        assertEquals(success(List.of(BigDecimal.valueOf(1.23), BigDecimal.valueOf(4.56e7))), DecimalArrayParameter.INSTANCE.apply("[ 1.23, 4.56e7]"));

        assertEquals(success(List.of(true)), BooleanArrayParameter.INSTANCE.apply("[true]"));
        assertEquals(success(List.of(true, false)), BooleanArrayParameter.INSTANCE.apply("[true, false ]"));

        assertEquals(success(List.of(OFFSET_DATE_TIME)), OffsetDateTimeArrayParameter.INSTANCE.apply("[2007-12-03T10:15:30+01:00]"));

        assertEquals(success(List.of(LOCAL_DATE_TIME)), LocalDateTimeArrayParameter.INSTANCE.apply("[2007-12-03T10:15:30]"));

        assertEquals(success(List.of(LOCAL_DATE)), LocalDateArrayParameter.INSTANCE.apply("[2007-12-03]"));

        assertEquals(success(List.of(LOCAL_TIME)), LocalTimeArrayParameter.INSTANCE.apply("[10:15:30]"));

        assertEquals(success(List.of(Duration.ofSeconds(25), Duration.ofDays(1).plusHours(4).plusMinutes(6))),
                     DurationArrayParameter.INSTANCE.apply("[25s,P1DT4H6M]"));
    }
}
