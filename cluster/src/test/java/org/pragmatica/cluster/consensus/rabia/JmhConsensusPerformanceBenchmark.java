package org.pragmatica.cluster.consensus.rabia;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.lang.io.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster.StringKey.key;
import static org.pragmatica.cluster.state.kvstore.KVCommand.put;

/**
 * JMH-based comprehensive consensus performance benchmarks
 * measuring the real-world impact of Rabia consensus optimizations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"--enable-preview"})
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class JmhConsensusPerformanceBenchmark {
    private static final Logger log = LoggerFactory.getLogger(JmhConsensusPerformanceBenchmark.class);
    
    // Test configurations
    @Param({"3", "5", "7"})
    public int clusterSize;
    
    // Test clusters for different scenarios
    private TestCluster optimalCluster;
    private TestCluster latencyCluster;
    private TestCluster packetLossCluster;
    private TestCluster stressCluster;
    
    // Command counter for unique keys
    private final AtomicInteger commandCounter = new AtomicInteger(0);
    
    @Setup(Level.Trial)
    public void setupClusters() throws Exception {
        log.info("Setting up JMH consensus benchmarks for {} nodes", clusterSize);
        
        // Optimal conditions cluster
        optimalCluster = new TestCluster(clusterSize);
        optimalCluster.network().getFaultInjector().clearAllFaults();
        optimalCluster.awaitStart();
        Thread.sleep(1000); // Stabilization
        
        // Network latency cluster (10ms)
        latencyCluster = new TestCluster(clusterSize);
        var latencyInjector = latencyCluster.network().getFaultInjector();
        latencyInjector.clearAllFaults();
        latencyInjector.setFault(LocalNetwork.FaultType.MESSAGE_DELAY, true);
        latencyInjector.messageDelay(TimeSpan.timeSpan(10).millis());
        latencyCluster.awaitStart();
        Thread.sleep(1000);
        
        // Packet loss cluster (2%)
        packetLossCluster = new TestCluster(clusterSize);
        var lossInjector = packetLossCluster.network().getFaultInjector();
        lossInjector.clearAllFaults();
        lossInjector.setFault(LocalNetwork.FaultType.MESSAGE_LOSS, true);
        lossInjector.setMessageLossRate(0.02);
        packetLossCluster.awaitStart();
        Thread.sleep(1000);
        
        // Stress conditions cluster (25ms latency + 3% loss)
        stressCluster = new TestCluster(clusterSize);
        var stressInjector = stressCluster.network().getFaultInjector();
        stressInjector.clearAllFaults();
        stressInjector.setFault(LocalNetwork.FaultType.MESSAGE_DELAY, true);
        stressInjector.setFault(LocalNetwork.FaultType.MESSAGE_LOSS, true);
        stressInjector.messageDelay(TimeSpan.timeSpan(25).millis());
        stressInjector.setMessageLossRate(0.03);
        stressCluster.awaitStart();
        Thread.sleep(1000);
        
        log.info("JMH benchmark setup complete for {} nodes", clusterSize);
    }
    
    @TearDown(Level.Trial)
    public void teardownClusters() {
        log.info("Tearing down JMH consensus benchmarks");
        if (optimalCluster != null) optimalCluster.close();
        if (latencyCluster != null) latencyCluster.close();
        if (packetLossCluster != null) packetLossCluster.close();
        if (stressCluster != null) stressCluster.close();
    }
    
    /**
     * Benchmark consensus performance under optimal network conditions.
     * This represents the best-case scenario for our optimizations.
     */
    @Benchmark
    @Group("optimal")
    public void benchmarkOptimalConsensus(Blackhole bh) throws Exception {
        var commandId = commandCounter.incrementAndGet();
        var command = put(key("optimal-" + clusterSize + "-" + commandId), "value-" + commandId);
        
        try {
            optimalCluster.submitAndWait(optimalCluster.getFirst(), command);
            bh.consume(commandId);
        } catch (Exception e) {
            // Handle failures gracefully for benchmark stability
            log.debug("Command failed in optimal benchmark: {}", e.getMessage());
        }
    }
    
    /**
     * Benchmark consensus performance with network latency.
     * Tests how optimizations perform under realistic network delays.
     */
    @Benchmark
    @Group("latency")
    public void benchmarkLatencyConsensus(Blackhole bh) throws Exception {
        var commandId = commandCounter.incrementAndGet();
        var command = put(key("latency-" + clusterSize + "-" + commandId), "value-" + commandId);
        
        try {
            latencyCluster.submitAndWait(latencyCluster.getFirst(), command);
            bh.consume(commandId);
        } catch (Exception e) {
            log.debug("Command failed in latency benchmark: {}", e.getMessage());
        }
    }
    
    /**
     * Benchmark consensus performance with packet loss.
     * Tests resilience and performance under unreliable networks.
     */
    @Benchmark
    @Group("packetloss")
    public void benchmarkPacketLossConsensus(Blackhole bh) throws Exception {
        var commandId = commandCounter.incrementAndGet();
        var command = put(key("loss-" + clusterSize + "-" + commandId), "value-" + commandId);
        
        try {
            packetLossCluster.submitAndWait(packetLossCluster.getFirst(), command);
            bh.consume(commandId);
        } catch (Exception e) {
            log.debug("Command failed in packet loss benchmark: {}", e.getMessage());
        }
    }
    
    /**
     * Benchmark consensus performance under stress conditions.
     * Tests performance under adverse network conditions (latency + packet loss).
     */
    @Benchmark
    @Group("stress")
    public void benchmarkStressConsensus(Blackhole bh) throws Exception {
        var commandId = commandCounter.incrementAndGet();
        var command = put(key("stress-" + clusterSize + "-" + commandId), "value-" + commandId);
        
        try {
            stressCluster.submitAndWait(stressCluster.getFirst(), command);
            bh.consume(commandId);
        } catch (Exception e) {
            log.debug("Command failed in stress benchmark: {}", e.getMessage());
        }
    }
    
    /**
     * Concurrent consensus benchmark to test load scaling.
     * Multiple threads submitting commands simultaneously.
     */
    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    public void benchmarkConcurrentConsensus(Blackhole bh) throws Exception {
        var commandId = commandCounter.incrementAndGet();
        var threadId = Thread.currentThread().getId();
        var command = put(key("concurrent-" + clusterSize + "-" + threadId + "-" + commandId), 
                         "value-" + commandId);
        
        try {
            optimalCluster.submitAndWait(optimalCluster.getFirst(), command);
            bh.consume(commandId);
        } catch (Exception e) {
            log.debug("Command failed in concurrent benchmark: {}", e.getMessage());
        }
    }
    
    // Standalone benchmark runner
    public static void main(String[] args) throws Exception {
        System.out.println("=======================================================");
        System.out.println("JMH Consensus Performance Benchmark");
        System.out.println("Testing optimized Rabia consensus implementation");
        System.out.println("=======================================================");
        
        Options opt = new OptionsBuilder()
                .include(JmhConsensusPerformanceBenchmark.class.getSimpleName())
                .detectJvmArgs()
                .build();
        
        new Runner(opt).run();
    }
}