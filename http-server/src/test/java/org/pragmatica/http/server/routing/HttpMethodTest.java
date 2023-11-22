package org.pragmatica.http.server.routing;

import org.junit.jupiter.api.Test;
import org.pragmatica.http.protocol.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.lang.Result.success;

class HttpMethodTest {

    @Test
    void connectFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.CONNECT), HttpMethod.fromString("CONNECT"));
        assertTrue(HttpMethod.fromString("connect").isFailure());
        assertTrue(HttpMethod.fromString("CONNEC").isFailure());
        assertTrue(HttpMethod.fromString("CONNEC1").isFailure());
        assertTrue(HttpMethod.fromString("CONNECT1").isFailure());
    }

    @Test
    void deleteFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.DELETE), HttpMethod.fromString("DELETE"));
        assertTrue(HttpMethod.fromString("delete").isFailure());
        assertTrue(HttpMethod.fromString("DELET").isFailure());
        assertTrue(HttpMethod.fromString("DELET1").isFailure());
        assertTrue(HttpMethod.fromString("DELETE1").isFailure());
    }

    @Test
    void getFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.GET), HttpMethod.fromString("GET"));
        assertTrue(HttpMethod.fromString("get").isFailure());
        assertTrue(HttpMethod.fromString("GE").isFailure());
        assertTrue(HttpMethod.fromString("GE1").isFailure());
        assertTrue(HttpMethod.fromString("GET1").isFailure());
    }

    @Test
    void headFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.HEAD), HttpMethod.fromString("HEAD"));
        assertTrue(HttpMethod.fromString("head").isFailure());
        assertTrue(HttpMethod.fromString("HEA").isFailure());
        assertTrue(HttpMethod.fromString("HEA1").isFailure());
        assertTrue(HttpMethod.fromString("HEAD1").isFailure());
    }

    @Test
    void optionsFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.OPTIONS), HttpMethod.fromString("OPTIONS"));
        assertTrue(HttpMethod.fromString("options").isFailure());
        assertTrue(HttpMethod.fromString("OPTIO").isFailure());
        assertTrue(HttpMethod.fromString("OPTIO1").isFailure());
        assertTrue(HttpMethod.fromString("OPTIONS1").isFailure());
    }

    @Test
    void patchFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.PATCH), HttpMethod.fromString("PATCH"));
        assertTrue(HttpMethod.fromString("patch").isFailure());
        assertTrue(HttpMethod.fromString("PATC").isFailure());
        assertTrue(HttpMethod.fromString("PATC1").isFailure());
        assertTrue(HttpMethod.fromString("PATCH1").isFailure());
    }

    @Test
    void postFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.POST), HttpMethod.fromString("POST"));
        assertTrue(HttpMethod.fromString("post").isFailure());
        assertTrue(HttpMethod.fromString("POS").isFailure());
        assertTrue(HttpMethod.fromString("POS1").isFailure());
        assertTrue(HttpMethod.fromString("POST1").isFailure());
    }

    @Test
    void putFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.PUT), HttpMethod.fromString("PUT"));
        assertTrue(HttpMethod.fromString("put").isFailure());
        assertTrue(HttpMethod.fromString("PU").isFailure());
        assertTrue(HttpMethod.fromString("PU1").isFailure());
        assertTrue(HttpMethod.fromString("PUT1").isFailure());
    }

    @Test
    void traceFromStringDecodesCorrectly() {
        assertEquals(success(HttpMethod.TRACE), HttpMethod.fromString("TRACE"));
        assertTrue(HttpMethod.fromString("trace").isFailure());
        assertTrue(HttpMethod.fromString("TRAC").isFailure());
        assertTrue(HttpMethod.fromString("TRAC1").isFailure());
        assertTrue(HttpMethod.fromString("TRACE1").isFailure());
    }
}