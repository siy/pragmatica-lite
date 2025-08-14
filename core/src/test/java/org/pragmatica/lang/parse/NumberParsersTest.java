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

class NumberParsersTest {

    @Test
    void testParseByteSuccess() {
        NumberParsers.parseByte("127")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Byte.valueOf((byte) 127), value));
    }

    @Test
    void testParseByteFailure() {
        NumberParsers.parseByte("128")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseByteWithRadix() {
        NumberParsers.parseByte("1111", 2)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Byte.valueOf((byte) 15), value));
    }

    @Test
    void testParseShortSuccess() {
        NumberParsers.parseShort("32767")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Short.valueOf((short) 32767), value));
    }

    @Test
    void testParseShortFailure() {
        NumberParsers.parseShort("32768")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseShortWithRadix() {
        NumberParsers.parseShort("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Short.valueOf((short) 255), value));
    }

    @Test
    void testParseIntSuccess() {
        NumberParsers.parseInt("2147483647")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Integer.valueOf(2147483647), value));
    }

    @Test
    void testParseIntFailure() {
        NumberParsers.parseInt("not_a_number")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseIntWithRadix() {
        NumberParsers.parseInt("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Integer.valueOf(255), value));
    }

    @Test
    void testParseLongSuccess() {
        NumberParsers.parseLong("9223372036854775807")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Long.valueOf(9223372036854775807L), value));
    }

    @Test
    void testParseLongFailure() {
        NumberParsers.parseLong("9223372036854775808")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseLongWithRadix() {
        NumberParsers.parseLong("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(Long.valueOf(255L), value));
    }

    @Test
    void testParseFloatSuccess() {
        NumberParsers.parseFloat("3.14159")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(3.14159f, value, 0.00001f));
    }

    @Test
    void testParseFloatFailure() {
        NumberParsers.parseFloat("not_a_float")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseDoubleSuccess() {
        NumberParsers.parseDouble("3.141592653589793")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(3.141592653589793, value, 0.000000000000001));
    }

    @Test
    void testParseDoubleFailure() {
        NumberParsers.parseDouble("not_a_double")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseBigIntegerSuccess() {
        BigInteger expected = new BigInteger("12345678901234567890");
        NumberParsers.parseBigInteger("12345678901234567890")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseBigIntegerFailure() {
        NumberParsers.parseBigInteger("not_a_bigint")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseBigIntegerWithRadix() {
        BigInteger expected = new BigInteger("255");
        NumberParsers.parseBigInteger("ff", 16)
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseBigDecimalSuccess() {
        BigDecimal expected = new BigDecimal("123.456789");
        NumberParsers.parseBigDecimal("123.456789")
                     .onFailureRun(Assertions::fail)
                     .onSuccess(value -> assertEquals(expected, value));
    }

    @Test
    void testParseBigDecimalFailure() {
        NumberParsers.parseBigDecimal("not_a_bigdecimal")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testNullInputs() {
        NumberParsers.parseInt(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseLong(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseDouble(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseBigInteger(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseBigDecimal(null)
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testEmptyStringInputs() {
        NumberParsers.parseInt("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseLong("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseDouble("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseBigInteger("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
                     
        NumberParsers.parseBigDecimal("")
                     .onSuccessRun(Assertions::fail)
                     .onFailure(Assertions::assertNotNull);
    }
}