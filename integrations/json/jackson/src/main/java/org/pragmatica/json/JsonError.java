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
 *
 */

package org.pragmatica.json;

import org.pragmatica.lang.Cause;
import tools.jackson.core.JacksonException;

/// Error types for JSON serialization/deserialization failures.
public enum JsonError implements Cause {
    SERIALIZATION_FAILED("Failed to serialize value to JSON"),
    DESERIALIZATION_FAILED("Failed to deserialize JSON to value"),
    INVALID_JSON("Invalid JSON format"),
    TYPE_MISMATCH("Type mismatch during deserialization");

    private final String message;

    JsonError(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }

    /// Maps Jackson exceptions to JsonError causes.
    ///
    /// @param throwable Exception to map
    ///
    /// @return Corresponding JsonError
    public static JsonError fromException(Throwable throwable) {
        return switch (throwable) {
            case JacksonException e when e.getMessage().contains("Cannot deserialize") -> TYPE_MISMATCH;
            case JacksonException e when e.getMessage().contains("parse") -> INVALID_JSON;
            case JacksonException ignored -> DESERIALIZATION_FAILED;
            default -> SERIALIZATION_FAILED;
        };
    }
}
