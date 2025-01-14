package org.pragmatica.http;

import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Cause;

public interface HttpError extends Cause {
    HttpStatus status();

    static HttpError httpError(HttpStatus status, Cause source) {
        record httpError(HttpStatus status, Cause origin) implements HttpError {
            @Override
            public String message() {
                var builder = new StringBuilder()
                    .append(status().message())
                    .append(": ")
                    .append(origin().message());

                var cause = origin().source();

                while (cause.isPresent()) {
                    cause.onPresent(c -> builder.append("\n\t").append(c.message()));
                    cause = cause.fold(Option::none, Cause::source);
                }

                return builder.toString();
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
