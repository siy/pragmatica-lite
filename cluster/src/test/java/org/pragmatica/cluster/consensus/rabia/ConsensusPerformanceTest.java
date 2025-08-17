package org.pragmatica.cluster.consensus.rabia;

import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.lang.io.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster.StringKey.key;
import static org.pragmatica.cluster.state.kvstore.KVCommand.put;

/**
 * Comprehensive consensus performance tests that measure how optimizations
 * translate into real consensus performance under various network conditions.
 */
public class ConsensusPerformanceTest {
    private static final Logger log = LoggerFactory.getLogger(ConsensusPerformanceTest.class);

    // Test parameters
    private static final int[] CLUSTER_SIZES = {3, 5, 7};
    private static final int COMMANDS_PER_TEST = 1000;
    private static final TimeSpan TEST_TIMEOUT = TimeSpan.timeSpan(60).seconds();

    @Test
    public void measureConsensusPerformanceOptimal() throws Exception {
        log.info("Consensus Performance Test - Optimal Network Conditions");
        log.info("======================================================");

        for (int clusterSize : CLUSTER_SIZES) {
            var result = runConsensusPerformanceTest(clusterSize, new NetworkConditions.Optimal());
            logPerformanceResult("Optimal", clusterSize, result);

            // Validate performance expectations
            assertTrue(result.throughput() > 100, "Should achieve >100 ops/sec in optimal conditions");
            assertTrue(result.avgLatency() < 100, "Should achieve <100ms latency in optimal conditions");
        }
    }

    @Test
    public void measureConsensusPerformanceWithLatency() throws Exception {
        log.info("Consensus Performance Test - Network Latency");
        log.info("============================================");

        var latencies = List.of(
                TimeSpan.timeSpan(10).millis(),
                TimeSpan.timeSpan(50).millis(),
                TimeSpan.timeSpan(100).millis()
        );

        for (var latency : latencies) {
            for (int clusterSize : CLUSTER_SIZES) {
                var conditions = new NetworkConditions.WithLatency(latency);
                var result = runConsensusPerformanceTest(clusterSize, conditions);
                logPerformanceResult("Latency-" + latency.millis() + "ms", clusterSize, result);

                // Performance should degrade gracefully with latency
                assertTrue(result.throughput() > 10, "Should maintain reasonable throughput with latency");
            }
        }
    }

    @Test
    public void measureConsensusPerformanceWithPacketLoss() throws Exception {
        log.info("Consensus Performance Test - Packet Loss");
        log.info("========================================");

        var lossRates = List.of(0.01, 0.05, 0.10); // 1%, 5%, 10%

        for (var lossRate : lossRates) {
            for (int clusterSize : CLUSTER_SIZES) {
                var conditions = new NetworkConditions.WithPacketLoss(lossRate);
                var result = runConsensusPerformanceTest(clusterSize, conditions);
                logPerformanceResult("PacketLoss-" + (int) (lossRate * 100) + "%", clusterSize, result);

                // Should maintain consensus even with packet loss
                assertTrue(result.successRate() > 0.95, "Should maintain >95% success rate with packet loss");
            }
        }
    }

    @Test
    public void measureConsensusPerformanceUnderStress() throws Exception {
        log.info("Consensus Performance Test - Stress Conditions");
        log.info("==============================================");

        // Combined adverse conditions
        var stressConditions = new NetworkConditions.Stress(
                TimeSpan.timeSpan(25).millis(), // 25ms latency
                0.02 // 2% packet loss
        );

        for (int clusterSize : CLUSTER_SIZES) {
            var result = runConsensusPerformanceTest(clusterSize, stressConditions);
            logPerformanceResult("Stress", clusterSize, result);

            // Should maintain basic functionality under stress
            assertTrue(result.successRate() > 0.90, "Should maintain >90% success rate under stress");
            assertTrue(result.throughput() > 5, "Should maintain >5 ops/sec under stress");
        }
    }

    @Test
    public void measureConsensusPerformanceWithPartitions() throws Exception {
        log.info("Consensus Performance Test - Network Partitions");
        log.info("===============================================");

        var clusterSize = 5; // Need odd number for partition tolerance
        var result = runPartitionPerformanceTest(clusterSize);

        log.info("Partition Recovery Results:");
        log.info("  Commands before partition: {}", result.commandsBeforePartition());
        log.info("  Commands during partition: {}", result.commandsDuringPartition());
        log.info("  Commands after recovery:   {}", result.commandsAfterRecovery());
        log.info("  Recovery time:             {} ms", result.recoveryTime());

        // Validate partition tolerance
        assertTrue(result.commandsBeforePartition() > 0, "Should process commands before partition");
        assertEquals(0, result.commandsDuringPartition(), "Should not process commands during partition");
        assertTrue(result.commandsAfterRecovery() > 0, "Should resume processing after recovery");
        assertTrue(result.recoveryTime() < 10000, "Should recover within 10 seconds");
    }

