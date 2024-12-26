package org.pragmatica.config.api;

import org.pragmatica.lang.Functions.Fn0;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.lang.utils.Causes;

import static org.pragmatica.lang.Option.none;

public sealed interface DataConversionError extends Cause {
    record KeyNotFound(String message) implements DataConversionError {}
    record InvalidInput(String message, Option<Cause> source) implements DataConversionError {}
    record CantRetrieveSubType(String message) implements DataConversionError {}

    static <T> Fn0<Result<T>> keyNotFound(String type, Object name) {
        return () -> Causes.trace(new KeyNotFound("The " + type + " not found: " + name)).result();
    }

    static Fn1<Cause, Throwable> invalidInput(String type, String value) {
        return throwable -> new InvalidInput("The value [" + value + "] can't be parsed into " + type + ": " + throwable.getMessage(), none());
    }

    static <T> Fn0<Result<T>> cantRetrieveSubTypeFrom(TypeToken<?> type) {
        return () -> Causes.trace(new CantRetrieveSubType("Can't retrieve sub-type from " + type)).result();
    }
}
