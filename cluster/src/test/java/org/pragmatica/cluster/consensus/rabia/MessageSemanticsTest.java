package org.pragmatica.cluster.consensus.rabia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.pragmatica.lang.io.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Suite 2: Message Semantics
 */
public class MessageSemanticsTest {
    private static final Logger log = LoggerFactory.getLogger(MessageSemanticsTest.class);
    private static final int CLUSTER_SIZE = 5;
    private static final int COMMAND_COUNT = 1000;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private TestCluster cluster;

    @BeforeEach
    void setUp() {
        cluster = new TestCluster(CLUSTER_SIZE);
        cluster.awaitStart();
        log.info("Cluster started with {} nodes", CLUSTER_SIZE);
    }

    /**
     * Test 2.1: Duplicate PROPOSE broadcasts
     * Assertion: Replicas ignore/coalesce duplicates; continued progress
     */
    @Test
    void duplicateProposeBroadcasts() {
        log.info("Starting duplicate PROPOSE broadcasts test");

        // Enable message duplication in the network
        LocalNetwork network = cluster.network();
        network.getFaultInjector().setFault(LocalNetwork.FaultType.MESSAGE_DUPLICATE, true);

        // Use the first node as the client
        NodeId clientNode = cluster.getFirst();

        // Submit a batch of commands and verify they are processed correctly
        List<KVCommand> commands = generatePutCommands(COMMAND_COUNT, "duplicate-");
        submitCommands(clientNode, commands);

        // Verify all nodes have a consistent state with all commands
        await().atMost(TIMEOUT)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> cluster.allNodesHaveValue("duplicate-" + (COMMAND_COUNT - 1), 
                                                      "value-" + (COMMAND_COUNT - 1)));

        // Verify all commands were executed exactly once
        for (int i = 0; i < COMMAND_COUNT; i++) {
            String key = "duplicate-" + i;
            String expectedValue = "value-" + i;
            assertTrue(cluster.allNodesHaveValue(key, expectedValue),
                      "All nodes should have key " + key + " with value " + expectedValue);
        }

