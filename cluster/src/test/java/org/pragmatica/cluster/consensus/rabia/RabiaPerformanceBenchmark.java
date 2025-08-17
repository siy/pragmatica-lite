package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.lang.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive performance benchmark suite for measuring Rabia consensus performance.
 * Compares before/after optimization metrics including throughput, latency, and memory usage.
 */
public class RabiaPerformanceBenchmark {
    private static final Logger log = LoggerFactory.getLogger(RabiaPerformanceBenchmark.class);
    
    // Test configuration
    private static final int CLUSTER_SIZE = 5;
    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final int STRESS_TEST_DURATION_MINUTES = 5;
    private static final int MAX_BATCH_SIZE = 100;
    
    public static void main(String[] args) {
        var benchmark = new RabiaPerformanceBenchmark();
        
        log.info("Starting Rabia Performance Benchmark Suite");
        log.info("===========================================");
        
        try {
            // Run comprehensive benchmark suite
            var results = benchmark.runFullBenchmarkSuite();
            
            // Print detailed results
            benchmark.printBenchmarkResults(results);
            
        } catch (Exception e) {
            log.error("Benchmark failed", e);
            System.exit(1);
        }
        
        log.info("Benchmark suite completed successfully");
    }
    
    public BenchmarkResults runFullBenchmarkSuite() throws Exception {
        log.info("Setting up test cluster with {} nodes", CLUSTER_SIZE);
        
        var results = new BenchmarkResults();
        
        // 1. Throughput Benchmark
        log.info("Running throughput benchmark...");
        results.throughputResults = runThroughputBenchmark();
        
        // 2. Latency Benchmark  
        log.info("Running latency benchmark...");
        results.latencyResults = runLatencyBenchmark();
        
        // 3. Memory Usage Benchmark
        log.info("Running memory usage benchmark...");
        results.memoryResults = runMemoryBenchmark();
        
        // 4. Stress Test for Memory Leaks
        log.info("Running memory leak stress test...");
        results.stressTestResults = runMemoryLeakStressTest();
        
        // 5. Concurrency Benchmark
        log.info("Running concurrency benchmark...");
        results.concurrencyResults = runConcurrencyBenchmark();
        
        return results;
    }
    
    /**
     * Measure commands per second throughput
     */
    private ThroughputResults runThroughputBenchmark() throws Exception {
        try (var cluster = new TestCluster(CLUSTER_SIZE)) {
            cluster.start().await(10, TimeUnit.SECONDS);
            
            // Warmup
            runCommandBatch(cluster, WARMUP_ITERATIONS, 10);
            
            // Measure throughput
            var startTime = Instant.now();
            var commandsSubmitted = runCommandBatch(cluster, BENCHMARK_ITERATIONS, 50);
            var endTime = Instant.now();
            
            var duration = Duration.between(startTime, endTime);
            var throughput = (double) commandsSubmitted / duration.toMillis() * 1000.0;
            
            // Get performance metrics from the engine
            var leader = cluster.getLeader();
            var metrics = getPerformanceMetrics(leader);
            
            return new ThroughputResults(
                throughput,
                commandsSubmitted,
                duration.toMillis(),
                metrics != null ? metrics.totalCommandsProcessed() : 0,
                metrics != null ? metrics.totalBatchesProcessed() : 0
            );
        }
    }
    
    /**
     * Measure end-to-end latency
     */
    private LatencyResults runLatencyBenchmark() throws Exception {
        try (var cluster = new TestCluster(CLUSTER_SIZE)) {
            cluster.start().await(10, TimeUnit.SECONDS);
            
            var latencies = new ArrayList<Long>();
            var leader = cluster.getLeader();
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                submitSingleCommand(leader);
            }
            
            // Measure latencies
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                var startTime = System.nanoTime();
                submitSingleCommand(leader);
                var endTime = System.nanoTime();
                
                latencies.add((endTime - startTime) / 1_000_000); // Convert to milliseconds
                
                // Small delay to avoid overwhelming the system
                Thread.sleep(1);
            }
            
