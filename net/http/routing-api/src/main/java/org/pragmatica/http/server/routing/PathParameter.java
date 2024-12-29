package org.pragmatica.http.server.routing;

import org.pragmatica.lang.Result;

import java.math.BigDecimal;
import java.time.*;

import static org.pragmatica.config.api.ParameterType.*;
import static org.pragmatica.http.protocol.HttpStatus.UNPROCESSABLE_ENTITY;

@SuppressWarnings("unused")
public interface PathParameter<T> {
    Result<T> parse(String value);

    static PathParameter<String> spacer(String text) {
        return value -> text.equals(value)
                        ? Result.success(value)
                        : RequestContext.NOT_FOUND;
    }

    static PathParameter<String> aString() {
        return value -> StringParameter.INSTANCE.apply(value)
                                                .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<Byte> aByte() {
        return value -> ByteParameter.INSTANCE.apply(value)
                                              .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<Short> aShort() {
        return value -> ShortParameter.INSTANCE.apply(value)
                                               .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<Integer> aInteger() {
        return value -> IntegerParameter.INSTANCE.apply(value)
                                                 .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<Long> aLong() {
        return value -> LongParameter.INSTANCE.apply(value)
                                              .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<BigDecimal> aDecimal() {
        return value -> DecimalParameter.INSTANCE.apply(value)
                                                 .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<Boolean> aBoolean() {
        return value -> BooleanParameter.INSTANCE.apply(value)
                                                 .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<OffsetDateTime> aOffsetDateTime() {
        return value -> OffsetDateTimeParameter.INSTANCE.apply(value)
                                                        .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<LocalDateTime> aLocalDateTime() {
        return value -> LocalDateTimeParameter.INSTANCE.apply(value)
                                                       .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<LocalDate> aLocalDate() {
        return value -> LocalDateParameter.INSTANCE.apply(value)
                                                   .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<LocalTime> aLocalTime() {
        return value -> LocalTimeParameter.INSTANCE.apply(value)
                                                   .mapError(UNPROCESSABLE_ENTITY::with);
    }

    static PathParameter<Duration> aDuration() {
        return value -> DurationParameter.INSTANCE.apply(value)
                                                  .mapError(UNPROCESSABLE_ENTITY::with);
    }
}
