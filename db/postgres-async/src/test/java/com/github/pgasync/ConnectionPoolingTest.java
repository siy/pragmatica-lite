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

import com.github.pgasync.net.ResultSet;
import org.junit.*;
import org.junit.jupiter.api.Tag;
import org.pragmatica.lang.Promise;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * Tests for connection pool concurrency.
 *
 * @author Antti Laisi
 */
@Tag("Slow")
@Ignore //TODO: fix this test
public class ConnectionPoolingTest {

    @Rule
    public final DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @Before
    public void create() {
        dbr.script(
            "DROP TABLE IF EXISTS CP_TEST;" +
            "CREATE TABLE CP_TEST (ID VARCHAR(255) PRIMARY KEY)"
        );
    }

    @After
    public void drop() {
        dbr.query("DROP TABLE CP_TEST");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRunAllQueuedCallbacks() {
        final int count = 1000;

        var queries = IntStream.range(0, count)
                 .mapToObj(value -> Promise.<ResultSet>promise(promise -> promise.resolve(dbr.pool()
                                                                                             .completeQuery("INSERT INTO CP_TEST VALUES($1)", value)
                                                                                             .await())))
                 .toList();

        queries.forEach(Promise::await);

        assertEquals(count, dbr.query("SELECT COUNT(*) FROM CP_TEST").unwrap().index(0).getLong(0).longValue());
    }
}
