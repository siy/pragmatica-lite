package org.pragmatica.http.server.error;

import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.utils.Causes;

public interface WebError extends Cause {
    HttpStatus status();

    static WebError fromThrowable(HttpStatus error, Throwable throwable) {
        return from(error, Causes.fromThrowable(throwable));
    }

    static WebError from(HttpStatus status, String message) {
        return from(status, Causes.cause(message));
    }

    static WebError from(HttpStatus status, Cause source) {
        record webError(HttpStatus status, Cause origin) implements WebError {
            @Override
            public String message() {
                return STR."\{status().message()}: \{origin().message()}";
            }

            @Override
            public HttpStatus status() {
                return status;
            }

            @Override
            public Option<Cause> source() {
                return Option.some(origin);
            }
        }

        return new webError(status, source);
    }
}
