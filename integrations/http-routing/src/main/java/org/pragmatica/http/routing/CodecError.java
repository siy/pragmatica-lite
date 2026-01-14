package org.pragmatica.http.routing;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.Option;

public sealed interface CodecError extends Cause {
    record SerializationFailed(String message, Throwable cause) implements CodecError {
        @Override
        public Option<Cause> source() {
            return Option.option(cause)
                         .map(Causes::fromThrowable);
        }
    }

    record DeserializationFailed(String message, Throwable cause) implements CodecError {
        @Override
        public Option<Cause> source() {
            return Option.option(cause)
                         .map(Causes::fromThrowable);
        }
    }

    static CodecError fromSerializationThrowable(Throwable t) {
        return new SerializationFailed("Serialization failed: " + t.getMessage(), t);
    }

    static CodecError fromDeserializationThrowable(Throwable t) {
        return new DeserializationFailed("Deserialization failed: " + t.getMessage(), t);
    }
}
