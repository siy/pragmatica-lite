package org.pragmatica.http.codec;

import org.pragmatica.lang.Result;

public sealed interface CodecError extends Result.Cause {
    record EncodingError(String message) implements CodecError {}

    record DecodingError(String message) implements CodecError {}

    static CodecError fromCodingThrowable(Throwable issue) {
        return new EncodingError(issue.getMessage());
    }

    static CodecError fromDecodingThrowable(Throwable issue) {
        return new DecodingError(STR."Decoding error \{issue.getMessage()} at \{issue.getStackTrace()[0]}");
    }
}
