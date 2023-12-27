package com.github.pgasync.async;

import org.pragmatica.lang.Result;

public record ThrowableCause(Throwable throwable) implements Result.Cause {
    @Override
    public String message() {
        return throwable.getMessage();
    }

    public static ThrowableCause asCause(Throwable throwable) {
        return new ThrowableCause(throwable);
    }
}
