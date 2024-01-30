package org.pragmatica.http.server.routing;

import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.lang.Result;

import java.math.BigDecimal;
import java.time.*;

import static org.pragmatica.config.api.ParameterType.*;

public interface PathParameter<T> {
    Result<T> parse(String value);

    static PathParameter<String> spacer(String text) {
        return value -> text.equals(value)
                        ? Result.success(value)
                        : Result.failure(HttpError.httpError(HttpStatus.NOT_FOUND, "Invalid request path"));
    }

    static PathParameter<String> aString() {
        return value -> StringParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<Byte> aByte() {
        return value -> ByteParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<Short> aShort() {
        return value -> ShortParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<Integer> aInteger() {
        return value -> IntegerParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<Long> aLong() {
        return value -> LongParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<BigDecimal> aDecimal() {
        return value -> DecimalParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<Boolean> aBoolean() {
        return value -> BooleanParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<OffsetDateTime> aOffsetDateTime() {
        return value -> OffsetDateTimeParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<LocalDateTime> aLocalDateTime() {
        return value -> LocalDateTimeParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<LocalDate> aLocalDate() {
        return value -> LocalDateParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<LocalTime> aLocalTime() {
        return value -> LocalTimeParameter.INSTANCE.apply(value).traceError();
    }

    static PathParameter<Duration> aDuration() {
        return value -> DurationParameter.INSTANCE.apply(value).traceError();
    }
}
