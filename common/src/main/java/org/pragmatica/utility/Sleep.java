package org.pragmatica.utility;

import org.pragmatica.lang.io.TimeSpan;

public sealed interface Sleep {
    static void sleep(TimeSpan span) {
        try {
            Thread.sleep(span.millis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unused")
    record unused() implements Sleep { }
}
