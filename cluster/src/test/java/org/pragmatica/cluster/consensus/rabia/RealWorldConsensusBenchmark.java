package org.pragmatica.cluster.consensus.rabia;

import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.pragmatica.lang.io.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster.StringKey.key;
import static org.pragmatica.cluster.state.kvstore.KVCommand.put;

/**
 * Real-world consensus performance benchmark that tests the optimized Rabia
 * implementation under realistic network conditions and deployment scenarios.
 */
public class RealWorldConsensusBenchmark {
    private static final Logger log = LoggerFactory.getLogger(RealWorldConsensusBenchmark.class);
    
    private static final int COMMANDS_PER_SCENARIO = 500;
    private static final int WARMUP_COMMANDS = 50;
    private static final TimeSpan TEST_TIMEOUT = TimeSpan.timeSpan(120).seconds();
    
    @Test
    public void benchmarkDatacenterDeployment() {
        log.info("Real-World Consensus Benchmark: Datacenter Deployment");
        log.info("====================================================");
        
        var scenarios = List.of(
            new DeploymentScenario("3-node datacenter", 3, RealisticNetworkSimulator.NetworkProfile.DATACENTER_LAN),
            new DeploymentScenario("5-node datacenter", 5, RealisticNetworkSimulator.NetworkProfile.DATACENTER_LAN),
            new DeploymentScenario("7-node datacenter", 7, RealisticNetworkSimulator.NetworkProfile.DATACENTER_LAN)
        );
        
        var results = runBenchmarkScenarios(scenarios);
        
        log.info("\nDatacenter Deployment Results Summary:");
        log.info("=====================================");
        for (var result : results) {
            log.info("  {}: {:.1f} ops/sec, {}ms latency, {:.1f}% success", 
                    result.scenario().name(),
                    result.performance().throughput(),
                    result.performance().avgLatency(),
                    result.performance().successRate() * 100);
            
            // Validate datacenter performance expectations
            assertTrue(result.performance().throughput() > 50, 
                    "Datacenter should achieve >50 ops/sec");
            assertTrue(result.performance().avgLatency() < 50, 
                    "Datacenter should achieve <50ms latency");
            assertTrue(result.performance().successRate() > 0.98, 
                    "Datacenter should achieve >98% success rate");
        }
    }
    
    @Test
    public void benchmarkCloudDeployment() {
        log.info("Real-World Consensus Benchmark: Cloud Deployment");
        log.info("===============================================");
        
        var scenarios = List.of(
            new DeploymentScenario("3-node cloud", 3, RealisticNetworkSimulator.NetworkProfile.CLOUD_PROVIDER),
            new DeploymentScenario("5-node cloud", 5, RealisticNetworkSimulator.NetworkProfile.CLOUD_PROVIDER),
            new DeploymentScenario("7-node cloud", 7, RealisticNetworkSimulator.NetworkProfile.CLOUD_PROVIDER)
        );
        
        var results = runBenchmarkScenarios(scenarios);
        
        log.info("\nCloud Deployment Results Summary:");
        log.info("================================");
        for (var result : results) {
            log.info("  {}: {:.1f} ops/sec, {}ms latency, {:.1f}% success", 
                    result.scenario().name(),
                    result.performance().throughput(),
                    result.performance().avgLatency(),
                    result.performance().successRate() * 100);
            
            // Validate cloud performance expectations
            assertTrue(result.performance().throughput() > 20, 
                    "Cloud should achieve >20 ops/sec");
            assertTrue(result.performance().avgLatency() < 100, 
                    "Cloud should achieve <100ms latency");
            assertTrue(result.performance().successRate() > 0.95, 
                    "Cloud should achieve >95% success rate");
        }
    }
    
