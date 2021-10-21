package org.pfj.http.server;

import org.pfj.lang.Cause;
import org.pfj.lang.Functions.FN1;

@FunctionalInterface
public interface CauseMapper extends FN1<CompoundCause, Cause> {
    static CompoundCause defaultConverter(Cause failure) {
        if (failure instanceof CompoundCause compoundCause) {
            return compoundCause;
        }

        return CompoundCause.from(WebError.INTERNAL_SERVER_ERROR.status(), failure);
    }
}
