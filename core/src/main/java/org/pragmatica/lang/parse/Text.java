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

import java.util.Base64;
import java.util.regex.Pattern;

/// Functional wrappers for JDK text processing APIs that return Result<T> instead of throwing exceptions
public sealed interface Text {
    /// Parse a string as an enum value
    ///
    /// @param enumType The enum class
    /// @param name String name of the enum constant
    ///
    /// @return Result containing parsed enum value or parsing error
    static <E extends Enum<E>> Result<E> parseEnum(Class<E> enumType, String name) {
        return Result.lift2(Enum::valueOf, enumType, name);
    }

    /// Compile a regular expression pattern
    ///
    /// @param regex Regular expression string
    ///
    /// @return Result containing compiled Pattern or parsing error
    static Result<Pattern> compilePattern(String regex) {
        return Result.lift1(Pattern::compile, regex);
    }

    /// Compile a regular expression pattern with flags
    ///
    /// @param regex Regular expression string
    /// @param flags Match flags (e.g., Pattern.CASE_INSENSITIVE)
    ///
    /// @return Result containing compiled Pattern or parsing error
    static Result<Pattern> compilePattern(String regex, int flags) {
        return Result.lift2(Pattern::compile, regex, flags);
    }

    /// Decode a Base64 encoded string to byte array
    ///
    /// @param src Base64 encoded string
    ///
    /// @return Result containing decoded byte array or parsing error
    static Result<byte[] > decodeBase64(String src) {
        return Result.lift1(Base64.getDecoder()::decode, src);
    }

    /// Decode a Base64 URL-safe encoded string to byte array
    ///
    /// @param src Base64 URL-safe encoded string
    ///
    /// @return Result containing decoded byte array or parsing error
    static Result<byte[] > decodeBase64URL(String src) {
        return Result.lift1(Base64.getUrlDecoder()::decode, src);
    }

    /// Decode a MIME Base64 encoded string to byte array
    ///
    /// @param src MIME Base64 encoded string
    ///
    /// @return Result containing decoded byte array or parsing error
    static Result<byte[] > decodeBase64MIME(String src) {
        return Result.lift1(Base64.getMimeDecoder()::decode, src);
    }

    record unused() implements Text {}
}
