package org.pragmatica.config.api;

import org.pragmatica.lang.Functions.Fn0;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;

public sealed interface DataConversionError extends Cause {
    record KeyNotFound(String message) implements DataConversionError {}

    static <T> Fn0<Result<T>> keyNotFound(String type, Object name) {
        return () -> new KeyNotFound(STR."The \{type} not found: \{name}").result();
    }
}
