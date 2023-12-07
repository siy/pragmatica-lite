package org.pragmatica.http.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {
    private final AtomicInteger marker = new AtomicInteger(0);
    private Supplier<String> stringSupplier = Utils.lazy(() -> stringSupplier = Utils.value(getStringValue()));

    @SuppressWarnings("SameReturnValue")    // This is not test
    private String getStringValue() {
        marker.incrementAndGet();
        return "Text";
    }

    @Test
    void normalizeProperlyHandlesPaths() {
        assertEquals("/", Utils.normalize(null));
        assertEquals("/", Utils.normalize(""));
        assertEquals("/", Utils.normalize("?"));
        assertEquals("/", Utils.normalize("/"));
        assertEquals("/", Utils.normalize("//"));
        assertEquals("/", Utils.normalize("//?"));
        assertEquals("/", Utils.normalize("//?//"));
    }

    @Test
    void lazyIsEvaluatedOnDemandAndOnlyOnce() {
        assertEquals(0, marker.get());
        assertEquals("Text", stringSupplier.get());
        assertEquals(1, marker.get());
        assertEquals("Text", stringSupplier.get());
        assertEquals(1, marker.get());
    }
}