    @Test
    public void benchmarkGeoDistributedDeployment() {
        log.info("Real-World Consensus Benchmark: Geo-Distributed Deployment");
        log.info("=========================================================");
        
        var scenarios = List.of(
            new DeploymentScenario("3-node geo", 3, RealisticNetworkSimulator.NetworkProfile.REGIONAL_WAN),
            new DeploymentScenario("5-node geo", 5, RealisticNetworkSimulator.NetworkProfile.REGIONAL_WAN),
            new DeploymentScenario("5-node cross-continental", 5, RealisticNetworkSimulator.NetworkProfile.CROSS_CONTINENTAL)
        );
        
        var results = runBenchmarkScenarios(scenarios);
        
        log.info("\nGeo-Distributed Deployment Results Summary:");
        log.info("==========================================");
        for (var result : results) {
            log.info("  {}: {:.1f} ops/sec, {}ms latency, {:.1f}% success", 
                    result.scenario().name(),
                    result.performance().throughput(),
                    result.performance().avgLatency(),
                    result.performance().successRate() * 100);
            
            // Validate geo-distributed performance expectations
            assertTrue(result.performance().throughput() > 5, 
                    "Geo-distributed should achieve >5 ops/sec");
            assertTrue(result.performance().avgLatency() < 500, 
                    "Geo-distributed should achieve <500ms latency");
            assertTrue(result.performance().successRate() > 0.90, 
                    "Geo-distributed should achieve >90% success rate");
        }
    }
    
    @Test
    public void benchmarkAdverseConditions() {
        log.info("Real-World Consensus Benchmark: Adverse Network Conditions");
        log.info("=========================================================");
        
        var scenarios = List.of(
            new DeploymentScenario("5-node degraded", 5, RealisticNetworkSimulator.NetworkProfile.DEGRADED),
            new DeploymentScenario("5-node mobile", 5, RealisticNetworkSimulator.NetworkProfile.MOBILE_WIFI),
            new DeploymentScenario("3-node satellite", 3, RealisticNetworkSimulator.NetworkProfile.SATELLITE)
        );
        
        var results = runBenchmarkScenarios(scenarios);
        
        log.info("\nAdverse Conditions Results Summary:");
        log.info("==================================");
        for (var result : results) {
            log.info("  {}: {:.1f} ops/sec, {}ms latency, {:.1f}% success", 
                    result.scenario().name(),
                    result.performance().throughput(),
                    result.performance().avgLatency(),
                    result.performance().successRate() * 100);
            
            // Validate that consensus still functions under adverse conditions
            assertTrue(result.performance().throughput() > 1, 
                    "Should maintain >1 ops/sec even under adverse conditions");
            assertTrue(result.performance().successRate() > 0.80, 
                    "Should maintain >80% success rate under adverse conditions");
        }
    }
    
