package org.pragmatica.config.api;

import org.pragmatica.lang.Option;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

/**
 * Supported configuration parameter types. Mostly resembles TOML specification with notable addition of {@link Duration} and
 * use of {@link BigDecimal} for values with fractional part.
 * <p>
 * Use of sealed interface and records enables preserving type information. All possible combinations of parameter types are defined as constants.
 */
public sealed interface ParameterType<T> {
    default <R> Option<ParameterType<R>> elementType() {
        return none();
    }

    record StringParameter() implements ParameterType<String> {
        public static final StringParameter INSTANCE = new StringParameter();
    }

    record LongParameter() implements ParameterType<Long> {
        public static final LongParameter INSTANCE = new LongParameter();
    }

    record DecimalParameter() implements ParameterType<BigDecimal> {
        public static final DecimalParameter INSTANCE = new DecimalParameter();
    }

    record BooleanParameter() implements ParameterType<Boolean> {
        public static final BooleanParameter INSTANCE = new BooleanParameter();
    }

    record OffsetDateTimeParameter() implements ParameterType<OffsetDateTime> {
        public static final OffsetDateTimeParameter INSTANCE = new OffsetDateTimeParameter();
    }

    record LocalDateTimeParameter() implements ParameterType<LocalDateTime> {
        public static final LocalDateTimeParameter INSTANCE = new LocalDateTimeParameter();
    }

    record LocalDateParameter() implements ParameterType<LocalDate> {
        public static final LocalDateParameter INSTANCE = new LocalDateParameter();
    }

    record LocalTimeParameter() implements ParameterType<LocalTime> {
        public static final LocalTimeParameter INSTANCE = new LocalTimeParameter();
    }

    record DurationParameter() implements ParameterType<Duration> {
        public static final DurationParameter INSTANCE = new DurationParameter();
    }

    record ArrayParameter<T>(Option<ParameterType<T>> elementType) implements ParameterType<List<T>> {
        public static final ArrayParameter<String> STRINGS = new ArrayParameter<>(some(StringParameter.INSTANCE));
        public static final ArrayParameter<Long> LONGS = new ArrayParameter<>(some(LongParameter.INSTANCE));
        public static final ArrayParameter<BigDecimal> DECIMALS = new ArrayParameter<>(some(DecimalParameter.INSTANCE));
        public static final ArrayParameter<Boolean> BOOLEANS = new ArrayParameter<>(some(BooleanParameter.INSTANCE));
        public static final ArrayParameter<OffsetDateTime> OFFSET_DATE_TIMES = new ArrayParameter<>(some(OffsetDateTimeParameter.INSTANCE));
        public static final ArrayParameter<LocalDateTime> LOCAL_DATE_TIMES = new ArrayParameter<>(some(LocalDateTimeParameter.INSTANCE));
        public static final ArrayParameter<LocalDate> LOCAL_DATES = new ArrayParameter<>(some(LocalDateParameter.INSTANCE));
        public static final ArrayParameter<LocalTime> LOCAL_TIMES = new ArrayParameter<>(some(LocalTimeParameter.INSTANCE));
        public static final ArrayParameter<Duration> DURATIONS = new ArrayParameter<>(some(DurationParameter.INSTANCE));
    }
}
