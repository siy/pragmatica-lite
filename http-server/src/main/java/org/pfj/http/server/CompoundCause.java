package org.pfj.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.pfj.lang.Cause;
import org.pfj.lang.Causes;

public interface CompoundCause extends Cause, StatusHolder {

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
        };
    }
}
