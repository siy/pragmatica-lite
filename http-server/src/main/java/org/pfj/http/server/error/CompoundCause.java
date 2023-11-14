package org.pfj.http.server.error;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.pfj.http.server.util.HttpStatusHolder;
import org.pragmatica.lang.Result.Cause;
import org.pragmatica.lang.utils.Causes;

import java.util.Objects;

public interface CompoundCause extends Cause, HttpStatusHolder {

    static CompoundCause fromThrowable(WebError error, Throwable throwable) {
        return from(error.status(), Causes.fromThrowable(throwable));
    }

    static CompoundCause from(HttpResponseStatus status, Cause failure) {
        return new CompoundCause() {
            @Override
            public HttpResponseStatus status() {
                return status;
            }

            @Override
            public String message() {
                return failure.message();
            }

            @Override
            public int hashCode() {
                return Objects.hash(status, failure.message());
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }

                return (obj instanceof CompoundCause other)
                    && Objects.equals(status, other.status())
                    && failure.message().equals(other.message());
            }
        };
    }
}
