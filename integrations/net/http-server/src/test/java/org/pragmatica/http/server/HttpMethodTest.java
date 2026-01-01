package org.pragmatica.http.server;

import org.junit.jupiter.api.Test;
import org.pragmatica.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.*;

class HttpMethodTest {

    @Test
    void from_parses_uppercase_method() {
        assertEquals(HttpMethod.GET, HttpMethod.from("GET").fold(_ -> null, v -> v));
        assertEquals(HttpMethod.POST, HttpMethod.from("POST").fold(_ -> null, v -> v));
        assertEquals(HttpMethod.PUT, HttpMethod.from("PUT").fold(_ -> null, v -> v));
        assertEquals(HttpMethod.DELETE, HttpMethod.from("DELETE").fold(_ -> null, v -> v));
    }

    @Test
    void from_parses_lowercase_method() {
        assertEquals(HttpMethod.GET, HttpMethod.from("get").fold(_ -> null, v -> v));
        assertEquals(HttpMethod.POST, HttpMethod.from("post").fold(_ -> null, v -> v));
    }

    @Test
    void from_parses_mixed_case_method() {
        assertEquals(HttpMethod.GET, HttpMethod.from("Get").fold(_ -> null, v -> v));
        assertEquals(HttpMethod.POST, HttpMethod.from("PoSt").fold(_ -> null, v -> v));
    }

    @Test
    void from_returns_error_for_unknown_method() {
        var result = HttpMethod.from("INVALID");

        assertTrue(result.isFailure());
        result.onFailure(cause -> {
            assertInstanceOf(HttpMethod.UnknownMethod.class, cause);
            assertTrue(cause.message().contains("Unknown HTTP method"));
        });
    }

    @Test
    void all_methods_are_parseable() {
        for (HttpMethod method : HttpMethod.values()) {
            assertEquals(method, HttpMethod.from(method.name()).fold(_ -> null, v -> v));
        }
    }
}
