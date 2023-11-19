package org.pragmatica.async.resource;

import org.pragmatica.lang.Promise;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A asyncResource, access to which can be obtained asynchronously. Unlike traditional synchronous locks, this tool does not block execution if
 * asyncResource is already locked. Instead, it returns an instance of {@link Promise} with the {@link ResourceTicket} inside. The
 * {@link ResourceTicket} allows direct access to the locked asyncResource and releasing asyncResource once access to it is no longer required.
 * <p>
 * It is guaranteed to only one returned {@link Promise} will be resolved at a time, so asyncResource can be freely accessed via
 * {@link ResourceTicket#resource()} without need of any additional locks or other synchronization mechanisms. The asyncResource is accessible until
 * {@link ResourceTicket#release()} is called. Once this method is called, no further attempts to access asyncResource should be performed, as
 * behavior of such access is undefined.
 */
public class AsyncResource<T> {
    private final ConcurrentLinkedQueue<Promise<ResourceTicket<T>>> waitQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<ResourceTicket<T>> lock = new AtomicReference<>();
    private final T resource;

    private AsyncResource(T resource) {
        this.resource = resource;
    }

    public static <T> AsyncResource<T> asyncResource(T resource) {
        return new AsyncResource<>(resource);
    }

    public Promise<ResourceTicket<T>> request() {
        var ticket = new ResourceTicketImpl();

        return lock.compareAndSet(null, ticket)
               ? Promise.successful(ticket)
               : Promise.promise(waitQueue::add);
    }

    private T resource() {
        return resource;
    }

    private void release(ResourceTicket<T> resourceTicket) {
        var next = waitQueue.poll();
        var nextTicket = next == null ? null : new ResourceTicketImpl();

        if (!lock.compareAndSet(resourceTicket, nextTicket)) {
            throw new IllegalStateException("Attempt to release asyncResource not owned by current ticket");
        }

        if (next != null) {
            next.async(promise -> promise.success(nextTicket));
        }
    }

    private class ResourceTicketImpl implements ResourceTicket<T> {
        @Override
        public T resource() {
            return AsyncResource.this.resource();
        }

        @Override
        public void release() {
            AsyncResource.this.release(this);
        }
    }
}
