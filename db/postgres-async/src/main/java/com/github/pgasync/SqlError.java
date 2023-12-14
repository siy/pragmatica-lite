package com.github.pgasync;

import org.pragmatica.lang.Result;

import javax.naming.CommunicationException;

public sealed interface SqlError extends Result.Cause {
    record ChannelClosed(String message) implements SqlError {}
    record SimultaneousUseDetected(String message) implements SqlError {}
    record ConnectionPoolClosed(String message) implements SqlError {}
    record BadAuthenticationSequence(String message) implements SqlError {}
    record InvalidCredentials(String message) implements SqlError {}
    record ServerErrorResponse(String message) implements SqlError {}
    record CommunicationError(String message) implements SqlError {}

    static SqlError fromThrowable(Throwable th) {
        return new CommunicationError(th.getMessage());
    }
}
