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

import org.pragmatica.lang.Result;

import java.math.BigDecimal;
import java.math.BigInteger;

/// Functional wrappers for JDK number parsing APIs that return Result<T> instead of throwing exceptions
public sealed interface Number {
    /// Parse a string as a Byte value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed Byte or parsing error
    static Result<Byte> parseByte(String s) {
        return Result.lift1(Byte::parseByte, s);
    }

    /// Parse a string as a Byte value with specified radix
    ///
    /// @param s String to parse
    /// @param radix The radix to use for parsing
    ///
    /// @return Result containing parsed Byte or parsing error
    static Result<Byte> parseByte(String s, int radix) {
        return Result.lift2(Byte::parseByte, s, radix);
    }

    /// Parse a string as a Short value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed Short or parsing error
    static Result<Short> parseShort(String s) {
        return Result.lift1(Short::parseShort, s);
    }

    /// Parse a string as a Short value with specified radix
    ///
    /// @param s String to parse
    /// @param radix The radix to use for parsing
    ///
    /// @return Result containing parsed Short or parsing error
    static Result<Short> parseShort(String s, int radix) {
        return Result.lift2(Short::parseShort, s, radix);
    }

    /// Parse a string as an Integer value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed Integer or parsing error
    static Result<Integer> parseInt(String s) {
        return Result.lift1(Integer::parseInt, s);
    }

    /// Parse a string as an Integer value with specified radix
    ///
    /// @param s String to parse
    /// @param radix The radix to use for parsing
    ///
    /// @return Result containing parsed Integer or parsing error
    static Result<Integer> parseInt(String s, int radix) {
        return Result.lift2(Integer::parseInt, s, radix);
    }

    /// Parse a string as a Long value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed Long or parsing error
    static Result<Long> parseLong(String s) {
        return Result.lift1(Long::parseLong, s);
    }

    /// Parse a string as a Long value with specified radix
    ///
    /// @param s String to parse
    /// @param radix The radix to use for parsing
    ///
    /// @return Result containing parsed Long or parsing error
    static Result<Long> parseLong(String s, int radix) {
        return Result.lift2(Long::parseLong, s, radix);
    }

    /// Parse a string as a Float value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed Float or parsing error
    static Result<Float> parseFloat(String s) {
        return Result.lift1(Float::parseFloat, s);
    }

    /// Parse a string as a Double value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed Double or parsing error
    static Result<Double> parseDouble(String s) {
        return Result.lift1(Double::parseDouble, s);
    }

    /// Parse a string as a BigInteger value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed BigInteger or parsing error
    static Result<BigInteger> parseBigInteger(String s) {
        return Result.lift1(BigInteger::new, s);
    }

    /// Parse a string as a BigInteger value with specified radix
    ///
    /// @param s String to parse
    /// @param radix The radix to use for parsing
    ///
    /// @return Result containing parsed BigInteger or parsing error
    static Result<BigInteger> parseBigInteger(String s, int radix) {
        return Result.lift2(BigInteger::new, s, radix);
    }

    /// Parse a string as a BigDecimal value
    ///
    /// @param s String to parse
    ///
    /// @return Result containing parsed BigDecimal or parsing error
    static Result<BigDecimal> parseBigDecimal(String s) {
        return Result.lift1(BigDecimal::new, s);
    }
    
    record unused() implements Number {}
}