    @Test
    public void measureConcurrentClientPerformance() throws Exception {
        log.info("Consensus Performance Test - Concurrent Clients");
        log.info("===============================================");

        var clusterSize = 5;
        var clientCounts = List.of(1, 2, 5, 10);

        for (var clientCount : clientCounts) {
            var result = runConcurrentClientTest(clusterSize, clientCount);

            log.info("Concurrent Clients ({}): {} ops/sec, {} ms avg latency",
                     clientCount, result.throughput(), result.avgLatency());

            // Should scale reasonably with concurrent clients
            assertTrue(result.throughput() > 10 * clientCount,
                       "Should scale throughput with concurrent clients");
        }
    }

    // Core test implementation

    private ConsensusPerformanceResult runConsensusPerformanceTest(int clusterSize, NetworkConditions conditions) throws
            Exception {
        try (var cluster = new TestCluster(clusterSize)) {
            // Apply network conditions
            conditions.apply(cluster.network().getFaultInjector());

            // Start cluster
            cluster.awaitStart();

            // Wait for stabilization
            Thread.sleep(1000);

            // Run performance test
            var leader = cluster.getFirst();
            var startTime = Instant.now();
            var successCount = new AtomicInteger(0);
            var totalLatency = new AtomicLong(0);
            var failures = new AtomicInteger(0);

            // Submit commands and measure performance
            for (int i = 0; i < COMMANDS_PER_TEST; i++) {
                var commandStart = System.nanoTime();
                var command = put(key("key-" + i), "value-" + i);

                try {
                    cluster.submitAndWait(leader, command);
                    var commandEnd = System.nanoTime();
                    var latency = (commandEnd - commandStart) / 1_000_000; // Convert to ms

                    successCount.incrementAndGet();
                    totalLatency.addAndGet(latency);
                } catch (Exception e) {
                    failures.incrementAndGet();
                    log.debug("Command {} failed: {}", i, e.getMessage());
                }

                // Small delay to avoid overwhelming the system
                Thread.sleep(1);
            }

            var endTime = Instant.now();
            var duration = Duration.between(startTime, endTime);

            var avgLatency = successCount.get() > 0 ? totalLatency.get() / successCount.get() : 0;
            var throughput = (double) successCount.get() / duration.toMillis() * 1000.0;
            var successRate = (double) successCount.get() / COMMANDS_PER_TEST;

            return new ConsensusPerformanceResult(
                    throughput,
                    avgLatency,
                    successRate,
                    successCount.get(),
                    failures.get(),
                    duration.toMillis()
            );
        }
    }

    private PartitionPerformanceResult runPartitionPerformanceTest(int clusterSize) throws Exception {
        try (var cluster = new TestCluster(clusterSize)) {
            cluster.awaitStart();
            Thread.sleep(1000);

            var leader = cluster.getFirst();
            var commandsBeforePartition = 0;
            var commandsDuringPartition = 0;
            var commandsAfterRecovery = 0;

            // Phase 1: Normal operation
            for (int i = 0; i < 50; i++) {
                try {
                    cluster.submitAndWait(leader, put(key("before-" + i), "value-" + i));
                    commandsBeforePartition++;
                } catch (Exception e) {
                    break;
                }
            }

            // Phase 2: Create partition (split cluster)
            var allNodes = cluster.ids();
            var group1 = allNodes.subList(0, clusterSize / 2);
            var group2 = allNodes.subList(clusterSize / 2, clusterSize);

            cluster.network().createPartition(group1, group2);
            Thread.sleep(1000); // Let partition take effect

            // Try to submit during partition (should fail or timeout)
            for (int i = 0; i < 20; i++) {
                try {
                    var command = put(key("during-" + i), "value-" + i);
                    cluster.engines().get(leader).apply(List.of(command))
                           .await(TimeSpan.timeSpan(2).seconds()); // Short timeout
                    commandsDuringPartition++;
                } catch (Exception e) {
                    // Expected to fail during partition
                    break;
                }
            }

            // Phase 3: Heal partition and measure recovery
            var recoveryStart = Instant.now();
            cluster.network().healPartitions();

            // Wait for recovery and resume processing
            Thread.sleep(2000);

            for (int i = 0; i < 50; i++) {
                try {
                    cluster.submitAndWait(leader, put(key("after-" + i), "value-" + i));
                    commandsAfterRecovery++;
                } catch (Exception e) {
                    // May take some time to fully recover
                    Thread.sleep(100);
                }
            }

            var recoveryEnd = Instant.now();
            var recoveryTime = Duration.between(recoveryStart, recoveryEnd).toMillis();

            return new PartitionPerformanceResult(
                    commandsBeforePartition,
                    commandsDuringPartition,
                    commandsAfterRecovery,
                    recoveryTime
            );
        }
    }

