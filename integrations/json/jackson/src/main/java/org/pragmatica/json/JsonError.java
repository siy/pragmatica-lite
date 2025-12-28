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
import org.pragmatica.lang.Option;
import org.pragmatica.lang.utils.Causes;

import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.MismatchedInputException;

/// Typed error causes for JSON serialization/deserialization operations.
/// Maps common Jackson exceptions to domain-friendly error types with context.
public sealed interface JsonError extends Cause {
    /// Serialization failed (converting object to JSON).
    record SerializationFailed(String details, Option<Throwable> cause) implements JsonError {
        public static SerializationFailed serializationFailed(String details) {
            return new SerializationFailed(details, Option.none());
        }

        public static SerializationFailed serializationFailed(String details, Throwable cause) {
            return new SerializationFailed(details, Option.option(cause));
        }

        @Override
        public String message() {
            return "Serialization failed: " + details;
        }
    }

    /// Deserialization failed (converting JSON to object).
    record DeserializationFailed(String details, Option<Throwable> cause) implements JsonError {
        public static DeserializationFailed deserializationFailed(String details) {
            return new DeserializationFailed(details, Option.none());
        }

        public static DeserializationFailed deserializationFailed(String details, Throwable cause) {
            return new DeserializationFailed(details, Option.option(cause));
        }

        @Override
        public String message() {
            return "Deserialization failed: " + details;
        }
    }

    /// Invalid JSON syntax (parse error).
    record InvalidJson(String details, Option<String> locationInfo) implements JsonError {
        public static InvalidJson invalidJson(String details) {
            return new InvalidJson(details, Option.none());
        }

        public static InvalidJson invalidJson(String details, String locationInfo) {
            return new InvalidJson(details, Option.option(locationInfo));
        }

        @Override
        public String message() {
            return locationInfo.map(loc -> "Invalid JSON at " + loc + ": " + details)
                                                .or("Invalid JSON: " + details);
        }
    }

    /// Type mismatch during deserialization.
    record TypeMismatch(String expectedType, String actualValue, Option<String> path) implements JsonError {
        public static TypeMismatch typeMismatch(String expectedType, String actualValue) {
            return new TypeMismatch(expectedType, actualValue, Option.none());
        }

        public static TypeMismatch typeMismatch(String expectedType, String actualValue, String path) {
            return new TypeMismatch(expectedType, actualValue, Option.option(path));
        }

        @Override
        public String message() {
            var base = "Type mismatch: expected " + expectedType + ", got " + actualValue;
            return path.map(p -> base + " at " + p)
                       .or(base);
        }
    }

    /// Maps Jackson exceptions to typed JsonError causes.
    ///
    /// @param throwable Exception to map
    ///
    /// @return Corresponding Cause (JsonError for known types, generic Cause otherwise)
    static Cause fromException(Throwable throwable) {
        return switch (throwable) {
            // Serialization failures (write-side)
            case StreamWriteException e ->
            SerializationFailed.serializationFailed(e.getMessage(), e);
            // Type mismatches during deserialization
            case MismatchedInputException e ->
            TypeMismatch.typeMismatch(
            e.getTargetType() != null
            ? e.getTargetType()
               .getSimpleName()
            : "unknown",
            extractValue(e),
            e.getPathReference());
            case InvalidDefinitionException e ->
            TypeMismatch.typeMismatch(
            e.getType() != null
            ? e.getType()
               .getTypeName()
            : "unknown",
            "invalid definition",
            e.getPathReference());
            // JSON parsing errors (malformed JSON)
            case StreamReadException e ->
            InvalidJson.invalidJson(e.getMessage(), extractLocation(e));
            // General databind failures
            case DatabindException e ->
            DeserializationFailed.deserializationFailed(e.getMessage(), e);
            // Other Jackson exceptions
            case JacksonException e ->
            DeserializationFailed.deserializationFailed(e.getMessage(), e);
            // Non-Jackson exceptions - use generic wrapper
            default -> Causes.fromThrowable(throwable);
        };
    }

    private static String extractValue(MismatchedInputException e) {
        var msg = e.getMessage();
        if (msg != null && msg.contains("Cannot deserialize value of type")) {
            var fromIdx = msg.indexOf("from ");
            if (fromIdx > 0) {
                var endIdx = msg.indexOf(" ", fromIdx + 5);
                if (endIdx > fromIdx) {
                    return msg.substring(fromIdx + 5, endIdx);
                }
            }
        }
        return "unknown";
    }

    private static String extractLocation(StreamReadException e) {
        var loc = e.getLocation();
        if (loc != null) {
            return "line " + loc.getLineNr() + ", column " + loc.getColumnNr();
        }
        return null;
    }
}
