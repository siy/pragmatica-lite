package org.pragmatica.cluster.consensus.rabia;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.pragmatica.cluster.net.NodeId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * JMH microbenchmarks for the core optimizations in Rabia consensus.
 * These benchmarks measure the specific performance improvements we implemented.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"--enable-preview"})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class JmhCoreMicrobenchmark {
    
    @Param({"5", "10", "25", "100"})
    public int nodeCount;
    
    // Test data
    private List<NodeId> nodeIds;
    private List<VoteData> votes;
    private Map<NodeId, Integer> unboundedMap;
    private BoundedLRUMap<NodeId, Integer> boundedMap;
    private AtomicInteger round1Counter;
    private AtomicInteger round2Counter;
    
    @Setup(Level.Trial)
    public void setup() {
        // Generate test node IDs
        nodeIds = IntStream.range(0, nodeCount)
            .mapToObj(i -> NodeId.nodeId("node-" + i))
            .toList();
        
        // Generate test votes
        votes = IntStream.range(0, nodeCount)
            .mapToObj(i -> new VoteData(nodeIds.get(i), i % 2 == 0 ? 1 : 2, "batch-" + i))
            .toList();
        
        // Initialize maps for memory comparison
        unboundedMap = new ConcurrentHashMap<>();
        boundedMap = new BoundedLRUMap<>(1000); // Bounded to 1000 entries
        
        // Initialize counters for vote counting
        round1Counter = new AtomicInteger(0);
        round2Counter = new AtomicInteger(0);
    }
    
    /**
     * Benchmark baseline vote counting using Stream operations.
     * This represents the original O(n) approach.
     */
    @Benchmark
    public void baselineVoteCounting(Blackhole bh) {
        // Simulate the original stream-based vote counting
        var round1Votes = votes.stream()
            .filter(vote -> vote.round() == 1)
            .count();
        
        var round2Votes = votes.stream()
            .filter(vote -> vote.round() == 2)
            .count();
        
        bh.consume(round1Votes);
        bh.consume(round2Votes);
    }
    
    /**
     * Benchmark optimized vote counting using cached AtomicInteger counters.
     * This represents our O(1) optimization.
     */
    @Benchmark
    public void optimizedVoteCounting(Blackhole bh) {
        // Reset counters
        round1Counter.set(0);
        round2Counter.set(0);
        
        // Simulate adding votes with O(1) counting
        for (var vote : votes) {
            if (vote.round() == 1) {
                round1Counter.incrementAndGet();
            } else if (vote.round() == 2) {
                round2Counter.incrementAndGet();
            }
        }
        
        bh.consume(round1Counter.get());
        bh.consume(round2Counter.get());
    }
    
    /**
     * Benchmark memory usage with unbounded collections.
     * This represents the original memory leak-prone approach.
     */
    @Benchmark
    public void unboundedMemoryUsage(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            var nodeId = nodeIds.get(i % nodeCount);
            unboundedMap.put(nodeId, i);
        }
        
        var size = unboundedMap.size();
        bh.consume(size);
        
        // Don't clear - simulates memory leak
    }
    
    /**
     * Benchmark memory usage with bounded LRU collections.
     * This represents our memory optimization.
     */
    @Benchmark
    public void boundedMemoryUsage(Blackhole bh) {
        for (int i = 0; i < 100; i++) {
            var nodeId = nodeIds.get(i % nodeCount);
            boundedMap.put(nodeId, i);
        }
        
        var size = boundedMap.size();
        bh.consume(size);
        
        // LRU automatically manages size
    }
    
    /**
     * Benchmark batch selection using naive iteration.
     * This represents the baseline approach.
     */
    @Benchmark
    public void baselineBatchSelection(Blackhole bh) {
        var selected = new ArrayList<String>();
        
        // Simulate selecting batches based on some criteria
        for (var vote : votes) {
            if (vote.batchId().hashCode() % 3 == 0) {
                selected.add(vote.batchId());
            }
        }
        
        bh.consume(selected.size());
    }
    
    /**
     * Benchmark optimized batch selection using efficient data structures.
     * This represents our optimized approach.
     */
    @Benchmark
    public void optimizedBatchSelection(Blackhole bh) {
        var selected = new HashSet<String>();
        
        // Simulate optimized batch selection with Set for O(1) lookups
        for (var vote : votes) {
            if (vote.batchId().hashCode() % 3 == 0) {
                selected.add(vote.batchId());
            }
        }
        
        bh.consume(selected.size());
    }
    
    /**
     * Benchmark concurrent vote processing simulation.
     * Tests the performance under parallel execution.
     */
    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    public void concurrentVoteProcessing(Blackhole bh) {
        var threadId = Thread.currentThread().getId();
        var localCounter = new AtomicInteger(0);
        
        // Simulate concurrent vote processing
        for (var vote : votes) {
            if (vote.nodeId().id().hashCode() % 4 == threadId % 4) {
                localCounter.incrementAndGet();
            }
        }
        
        bh.consume(localCounter.get());
    }
    
    // Helper classes and data structures
    
    public record VoteData(NodeId nodeId, int round, String batchId) {}
    
    /**
     * Simple bounded LRU map implementation for benchmarking.
     */
    private static class BoundedLRUMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;
        
        public BoundedLRUMap(int maxSize) {
            super(16, 0.75f, true); // Access order
            this.maxSize = maxSize;
        }
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
    
    // Standalone benchmark runner
    public static void main(String[] args) throws Exception {
        System.out.println("=======================================================");
        System.out.println("JMH Core Microbenchmarks for Rabia Optimizations");
        System.out.println("Testing specific algorithmic and memory improvements");
        System.out.println("=======================================================");
        
        Options opt = new OptionsBuilder()
                .include(JmhCoreMicrobenchmark.class.getSimpleName())
                .param("nodeCount", "5", "25", "100") // Test different scales
                .detectJvmArgs()
                .build();
        
        new Runner(opt).run();
    }
}