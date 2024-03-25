package org.pragmatica.lite.interfaces;

import org.junit.jupiter.api.Test;
import org.pragmatica.lite.interfaces.Maybe.Just;
import org.pragmatica.lite.interfaces.Maybe.Nothing;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaybeTest {
    @Test
    void patternMatchingWorks() {
        var maybe = Maybe.maybe("Hello");

        var result = switch (maybe) {
            case Just<String>(var value) -> value;
            case Nothing<String> _ -> "Nothing";
        };

        assertEquals("Hello", result);
    }
}