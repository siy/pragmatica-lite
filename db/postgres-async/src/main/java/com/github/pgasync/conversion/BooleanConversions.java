package com.github.pgasync.conversion;

import com.github.pgasync.net.SqlException;
import com.github.pgasync.Oid;

/**
 * @author Antti Laisi
 */
class BooleanConversions {

    private static final String TRUE = "t";
    private static final String FALSE = "f";

    static boolean toBoolean(Oid oid, String value) {
        return switch (oid) { // fallthrough
            case UNSPECIFIED, BOOL -> TRUE.equals(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> boolean");
        };
    }

    static String fromBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }
}
