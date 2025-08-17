package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.net.NodeId;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Micro-benchmark to demonstrate the performance improvement from optimizing vote counting.
 * Compares the old stream-based approach with the new cached counter approach.
 */
public class VoteCountingPerformanceTest {
    private static final Logger log = LoggerFactory.getLogger(VoteCountingPerformanceTest.class);
    
    private static final int NODE_COUNTS[] = {5, 10, 25, 50, 100, 500};
    private static final int ITERATIONS = 100_000;
    
    @Test
    public void compareVoteCountingPerformance() {
        log.info("Vote Counting Performance Comparison");
        log.info("===================================");
        
        for (int nodeCount : NODE_COUNTS) {
            log.info("\nTesting with {} nodes:", nodeCount);
            
            var oldTime = benchmarkOldVoteCounting(nodeCount);
            var newTime = benchmarkNewVoteCounting(nodeCount);
            
            var improvement = (double) (oldTime - newTime) / oldTime * 100;
            
            log.info("  Old approach: {} ms", oldTime);
            log.info("  New approach: {} ms", newTime);
            log.info("  Improvement: {:.1f}% faster", improvement);
            log.info("  Speedup: {:.2f}x", (double) oldTime / newTime);
        }
    }
    
    /**
     * Benchmark the old stream-based vote counting approach
     */
    private long benchmarkOldVoteCounting(int nodeCount) {
        var votes = generateRandomVotes(nodeCount);
        
        var start = Instant.now();
        
        for (int i = 0; i < ITERATIONS; i++) {
            // Simulate the old approach: count votes using streams
            var v0Count = votes.values().stream()
                              .mapToInt(value -> value == StateValue.V0 ? 1 : 0)
                              .sum();
            var v1Count = votes.values().stream()
                              .mapToInt(value -> value == StateValue.V1 ? 1 : 0)
                              .sum();
            var vQuestionCount = votes.values().stream()
                                     .mapToInt(value -> value == StateValue.VQUESTION ? 1 : 0)
                                     .sum();
            
            // Prevent optimization
            if (v0Count + v1Count + vQuestionCount != nodeCount) {
                throw new IllegalStateException("Vote counting error");
            }
        }
        
        return Duration.between(start, Instant.now()).toMillis();
    }
    
    /**
     * Benchmark the new cached counter approach
     */
    private long benchmarkNewVoteCounting(int nodeCount) {
        var mockPhaseData = new MockOptimizedPhaseData(nodeCount);
        
        var start = Instant.now();
        
        for (int i = 0; i < ITERATIONS; i++) {
            // Simulate the new approach: cached counters
            var v0Count = mockPhaseData.countVotesForValue(StateValue.V0);
            var v1Count = mockPhaseData.countVotesForValue(StateValue.V1);
            var vQuestionCount = mockPhaseData.countVotesForValue(StateValue.VQUESTION);
            
            // Prevent optimization
            if (v0Count + v1Count + vQuestionCount != nodeCount) {
                throw new IllegalStateException("Vote counting error");
            }
        }
        
        return Duration.between(start, Instant.now()).toMillis();
    }
    
    private Map<NodeId, StateValue> generateRandomVotes(int nodeCount) {
        var votes = new HashMap<NodeId, StateValue>();
        var values = StateValue.values();
        
        for (int i = 0; i < nodeCount; i++) {
            var nodeId = NodeId.nodeId("node-" + i);
            var value = values[ThreadLocalRandom.current().nextInt(values.length)];
            votes.put(nodeId, value);
        }
        
        return votes;
    }
    
    /**
     * Mock implementation of optimized phase data for benchmarking
     */
    private static class MockOptimizedPhaseData {
        private final Map<StateValue, Integer> voteCounts = new HashMap<>();
        
        public MockOptimizedPhaseData(int nodeCount) {
            // Initialize with random vote distribution
            var v0Count = nodeCount / 3;
            var v1Count = nodeCount / 3;
            var vQuestionCount = nodeCount - v0Count - v1Count;
            
            voteCounts.put(StateValue.V0, v0Count);
            voteCounts.put(StateValue.V1, v1Count);
            voteCounts.put(StateValue.VQUESTION, vQuestionCount);
        }
        
        public int countVotesForValue(StateValue value) {
            return voteCounts.getOrDefault(value, 0);
        }
    }
}