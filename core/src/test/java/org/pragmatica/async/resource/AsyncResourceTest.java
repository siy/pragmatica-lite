package org.pragmatica.async.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.utils.Causes;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AsyncResourceTest {

    @Test
    void resourceCanBeAcquiredAndReleased() {
        var resource = AsyncResource.asyncResource("value");
        var ticketPromise = resource.request();

        assertTrue(ticketPromise.isResolved());

        ticketPromise
            .onFailure(f -> fail(f.message()))
            .onSuccess(ticket -> assertEquals("value", ticket.resource()))
            .join();
    }

    @Test
    void resourceCanBeLockedAsynchronously() throws InterruptedException {
        var numRequests = 1_000_000;
        var resource = AsyncResource.asyncResource(new AtomicInteger(0));
        var latch = new CountDownLatch(numRequests);
        var buffer = new StringBuffer(); //synchronized version is required here

        try (var executor = Executors.newFixedThreadPool(8)) {

            for (int i = 1; i <= numRequests; i++) {
                var localI = i;

                executor.submit(
                    () -> resource.request()
                                  .onSuccess(ticket -> testSingleTicket(ticket, localI, latch, buffer))
                );
            }
            latch.await();

            if (!buffer.isEmpty()) {
                fail(buffer.toString());
            }
        }
    }

    private void testSingleTicket(ResourceTicket<AtomicInteger> ticket, int localI,
                                  CountDownLatch latch, StringBuffer buffer) {
        try {
            if (!ticket.resource().compareAndSet(0, localI)) {
                var message = "0 -> " + localI + " transition failed: " + ticket.resource().get() + "\n";
                buffer.append(message);
            }

            if (!ticket.resource().compareAndSet(localI, 0)) {
                var message = localI + " -> 0 transition failed: " + ticket.resource().get() + "\n";
                buffer.append(message);
            }

            ticket.resource().set(0);
        } finally {
            latch.countDown();
            ticket.release();
        }
    }

    @Test
    void resourceMadeAccessibleToOneRequesterAtATime() {
        var resource = AsyncResource.asyncResource(new AtomicInteger(0));
        var promise1 = resource.request();
        var promise2 = resource.request();

        assertTrue(promise1.isResolved());
        assertFalse(promise2.isResolved());

        promise1.onSuccess(ticket -> ticket.resource().set(1));

        assertFalse(promise2.isResolved());

        promise1.onSuccess(ResourceTicket::release);
        var result = promise2.join();

        assertTrue(promise2.isResolved());

        result.flatMap(ticket -> ticket.perform(Causes::fromThrowable, res -> 1 == res.get()))
              .onFailureDo(Assertions::fail)
              .onSuccess(Assertions::assertTrue);
    }
}