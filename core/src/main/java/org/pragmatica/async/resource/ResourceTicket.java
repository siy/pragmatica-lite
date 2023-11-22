package org.pragmatica.async.resource;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;

import static org.pragmatica.lang.Result.lift;

/**
 * The container used by {@link AsyncResource} to access shared asyncResource.
 * The access to asyncResource is available until {@link #release()} method is invoked.
 */
public interface ResourceTicket<T> {
    /**
     * Get access to the resource.
     *
     * @return the resource.
     */
    T resource();

    /**
     * Release resource and enable other requesters to get access to it.
     */
    void release();

    /**
     * Convenience on-step method which performs automatic release of the ticket after applying provided transformation.
     *
     * @param transformation the transformation to perform on the asyncResource.
     */
    default <R> Result<R> perform(Fn1<? extends Cause, ? super Throwable> exceptionMapper, Fn1<R, T> transformation) {
        return lift(exceptionMapper, () -> transformation.apply(resource()))
            .onResultDo(this::release);
    }
}
