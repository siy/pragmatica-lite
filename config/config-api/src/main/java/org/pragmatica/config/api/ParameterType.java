package org.pragmatica.config.api;

import org.pragmatica.config.api.DataConversionError.InvalidInput;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.pragmatica.config.api.DataConversionError.invalidInput;
import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;
import static org.pragmatica.lang.Result.success;

/**
 * Supported configuration parameter types. Mostly resembles TOML specification with notable addition of {@link Duration} and use of
 * {@link BigDecimal} for values with fractional part.
 * <p>
 * Use of sealed interface and records enables preserving type information. All possible combinations of parameter types are defined as constants.
 */
public sealed interface ParameterType<T> extends Fn1<Result<T>, String> {
    default <R> Option<ParameterType<R>> elementType() {
        return none();
    }

    TypeToken<T> token();

    record StringParameter() implements ParameterType<String> {
        @Override
        public TypeToken<String> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<String> apply(String param1) {
            return success(param1);
        }

        public static final StringParameter INSTANCE = new StringParameter();
    }

    record LongParameter() implements ParameterType<Long> {
        @Override
        public TypeToken<Long> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<Long> apply(String param1) {
            return Result.lift(invalidInput("Long", param1), () -> Long.parseLong(param1));
        }

        public static final LongParameter INSTANCE = new LongParameter();
    }

    record DecimalParameter() implements ParameterType<BigDecimal> {
        @Override
        public TypeToken<BigDecimal> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<BigDecimal> apply(String param1) {
            return Result.lift(invalidInput("BigDecimal", param1), () -> new BigDecimal(param1));
        }

        public static final DecimalParameter INSTANCE = new DecimalParameter();
    }

    record BooleanParameter() implements ParameterType<Boolean> {
        @Override
        public TypeToken<Boolean> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<Boolean> apply(String param1) {
            return Result.lift(invalidInput("Boolean", param1), () -> Boolean.parseBoolean(param1));
        }

        public static final BooleanParameter INSTANCE = new BooleanParameter();
    }

    //TODO: might be necessary to support few different formats
    record OffsetDateTimeParameter() implements ParameterType<OffsetDateTime> {
        @Override
        public TypeToken<OffsetDateTime> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<OffsetDateTime> apply(String param1) {
            return Result.lift(invalidInput("OffsetDateTime", param1), () -> OffsetDateTime.parse(param1));
        }

        public static final OffsetDateTimeParameter INSTANCE = new OffsetDateTimeParameter();
    }

    //TODO: might be necessary to support few different formats
    record LocalDateTimeParameter() implements ParameterType<LocalDateTime> {
        @Override
        public TypeToken<LocalDateTime> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<LocalDateTime> apply(String param1) {
            return Result.lift(invalidInput("LocalDateTime", param1), () -> LocalDateTime.parse(param1));
        }

        public static final LocalDateTimeParameter INSTANCE = new LocalDateTimeParameter();
    }

    //TODO: might be necessary to support few different formats
    record LocalDateParameter() implements ParameterType<LocalDate> {
        @Override
        public TypeToken<LocalDate> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<LocalDate> apply(String param1) {
            return Result.lift(invalidInput("LocalDate", param1), () -> LocalDate.parse(param1));
        }

        public static final LocalDateParameter INSTANCE = new LocalDateParameter();
    }

    //TODO: might be necessary to support few different formats
    record LocalTimeParameter() implements ParameterType<LocalTime> {
        @Override
        public TypeToken<LocalTime> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<LocalTime> apply(String param1) {
            return Result.lift(invalidInput("LocalTime", param1), () -> LocalTime.parse(param1));
        }

        public static final LocalTimeParameter INSTANCE = new LocalTimeParameter();
    }

    record DurationParameter() implements ParameterType<Duration> {
        @Override
        public TypeToken<Duration> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<Duration> apply(String param1) {
            var upperCase = param1.toUpperCase(Locale.ROOT);

            return tryParse(upperCase)
                .orElse(() -> tryParse(STR."PT\{upperCase}"));
        }

        private static Result<Duration> tryParse(String param1) {
            return Result.lift(invalidInput("Duration", param1), () -> Duration.parse(param1));
        }

        public static final DurationParameter INSTANCE = new DurationParameter();
    }

    //TODO: support for quoted and escaped strings
    record StringArrayParameter(Option<ParameterType<String>> elementType) implements ParameterType<List<String>> {
        @Override
        public TypeToken<List<String>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<String>> apply(String param1) {
            return tryParse(param1, "String array")
                .map(Stream::toList);
        }

        static Result<Stream<String>> tryParse(String param1, String type) {
            var input = param1.trim();

            if (input.startsWith("[") && input.endsWith("]")) {
                var values1 = input.substring(1, input.length() - 1).trim();

                if (values1.isEmpty()) {
                    return success(Stream.empty());
                }

                return success(Stream.of(values1.split(",")).map(String::trim));
            } else {
                return new InvalidInput(STR."The value [\{param1}] can't be parsed into \{type}").result();
            }
        }

