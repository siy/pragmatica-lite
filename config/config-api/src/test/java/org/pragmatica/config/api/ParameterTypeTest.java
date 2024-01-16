package org.pragmatica.config.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.config.api.ParameterType.*;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterTypeTest {
    public static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.of(2007, 12, 3, 10, 15, 30, 0, ZoneOffset.ofHours(1));
    public static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2007, 12, 3, 10, 15, 30);
    public static final LocalDate LOCAL_DATE = LocalDate.of(2007, 12, 3);
    public static final LocalTime LOCAL_TIME = LocalTime.of(10, 15, 30);

    @Test
    void tokensAreOfCorrectTypeForPlainTypes() {
        assertEquals(StringParameter.INSTANCE.token(), new TypeToken<String>() {});
        assertEquals(LongParameter.INSTANCE.token(), new TypeToken<Long>() {});
        assertEquals(DecimalParameter.INSTANCE.token(), new TypeToken<BigDecimal>() {});
        assertEquals(BooleanParameter.INSTANCE.token(), new TypeToken<Boolean>() {});
        assertEquals(OffsetDateTimeParameter.INSTANCE.token(), new TypeToken<OffsetDateTime>() {});
        assertEquals(LocalDateTimeParameter.INSTANCE.token(), new TypeToken<LocalDateTime>() {});
        assertEquals(LocalDateParameter.INSTANCE.token(), new TypeToken<LocalDate>() {});
        assertEquals(LocalTimeParameter.INSTANCE.token(), new TypeToken<LocalTime>() {});
        assertEquals(DurationParameter.INSTANCE.token(), new TypeToken<Duration>() {});
    }

    @Test
    void tokensAreOfCorrectTypeForArrayTypes() {
        assertEquals(StringArrayParameter.INSTANCE.token(), new TypeToken<List<String>>() {});
        assertEquals(LongArrayParameter.INSTANCE.token(), new TypeToken<List<Long>>() {});
        assertEquals(DecimalArrayParameter.INSTANCE.token(), new TypeToken<List<BigDecimal>>() {});
        assertEquals(BooleanArrayParameter.INSTANCE.token(), new TypeToken<List<Boolean>>() {});
        assertEquals(OffsetDateTimeArrayParameter.INSTANCE.token(), new TypeToken<List<OffsetDateTime>>() {});
        assertEquals(LocalDateTimeArrayParameter.INSTANCE.token(), new TypeToken<List<LocalDateTime>>() {});
        assertEquals(LocalDateArrayParameter.INSTANCE.token(), new TypeToken<List<LocalDate>>() {});
        assertEquals(LocalTimeArrayParameter.INSTANCE.token(), new TypeToken<List<LocalTime>>() {});
        assertEquals(DurationArrayParameter.INSTANCE.token(), new TypeToken<List<Duration>>() {});
    }

    //TODO: negative tests
    @Test
    void parsingOfCorrectDataWorksAsExpected() {
        assertEquals(StringParameter.INSTANCE.apply("test"), Result.success("test"));
        assertEquals(LongParameter.INSTANCE.apply("123456789"), Result.success(123456789L));
        assertEquals(DecimalParameter.INSTANCE.apply("-3.1415925e-4"), Result.success(BigDecimal.valueOf(-3.1415925e-4)));
        assertEquals(BooleanParameter.INSTANCE.apply("true"), Result.success(true));
        assertEquals(BooleanParameter.INSTANCE.apply("false"), Result.success(false));
        assertEquals(OffsetDateTimeParameter.INSTANCE.apply("2007-12-03T10:15:30+01:00"), Result.success(OFFSET_DATE_TIME));
        assertEquals(LocalDateTimeParameter.INSTANCE.apply("2007-12-03T10:15:30"), Result.success(LOCAL_DATE_TIME));
        assertEquals(LocalDateParameter.INSTANCE.apply("2007-12-03"), Result.success(LOCAL_DATE));
        assertEquals(LocalTimeParameter.INSTANCE.apply("10:15:30"), Result.success(LOCAL_TIME));
        assertEquals(DurationParameter.INSTANCE.apply("PT3456S"), Result.success(Duration.ofSeconds(3456)));
        // without PT prefix
        assertEquals(DurationParameter.INSTANCE.apply("3456s"), Result.success(Duration.ofSeconds(3456)));
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
        assertEquals(Result.success(List.of("one")), StringArrayParameter.INSTANCE.apply("[ one ]"));
        assertEquals(Result.success(List.of("one", "two")), StringArrayParameter.INSTANCE.apply("[ one, two ]"));
        assertEquals(Result.success(List.of("one", "two")), StringArrayParameter.INSTANCE.apply("[  'one' ,  'two'   ]"));
        assertEquals(Result.success(List.of("one", "two")), StringArrayParameter.INSTANCE.apply("['one','two']"));
        assertEquals(Result.success(List.of("o\ne", "two")), StringArrayParameter.INSTANCE.apply("['o\\ne','t\\wo']"));
        assertEquals(Result.success(List.of("'one", "two'")), StringArrayParameter.INSTANCE.apply("['\\'one','two\\'']"));

        assertEquals(LongArrayParameter.INSTANCE.apply("[123]"), Result.success(List.of(123L)));
        assertEquals(LongArrayParameter.INSTANCE.apply("[ 123, -456]"), Result.success(List.of(123L, -456L)));

        assertEquals(DecimalArrayParameter.INSTANCE.apply("[ 1.23]"), Result.success(List.of(BigDecimal.valueOf(1.23))));
        assertEquals(DecimalArrayParameter.INSTANCE.apply("[ 1.23, 4.56e7]"), Result.success(List.of(BigDecimal.valueOf(1.23), BigDecimal.valueOf(4.56e7))));

        assertEquals(BooleanArrayParameter.INSTANCE.apply("[true]"), Result.success(List.of(true)));
        assertEquals(BooleanArrayParameter.INSTANCE.apply("[true, false ]"), Result.success(List.of(true, false)));

        assertEquals(OffsetDateTimeArrayParameter.INSTANCE.apply("[2007-12-03T10:15:30+01:00]"), Result.success(List.of(OFFSET_DATE_TIME)));

        assertEquals(LocalDateTimeArrayParameter.INSTANCE.apply("[2007-12-03T10:15:30]"), Result.success(List.of(LOCAL_DATE_TIME)));

        assertEquals(LocalDateArrayParameter.INSTANCE.apply("[2007-12-03]"), Result.success(List.of(LOCAL_DATE)));

        assertEquals(LocalTimeArrayParameter.INSTANCE.apply("[10:15:30]"), Result.success(List.of(LOCAL_TIME)));

        assertEquals(DurationArrayParameter.INSTANCE.apply("[25s,P1DT4H6M]"), Result.success(List.of(Duration.ofSeconds(25), Duration.ofDays(1).plusHours(4).plusMinutes(6))));
    }
}
