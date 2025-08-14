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

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Result;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class NumberParsersTest {

    @Test
    void testParseByteSuccess() {
        Result<Byte> result = NumberParsers.parseByte("127");
        assertTrue(result.isSuccess());
        assertEquals(Byte.valueOf((byte) 127), result.unwrap());
    }

    @Test
    void testParseByteFailure() {
        Result<Byte> result = NumberParsers.parseByte("128");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseByteWithRadix() {
        Result<Byte> result = NumberParsers.parseByte("1111", 2);
        assertTrue(result.isSuccess());
        assertEquals(Byte.valueOf((byte) 15), result.unwrap());
    }

    @Test
    void testParseShortSuccess() {
        Result<Short> result = NumberParsers.parseShort("32767");
        assertTrue(result.isSuccess());
        assertEquals(Short.valueOf((short) 32767), result.unwrap());
    }

    @Test
    void testParseShortFailure() {
        Result<Short> result = NumberParsers.parseShort("32768");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseShortWithRadix() {
        Result<Short> result = NumberParsers.parseShort("ff", 16);
        assertTrue(result.isSuccess());
        assertEquals(Short.valueOf((short) 255), result.unwrap());
    }

    @Test
    void testParseIntSuccess() {
        Result<Integer> result = NumberParsers.parseInt("2147483647");
        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(2147483647), result.unwrap());
    }

    @Test
    void testParseIntFailure() {
        Result<Integer> result = NumberParsers.parseInt("not_a_number");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseIntWithRadix() {
        Result<Integer> result = NumberParsers.parseInt("ff", 16);
        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(255), result.unwrap());
    }

    @Test
    void testParseLongSuccess() {
        Result<Long> result = NumberParsers.parseLong("9223372036854775807");
        assertTrue(result.isSuccess());
        assertEquals(Long.valueOf(9223372036854775807L), result.unwrap());
    }

    @Test
    void testParseLongFailure() {
        Result<Long> result = NumberParsers.parseLong("9223372036854775808");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseLongWithRadix() {
        Result<Long> result = NumberParsers.parseLong("ff", 16);
        assertTrue(result.isSuccess());
        assertEquals(Long.valueOf(255L), result.unwrap());
    }

    @Test
    void testParseFloatSuccess() {
        Result<Float> result = NumberParsers.parseFloat("3.14159");
        assertTrue(result.isSuccess());
        assertEquals(3.14159f, result.unwrap(), 0.00001f);
    }

    @Test
    void testParseFloatFailure() {
        Result<Float> result = NumberParsers.parseFloat("not_a_float");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseDoubleSuccess() {
        Result<Double> result = NumberParsers.parseDouble("3.141592653589793");
        assertTrue(result.isSuccess());
        assertEquals(3.141592653589793, result.unwrap(), 0.000000000000001);
    }

    @Test
    void testParseDoubleFailure() {
        Result<Double> result = NumberParsers.parseDouble("not_a_double");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseBigIntegerSuccess() {
        Result<BigInteger> result = NumberParsers.parseBigInteger("12345678901234567890");
        assertTrue(result.isSuccess());
        assertEquals(new BigInteger("12345678901234567890"), result.unwrap());
    }

    @Test
    void testParseBigIntegerFailure() {
        Result<BigInteger> result = NumberParsers.parseBigInteger("not_a_bigint");
        assertTrue(result.isFailure());
    }

    @Test
    void testParseBigIntegerWithRadix() {
        Result<BigInteger> result = NumberParsers.parseBigInteger("ff", 16);
        assertTrue(result.isSuccess());
        assertEquals(new BigInteger("255"), result.unwrap());
    }

    @Test
    void testParseBigDecimalSuccess() {
        Result<BigDecimal> result = NumberParsers.parseBigDecimal("123.456789");
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("123.456789"), result.unwrap());
    }

    @Test
    void testParseBigDecimalFailure() {
        Result<BigDecimal> result = NumberParsers.parseBigDecimal("not_a_bigdecimal");
        assertTrue(result.isFailure());
    }

    @Test
    void testNullInputs() {
        assertTrue(NumberParsers.parseInt(null).isFailure());
        assertTrue(NumberParsers.parseLong(null).isFailure());
        assertTrue(NumberParsers.parseDouble(null).isFailure());
        assertTrue(NumberParsers.parseBigInteger(null).isFailure());
        assertTrue(NumberParsers.parseBigDecimal(null).isFailure());
    }

    @Test
    void testEmptyStringInputs() {
        assertTrue(NumberParsers.parseInt("").isFailure());
        assertTrue(NumberParsers.parseLong("").isFailure());
        assertTrue(NumberParsers.parseDouble("").isFailure());
        assertTrue(NumberParsers.parseBigInteger("").isFailure());
        assertTrue(NumberParsers.parseBigDecimal("").isFailure());
    }
}