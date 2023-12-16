package com.github.pgasync.conversion;

import com.github.pgasync.Oid;
import com.github.pgasync.net.SqlException;

/**
 * @author Antti Laisi
 */
final class StringConversions {
    private StringConversions() {}

    static String toString(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, TEXT, CHAR, BPCHAR, UUID, VARCHAR -> value;
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> String");
        };
    }

    static Character toChar(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, CHAR, BPCHAR -> value.charAt(0);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> Char");
        };
    }
}
