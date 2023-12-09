package com.github.pgasync.conversion;

import com.github.pgasync.net.SqlException;
import com.github.pgasync.Oid;

import static com.github.pgasync.util.HexConverter.parseHexBinary;
import static com.github.pgasync.util.HexConverter.printHexBinary;

/**
 * @author Antti Laisi
 */
class BlobConversions {

    static byte[] toBytes(Oid oid, String value) {
        return switch (oid) {
            // TODO: Add theses considerations somewhere to the code:
            //  1. (2, length-2)
            //  2. According to postgres rules bytea should be encoded as ASCII sequence
            case UNSPECIFIED, BYTEA ->
                parseHexBinary(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> byte[]");
        };
    }

    static String fromBytes(byte[] bytes) {
        return (STR."\\x\{printHexBinary(bytes)}");
    }
}
