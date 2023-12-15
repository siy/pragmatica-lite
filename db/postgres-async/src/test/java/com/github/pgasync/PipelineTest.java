/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync;

import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.Connection;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.pragmatica.lang.Promise;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for statements pipelining.
 *
 * @author Mikko Tiihonen
 * @author Marat Gainullin
 */
@Tag("Slow")
public class PipelineTest {

    @ClassRule
    public static DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    private Connection c;
    private Connectible pool;

    @Before
    public void setupPool() {
        pool = dbr.builder.pool();
    }

    @After
    public void closePool() {
        if (c != null) {
            c.close().await();
        }
        if (pool != null) {
            pool.close().await();
        }
    }

    @Test
    public void connectionPoolPipelinesQueries() throws InterruptedException {
        int count = 5;
        double sleep = 0.5;
        Deque<Long> results = new LinkedBlockingDeque<>();
        long startWrite = currentTimeMillis();
        for (int i = 0; i < count; ++i) {
            pool.completeQuery(STR."select \{i}, pg_sleep(\{sleep})")
                .onSuccessRun(() -> results.add(currentTimeMillis()));
        }

        long writeTime = currentTimeMillis() - startWrite;
        long remoteWaitTimeSeconds = (long) (sleep * count);
        SECONDS.sleep(2 + remoteWaitTimeSeconds);
        long readTime = results.getLast() - results.getFirst();

        assertThat(results.size(), is(count));
        assertThat(MILLISECONDS.toSeconds(writeTime), is(0L));
        assertThat(MILLISECONDS.toSeconds(readTime + 999) >= remoteWaitTimeSeconds, is(true));
    }

    private Connection getConnection() {
        var connQueue = new SynchronousQueue<Connection>();
        pool.connection()
            .onSuccess(connQueue::offer);
        try {
            return c = connQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test //TODO: what is this test for?
    public void messageStreamEnsuresSequentialAccess() {
        Connection connection = getConnection();

        Promise.allOf(IntStream.range(0, 10)
                               .mapToObj(i -> connection.completeQuery(STR."select \{i}, pg_sleep(10)"))
                               .toList())
               .await();
    }
}