    @Test
    public void benchmarkLoadScaling() {
        log.info("Real-World Consensus Benchmark: Load Scaling");
        log.info("===========================================");
        
        var clusterSize = 5;
        var clientCounts = List.of(1, 2, 5, 10, 20);
        
        try (var cluster = new TestCluster(clusterSize)) {
            // Use cloud provider network profile
            var injector = RealisticNetworkSimulator.createRealisticInjector(
                    RealisticNetworkSimulator.NetworkProfile.CLOUD_PROVIDER);
            cluster.network().setFaultInjector(injector);
            
            cluster.awaitStart();
            Thread.sleep(2000); // Stabilization time
            
            log.info("\nLoad Scaling Results:");
            log.info("--------------------");
            
            for (var clientCount : clientCounts) {
                var result = runLoadTest(cluster, clientCount, COMMANDS_PER_SCENARIO / 2);
                
                log.info("  {} clients: {:.1f} ops/sec, {}ms latency, {:.1f}% success",
                        clientCount,
                        result.throughput(),
                        result.avgLatency(),
                        result.successRate() * 100);
                
                // Validate scaling behavior
                if (clientCount <= 5) {
                    assertTrue(result.throughput() > clientCount * 5, 
                            "Should scale reasonably with client count");
                }
                assertTrue(result.successRate() > 0.90, 
                        "Should maintain high success rate under load");
            }
        } catch (Exception e) {
            fail("Load scaling test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void benchmarkNetworkDegradation() {
        log.info("Real-World Consensus Benchmark: Network Degradation");
        log.info("==================================================");
        
        try (var cluster = new TestCluster(5)) {
            var injector = RealisticNetworkSimulator.createRealisticInjector(
                    RealisticNetworkSimulator.NetworkProfile.CLOUD_PROVIDER);
            cluster.network().setFaultInjector(injector);
            
            cluster.awaitStart();
            Thread.sleep(2000);
            
            // Phase 1: Normal operation
            log.info("Phase 1: Normal operation");
            var normalResult = runPerformanceTest(cluster, COMMANDS_PER_SCENARIO / 3);
            log.info("  Normal: {:.1f} ops/sec, {}ms latency", 
                    normalResult.throughput(), normalResult.avgLatency());
            
            // Phase 2: Simulate gradual degradation
            log.info("Phase 2: Gradual network degradation");
            injector.simulateGradualDegradation(
                    RealisticNetworkSimulator.NetworkProfile.DEGRADED, 
                    5000); // 5 second transition
            
            Thread.sleep(6000); // Wait for degradation to complete
            
            var degradedResult = runPerformanceTest(cluster, COMMANDS_PER_SCENARIO / 3);
            log.info("  Degraded: {:.1f} ops/sec, {}ms latency", 
                    degradedResult.throughput(), degradedResult.avgLatency());
            
            // Phase 3: Recovery
            log.info("Phase 3: Network recovery");
            injector.setProfile(RealisticNetworkSimulator.NetworkProfile.CLOUD_PROVIDER);
            Thread.sleep(3000); // Recovery time
            
            var recoveredResult = runPerformanceTest(cluster, COMMANDS_PER_SCENARIO / 3);
            log.info("  Recovered: {:.1f} ops/sec, {}ms latency", 
                    recoveredResult.throughput(), recoveredResult.avgLatency());
            
            // Validate degradation behavior
            assertTrue(degradedResult.throughput() < normalResult.throughput(), 
                    "Performance should degrade under poor network conditions");
            assertTrue(recoveredResult.throughput() > degradedResult.throughput(), 
                    "Performance should recover when network improves");
            assertTrue(degradedResult.successRate() > 0.80, 
                    "Should maintain functionality even during degradation");
            
        } catch (Exception e) {
            fail("Network degradation test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void benchmarkPartitionRecovery() {
        log.info("Real-World Consensus Benchmark: Partition Recovery");
        log.info("=================================================");
        
        try (var cluster = new TestCluster(5)) {
            var injector = RealisticNetworkSimulator.createRealisticInjector(
                    RealisticNetworkSimulator.NetworkProfile.CLOUD_PROVIDER);
            cluster.network().setFaultInjector(injector);
            
            cluster.awaitStart();
            Thread.sleep(2000);
            
            var leader = cluster.getFirst();
            var allNodes = cluster.ids();
            
            // Phase 1: Normal operation before partition
            log.info("Phase 1: Pre-partition operation");
            var prePartitionCommands = 0;
            for (int i = 0; i < 100; i++) {
                try {
                    cluster.submitAndWait(leader, put(key("pre-" + i), "value-" + i));
                    prePartitionCommands++;
                } catch (Exception e) {
                    break;
                }
            }
            log.info("  Commands processed before partition: {}", prePartitionCommands);
            
            // Phase 2: Create network partition
            log.info("Phase 2: Network partition");
            var group1 = allNodes.subList(0, 2);  // Minority
            var group2 = allNodes.subList(2, 5);  // Majority
            
            cluster.network().createPartition(group1, group2);
            Thread.sleep(1000);
            
            // Try to process during partition (should fail for minority, succeed for majority)
            var duringPartitionCommands = 0;
            for (int i = 0; i < 50; i++) {
                try {
                    // Try with a node from the majority group
                    var majorityLeader = group2.get(0);
                    cluster.submitAndWait(majorityLeader, put(key("during-" + i), "value-" + i));
                    duringPartitionCommands++;
                } catch (Exception e) {
                    // Expected failures during partition resolution
                    break;
                }
                Thread.sleep(50);
            }
            log.info("  Commands processed during partition: {}", duringPartitionCommands);
            
            // Phase 3: Heal partition and measure recovery
            log.info("Phase 3: Partition recovery");
            var recoveryStart = Instant.now();
            cluster.network().healPartitions();
            
            // Wait for network to stabilize
            Thread.sleep(3000);
            
            // Resume normal operation
            var postRecoveryCommands = 0;
            for (int i = 0; i < 100; i++) {
                try {
                    cluster.submitAndWait(leader, put(key("post-" + i), "value-" + i));
                    postRecoveryCommands++;
                } catch (Exception e) {
                    // May take time to fully recover
                    Thread.sleep(100);
                }
            }
            
            var recoveryEnd = Instant.now();
            var recoveryTime = Duration.between(recoveryStart, recoveryEnd).toMillis();
            
            log.info("  Commands processed after recovery: {}", postRecoveryCommands);
            log.info("  Recovery time: {} ms", recoveryTime);
            
            // Validate partition tolerance
            assertTrue(prePartitionCommands > 50, "Should process commands before partition");
            assertTrue(postRecoveryCommands > 50, "Should resume processing after recovery");
            assertTrue(recoveryTime < 30000, "Should recover within 30 seconds");
            
        } catch (Exception e) {
            fail("Partition recovery test failed: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private List<BenchmarkResult> runBenchmarkScenarios(List<DeploymentScenario> scenarios) {
        var results = new ArrayList<BenchmarkResult>();
        
        for (var scenario : scenarios) {
            try (var cluster = new TestCluster(scenario.clusterSize())) {
                // Configure network conditions
                var injector = RealisticNetworkSimulator.createRealisticInjector(scenario.networkProfile());
                injector.enableVariableLatency(true);
                injector.enableBurstLoss(scenario.networkProfile() != RealisticNetworkSimulator.NetworkProfile.DATACENTER_LAN);
                cluster.network().setFaultInjector(injector);
                
                // Start cluster and run test
                cluster.awaitStart();
                Thread.sleep(2000); // Stabilization time
                
                // Warmup
                runPerformanceTest(cluster, WARMUP_COMMANDS);
                
                // Actual measurement
                var performance = runPerformanceTest(cluster, COMMANDS_PER_SCENARIO);
                results.add(new BenchmarkResult(scenario, performance));
                
            } catch (Exception e) {
                log.error("Scenario {} failed: {}", scenario.name(), e.getMessage());
                fail("Benchmark scenario failed: " + scenario.name());
            }
        }
        
        return results;
    }
    
    private ConsensusPerformanceResult runPerformanceTest(TestCluster cluster, int commandCount) {
        var leader = cluster.getFirst();
        var startTime = Instant.now();
        var successCount = new AtomicInteger(0);
        var totalLatency = new AtomicLong(0);
        var failures = new AtomicInteger(0);
        
        for (int i = 0; i < commandCount; i++) {
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
            }
            
            // Small delay to avoid overwhelming
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        var endTime = Instant.now();
        var duration = Duration.between(startTime, endTime);
        
        var avgLatency = successCount.get() > 0 ? totalLatency.get() / successCount.get() : 0;
        var throughput = (double) successCount.get() / duration.toMillis() * 1000.0;
        var successRate = (double) successCount.get() / commandCount;
        
        return new ConsensusPerformanceResult(
                throughput,
                avgLatency,
                successRate,
                successCount.get(),
                failures.get(),
                duration.toMillis()
        );
    }
    
    private ConsensusPerformanceResult runLoadTest(TestCluster cluster, int clientCount, int commandsPerClient) {
        var executor = Executors.newFixedThreadPool(clientCount);
        var latch = new CountDownLatch(clientCount);
        var successCount = new AtomicInteger(0);
        var totalLatency = new AtomicLong(0);
        var failures = new AtomicInteger(0);
        
        var startTime = Instant.now();
        
        for (int client = 0; client < clientCount; client++) {
            final int clientId = client;
            executor.submit(() -> {
                try {
                    var leader = cluster.getFirst();
                    
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
                        
                        Thread.sleep(5); // Small delay
                    }
                } catch (Exception e) {
                    log.debug("Client {} failed: {}", clientId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(TEST_TIMEOUT.duration().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
        
        var endTime = Instant.now();
        var duration = Duration.between(startTime, endTime);
        
        var totalCommands = clientCount * commandsPerClient;
        var avgLatency = successCount.get() > 0 ? totalLatency.get() / successCount.get() : 0;
        var throughput = (double) successCount.get() / duration.toMillis() * 1000.0;
        var successRate = (double) successCount.get() / totalCommands;
        
        return new ConsensusPerformanceResult(
                throughput,
                avgLatency,
                successRate,
                successCount.get(),
                failures.get(),
                duration.toMillis()
        );
    }
    
    // Data classes
    
    public record DeploymentScenario(
            String name,
            int clusterSize,
            RealisticNetworkSimulator.NetworkProfile networkProfile
    ) {}
    
    public record ConsensusPerformanceResult(
            double throughput,
            long avgLatency,
            double successRate,
            int successCount,
            int failures,
            long durationMs
    ) {}
    
    public record BenchmarkResult(
            DeploymentScenario scenario,
            ConsensusPerformanceResult performance
    ) {}
}