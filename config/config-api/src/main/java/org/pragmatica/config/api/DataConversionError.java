package org.pragmatica.config.api;

import org.pragmatica.lang.Functions.Fn0;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.type.TypeToken;

public sealed interface DataConversionError extends Cause {
    record KeyNotFound(String message) implements DataConversionError {}
    record InvalidInput(String message) implements DataConversionError {}
    record CantRetrieveSubType(String message) implements DataConversionError {}

    static <T> Fn0<Result<T>> keyNotFound(String type, Object name) {
        return () -> new KeyNotFound(STR."The \{type} not found: \{name}").result();
    }

    static Fn1<Cause, Throwable> invalidInput(String type, String value) {
        return throwable -> new InvalidInput(STR."The value [\{value}] can't be parsed into \{type}: \{throwable.getMessage()}");
    }

    static <T> Fn0<Result<T>> cantRetrieveSubTypeFrom(TypeToken<?> type) {
        return () -> new CantRetrieveSubType(STR."Can't retrieve sub-type from \{type}").result();
    }
}
