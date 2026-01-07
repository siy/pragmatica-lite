/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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

package org.pragmatica.config.toml;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

/// Typed errors for TOML parsing operations.
public sealed interface TomlError extends Cause {
    record SyntaxError(int line, String details) implements TomlError {
        @Override
        public String message() {
            return "TOML syntax error at line " + line + ": " + details;
        }
    }

    record InvalidValue(int line, String value, String expectedType) implements TomlError {
        @Override
        public String message() {
            return "Invalid value at line " + line + ": expected " + expectedType + ", got '" + value + "'";
        }
    }

    record UnterminatedString(int line) implements TomlError {
        @Override
        public String message() {
            return "Unterminated string at line " + line;
        }
    }

    record UnterminatedArray(int line) implements TomlError {
        @Override
        public String message() {
            return "Unterminated array at line " + line;
        }
    }

    record UnterminatedMultilineString(int line) implements TomlError {
        @Override
        public String message() {
            return "Unterminated multiline string starting at line " + line;
        }
    }

    record FileReadFailed(String path, String details) implements TomlError {
        @Override
        public String message() {
            return "Failed to read file '" + path + "': " + details;
        }
    }

    record DuplicateKey(int line, String key) implements TomlError {
        @Override
        public String message() {
            return "Duplicate key at line " + line + ": '" + key + "'";
        }
    }

    record InvalidEscapeSequence(int line, String sequence) implements TomlError {
        @Override
        public String message() {
            return "Invalid escape sequence at line " + line + ": '\\" + sequence + "'";
        }
    }

    record TableTypeMismatch(int line, String name, String existingType) implements TomlError {
        @Override
        public String message() {
            return "Table type mismatch at line " + line + ": '" + name + "' was previously defined as " + existingType;
        }
    }

    static TomlError syntaxError(int line, String details) {
        return new SyntaxError(line, details);
    }

    static TomlError invalidValue(int line, String value, String expectedType) {
        return new InvalidValue(line, value, expectedType);
    }

    static TomlError unterminatedString(int line) {
        return new UnterminatedString(line);
    }

    static TomlError unterminatedArray(int line) {
        return new UnterminatedArray(line);
    }

    static TomlError unterminatedMultilineString(int line) {
        return new UnterminatedMultilineString(line);
    }

    static TomlError fileReadFailed(String path, String details) {
        return new FileReadFailed(path, details);
    }

    static TomlError duplicateKey(int line, String key) {
        return new DuplicateKey(line, key);
    }

    static TomlError invalidEscapeSequence(int line, String sequence) {
        return new InvalidEscapeSequence(line, sequence);
    }

    static TomlError tableTypeMismatch(int line, String name, String existingType) {
        return new TableTypeMismatch(line, name, existingType);
    }

    default <T> Result<T> result() {
        return Result.failure(this);
    }
}