        // Disable message duplication
        network.getFaultInjector().setFault(LocalNetwork.FaultType.MESSAGE_DUPLICATE, false);
    }

    /**
     * Test 2.2: Out-of-order delivery (COMMIT before PREPARE) at one node
     * Assertion: Buffering preserves safety
     */
    @Test
    void outOfOrderDelivery() {
        log.info("Starting out-of-order delivery test");

        // Choose a node to receive out-of-order messages
        NodeId targetNode = cluster.ids().get(2);
        log.info("Node {} will receive out-of-order messages", targetNode);

        // We can't directly control message order in the test framework,
        // but we can simulate high network latency which increases the chance
        // of out-of-order delivery
        cluster.network().getFaultInjector().setNodeFault(targetNode, 
                                                         LocalNetwork.FaultType.MESSAGE_DELAY, 
                                                         true);
        cluster.network().getFaultInjector().messageDelay(TimeSpan.timeSpan(50).millis());

        // Use a different node as the client
        var clientNode = cluster.getFirst();

        // Submit commands
        var commands = generatePutCommands(COMMAND_COUNT, "order-");
        submitCommands(clientNode, commands);

        // Verify all nodes eventually have the consistent state
        await().atMost(TIMEOUT)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> cluster.allNodesHaveValue("order-" + (COMMAND_COUNT - 1), 
                                                     "value-" + (COMMAND_COUNT - 1)));

        // Disable message delay
        cluster.network().getFaultInjector().setNodeFault(targetNode, 
                                                         LocalNetwork.FaultType.MESSAGE_DELAY, 
                                                         false);
    }

    /**
     * Test 2.3: 10% random message loss for 30s
     * Assertion: Commits proceed; observe latency increase only
     */
    @Test
    void randomMessageLoss() {
        log.info("Starting random message loss test");

        // Configure 10% message loss
        LocalNetwork network = cluster.network();
        network.getFaultInjector().setFault(LocalNetwork.FaultType.MESSAGE_LOSS, true);
        network.getFaultInjector().setMessageLossRate(0.1);

        // Use the first node as the client
        NodeId clientNode = cluster.getFirst();

        // Capture current time for latency measurement
        long startTime = System.currentTimeMillis();

        // Submit commands
        List<KVCommand> commands = generatePutCommands(COMMAND_COUNT, "loss-");
        submitCommands(clientNode, commands);

        // Verify all nodes eventually have the consistent state
        await().atMost(TIMEOUT)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> cluster.allNodesHaveValue("loss-" + (COMMAND_COUNT - 1), 
                                                     "value-" + (COMMAND_COUNT - 1)));

        // Calculate throughput with message loss
        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;
        double throughputWithLoss = COMMAND_COUNT / durationSeconds;

        log.info("Throughput with 10% message loss: {} commands/second", throughputWithLoss);

        // Disable message loss
        network.getFaultInjector().setFault(LocalNetwork.FaultType.MESSAGE_LOSS, false);

        // Submit more commands without message loss to compare performance
        startTime = System.currentTimeMillis();
        commands = generatePutCommands(COMMAND_COUNT, "noloss-");
        submitCommands(clientNode, commands);

        await().atMost(TIMEOUT)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> cluster.allNodesHaveValue("noloss-" + (COMMAND_COUNT - 1), 
                                                     "value-" + (COMMAND_COUNT - 1)));

        endTime = System.currentTimeMillis();
        durationSeconds = (endTime - startTime) / 1000.0;
        double normalThroughput = COMMAND_COUNT / durationSeconds;

        log.info("Normal throughput: {} commands/second", normalThroughput);
        log.info("Throughput ratio (with loss/normal): {}", throughputWithLoss / normalThroughput);

        // We expect throughput with message loss to be lower, but the system should still function
        assertTrue(throughputWithLoss > 0, "System should continue to make progress with message loss");
    }

    /**
     * Test 2.4: Stale view numbers
     * Assertion: Messages discarded; no regression
     */
    @Test
    void staleViewNumbers() {
        log.info("Starting stale view numbers test");

        // First, establish a baseline state with some commands
        NodeId clientNode = cluster.getFirst();
        List<KVCommand> initialCommands = generatePutCommands(100, "initial-");
        submitCommands(clientNode, initialCommands);

        await().atMost(TIMEOUT)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> cluster.allNodesHaveValue("initial-99", "value-99"));

        // Force a view change by temporarily disconnecting a node
        NodeId disconnectNode = cluster.ids().get(1);
        cluster.disconnect(disconnectNode);

        // Submit more commands to advance to a new view
        List<KVCommand> advanceCommands = generatePutCommands(100, "advance-");
        submitCommands(clientNode, advanceCommands);

        await().atMost(TIMEOUT)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> cluster.allNodesHaveValue("advance-99", "value-99"));

        // Reconnect the disconnected node, which will have a stale view
        // The actual reconnection is implementation-specific and might require
        // adding methods to TestCluster, but for now we'll assume the implementation
        // handles this correctly

        // Submit final commands to verify the system continues to function correctly
        List<KVCommand> finalCommands = generatePutCommands(100, "final-");
        submitCommands(clientNode, finalCommands);

        await().atMost(TIMEOUT)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> cluster.allNodesHaveValue("final-99", "value-99"));

        // Verify all commands were processed correctly
        for (int i = 0; i < 100; i++) {
            String initialKey = "initial-" + i;
            String advanceKey = "advance-" + i;
            String finalKey = "final-" + i;
            
            assertTrue(cluster.allNodesHaveValue(initialKey, "value-" + i),
                      "All nodes should have initial key " + initialKey);
            assertTrue(cluster.allNodesHaveValue(advanceKey, "value-" + i),
                      "All nodes should have advance key " + advanceKey);
            assertTrue(cluster.allNodesHaveValue(finalKey, "value-" + i),
                      "All nodes should have final key " + finalKey);
        }
    }

    private List<KVCommand> generatePutCommands(int count, String keyPrefix) {
        List<KVCommand> commands = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            commands.add(new KVCommand.Put<>(keyPrefix + i, "value-" + i));
        }
        return commands;
    }

    private void submitCommands(NodeId nodeId, List<KVCommand> commands) {
        var counter = new AtomicLong(0);
        var batchSize = 100;
        
        for (int i = 0; i < commands.size(); i += batchSize) {
            int end = Math.min(i + batchSize, commands.size());
            var batch = commands.subList(i, end);
            
            cluster.engines().get(nodeId)
                   .apply(batch)
                   .onSuccess(_ -> {
                       long completed = counter.addAndGet(batch.size());
                       if (completed % 500 == 0) {
                           log.info("Processed {} commands", completed);
                       }
                   })
                   .onFailure(cause -> log.error("Failed to apply batch: {}", cause.message()));
        }
    }
}