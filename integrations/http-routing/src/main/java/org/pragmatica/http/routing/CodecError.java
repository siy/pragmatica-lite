package org.pragmatica.http.routing;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.utils.Causes;

public sealed interface CodecError extends Cause {
    record SerializationFailed(String message, Option<Cause> source) implements CodecError {
        @Override
        public String message() {
            return message;
        }
    }

    record DeserializationFailed(String message, Option<Cause> source) implements CodecError {
        @Override
        public String message() {
            return message;
        }
    }

    static CodecError serializationFailed(String message, Cause source) {
        return new SerializationFailed(message, Option.option(source));
    }

    static CodecError deserializationFailed(String message, Cause source) {
        return new DeserializationFailed(message, Option.option(source));
    }

    static CodecError fromSerializationThrowable(Throwable t) {
        return new SerializationFailed("Serialization failed: " + t.getMessage(),
                                       Option.option(Causes.fromThrowable(t)));
    }

    static CodecError fromDeserializationThrowable(Throwable t) {
        return new DeserializationFailed("Deserialization failed: " + t.getMessage(),
                                         Option.option(Causes.fromThrowable(t)));
    }
}
