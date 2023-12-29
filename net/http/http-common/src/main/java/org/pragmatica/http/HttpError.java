package org.pragmatica.http;

import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.utils.Causes;

public interface HttpError extends Cause {
    HttpStatus status();

    static HttpError httpError(HttpStatus error, Throwable throwable) {
        return httpError(error, Causes.fromThrowable(throwable));
    }

    static HttpError httpError(HttpStatus status, String message) {
        return httpError(status, Causes.cause(message));
    }

    static HttpError httpError(HttpStatus status, Cause source) {
        record httpError(HttpStatus status, Cause origin) implements HttpError {
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

        return new httpError(status, source);
    }
}
