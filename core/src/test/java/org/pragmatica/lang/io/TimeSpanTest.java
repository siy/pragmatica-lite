package org.pragmatica.lang.io;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class TimeSpanTest {

    @Test
    void testRandomization() {
        testForScale(timeSpan(1).millis(), 0.1);
        testForScale(timeSpan(1).millis(), 0.2);
        testForScale(timeSpan(1).millis(), 0.3);
        testForScale(timeSpan(1).millis(), 0.4);
        testForScale(timeSpan(1).millis(), 0.5);
        testForScale(timeSpan(1).millis(), 0.6);
        testForScale(timeSpan(1).millis(), 0.7);
        testForScale(timeSpan(1).millis(), 0.8);
        testForScale(timeSpan(1).millis(), 0.9);
    }

    private static void testForScale(TimeSpan timeSpan, double scale) {
        var nominalNanos = timeSpan.nanos();

        var values = IntStream.range(0, 10000)
                .mapToObj(_ -> timeSpan.randomize(scale))
                .toList();

        var min = values.stream().mapToLong(TimeSpan::nanos).min().orElseThrow();
        var max = values.stream().mapToLong(TimeSpan::nanos).max().orElseThrow();

        System.out.println("Min: " + min + " Max: " + max + " Nominal: " + nominalNanos);

        assertTrue(min >= nominalNanos - nominalNanos * scale);
        assertTrue(max <= nominalNanos + nominalNanos * scale);
        assertTrue(min < nominalNanos);
        assertTrue(max > nominalNanos);
    }
}