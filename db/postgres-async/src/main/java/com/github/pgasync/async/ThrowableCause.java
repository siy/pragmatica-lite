package com.github.pgasync.async;

import com.github.pgasync.SqlError;
import com.github.pgasync.net.SqlException;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result.Cause;

import java.util.Arrays;

public record ThrowableCause(Throwable throwable, Option<Cause> source) implements Cause {
    @Override
    public String message() {
        return throwable.getMessage() + "\n" + Arrays.toString(throwable.getStackTrace());
    }

    public static ThrowableCause asCause(Throwable throwable) {
        return new ThrowableCause(throwable, Option.none());
    }

    public static ThrowableCause forError(SqlError cause) {
        return new ThrowableCause(new SqlException(cause.message()), Option.some(cause));
    }
}
