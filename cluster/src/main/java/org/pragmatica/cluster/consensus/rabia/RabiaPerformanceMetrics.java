package org.pragmatica.cluster.consensus.rabia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * Performance metrics collection for Rabia consensus engine.
 * Tracks key performance indicators to identify bottlenecks and optimize performance.
 */
public class RabiaPerformanceMetrics {
    private static final Logger log = LoggerFactory.getLogger(RabiaPerformanceMetrics.class);
    
    // Throughput metrics
    private final AtomicLong totalCommandsProcessed = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final AtomicLong totalPhasesCompleted = new AtomicLong(0);
    
    // Latency metrics
    private final LongAccumulator maxConsensusLatency = new LongAccumulator(Long::max, 0);
    private final LongAccumulator minConsensusLatency = new LongAccumulator(Long::min, Long.MAX_VALUE);
    private final AtomicLong totalConsensusLatency = new AtomicLong(0);
    private final AtomicLong consensusLatencyCount = new AtomicLong(0);
    
    // Memory metrics
    private final AtomicLong maxPendingBatches = new AtomicLong(0);
    private final AtomicLong maxActivePhases = new AtomicLong(0);
    
    // Error metrics
    private final AtomicLong consensusTimeouts = new AtomicLong(0);
    private final AtomicLong syncRequestCount = new AtomicLong(0);
    private final AtomicLong failedOperations = new AtomicLong(0);
    
    // Performance tracking for individual operations
    private final ConcurrentHashMap<CorrelationId, Instant> batchStartTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Phase, Instant> phaseStartTimes = new ConcurrentHashMap<>();
    
    /**
     * Record when a batch is submitted for consensus
     */
    public void recordBatchSubmitted(CorrelationId batchId) {
        batchStartTimes.put(batchId, Instant.now());
    }
    
    /**
     * Record when a batch completes consensus
     */
    public void recordBatchCompleted(CorrelationId batchId, int commandCount) {
        var startTime = batchStartTimes.remove(batchId);
        if (startTime != null) {
            var latency = Duration.between(startTime, Instant.now()).toMillis();
            updateConsensusLatency(latency);
        }
        
        totalCommandsProcessed.addAndGet(commandCount);
        totalBatchesProcessed.incrementAndGet();
    }
    
    /**
     * Record when a consensus phase starts
     */
    public void recordPhaseStarted(Phase phase) {
        phaseStartTimes.put(phase, Instant.now());
    }
    
    /**
     * Record when a consensus phase completes
     */
    public void recordPhaseCompleted(Phase phase) {
        phaseStartTimes.remove(phase);
        totalPhasesCompleted.incrementAndGet();
    }
    
    /**
     * Update memory usage metrics
     */
    public void updateMemoryUsage(int pendingBatches, int activePhases) {
        maxPendingBatches.updateAndGet(current -> Math.max(current, pendingBatches));
        maxActivePhases.updateAndGet(current -> Math.max(current, activePhases));
    }
    
    /**
     * Record consensus timeout
     */
    public void recordConsensusTimeout() {
        consensusTimeouts.incrementAndGet();
    }
    
    /**
     * Record sync request
     */
    public void recordSyncRequest() {
        syncRequestCount.incrementAndGet();
    }
    
    /**
     * Record failed operation
     */
    public void recordFailedOperation() {
        failedOperations.incrementAndGet();
    }
    
    private void updateConsensusLatency(long latencyMs) {
        maxConsensusLatency.accumulate(latencyMs);
        minConsensusLatency.accumulate(latencyMs);
        totalConsensusLatency.addAndGet(latencyMs);
        consensusLatencyCount.incrementAndGet();
    }
    
    /**
     * Get current performance summary
     */
    public PerformanceSummary getSummary() {
        var count = consensusLatencyCount.get();
        var avgLatency = count > 0 ? totalConsensusLatency.get() / count : 0;
        
        return new PerformanceSummary(
            totalCommandsProcessed.get(),
            totalBatchesProcessed.get(),
            totalPhasesCompleted.get(),
            avgLatency,
            maxConsensusLatency.get(),
            minConsensusLatency.get() == Long.MAX_VALUE ? 0 : minConsensusLatency.get(),
            maxPendingBatches.get(),
            maxActivePhases.get(),
            consensusTimeouts.get(),
            syncRequestCount.get(),
            failedOperations.get(),
            batchStartTimes.size(),
            phaseStartTimes.size()
        );
    }
    
    /**
     * Log performance summary periodically
     */
    public void logPerformanceSummary() {
        var summary = getSummary();
        log.info("Rabia Performance Summary: " +
                "Commands: {}, Batches: {}, Phases: {}, " +
                "Avg Latency: {}ms, Max Latency: {}ms, " +
                "Pending Batches: {}, Active Phases: {}, " +
                "Timeouts: {}, Sync Requests: {}, Failures: {}",
                summary.totalCommandsProcessed(),
                summary.totalBatchesProcessed(),
                summary.totalPhasesCompleted(),
                summary.avgConsensusLatency(),
                summary.maxConsensusLatency(),
                summary.currentPendingBatches(),
                summary.currentActivePhases(),
                summary.consensusTimeouts(),
                summary.syncRequestCount(),
                summary.failedOperations());
    }
    
    /**
     * Reset all metrics (useful for testing)
     */
    public void reset() {
        totalCommandsProcessed.set(0);
        totalBatchesProcessed.set(0);
        totalPhasesCompleted.set(0);
        maxConsensusLatency.reset();
        minConsensusLatency.reset();
        totalConsensusLatency.set(0);
        consensusLatencyCount.set(0);
        maxPendingBatches.set(0);
        maxActivePhases.set(0);
        consensusTimeouts.set(0);
        syncRequestCount.set(0);
        failedOperations.set(0);
        batchStartTimes.clear();
        phaseStartTimes.clear();
    }
    
    public record PerformanceSummary(
        long totalCommandsProcessed,
        long totalBatchesProcessed,
        long totalPhasesCompleted,
        long avgConsensusLatency,
        long maxConsensusLatency,
        long minConsensusLatency,
        long maxPendingBatches,
        long maxActivePhases,
        long consensusTimeouts,
        long syncRequestCount,
        long failedOperations,
        int currentPendingBatches,
        int currentActivePhases
    ) {}
}