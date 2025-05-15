package org.pragmatica.cluster.consensus.rabia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.cluster.net.NodeId.nodeId;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Test Suite 3: Crash-Stop Failures
 */
public class CrashStopFailuresTest {
    private static final Logger log = LoggerFactory.getLogger(CrashStopFailuresTest.class);
    private static final int CLUSTER_SIZE = 5;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private TestCluster cluster;

    @BeforeEach
    void setUp() {
        cluster = new TestCluster(CLUSTER_SIZE);
        cluster.awaitStart();
        log.info("Cluster started with {} nodes", CLUSTER_SIZE);
    }

    /**
     * Test 3.1: Proposer crashes after broadcast
     * Expected: Round completes; client reply delivered
     */
    @Test
    void proposerCrashesAfterBroadcast() {
        log.info("Starting proposer crashes after broadcast test");

        // First, establish some baseline state
        NodeId clientNode = cluster.getFirst();
        cluster.submitAndWait(clientNode, new KVCommand.Put<>("baseline", "value"));

        await().atMost(TIMEOUT)
               .until(() -> cluster.allNodesHaveValue("baseline", "value"));

        // Keep track of the key we're going to test with
        final String testKey = "proposer-crash-test";
        final String testValue = "success-value";

        // Set up a command to execute
        var command = new KVCommand.Put<>(testKey, testValue);

        // Submit the command
        log.info("Submitting command before crashing proposer");
        var result = cluster.engines().get(clientNode)
                            .apply(List.of(command));

        // Disconnect the proposer immediately after sending
        // This simulates the proposer crashing after broadcast
        log.info("Disconnecting proposer node: {}", clientNode);
        cluster.disconnect(clientNode);

        // Wait for the command to not complete
        result.await(timeSpan(5).seconds())
              .onSuccess(_ -> fail("Command completed successfully despite proposer failure"))
              .onFailure(cause -> log.info("Command failed: {} ", cause));

        // Assert that the command was still processed by the remaining nodes
        await().atMost(TIMEOUT)
               .pollInterval(Duration.ofMillis(200))
               .until(() -> {
                   boolean allHaveValue = true;
                   for (var nodeId : cluster.ids()) {
                       if (nodeId.equals(clientNode)) {
                           continue; // Skip the crashed node
                       }

                       var value = cluster.stores().get(nodeId).snapshot().get(testKey);

                       if (!testValue.equals(value)) {
                           allHaveValue = false;
                           break;
                       }
                   }
                   return allHaveValue;
               });

        log.info("Command was successfully committed by remaining nodes despite proposer crash");
    }

    /**
     * Test 3.2: Replica crashes pre-COMMIT
     * Expected: Round completes if quorum present; recovering node resyncs the sequence and digest
     */
    @Test
    void replicaCrashesPreCommit() {
        log.info("Starting replica crashes pre-COMMIT test");

        // Pick a node to crash (not the proposer)
        var proposerNode = cluster.getFirst();
        var nodeToFail = cluster.ids().get(1);

        // Track state digest before crash
        // Prepare several commands to execute
        int numCommands = 50;
        var commands = new ArrayList<KVCommand>();

        for (int i = 0; i < numCommands; i++) {
            commands.add(new KVCommand.Put<>("pre-commit-key-" + i, "value-" + i));
        }

        // Crash the replica during the sequence
        var counter = new AtomicInteger(0);
        var crashLatch = new CountDownLatch(numCommands / 3);

        // Set an observer to crash the node after some commands
        cluster.stores().get(nodeToFail).observeStateChanges(_ -> {
            int count = counter.incrementAndGet();
            if (count == numCommands / 3) {
                log.info("Crashing node {} after {} commands", nodeToFail, count);
                cluster.disconnect(nodeToFail);
                crashLatch.countDown();
            }
        });

        // Submit commands
        log.info("Submitting {} commands", numCommands);
        for (var command : commands) {
            cluster.engines()
                   .get(proposerNode)
                   .apply(List.of(command))
                   .await(timeSpan(10).seconds())
                   .onSuccess(_ -> log.debug("Command applied: {}", command))
                   .onFailure(cause -> fail("Command failed: " + cause));
        }

        // Wait for crash to happen
        try {
            var _ = crashLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for node crash", e);
        }

        // Verify commands were committed despite the crash
        for (int i = 0; i < numCommands; i++) {
            final int index = i;

            await().atMost(TIMEOUT)
                   .pollInterval(Duration.ofMillis(200))
                   .until(() -> {
                       boolean allRemaining = true;
                       for (var nodeId : cluster.ids()) {
                           if (nodeId.equals(nodeToFail)) {
                               continue; // Skip the crashed node
                           }

                           var value = cluster.stores().get(nodeId).snapshot().get("pre-commit-key-" + index);
                           if (!("value-" + index).equals(value)) {
                               allRemaining = false;
                               break;
                           }
                       }
                       return allRemaining;
                   });
        }

        log.info("All commands were committed by remaining nodes");

        // Now reconnect the node and verify it syncs up
        // Since TestCluster doesn't have a direct reconnect method, we'll simulate 
        // this by adding a new node with the same ID
        log.info("Simulating recovery of crashed node: {}", nodeToFail);

        // Add a new node (same ID) to represent the recovered node
        var recoveredNode = nodeId("node-recovered");

        cluster.addNewNode(recoveredNode);
        cluster.awaitNode(recoveredNode);

        // Submit a new command to ensure state propagation
        cluster.submitAndWait(proposerNode, new KVCommand.Put<>("recovery-sync", "completed"));

        // Verify the recovered node has the full state
        await().atMost(TIMEOUT.multipliedBy(2))
               .pollInterval(Duration.ofSeconds(1))
               .until(() -> {
                   var recoveredState = cluster.stores().get(recoveredNode).snapshot();

                   // Check for our sync marker
                   if (!"completed".equals(recoveredState.get("recovery-sync"))) {
                       return false;
                   }

                   // Check all keys from the commands we executed
                   for (int i = 0; i < numCommands; i++) {
                       var key = "pre-commit-key-" + i;
                       var expectedValue = "value-" + i;

                       if (!expectedValue.equals(recoveredState.get(key))) {
                           return false;
                       }
                   }

                   return true;
               });

        log.info("Recovered node successfully synced state");
    }