    private ConsensusPerformanceResult runConcurrentClientTest(int clusterSize, int clientCount) throws Exception {
        try (var cluster = new TestCluster(clusterSize)) {
            cluster.awaitStart();
            Thread.sleep(1000);

            var executor = Executors.newFixedThreadPool(clientCount);
            var latch = new CountDownLatch(clientCount);
            var successCount = new AtomicInteger(0);
            var totalLatency = new AtomicLong(0);
            var failures = new AtomicInteger(0);

            var startTime = Instant.now();

            // Submit commands concurrently from multiple clients
            for (int client = 0; client < clientCount; client++) {
                final int clientId = client;
                executor.submit(() -> {
                    try {
                        var leader = cluster.getFirst();
                        var commandsPerClient = COMMANDS_PER_TEST / clientCount;

                        for (int i = 0; i < commandsPerClient; i++) {
                            var commandStart = System.nanoTime();
                            var command = put(key("client-" + clientId + "-" + i), "value-" + i);

                            try {
                                cluster.submitAndWait(leader, command);
                                var commandEnd = System.nanoTime();
                                var latency = (commandEnd - commandStart) / 1_000_000;

                                successCount.incrementAndGet();
                                totalLatency.addAndGet(latency);
                            } catch (Exception e) {
                                failures.incrementAndGet();
                            }

                            Thread.sleep(2); // Small delay
                        }
                    } catch (Exception e) {
                        log.error("Client {} failed", clientId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(TEST_TIMEOUT.duration().toMillis(), TimeUnit.MILLISECONDS);
            executor.shutdown();

            var endTime = Instant.now();
            var duration = Duration.between(startTime, endTime);

            var avgLatency = successCount.get() > 0 ? totalLatency.get() / successCount.get() : 0;
            var throughput = (double) successCount.get() / duration.toMillis() * 1000.0;
            var successRate = (double) successCount.get() / COMMANDS_PER_TEST;

            return new ConsensusPerformanceResult(
                    throughput,
                    avgLatency,
                    successRate,
                    successCount.get(),
                    failures.get(),
                    duration.toMillis()
            );
        }
    }

    private void logPerformanceResult(String condition, int clusterSize, ConsensusPerformanceResult result) {
        log.info("  {}-node cluster, {} conditions:", clusterSize, condition);
        log.info("    Throughput:    {:.1f} ops/sec", result.throughput());
        log.info("    Avg Latency:   {} ms", result.avgLatency());
        log.info("    Success Rate:  {:.1f}%", result.successRate() * 100);
        log.info("    Total Success: {}", result.successCount());
        log.info("    Total Failures: {}", result.failures());
    }

    // Network condition configurations

    public interface NetworkConditions {
        void apply(LocalNetwork.FaultInjector faultInjector);

        class Optimal implements NetworkConditions {
            @Override
            public void apply(LocalNetwork.FaultInjector faultInjector) {
                faultInjector.clearAllFaults();
            }
        }

        class WithLatency implements NetworkConditions {
            private final TimeSpan latency;

            public WithLatency(TimeSpan latency) {
                this.latency = latency;
            }

            @Override
            public void apply(LocalNetwork.FaultInjector faultInjector) {
                faultInjector.clearAllFaults();
                faultInjector.setFault(LocalNetwork.FaultType.MESSAGE_DELAY, true);
                faultInjector.messageDelay(latency);
            }
        }

        class WithPacketLoss implements NetworkConditions {
            private final double lossRate;

            public WithPacketLoss(double lossRate) {
                this.lossRate = lossRate;
            }

            @Override
            public void apply(LocalNetwork.FaultInjector faultInjector) {
                faultInjector.clearAllFaults();
                faultInjector.setFault(LocalNetwork.FaultType.MESSAGE_LOSS, true);
                faultInjector.setMessageLossRate(lossRate);
            }
        }

        class Stress implements NetworkConditions {
            private final TimeSpan latency;
            private final double lossRate;

            public Stress(TimeSpan latency, double lossRate) {
                this.latency = latency;
                this.lossRate = lossRate;
            }

            @Override
            public void apply(LocalNetwork.FaultInjector faultInjector) {
                faultInjector.clearAllFaults();
                faultInjector.setFault(LocalNetwork.FaultType.MESSAGE_DELAY, true);
                faultInjector.setFault(LocalNetwork.FaultType.MESSAGE_LOSS, true);
                faultInjector.messageDelay(latency);
                faultInjector.setMessageLossRate(lossRate);
            }
        }
    }

    // Result data classes

    public record ConsensusPerformanceResult(
            double throughput,
            long avgLatency,
            double successRate,
            int successCount,
            int failures,
            long durationMs
    ) {}

    public record PartitionPerformanceResult(
            int commandsBeforePartition,
            int commandsDuringPartition,
            int commandsAfterRecovery,
            long recoveryTime
    ) {}
}