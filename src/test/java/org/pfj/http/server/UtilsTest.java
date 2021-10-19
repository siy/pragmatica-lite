package org.pfj.http.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
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
}