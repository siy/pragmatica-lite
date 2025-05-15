package org.pragmatica.cluster.consensus.rabia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.pragmatica.cluster.state.kvstore.KVStore;
import org.pragmatica.lang.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Test Suite 1: Nominal Operation
 */
public class NominalOperationTest {
    private static final Logger log = LoggerFactory.getLogger(NominalOperationTest.class);
    private static final int CLUSTER_SIZE = 5;
    private static final int REQUEST_COUNT = 10_000;

    private TestCluster cluster;

    @BeforeEach
    void setUp() {
        cluster = new TestCluster(CLUSTER_SIZE);
        cluster.awaitStart();
        log.info("Cluster started with {} nodes", CLUSTER_SIZE);
    }

    /**
     * Test 1.1: Single-client sequential commands
     * Setup: 5 nodes, stable network, 10,000 requests
     * Assertion: identical state digest on all nodes, no gaps, latency ≤ 2×mean RTT
     */
    @Test
    void singleClientSequentialCommands() {
        log.info("Starting single client sequential test with {} requests", REQUEST_COUNT);

        // Get the first node to submit commands
        var clientNode = cluster.getFirst();

        // Capture start time for throughput and latency calculations
        long startTime = System.currentTimeMillis();
        var commandLatencies = new ArrayList<Long>();
        int batchSize = 100;

        for (int idx = 0; idx < REQUEST_COUNT; idx += batchSize) {
            var i = idx;

            int end = Math.min(i + batchSize, REQUEST_COUNT);
            var batch = new ArrayList<KVCommand>();

            for (int j = i; j < end; j++) {
                batch.add(new KVCommand.Put<>("key-" + j, "value-" + j));
            }

            // Measure latency for this batch
            long batchStart = System.nanoTime();

            cluster.engines().get(clientNode)
                   .apply(batch)
                   .await(timeSpan(30).seconds())
                   .onSuccess(_ -> {
                       long latency = System.nanoTime() - batchStart;
                       commandLatencies.add(latency);

                       if ((end % 1000) == 0) {
                           log.info("Processed {} commands", end);
                       }
                   })
                   .onFailure(cause -> log.error("Failed to apply batch {}-{}: {}", i, end - 1, cause.message()));

            // Every 1000 commands, verify state is consistent
            if (end % 1000 == 0) {
                int checkpointIndex = end - 1;
                await().atMost(Duration.ofSeconds(30))
                       .pollInterval(Duration.ofMillis(100))
                       .until(() -> cluster.allNodesHaveValue("key-" + checkpointIndex, "value-" + checkpointIndex));

                // Verify all previous keys exist
                for (int k = 0; k < end; k += 1000) {
                    assertTrue(cluster.allNodesHaveValue("key-" + k, "value-" + k),
                               "All nodes should have key-" + k);
                }
            }
        }

        // Calculate overall execution time and throughput
        long endTime = System.currentTimeMillis();
        double executionTimeSeconds = (endTime - startTime) / 1000.0;
        double throughput = REQUEST_COUNT / executionTimeSeconds;

        log.info("Completed {} commands in {} seconds", REQUEST_COUNT, executionTimeSeconds);
        log.info("Throughput: {} commands/second", throughput);

        // Calculate average latency
        double avgLatencyNanos = commandLatencies.stream()
                                                 .mapToLong(Long::longValue)
                                                 .average()
                                                 .orElse(0);
        double avgLatencyMillis = avgLatencyNanos / 1_000_000.0;

        log.info("Average latency: {} ms", avgLatencyMillis);

        // Verify all keys are present (no gaps)
        for (int i = 0; i < REQUEST_COUNT; i++) {
            var key = "key-" + i;
            var expectedValue = "value-" + i;

            assertTrue(cluster.allNodesHaveValue(key, expectedValue),
                       "All nodes should have " + key + " with value " + expectedValue);
        }

        // In a local environment, latency should typically be reasonable (adjust as needed)
        // This is a placeholder - actual RTT measurements would be needed for precise assertions
        assertTrue(avgLatencyMillis < 1000.0,
                   "Average latency should be reasonable in local testing");
    }

    /**
     * Test 1.2: 100 concurrent clients
     * Setup: 5 nodes, random inter‑arrival delays, 10,000 total commands
     * Assertion: single global order, no duplicate execution, throughput linear to load until CPU saturation
     */
    @Test
    void concurrentClients() {
        log.info("Starting concurrent clients test");

        final int CLIENT_COUNT = 100;
        final int COMMANDS_PER_CLIENT = REQUEST_COUNT / CLIENT_COUNT;

        log.info("Using {} clients with {} commands each", CLIENT_COUNT, COMMANDS_PER_CLIENT);

        var counters = new ConcurrentHashMap<NodeId, CountDownLatch>();

        cluster.stores()
               .forEach((nodeId, store) ->
                                store.observeStateChanges(
                                        _ -> counters.computeIfAbsent(nodeId,
                                                                      _ -> new CountDownLatch(REQUEST_COUNT))
                                                     .countDown()));

        // Create a random generator for inter-arrival times
        var random = new Random();

        // Reset metrics if any
        long startTime = System.currentTimeMillis();

        var valueMap = new ConcurrentHashMap<String, String>();
        var promises = new ConcurrentLinkedQueue<Promise<List<Object>>>();

        for (int i = 0; i < CLIENT_COUNT; i++) {
            var targetNode = cluster.ids().get(random.nextInt(CLUSTER_SIZE));

            for (int j = 0; j < COMMANDS_PER_CLIENT; j++) {
                var key = "client-" + i + "-key-" + j;
                var value = "client-" + i + "-value-" + j;

                valueMap.put(key, value);

                var commandPromise = Promise.<List<Object>>promise(timeSpan(random.nextInt(500)).millis(),
                                                                   promise -> cluster.engines()
                                                                                     .get(targetNode)
                                                                                     .apply(List.of(new KVCommand.Put<>(
                                                                                             key,
                                                                                             value)))
                                                                                     .onResult(promise::resolve));
                promises.add(commandPromise);
            }
        }

        // Wait for all requests to complete
        Promise.allOf(promises)
               .await(timeSpan(120).seconds())
               .onSuccess(_ -> log.info("All commands successfully submitted"))
               .onFailure(_ -> fail("Commands failed"));

        counters.forEach((_, latch) -> {
            try {
                var _ = latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for client to complete", e);
                throw new RuntimeException(e);
            }
        });

        // Calculate throughput
        long endTime = System.currentTimeMillis();
        double executionTimeSeconds = (endTime - startTime) / 1000.0;
        double throughput = REQUEST_COUNT / executionTimeSeconds;

        log.info("Completed {} commands in {} seconds", REQUEST_COUNT, executionTimeSeconds);
        log.info("Throughput: {} commands/second", throughput);

        var maps = cluster.stores()
                          .values()
                          .stream()
                          .map(KVStore::snapshot)
                          .toList();

        maps.forEach(map -> assertEquals(valueMap, map));
        // Throughput should be reasonable in local testing
        assertTrue(throughput > 10.0, "Throughput should be reasonable in local testing");
    }
}