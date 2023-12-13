package com.github.pgasync;

import org.pragmatica.lang.Result;

public sealed interface SqlError extends Result.Cause {
    record ChannelClosed(String message) implements SqlError {}

    record SimultaneousUseDetected(String message) implements SqlError {}

    record ConnectionPoolClosed(String message) implements SqlError {}

    record BadAuthenticationSequence(String message) implements SqlError {}

    static Result.Cause fromThrowable(Throwable th) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
