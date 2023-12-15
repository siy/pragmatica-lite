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
import com.github.pgasync.net.PreparedStatement;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.AsyncCloseable;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
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
        System.setProperty("io.netty.eventLoopThreads", "1");
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

    @SuppressWarnings("deprecation")
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
                                   .mapToObj(i -> pool.connection().await().unwrap())
                                   .toList();
        connections.forEach(connection -> {
            connection.prepareStatement(SELECT_42).await().unwrap().close().await();
            connection.close().await();
        });
    }

    @After
    public void tearDown() {
        pool.close().await();
    }

    @Test
    public void observeBatches() {
        performBatches(simpleQueryResults, i -> new Batch(batchSize).startWithSimpleQuery());
        performBatches(preparedStatementResults, i -> new Batch(batchSize).startWithPreparedStatement());
    }

    private void performBatches(SortedMap<Integer, SortedMap<Integer, Long>> results, IntFunction<CompletableFuture<Long>> batchStarter) {
        double mean = LongStream.range(0, repeats)
                                .map(_ -> {
                                    try {
                                        var batches = IntStream.range(0, poolSize)
                                                               .mapToObj(batchStarter)
                                                               .toList();
                                        CompletableFuture.allOf(batches.toArray(new CompletableFuture<?>[]{})).get();
                                        return batches.stream().map(CompletableFuture::join).max(Long::compare).get();
                                    } catch (Exception ex) {
                                        throw new RuntimeException(ex);
                                    }
                                })
                                .average()
                                .getAsDouble();
        results.computeIfAbsent(poolSize, k -> new TreeMap<>())
               .put(numThreads, Math.round(mean));
    }

    private class Batch {

        private final long batchSize;
        private AtomicLong performed = new AtomicLong(0L);
        private long startedAt;
        private CompletableFuture<Long> onBatch;

        Batch(long batchSize) {
            this.batchSize = batchSize;
        }

        private CompletableFuture<Long> startWithPreparedStatement() {
            onBatch = new CompletableFuture<>();
            startedAt = System.currentTimeMillis();
            nextSamplePreparedStatement();
            return onBatch;
        }

        private CompletableFuture<Long> startWithSimpleQuery() {
            onBatch = new CompletableFuture<>();
            startedAt = System.currentTimeMillis();
            nextSampleSimpleQuery();
            return onBatch;
        }

        private void nextSamplePreparedStatement() {
            pool.connection()
                .onSuccessDo(connection ->
                                 connection.prepareStatement(SELECT_42)
                                           .onSuccessDo(PreparedStatement::query)
                                           .onSuccessDo(AsyncCloseable::close))
                .onSuccessDo(Connection::close)
                .onResult(() -> {
                    if (performed.incrementAndGet() < batchSize) {
                        Promise.runAsync(this::nextSamplePreparedStatement);
                    } else {
                        long duration = currentTimeMillis() - startedAt;
                        onBatch.complete(duration);
                    }
                });
        }

        private void nextSampleSimpleQuery() {
            pool.completeScript(SELECT_42)
                .onResult(() -> {
                    if (performed.incrementAndGet() < batchSize) {
                        Promise.runAsync(this::nextSamplePreparedStatement);
                    } else {
                        long duration = currentTimeMillis() - startedAt;
                        onBatch.complete(duration);
                    }
                });
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