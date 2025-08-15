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
                     .onFailure(Assertions::assertNotNull);
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
                     .onFailure(Assertions::assertNotNull);
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
                     .onFailure(Assertions::assertNotNull);
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
                     .onFailure(Assertions::assertNotNull);
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
                     .onFailure(Assertions::assertNotNull);
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
                     .onFailure(Assertions::assertNotNull);
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
                     .onFailure(Assertions::assertNotNull);
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
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testNullInputs() {
        Number.parseInt(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseLong(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseDouble(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseBigInteger(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseBigDecimal(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testEmptyStringInputs() {
        Number.parseInt("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseLong("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseDouble("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseBigInteger("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        Number.parseBigDecimal("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }
}