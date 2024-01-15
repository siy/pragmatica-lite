package com.github.pgasync.conversion;

import com.github.pgasync.Oid;

import static com.github.pgasync.conversion.Common.returnError;

/**
 * @author Antti Laisi
 */
final class StringConversions {
    private StringConversions() {}

    static String asString(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, TEXT, CHAR, BPCHAR, UUID, VARCHAR -> value;
            default -> returnError(oid, "String");
        };
    }

    static Character toChar(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, CHAR, BPCHAR -> value.charAt(0);
            default -> returnError(oid, "Character");
        };
    }
}
