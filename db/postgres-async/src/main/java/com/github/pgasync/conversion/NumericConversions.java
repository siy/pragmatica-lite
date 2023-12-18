package com.github.pgasync.conversion;

import com.github.pgasync.Oid;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.github.pgasync.conversion.Common.returnError;

/**
 * @author Antti Laisi
 */
final class NumericConversions {
    private NumericConversions() {}

    static Long toLong(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8 -> Long.valueOf(value);
            default -> returnError(oid, "Long");
        };
    }

    static Integer toInteger(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4 -> Integer.valueOf(value);
            default -> returnError(oid, "Integer");
        };
    }

    static Short toShort(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2 -> Short.valueOf(value);
            default -> returnError(oid, "Short");
        };
    }

    static Byte toByte(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2 -> Byte.valueOf(value);
            default -> returnError(oid, "Byte");
        };
    }

    static BigInteger toBigInteger(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8 -> new BigInteger(value);
            default -> returnError(oid, "BigInteger");
        };
    }

    static BigDecimal toBigDecimal(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8, NUMERIC, FLOAT4, FLOAT8 -> new BigDecimal(value);
            default -> returnError(oid, "BigDecimal");
        };
    }

    static Double toDouble(Oid oid, String value) {
        return switch (oid) {
            case UNSPECIFIED, INT2, INT4, INT8, NUMERIC, FLOAT4, FLOAT8 -> Double.valueOf(value);
            default -> returnError(oid, "Double");
        };
    }
}