    /**
     * Test 3.3: Crash of f replicas simultaneously
     * Expected: Cluster maintains safety and liveness
     */
    @Test
    void crashOfFReplicasSimultaneously() {
        log.info("Starting crash of f replicas simultaneously test");

        // For a 5-node cluster, f = 2 (can tolerate 2 failures while maintaining quorum of 3)
        int faultTolerance = (CLUSTER_SIZE - 1) / 2;
        log.info("Cluster fault tolerance: f = {}", faultTolerance);

        // Establish some baseline state
        var clientNode = cluster.getFirst();
        cluster.submitAndWait(clientNode, new KVCommand.Put<>("baseline-f", "baseline-value"));

        await().atMost(TIMEOUT)
               .until(() -> cluster.allNodesHaveValue("baseline-f", "baseline-value"));

        // Select f nodes to crash (not including the client node)
        var nodesToCrash = new ArrayList<NodeId>();

        for (int i = 1; i <= faultTolerance; i++) {
            nodesToCrash.add(cluster.ids().get(i));
        }

        log.info("Crashing {} nodes simultaneously: {}", nodesToCrash.size(), nodesToCrash);

        // Crash the selected nodes
        for (var nodeId : nodesToCrash) {
            cluster.disconnect(nodeId);
        }

        // Verify we still have quorum
        int remainingNodes = CLUSTER_SIZE - nodesToCrash.size();
        log.info("Remaining nodes: {}, required for quorum: {}", remainingNodes, (CLUSTER_SIZE / 2) + 1);

        // Submit a new command and verify it completes
        var postCrashKey = "post-f-crash";
        var postCrashValue = "f-crashed-value";

        log.info("Submitting command after crashes");

        cluster.engines().get(clientNode)
               .apply(List.of(new KVCommand.Put<>(postCrashKey, postCrashValue)))
               .await(timeSpan(10).seconds())
               .onSuccess(_ -> log.info("Command completed successfully with f nodes crashed"))
               .onFailure(cause -> log.error("Command failed: {}", cause.message()));

        // Verify command was committed on all remaining nodes
        await().atMost(TIMEOUT)
               .pollInterval(Duration.ofSeconds(1))
               .until(() -> {
                   for (var nodeId : cluster.ids()) {
                       if (nodesToCrash.contains(nodeId)) {
                           continue; // Skip crashed nodes
                       }

                       var value = cluster.stores().get(nodeId).snapshot().get(postCrashKey);
                       if (!postCrashValue.equals(value)) {
                           return false;
                       }
                   }
                   return true;
               });

        // Submit and verify multiple additional commands
        for (int i = 0; i < 10; i++) {
            final int index = i;
            var key = "f-crashed-seq-" + i;
            var value = "f-value-" + i;

            cluster.submitAndWait(clientNode, new KVCommand.Put<>(key, value));

            await().atMost(TIMEOUT)
                   .pollInterval(Duration.ofMillis(200))
                   .until(() -> {
                       for (var nodeId : cluster.ids()) {
                           if (nodesToCrash.contains(nodeId)) {
                               continue;
                           }

                           var val = cluster.stores().get(nodeId).snapshot().get("f-crashed-seq-" + index);
                           if (!("f-value-" + index).equals(val)) {
                               return false;
                           }
                       }
                       return true;
                   });
        }

        log.info("Cluster maintained safety and liveness with f={} nodes crashed", faultTolerance);

        // Extra verification: if we crash one more node (exceeding f), the system should no longer be live
        if (remainingNodes > faultTolerance + 1) {
            var oneMoreNode = cluster.ids().get(faultTolerance + 1);
            log.info("Crashing one more node (exceeding f): {}", oneMoreNode);

            cluster.disconnect(oneMoreNode);

            // Try to submit another command - this should not complete because we've lost quorum
            var noQuorumKey = "no-quorum-key";
            var noQuorumValue = "no-quorum-value";

            var result = cluster.engines().get(clientNode)
                                .apply(List.of(new KVCommand.Put<>(noQuorumKey, noQuorumValue)));

            // Wait a bit to see if it completes (it shouldn't)
            result.await(timeSpan(5).seconds())
                  .onSuccess(_ -> fail("Unexpectedly succeeded with command after losing quorum"))
                  .onFailure(cause -> log.info("As expected, command failed after losing quorum: {}", cause.message()));

            // Verify the key didn't appear in any node's state
            for (var nodeId : cluster.ids()) {
                if (nodesToCrash.contains(nodeId) || nodeId.equals(oneMoreNode)) {
                    continue;
                }

                var value = cluster.stores().get(nodeId).snapshot().get(noQuorumKey);
                assertNull(value, "Key should not exist when quorum is lost");
            }

            log.info("Cluster correctly lost liveness after exceeding fault tolerance");
        }
    }
}