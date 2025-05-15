package org.pragmatica.utility;

import org.pragmatica.lang.io.TimeSpan;

public class Sleep {
    public static void sleep(TimeSpan span) {
        try {
            Thread.sleep(span.millis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