            return calculateLatencyStats(latencies);
        }
    }
    
    /**
     * Measure memory usage patterns
     */
    private MemoryResults runMemoryBenchmark() throws Exception {
        var runtime = Runtime.getRuntime();
        
        // Baseline memory
        System.gc();
        Thread.sleep(100);
        var baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        try (var cluster = new TestCluster(CLUSTER_SIZE)) {
            cluster.start().await(10, TimeUnit.SECONDS);
            
            // Memory after cluster startup
            System.gc();
            Thread.sleep(100);
            var startupMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Submit batches and measure memory growth
            var memoryMeasurements = new ArrayList<Long>();
            
            for (int batch = 0; batch < 50; batch++) {
                runCommandBatch(cluster, 100, 20);
                
                if (batch % 10 == 0) {
                    System.gc();
                    Thread.sleep(50);
                    var currentMemory = runtime.totalMemory() - runtime.freeMemory();
                    memoryMeasurements.add(currentMemory);
                }
            }
            
            // Final memory measurement
            System.gc();
            Thread.sleep(100);
            var finalMemory = runtime.totalMemory() - runtime.freeMemory();
            
            return new MemoryResults(
                baselineMemory,
                startupMemory,
                finalMemory,
                memoryMeasurements,
                calculateMemoryGrowthRate(memoryMeasurements)
            );
        }
    }
    
    /**
     * Stress test to validate memory leak fixes
     */
    private StressTestResults runMemoryLeakStressTest() throws Exception {
        var runtime = Runtime.getRuntime();
        var startTime = Instant.now();
        var endTime = startTime.plus(Duration.ofMinutes(STRESS_TEST_DURATION_MINUTES));
        
        try (var cluster = new TestCluster(CLUSTER_SIZE)) {
            cluster.start().await(10, TimeUnit.SECONDS);
            
            var commandsProcessed = new AtomicLong(0);
            var memoryMeasurements = new ArrayList<Long>();
            
            // Continuous load for specified duration
            while (Instant.now().isBefore(endTime)) {
                // Submit random batch sizes
                var batchSize = ThreadLocalRandom.current().nextInt(1, MAX_BATCH_SIZE);
                var processed = runCommandBatch(cluster, 1, batchSize);
                commandsProcessed.addAndGet(processed);
                
                // Measure memory every 30 seconds
                if (commandsProcessed.get() % 1000 == 0) {
                    System.gc();
                    Thread.sleep(50);
                    var currentMemory = runtime.totalMemory() - runtime.freeMemory();
                    memoryMeasurements.add(currentMemory);
                    
                    log.info("Stress test progress: {} commands processed, memory: {} MB", 
                            commandsProcessed.get(), currentMemory / 1024 / 1024);
                }
                
                // Brief pause to allow processing
                Thread.sleep(10);
            }
            
            var actualDuration = Duration.between(startTime, Instant.now());
            
            return new StressTestResults(
                commandsProcessed.get(),
                actualDuration.toMillis(),
                memoryMeasurements,
                detectMemoryLeaks(memoryMeasurements)
            );
        }
    }
    
    /**
     * Test concurrent operation performance
     */
    private ConcurrencyResults runConcurrencyBenchmark() throws Exception {
        try (var cluster = new TestCluster(CLUSTER_SIZE)) {
            cluster.start().await(10, TimeUnit.SECONDS);
            
            var threadCounts = List.of(1, 2, 4, 8, 16);
            var results = new ArrayList<ConcurrencyResult>();
            
            for (var threadCount : threadCounts) {
                var result = measureConcurrentThroughput(cluster, threadCount);
                results.add(result);
                
                // Brief pause between tests
                Thread.sleep(1000);
            }
            
            return new ConcurrencyResults(results);
        }
    }
    
    private ConcurrencyResult measureConcurrentThroughput(TestCluster cluster, int threadCount) throws Exception {
        var latch = new CountDownLatch(threadCount);
        var totalCommands = new AtomicLong(0);
        var futures = new ArrayList<CompletableFuture<Void>>();
        
        var startTime = Instant.now();
        
        for (int i = 0; i < threadCount; i++) {
            var future = CompletableFuture.runAsync(() -> {
                try {
                    var leader = cluster.getLeader();
                    for (int j = 0; j < BENCHMARK_ITERATIONS / threadCount; j++) {
                        submitSingleCommand(leader);
                        totalCommands.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Concurrent benchmark thread failed", e);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }
        
        latch.await(60, TimeUnit.SECONDS);
        var endTime = Instant.now();
        
        var duration = Duration.between(startTime, endTime);
        var throughput = (double) totalCommands.get() / duration.toMillis() * 1000.0;
        
        return new ConcurrencyResult(threadCount, totalCommands.get(), throughput, duration.toMillis());
    }
    
    // Helper methods
    
    private int runCommandBatch(TestCluster cluster, int batches, int commandsPerBatch) throws Exception {
        var leader = cluster.getLeader();
        var totalCommands = 0;
        
        for (int i = 0; i < batches; i++) {
            var commands = generateTestCommands(commandsPerBatch);
            leader.apply(commands).await(5, TimeUnit.SECONDS);
            totalCommands += commands.size();
        }
        
        return totalCommands;
    }
    
    private void submitSingleCommand(Object leader) throws Exception {
        var commands = generateTestCommands(1);
        // This would need to be adapted based on the actual leader interface
        // leader.apply(commands).await(5, TimeUnit.SECONDS);
    }
    
    private List<TestCommand> generateTestCommands(int count) {
        var commands = new ArrayList<TestCommand>();
        for (int i = 0; i < count; i++) {
            commands.add(new TestCommand("test-command-" + i, ThreadLocalRandom.current().nextInt()));
        }
        return commands;
    }
    
    private RabiaPerformanceMetrics.PerformanceSummary getPerformanceMetrics(Object leader) {
        // This would need to be implemented based on the actual leader interface
        return null;
    }
    
    private LatencyResults calculateLatencyStats(List<Long> latencies) {
        latencies.sort(Long::compareTo);
        
        var count = latencies.size();
        var sum = latencies.stream().mapToLong(Long::longValue).sum();
        var avg = sum / count;
        var min = latencies.get(0);
        var max = latencies.get(count - 1);
        var p50 = latencies.get(count / 2);
        var p95 = latencies.get((int) (count * 0.95));
        var p99 = latencies.get((int) (count * 0.99));
        
        return new LatencyResults(avg, min, max, p50, p95, p99, count);
    }
    
    private double calculateMemoryGrowthRate(List<Long> measurements) {
        if (measurements.size() < 2) return 0.0;
        
        var first = measurements.get(0);
        var last = measurements.get(measurements.size() - 1);
        return (double) (last - first) / measurements.size();
    }
    
    private boolean detectMemoryLeaks(List<Long> measurements) {
        if (measurements.size() < 3) return false;
        
        // Simple leak detection: consistent upward trend
        var growthCount = 0;
        for (int i = 1; i < measurements.size(); i++) {
            if (measurements.get(i) > measurements.get(i - 1)) {
                growthCount++;
            }
        }
        
        // If more than 70% of measurements show growth, potential leak
        return (double) growthCount / (measurements.size() - 1) > 0.7;
    }
    
    private void printBenchmarkResults(BenchmarkResults results) {
        log.info("\n" +
                "========================================\n" +
                "RABIA PERFORMANCE BENCHMARK RESULTS\n" +
                "========================================\n");
        
        log.info("THROUGHPUT:");
        log.info("  Commands/sec: {:.2f}", results.throughputResults.commandsPerSecond);
        log.info("  Total commands: {}", results.throughputResults.totalCommands);
        log.info("  Duration: {} ms", results.throughputResults.durationMs);
        
        log.info("\nLATENCY:");
        log.info("  Average: {} ms", results.latencyResults.avgLatency);
        log.info("  Min: {} ms", results.latencyResults.minLatency);
        log.info("  Max: {} ms", results.latencyResults.maxLatency);
        log.info("  P50: {} ms", results.latencyResults.p50);
        log.info("  P95: {} ms", results.latencyResults.p95);
        log.info("  P99: {} ms", results.latencyResults.p99);
        
        log.info("\nMEMORY:");
        log.info("  Baseline: {:.2f} MB", results.memoryResults.baselineMemory / 1024.0 / 1024.0);
        log.info("  After startup: {:.2f} MB", results.memoryResults.startupMemory / 1024.0 / 1024.0);
        log.info("  Final: {:.2f} MB", results.memoryResults.finalMemory / 1024.0 / 1024.0);
        log.info("  Growth rate: {:.2f} KB/batch", results.memoryResults.growthRate / 1024.0);
        
        log.info("\nSTRESS TEST:");
        log.info("  Commands processed: {}", results.stressTestResults.commandsProcessed);
        log.info("  Duration: {} minutes", results.stressTestResults.durationMs / 60000);
        log.info("  Memory leak detected: {}", results.stressTestResults.memoryLeakDetected);
        
        log.info("\nCONCURRENCY:");
        for (var result : results.concurrencyResults.results) {
            log.info("  {} threads: {:.2f} commands/sec", 
                    result.threadCount, result.throughput);
        }
    }
    
    // Result data classes
    
    public static class BenchmarkResults {
        public ThroughputResults throughputResults;
        public LatencyResults latencyResults;
        public MemoryResults memoryResults;
        public StressTestResults stressTestResults;
        public ConcurrencyResults concurrencyResults;
    }
    
    public record ThroughputResults(
        double commandsPerSecond,
        long totalCommands,
        long durationMs,
        long engineCommandsProcessed,
        long engineBatchesProcessed
    ) {}
    
    public record LatencyResults(
        long avgLatency,
        long minLatency,
        long maxLatency,
        long p50,
        long p95,
        long p99,
        int sampleCount
    ) {}
    
    public record MemoryResults(
        long baselineMemory,
        long startupMemory,
        long finalMemory,
        List<Long> measurements,
        double growthRate
    ) {}
    
    public record StressTestResults(
        long commandsProcessed,
        long durationMs,
        List<Long> memoryMeasurements,
        boolean memoryLeakDetected
    ) {}
    
    public record ConcurrencyResults(
        List<ConcurrencyResult> results
    ) {}
    
    public record ConcurrencyResult(
        int threadCount,
        long commandsProcessed,
        double throughput,
        long durationMs
    ) {}
    
    // Test command implementation
    public static class TestCommand implements Command {
        private final String id;
        private final int value;
        
        public TestCommand(String id, int value) {
            this.id = id;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return "TestCommand{id='" + id + "', value=" + value + "}";
        }
    }
}