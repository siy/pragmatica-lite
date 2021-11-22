package org.pfj.resource;

import java.util.function.Consumer;

/**
 * The container used by {@link AsyncResource} to access shared resource.
 * The access to resource is available until {@link #release()} method is invoked.
 */
public interface ResourceTicket<T> {
    /**
     * Get access to the resource.
     *
     * @return the resource.
     */
    T access();

    /**
     * Release resource and enable other requesters to get access to it.
     */
    void release();

    /**
     * Convenience method which enables convenient fluent access to the resource.
     *
     * @param action the action to perform on the resource.
     * @return current instance of ticket
     */
    default ResourceTicket<T> act(Consumer<T> action) {
        action.accept(access());
        return this;
    }

    /**
     * Convenience on-step method which performs automatic release of the ticket after applying provided action.
     *
     * @param action the action to perform on the resource.
     */
    default void transact(Consumer<T> action) {
        action.accept(access());
        release();
    }
}
