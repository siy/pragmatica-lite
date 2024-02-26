package org.pragmatica.id.nanoid;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NanoIdTest {
    @Test
    public void testKnownValues() {
        var random = new Random(12345);
        var expectedIds = new String[]{
            "kutqLNv1wDmIS56EcT3j7",
            "U497UttnWzKWWRPMHpLD7",
            "7nj2dWW1gjKLtgfzeI8eC",
            "I6BXYvyjszq6xV7L9k2A9",
            "uIolcQEyyQIcn3iM6Odoa"
        };

        for (final String expectedId : expectedIds) {
            var generatedId = NanoId.customNanoId(random, expectedId.length());
            assertEquals(expectedId, generatedId.value());
        }
    }

    @Test
    void microbenchmark() {
        //Warmup (kind of)
        int count = 2_000_000;
        for (int i = 0; i < count; i++) {
            assertEquals(21, NanoId.secureNanoId().value().length());
            assertEquals(21, NanoId.nonSecureNanoId().value().length());
        }

        var start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            assertEquals(21, NanoId.secureNanoId().value().length());
        }
        var end = System.nanoTime();
        System.out.println(STR."Secure: \{(end - start) / count }ns per id");

        start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            assertEquals(21, NanoId.nonSecureNanoId().value().length());
        }
        end = System.nanoTime();
        System.out.println(STR."Non-secure: \{(end - start) / count }ns per id");
    }
}