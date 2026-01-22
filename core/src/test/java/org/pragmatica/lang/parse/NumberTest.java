/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.lang.parse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class NumberTest {

    @Test
    void testParseByteSuccess() {
        Number.parseByte("127")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Byte.valueOf((byte) 127), value));
    }

    @Test
    void testParseByteFailure() {
        Number.parseByte("128")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseByteWithRadix() {
        Number.parseByte("1111", 2)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Byte.valueOf((byte) 15), value));
    }

    @Test
    void testParseShortSuccess() {
        Number.parseShort("32767")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Short.valueOf((short) 32767), value));
    }

    @Test
    void testParseShortFailure() {
        Number.parseShort("32768")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseShortWithRadix() {
        Number.parseShort("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Short.valueOf((short) 255), value));
    }

    @Test
    void testParseIntSuccess() {
        Number.parseInt("2147483647")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Integer.valueOf(2147483647), value));
    }

    @Test
    void testParseIntFailure() {
        Number.parseInt("not_a_number")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseIntWithRadix() {
        Number.parseInt("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Integer.valueOf(255), value));
    }

    @Test
    void testParseLongSuccess() {
        Number.parseLong("9223372036854775807")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Long.valueOf(9223372036854775807L), value));
    }

    @Test
    void testParseLongFailure() {
        Number.parseLong("9223372036854775808")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseLongWithRadix() {
        Number.parseLong("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Long.valueOf(255L), value));
    }

    @Test
    void testParseFloatSuccess() {
        Number.parseFloat("3.14159")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(3.14159f, value, 0.00001f));
    }

    @Test
    void testParseFloatFailure() {
        Number.parseFloat("not_a_float")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseDoubleSuccess() {
        Number.parseDouble("3.141592653589793")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(3.141592653589793, value, 0.000000000000001));
    }

    @Test
    void testParseDoubleFailure() {
        Number.parseDouble("not_a_double")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseBigIntegerSuccess() {
        var expected = new BigInteger("12345678901234567890");
        Number.parseBigInteger("12345678901234567890")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseBigIntegerFailure() {
        Number.parseBigInteger("not_a_bigint")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testParseBigIntegerWithRadix() {
        var expected = new BigInteger("255");
        Number.parseBigInteger("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseBigDecimalSuccess() {
        var expected = new BigDecimal("123.456789");
        Number.parseBigDecimal("123.456789")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseBigDecimalFailure() {
        Number.parseBigDecimal("not_a_bigdecimal")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testNullInputs() {
        Number.parseByte(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseShort(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseInt(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseLong(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseFloat(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseDouble(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseBigInteger(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseBigDecimal(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    @Test
    void testEmptyStringInputs() {
        Number.parseByte("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseShort("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseInt("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseLong("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseFloat("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseDouble("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseBigInteger("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseBigDecimal("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }

    // Edge cases

    @Test
    void testParseMinValues() {
        Number.parseByte("-128")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Byte.MIN_VALUE, value));

        Number.parseShort("-32768")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Short.MIN_VALUE, value));

        Number.parseInt("-2147483648")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Integer.MIN_VALUE, value));

        Number.parseLong("-9223372036854775808")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Long.MIN_VALUE, value));
    }

    @Test
    void testParseZero() {
        Number.parseInt("0")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(0, value));

        Number.parseDouble("0.0")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(0.0, value));
    }

    @Test
    void testParseNegativeNumbers() {
        Number.parseInt("-123")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(-123, value));

        Number.parseDouble("-3.14")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(-3.14, value, 0.001));
    }

    @Test
    void testParseSpecialFloatValues() {
        Number.parseDouble("NaN")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertTrue(Double.isNaN(value)));

        Number.parseDouble("Infinity")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Double.POSITIVE_INFINITY, value));

        Number.parseDouble("-Infinity")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Double.NEGATIVE_INFINITY, value));

        Number.parseFloat("NaN")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertTrue(Float.isNaN(value)));
    }

    @Test
    void testParseScientificNotation() {
        Number.parseDouble("1.5e10")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(1.5e10, value));

        Number.parseDouble("3.14E-5")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(3.14E-5, value, 1e-10));
    }

    @Test
    void testParseWithPlusSign() {
        Number.parseInt("+123")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(123, value));
    }

    @Test
    void testParseRadixFailure() {
        Number.parseByte("GG", 16)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseShort("ZZ", 16)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseInt("XYZ", 16)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));

        Number.parseLong("!!!", 16)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(cause -> assertNotNull(cause.message()));
    }
}