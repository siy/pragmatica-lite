package org.pragmatica.codec.json;

import org.pragmatica.lang.Result;

public sealed interface SerializerError extends Result.Cause {
    record SerializationError(String message) implements SerializerError {}

    record DeserializationError(String message) implements SerializerError {}

    static SerializerError fromSerializationThrowable(Throwable issue) {
        return new SerializationError(issue.getMessage());
    }

    static SerializerError fromDeserializationThrowable(Throwable issue) {
        return new DeserializationError(issue.getMessage());
    }
}
