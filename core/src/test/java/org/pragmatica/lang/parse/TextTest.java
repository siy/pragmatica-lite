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

import java.util.Base64;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TextTest {

    enum TestEnum {
        VALUE_ONE,
        VALUE_TWO,
        VALUE_THREE
    }

    @Test
    void testParseEnumSuccess() {
        Text.parseEnum(TestEnum.class, "VALUE_ONE")
                .onFailureRun(Assertions::fail)
                .onSuccess(value -> assertEquals(TestEnum.VALUE_ONE, value));
    }

    @Test
    void testParseEnumFailure() {
        Text.parseEnum(TestEnum.class, "INVALID_VALUE")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseEnumNull() {
        Text.parseEnum(TestEnum.class, null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testParseEnumEmpty() {
        Text.parseEnum(TestEnum.class, "")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testCompilePatternSuccess() {
        Text.compilePattern("[a-z]+")
                .onFailureRun(Assertions::fail)
                .onSuccess(pattern -> {
                    assertTrue(pattern.matcher("abc").matches());
                    assertFalse(pattern.matcher("123").matches());
                });
    }

    @Test
    void testCompilePatternFailure() {
        Text.compilePattern("[invalid(")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testCompilePatternNull() {
        Text.compilePattern(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testCompilePatternWithFlags() {
        Text.compilePattern("[a-z]+", Pattern.CASE_INSENSITIVE)
                .onFailureRun(Assertions::fail)
                .onSuccess(pattern -> {
                    assertTrue(pattern.matcher("ABC").matches());
                    assertTrue(pattern.matcher("abc").matches());
                });
    }

    @Test
    void testCompilePatternWithFlagsFailure() {
        Text.compilePattern("[invalid(", Pattern.CASE_INSENSITIVE)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testDecodeBase64Success() {
        var original = "Hello, World!";
        var encoded = Base64.getEncoder().encodeToString(original.getBytes());

        Text.decodeBase64(encoded)
                .onFailureRun(Assertions::fail)
                .onSuccess(decoded -> assertEquals(original, new String(decoded)));
    }

    @Test
    void testDecodeBase64Failure() {
        Text.decodeBase64("not-valid-base64!!!")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testDecodeBase64Null() {
        Text.decodeBase64(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testDecodeBase64Empty() {
        Text.decodeBase64("")
                .onFailureRun(Assertions::fail)
                .onSuccess(decoded -> assertEquals(0, decoded.length));
    }

    @Test
    void testDecodeBase64URLSuccess() {
        var original = "Hello, World!";
        var encoded = Base64.getUrlEncoder().encodeToString(original.getBytes());

        Text.decodeBase64URL(encoded)
                .onFailureRun(Assertions::fail)
                .onSuccess(decoded -> assertEquals(original, new String(decoded)));
    }

    @Test
    void testDecodeBase64URLFailure() {
        Text.decodeBase64URL("not/valid+base64")  // Contains non-URL-safe characters
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testDecodeBase64URLNull() {
        Text.decodeBase64URL(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testDecodeBase64MIMESuccess() {
        var original = "Hello, World!";
        var encoded = Base64.getMimeEncoder().encodeToString(original.getBytes());

        Text.decodeBase64MIME(encoded)
                .onFailureRun(Assertions::fail)
                .onSuccess(decoded -> assertEquals(original, new String(decoded)));
    }

    @Test
    void testDecodeBase64MIMEWithLineBreaks() {
        var original = "This is a longer string that will have line breaks when MIME encoded";
        var encoded = Base64.getMimeEncoder().encodeToString(original.getBytes());

        Text.decodeBase64MIME(encoded)
                .onFailureRun(Assertions::fail)
                .onSuccess(decoded -> assertEquals(original, new String(decoded)));
    }

    @Test
    void testDecodeBase64MIMEFailure() {
        Text.decodeBase64MIME("invalid!!!base64")
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }

    @Test
    void testDecodeBase64MIMENull() {
        Text.decodeBase64MIME(null)
                .onSuccessRun(Assertions::fail)
                .onFailure(Assertions::assertNotNull);
    }
}
