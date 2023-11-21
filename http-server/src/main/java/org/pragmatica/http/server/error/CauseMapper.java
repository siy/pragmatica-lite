package org.pragmatica.http.server.error;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result.Cause;

@FunctionalInterface
public interface CauseMapper extends Fn1<CompoundCause, Cause> {
    static CompoundCause defaultConverter(Cause failure) {
        if (failure instanceof CompoundCause compoundCause) {
            return compoundCause;
        }

        return CompoundCause.from(WebError.INTERNAL_SERVER_ERROR.status(), failure);
    }
}
