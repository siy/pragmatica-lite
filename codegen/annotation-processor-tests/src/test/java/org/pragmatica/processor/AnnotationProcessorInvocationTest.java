package org.pragmatica.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Option;
import org.pragmatica.processor.record.NameAge;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationProcessorInvocationTest {
    @Test
    void simpleRecordIsProcessed() {
        var record = new NameAge("John", "Doe", Option.none(), 42);
        Assertions.assertEquals("John", record.firstName());
        Assertions.assertEquals(42, record.age());
    }
}