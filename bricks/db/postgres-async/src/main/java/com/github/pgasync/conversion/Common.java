package com.github.pgasync.conversion;

import com.github.pgasync.Oid;
import com.github.pgasync.net.SqlException;

public final class Common {
    private Common() {}

    public static <T> T returnError(Oid oid, String typeName) {
        throw new SqlException("Unsupported conversion " + oid.name() + " -> " + typeName);
    }
}