        public static final StringArrayParameter INSTANCE = new StringArrayParameter(some(StringParameter.INSTANCE));
    }

    record LongArrayParameter(Option<ParameterType<Long>> elementType) implements ParameterType<List<Long>> {
        @Override
        public TypeToken<List<Long>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<Long>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Long array")
                                       .map(stream -> stream.map(LongParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final LongArrayParameter INSTANCE = new LongArrayParameter(some(LongParameter.INSTANCE));
    }

    record DecimalArrayParameter(Option<ParameterType<BigDecimal>> elementType) implements ParameterType<List<BigDecimal>> {
        @Override
        public TypeToken<List<BigDecimal>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<BigDecimal>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Decimal array")
                                       .map(stream -> stream.map(DecimalParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final DecimalArrayParameter INSTANCE = new DecimalArrayParameter(some(DecimalParameter.INSTANCE));
    }

    record BooleanArrayParameter(Option<ParameterType<Boolean>> elementType) implements ParameterType<List<Boolean>> {
        @Override
        public TypeToken<List<Boolean>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<Boolean>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Boolean array")
                                       .map(stream -> stream.map(BooleanParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final BooleanArrayParameter INSTANCE = new BooleanArrayParameter(some(BooleanParameter.INSTANCE));
    }

    record OffsetDateTimeArrayParameter(Option<ParameterType<OffsetDateTime>> elementType) implements ParameterType<List<OffsetDateTime>> {
        @Override
        public TypeToken<List<OffsetDateTime>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<OffsetDateTime>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Offset date/time array")
                                       .map(stream -> stream.map(OffsetDateTimeParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final OffsetDateTimeArrayParameter INSTANCE = new OffsetDateTimeArrayParameter(some(OffsetDateTimeParameter.INSTANCE));
    }

    record LocalDateTimeArrayParameter(Option<ParameterType<LocalDateTime>> elementType) implements ParameterType<List<LocalDateTime>> {
        @Override
        public TypeToken<List<LocalDateTime>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<LocalDateTime>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Local date/time array")
                                       .map(stream -> stream.map(LocalDateTimeParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final LocalDateTimeArrayParameter INSTANCE = new LocalDateTimeArrayParameter(some(LocalDateTimeParameter.INSTANCE));
    }

    record LocalDateArrayParameter(Option<ParameterType<LocalDate>> elementType) implements ParameterType<List<LocalDate>> {
        @Override
        public TypeToken<List<LocalDate>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<LocalDate>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Local date array")
                                       .map(stream -> stream.map(LocalDateParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final LocalDateArrayParameter INSTANCE = new LocalDateArrayParameter(some(LocalDateParameter.INSTANCE));
    }

    record LocalTimeArrayParameter(Option<ParameterType<LocalTime>> elementType) implements ParameterType<List<LocalTime>> {
        @Override
        public TypeToken<List<LocalTime>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<LocalTime>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Local time array")
                                       .map(stream -> stream.map(LocalTimeParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final LocalTimeArrayParameter INSTANCE = new LocalTimeArrayParameter(some(LocalTimeParameter.INSTANCE));
    }

    record DurationArrayParameter(Option<ParameterType<Duration>> elementType) implements ParameterType<List<Duration>> {
        @Override
        public TypeToken<List<Duration>> token() {
            return new TypeToken<>() {};
        }

        @Override
        public Result<List<Duration>> apply(String param1) {
            return StringArrayParameter.tryParse(param1, "Duration array")
                                       .map(stream -> stream.map(DurationParameter.INSTANCE::apply))
                                       .flatMap(Result::allOf);
        }

        public static final DurationArrayParameter INSTANCE = new DurationArrayParameter(some(DurationParameter.INSTANCE));
    }

    record BuiltInTypes() {
        public static final List<ParameterType<?>> LIST = List.of(
            StringParameter.INSTANCE,
            LongParameter.INSTANCE,
            DecimalParameter.INSTANCE,
            BooleanParameter.INSTANCE,
            OffsetDateTimeParameter.INSTANCE,
            LocalDateTimeParameter.INSTANCE,
            LocalDateParameter.INSTANCE,
            LocalTimeParameter.INSTANCE,
            DurationParameter.INSTANCE,

            StringArrayParameter.INSTANCE,
            LongArrayParameter.INSTANCE,
            DecimalArrayParameter.INSTANCE,
            BooleanArrayParameter.INSTANCE,
            OffsetDateTimeArrayParameter.INSTANCE,
            LocalDateTimeArrayParameter.INSTANCE,
            LocalDateArrayParameter.INSTANCE,
            LocalTimeArrayParameter.INSTANCE,
            DurationArrayParameter.INSTANCE
        );

    }
}
