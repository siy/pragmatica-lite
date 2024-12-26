package org.pragmatica.lang;

/**
 * Basic interface for failure cause types.
 */
public interface Cause {
    /**
     * Message associated with the failure.
     */
    String message();

    /**
     * The original cause (if any) of the error.
     */
    default Option<Cause> source() {
        return Option.empty();
    }

    /**
     * Represent cause as a failure {@link Result} instance.
     *
     * @return cause converted into {@link Result} with necessary type.
     */
    default <T> Result<T> result() {
        return Result.failure(this);
    }

    /**
     * Represent cause as a failure {@link Result} instance.
     *
     * @return cause converted into {@link Result} with necessary type.
     */
    default <T> Promise<T> promise() {
        return Promise.failure(this);
    }
}
