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

import com.github.pgasync.async.ThrowingPromise;
import com.github.pgasync.net.Connectible;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.pragmatica.lang.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

@Tag("Slow")
@RunWith(Parameterized.class)
public class PerformanceTest {
    private static final DatabaseRule dbr;

    static {
        // Uncomment to run with single event loop thread, although I see no big value in it
//        System.setProperty("io.netty.eventLoopThreads", "1");
        dbr = DatabaseRule.withMaxConnections(1);
    }

    private static final String SELECT_42 = "select 42";


    @Parameters(name = "{index}: maxConnections={0}, threads={1}")
    public static Iterable<Object[]> data() {
        var testData = new ArrayList<Object[]>();
        var numbers = List.of(1, 6, 12);

        for (var poolSize : numbers) {
            for (var threads : numbers) {
                testData.add(new Object[]{poolSize, threads});
            }
        }
        return testData;
    }

    private static final int batchSize = 1000;
    private static final int repeats = 5;
    private static final SortedMap<Integer, SortedMap<Integer, Long>> simpleQueryResults = new TreeMap<>();
    private static final SortedMap<Integer, SortedMap<Integer, Long>> preparedStatementResults = new TreeMap<>();

    private final int poolSize;
    private final int numThreads;
    private Connectible pool;

    public PerformanceTest(int poolSize, int numThreads) {
        this.poolSize = poolSize;
        this.numThreads = numThreads;
    }


    @Before
    public void setup() {
        DatabaseRule.postgres.start();
        pool = dbr.builder
            .maxConnections(poolSize)
            .port(DatabaseRule.postgres.getMappedPort(5432))
            .hostname(DatabaseRule.postgres.getHost())
            .password(DatabaseRule.postgres.getPassword())
            .database(DatabaseRule.postgres.getDatabaseName())
            .username(DatabaseRule.postgres.getUsername())
            .pool();
        var connections = IntStream.range(0, poolSize)
                                   .mapToObj(_ -> pool.getConnection().await()).toList();
        connections.forEach(connection -> {
            connection.prepareStatement(SELECT_42).await().close().await();
            connection.close().await();
        });
    }

    @After
    public void tearDown() {
        pool.close().await();
    }

    @Test
    public void observeBatches() {
        performBatches(simpleQueryResults, _ -> new Batch(batchSize).startWithSimpleQuery());
        performBatches(preparedStatementResults, _ -> new Batch(batchSize).startWithPreparedStatement());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void performBatches(SortedMap<Integer, SortedMap<Integer, Long>> results, IntFunction<ThrowingPromise<Long>> batchStarter) {
        double mean = LongStream.range(0, repeats)
                                .map(_ -> {
                                    try {
                                        var batches = IntStream.range(0, poolSize)
                                                               .mapToObj(batchStarter)
                                                               .toList();
                                        ThrowingPromise.allOf(batches.stream())
                                                       .await();
                                        return batches.stream()
                                                      .map(ThrowingPromise::await)
                                                      .max(Long::compare)
                                                      .get();
                                    } catch (Exception ex) {
                                        throw new RuntimeException(ex);
                                    }
                                })
                                .average()
                                .getAsDouble();
        results.computeIfAbsent(poolSize, _ -> new TreeMap<>())
               .put(numThreads, Math.round(mean));
    }

    private class Batch {
        private final long batchSize;
        private long performed;
        private long startedAt;
        private ThrowingPromise<Long> onBatch;

        Batch(long batchSize) {
            this.batchSize = batchSize;
        }

        private ThrowingPromise<Long> startWithPreparedStatement() {
            onBatch = ThrowingPromise.create();
            startedAt = System.currentTimeMillis();
            nextSamplePreparedStatement();
            return onBatch;
        }

        private ThrowingPromise<Long> startWithSimpleQuery() {
            onBatch = ThrowingPromise.create();
            startedAt = System.currentTimeMillis();
            nextSampleSimpleQuery();
            return onBatch;
        }

        private void nextSamplePreparedStatement() {
            pool.getConnection()
                .flatMap(connection ->
                             connection.prepareStatement(SELECT_42)
                                       .flatMap(stmt ->
                                                    stmt.query()
                                                        .fold(_ -> stmt.close())
                                                        .fold(_ -> connection.close())))

                .onSuccess(_ -> {
                    if (++performed < batchSize) {
                        nextSamplePreparedStatement();
                    } else {
                        long duration = currentTimeMillis() - startedAt;
                        onBatch.succeed(duration);
                    }
                })
                .tryRecover(th -> {
                    onBatch.fail(th);
                    return Unit.aUnit();
                });

        }

        private void nextSampleSimpleQuery() {
            pool.completeScript(SELECT_42)
                .onSuccess(_ -> {
                    if (++performed < batchSize) {
                        nextSamplePreparedStatement();
                    } else {
                        long duration = currentTimeMillis() - startedAt;
                        onBatch.succeed(duration);
                    }
                })
                .withFailure(th -> onBatch.fail(th));
        }
    }

    @AfterClass
    public static void printResults() {
        out.println();
        out.println("Requests per second, Hz:");
        out.println();
        out.println("Simple query protocol");
        printResults(simpleQueryResults);
        out.println();
        out.println("Extended query protocol (reusing prepared statement)");
        printResults(preparedStatementResults);
    }

    private static void printResults(SortedMap<Integer, SortedMap<Integer, Long>> results) {
        out.print(" threads");
        results.keySet().forEach(i -> out.printf("\t%d conn\t", i));
        out.println();

        results.values().iterator().next().keySet().forEach(threads -> {
            out.print(STR."    \{threads}");
            results.keySet().forEach(connections -> {
                long batchDuration = results.get(connections).get(threads);
                double rps = 1000 * batchSize * connections / (double) batchDuration;
                out.printf("\t\t%d", Math.round(rps));
            });
            out.println();
        });
    }
}