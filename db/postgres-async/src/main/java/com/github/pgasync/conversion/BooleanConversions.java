package com.github.pgasync.conversion;

import com.github.pgasync.Oid;
import com.github.pgasync.net.SqlException;

/**
 * @author Antti Laisi
 */
final class BooleanConversions {
    private BooleanConversions() {}

    private static final String TRUE = "t";
    private static final String FALSE = "f";

    static boolean toBoolean(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, BOOL -> TRUE.equals(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> boolean");
        };
    }

    static String fromBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }
}
