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
public final class NumberParsers {
    private NumberParsers() {}

    /// Parse a string as a Byte value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed Byte or parsing error
    public static Result<Byte> parseByte(String s) {
        return Result.lift(() -> Byte.parseByte(s));
    }

    /// Parse a string as a Byte value with specified radix
    /// - **s**: String to parse
    /// - **radix**: The radix to use for parsing
    /// - **Returns**: Result containing parsed Byte or parsing error
    public static Result<Byte> parseByte(String s, int radix) {
        return Result.lift(() -> Byte.parseByte(s, radix));
    }

    /// Parse a string as a Short value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed Short or parsing error
    public static Result<Short> parseShort(String s) {
        return Result.lift(() -> Short.parseShort(s));
    }

    /// Parse a string as a Short value with specified radix
    /// - **s**: String to parse
    /// - **radix**: The radix to use for parsing
    /// - **Returns**: Result containing parsed Short or parsing error
    public static Result<Short> parseShort(String s, int radix) {
        return Result.lift(() -> Short.parseShort(s, radix));
    }

    /// Parse a string as an Integer value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed Integer or parsing error
    public static Result<Integer> parseInt(String s) {
        return Result.lift(() -> Integer.parseInt(s));
    }

    /// Parse a string as an Integer value with specified radix
    /// - **s**: String to parse
    /// - **radix**: The radix to use for parsing
    /// - **Returns**: Result containing parsed Integer or parsing error
    public static Result<Integer> parseInt(String s, int radix) {
        return Result.lift(() -> Integer.parseInt(s, radix));
    }

    /// Parse a string as a Long value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed Long or parsing error
    public static Result<Long> parseLong(String s) {
        return Result.lift(() -> Long.parseLong(s));
    }

    /// Parse a string as a Long value with specified radix
    /// - **s**: String to parse
    /// - **radix**: The radix to use for parsing
    /// - **Returns**: Result containing parsed Long or parsing error
    public static Result<Long> parseLong(String s, int radix) {
        return Result.lift(() -> Long.parseLong(s, radix));
    }

    /// Parse a string as a Float value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed Float or parsing error
    public static Result<Float> parseFloat(String s) {
        return Result.lift(() -> Float.parseFloat(s));
    }

    /// Parse a string as a Double value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed Double or parsing error
    public static Result<Double> parseDouble(String s) {
        return Result.lift(() -> Double.parseDouble(s));
    }

    /// Parse a string as a BigInteger value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed BigInteger or parsing error
    public static Result<BigInteger> parseBigInteger(String s) {
        return Result.lift(() -> new BigInteger(s));
    }

    /// Parse a string as a BigInteger value with specified radix
    /// - **s**: String to parse
    /// - **radix**: The radix to use for parsing
    /// - **Returns**: Result containing parsed BigInteger or parsing error
    public static Result<BigInteger> parseBigInteger(String s, int radix) {
        return Result.lift(() -> new BigInteger(s, radix));
    }

    /// Parse a string as a BigDecimal value
    /// - **s**: String to parse
    /// - **Returns**: Result containing parsed BigDecimal or parsing error
    public static Result<BigDecimal> parseBigDecimal(String s) {
        return Result.lift(() -> new BigDecimal(s));
    }
}