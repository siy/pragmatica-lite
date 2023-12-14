package com.github.pgasync;

import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.Connection;
import com.github.pgasync.net.Listening;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Antti Laisi
 */
@Tag("Slow")
public class ListenNotifyTest {

    @ClassRule
    public static DatabaseRule dbr = DatabaseRule.withMaxConnections(5);

    private Connectible pool;

    @Before
    public void setup() {
        pool = dbr.pool();
    }

    @After
    public void shutdown() {
        pool.close().await();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldReceiveNotificationsOnListenedChannel() throws InterruptedException {
        var result = new LinkedBlockingQueue<String>(5);

        var conn = pool.connection().await().unwrap();
        try {
            var subscription = conn.subscribe("example", result::offer).await().unwrap();
            try {
                TimeUnit.SECONDS.sleep(2);

                pool.completeScript("notify example, 'msg-1'").await();
                pool.completeScript("notify example, 'msg-2'").await();
                pool.completeScript("notify example, 'msg-3'").await();

                assertEquals("msg-1", result.poll(2, TimeUnit.SECONDS));
                assertEquals("msg-2", result.poll(2, TimeUnit.SECONDS));
                assertEquals("msg-3", result.poll(2, TimeUnit.SECONDS));
            } finally {
                subscription.unlisten().await();
            }
        } finally {
            conn.close().await();
        }

        pool.completeQuery("notify example, 'msg'").await();
        assertNull(result.poll(2, TimeUnit.SECONDS));
    }
}
