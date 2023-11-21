package org.pragmatica.http.server.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.http.server.util.Utils.lazy;
import static org.pragmatica.http.server.util.Utils.value;

class UtilsTest {
    private final AtomicInteger marker = new AtomicInteger(0);
    private Supplier<String> stringSupplier = lazy(() -> stringSupplier = value(getStringValue()));

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