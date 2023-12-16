package com.github.pgasync.conversion;

import com.github.pgasync.Oid;
import com.github.pgasync.net.SqlException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Antti Laisi
 */
final class NumericConversions {
    private NumericConversions() {}

    static Long toLong(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8 -> Long.valueOf(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> Long");
        };
    }

    static Integer toInteger(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4 -> Integer.valueOf(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> Integer");
        };
    }

    static Short toShort(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2 -> Short.valueOf(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> Short");
        };
    }

    static Byte toByte(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2 -> Byte.valueOf(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> Byte");
        };
    }

    static BigInteger toBigInteger(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8 -> new BigInteger(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> BigInteger");
        };
    }

    static BigDecimal toBigDecimal(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8, NUMERIC, FLOAT4, FLOAT8 -> new BigDecimal(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> BigDecimal");
        };
    }

    static Double toDouble(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8, NUMERIC, FLOAT4, FLOAT8 -> Double.valueOf(value);
            default -> throw new SqlException(STR."Unsupported conversion \{oid.name()} -> Double");
        };
    }
}
