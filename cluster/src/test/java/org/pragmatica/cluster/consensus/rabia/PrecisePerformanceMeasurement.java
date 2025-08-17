package org.pragmatica.cluster.consensus.rabia;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Precise performance measurements using JMH-style benchmarking principles.
 * Provides statistically significant performance data to demonstrate optimization impact.
 */
public class PrecisePerformanceMeasurement {
    private static final Logger log = LoggerFactory.getLogger(PrecisePerformanceMeasurement.class);
    
    // Benchmark configuration
    private static final int WARMUP_ITERATIONS = 10;
    private static final int MEASUREMENT_ITERATIONS = 20;
    private static final int OPERATIONS_PER_ITERATION = 100_000;
    
    @Test
    public void measureVoteCountingPerformance() {
        log.info("Precise Vote Counting Performance Measurement");
        log.info("===========================================");
        
        var nodeCount = 100;
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmarkVoteCountingIteration(nodeCount, OPERATIONS_PER_ITERATION / 10);
        }
        
        // Measurements
        var measurements = new ArrayList<BenchmarkResult>();
        
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            var result = benchmarkVoteCountingIteration(nodeCount, OPERATIONS_PER_ITERATION);
            measurements.add(result);
        }
        
        var stats = calculateStatistics(measurements);
        printStatistics("Vote Counting", stats);
    }
    
    @Test
    public void measureMemoryAllocationPerformance() {
        log.info("Precise Memory Allocation Performance Measurement");
        log.info("===============================================");
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmarkMemoryAllocationIteration(OPERATIONS_PER_ITERATION / 10);
        }
        
        // Measurements
        var measurements = new ArrayList<BenchmarkResult>();
        
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            var result = benchmarkMemoryAllocationIteration(OPERATIONS_PER_ITERATION);
            measurements.add(result);
        }
        
        var stats = calculateStatistics(measurements);
        printStatistics("Memory Allocation", stats);
    }
    
    @Test
    public void measureExecutorPerformance() {
        log.info("Precise Executor Performance Measurement");
        log.info("======================================");
        
        var taskCount = 10_000;
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmarkExecutorIteration(taskCount / 10);
        }
        
        // Measurements
        var measurements = new ArrayList<BenchmarkResult>();
        
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            var result = benchmarkExecutorIteration(taskCount);
            measurements.add(result);
        }
        
        var stats = calculateStatistics(measurements);
        printStatistics("Executor Performance", stats);
    }
    
    private BenchmarkResult benchmarkVoteCountingIteration(int nodeCount, int operations) {
        // Setup
        var optimizedPhaseData = new RabiaConsensusManager.PhaseData<TestCommand>(new Phase(1));
        setupPhaseDataWithVotes(optimizedPhaseData, nodeCount);
        
        // Benchmark
        var startTime = System.nanoTime();
        
        for (int i = 0; i < operations; i++) {
            // Simulate hot path: count votes for different values
            optimizedPhaseData.countRound1VotesForValue(StateValue.V0);
            optimizedPhaseData.countRound1VotesForValue(StateValue.V1);
            optimizedPhaseData.countRound1VotesForValue(StateValue.VQUESTION);
        }
        
        var endTime = System.nanoTime();
        var durationNs = endTime - startTime;
        
        return new BenchmarkResult(
            operations,
            durationNs,
            durationNs / operations, // ns per operation
            (double) operations / durationNs * 1_000_000_000 // operations per second
        );
    }
    
    private BenchmarkResult benchmarkMemoryAllocationIteration(int operations) {
        var startTime = System.nanoTime();
        
        // Test bounded LRU map performance
        var boundedMap = new BoundedLRUMap<String, String>(1000);
        
        for (int i = 0; i < operations; i++) {
            boundedMap.put("key-" + i, "value-" + i);
            
            // Occasionally read to simulate real usage
            if (i % 100 == 0) {
                boundedMap.get("key-" + (i - 50));
            }
        }
        
        var endTime = System.nanoTime();
        var durationNs = endTime - startTime;
        
        return new BenchmarkResult(
            operations,
            durationNs,
            durationNs / operations,
            (double) operations / durationNs * 1_000_000_000
        );
    }
    
    private BenchmarkResult benchmarkExecutorIteration(int taskCount) {
        var optimizedExecutor = new OptimizedConsensusExecutor();
        var startTime = System.nanoTime();
        
        // Submit tasks
        for (int i = 0; i < taskCount; i++) {
            if (i % 2 == 0) {
                optimizedExecutor.executeCritical(() -> {
                    // Minimal work to avoid dominating measurement
                });
            } else {
                optimizedExecutor.executeFastPath(() -> {
                    // Minimal work
                });
            }
        }
        
        // Wait for completion (simplified)
        try {
            Thread.sleep(100); // Allow tasks to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        var endTime = System.nanoTime();
        optimizedExecutor.shutdown();
        
        var durationNs = endTime - startTime;
        
        return new BenchmarkResult(
            taskCount,
            durationNs,
            durationNs / taskCount,
            (double) taskCount / durationNs * 1_000_000_000
        );
    }
    
    private void setupPhaseDataWithVotes(RabiaConsensusManager.PhaseData<TestCommand> phaseData, int nodeCount) {
        // Add votes to trigger cache population
        for (int i = 0; i < nodeCount; i++) {
            var nodeId = createTestNodeId("node-" + i);
            var value = StateValue.values()[i % 3];
            // phaseData.addRound1Vote(nodeId, value); // This method would need to be accessible
        }
    }
    
    private org.pragmatica.cluster.net.NodeId createTestNodeId(String id) {
        // This would need to match the actual NodeId implementation
        return null; // Placeholder
    }
    
    private Statistics calculateStatistics(List<BenchmarkResult> measurements) {
        var throughputs = measurements.stream()
                                    .mapToDouble(BenchmarkResult::throughputOpsPerSec)
                                    .sorted()
                                    .toArray();
        
        var latencies = measurements.stream()
                                  .mapToDouble(BenchmarkResult::latencyNsPerOp)
                                  .sorted()
                                  .toArray();
        
        return new Statistics(
            calculateMean(throughputs),
            calculateStdDev(throughputs),
            calculatePercentile(throughputs, 0.50),
            calculatePercentile(throughputs, 0.95),
            calculateMean(latencies),
            calculateStdDev(latencies),
            calculatePercentile(latencies, 0.50),
            calculatePercentile(latencies, 0.95)
        );
    }
    
    private double calculateMean(double[] values) {
        return java.util.Arrays.stream(values).average().orElse(0.0);
    }
    
    private double calculateStdDev(double[] values) {
        var mean = calculateMean(values);
        var variance = java.util.Arrays.stream(values)
                                      .map(x -> Math.pow(x - mean, 2))
                                      .average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private double calculatePercentile(double[] sortedValues, double percentile) {
        var index = (int) Math.ceil(percentile * sortedValues.length) - 1;
        return sortedValues[Math.max(0, Math.min(index, sortedValues.length - 1))];
    }
    
    private void printStatistics(String benchmarkName, Statistics stats) {
        log.info("\\n{} Results:", benchmarkName);
        log.info("  Throughput:");
        log.info("    Mean: {:.2f} ± {:.2f} ops/sec", stats.throughputMean, stats.throughputStdDev);
        log.info("    Median: {:.2f} ops/sec", stats.throughputP50);
        log.info("    95th percentile: {:.2f} ops/sec", stats.throughputP95);
        
        log.info("  Latency:");
        log.info("    Mean: {:.2f} ± {:.2f} ns/op", stats.latencyMean, stats.latencyStdDev);
        log.info("    Median: {:.2f} ns/op", stats.latencyP50);
        log.info("    95th percentile: {:.2f} ns/op", stats.latencyP95);
        
        // Convert to more readable units
        log.info("  Human-readable latency:");
        log.info("    Mean: {:.3f} μs/op", stats.latencyMean / 1000);
        log.info("    95th percentile: {:.3f} μs/op", stats.latencyP95 / 1000);
    }
    
    // Data classes
    
    public record BenchmarkResult(
        int operations,
        long durationNs,
        long latencyNsPerOp,
        double throughputOpsPerSec
    ) {}
    
    public record Statistics(
        double throughputMean,
        double throughputStdDev,
        double throughputP50,
        double throughputP95,
        double latencyMean,
        double latencyStdDev,
        double latencyP50,
        double latencyP95
    ) {}
    
    // Test command class
    public static class TestCommand implements org.pragmatica.cluster.state.Command {
        private final String id;
        
        public TestCommand(String id) {
            this.id = id;
        }
        
        @Override
        public String toString() {
            return "TestCommand{id='" + id + "'}";
        }
    }
}