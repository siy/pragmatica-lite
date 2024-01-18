package org.pragmatica.http.example.qrgenerator;

import org.pragmatica.lang.Result.Cause;

public sealed interface QrEncoderError extends Cause {
    record IllegalArgument(String message) implements QrEncoderError {}
    record EncodingError(String message) implements QrEncoderError {}
    record WritingError(String message) implements QrEncoderError {}
    record UnexpectedError(String message) implements QrEncoderError {}
}
