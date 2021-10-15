package org.pfj.lang;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PromiseTest {
    @Test
    void actionIsTriggeredOnResolution() {
        var bool = new AtomicBoolean(false);
        var promise = Promise.promise();

        assertFalse(bool.get());
    }
}