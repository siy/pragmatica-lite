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
import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Test Suite 5: Cluster Reconfiguration
 */
public class ReconfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(ReconfigurationTest.class);
    private static final int INITIAL_CLUSTER_SIZE = 5;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private TestCluster cluster;

    @BeforeEach
    void setUp() {
        cluster = new TestCluster(INITIAL_CLUSTER_SIZE);
        cluster.awaitStart();
        log.info("Cluster started with {} nodes", INITIAL_CLUSTER_SIZE);
    }

    /**
     * Test 5.1: Add node to active cluster
     * Assertion: New node syncs and participates; quorum size increases
     */
    @Test
    void addNodeToActiveCluster() {
        log.info("Starting adding node to active cluster test");

        // First, establish some baseline state in the cluster
        var clientNode = cluster.getFirst();
        int initialCommands = 100;

        for (int i = 0; i < initialCommands; i++) {
            cluster.submitAndWait(clientNode, new KVCommand.Put<>("initial-key-" + i, "value-" + i));
        }
        
        log.info("Initial state established with {} commands", initialCommands);
        
        // Verify the initial state across all nodes
        await().atMost(TIMEOUT)
               .until(() -> cluster.allNodesHaveValue("initial-key-" + (initialCommands - 1), 
                                                     "value-" + (initialCommands - 1)));
        
        // Capture the current state snapshot before adding a new node
        var beforeSnapshot = cluster.stores().get(clientNode).snapshot();
        int beforeSize = beforeSnapshot.size();
        log.info("Current state size: {} entries", beforeSize);
        
        // Add a new node to the cluster
        var newNode = NodeId.nodeId("node-" + (INITIAL_CLUSTER_SIZE + 1));
        log.info("Adding new node: {}", newNode);
        
        cluster.addNewNode(newNode);
        cluster.awaitNode(newNode);
        
        log.info("New node added and started");
        
        // Submit a command after adding the new node to trigger state sync
        var syncKey = "post-add-sync";
        var syncValue = "sync-value";
        cluster.submitAndWait(clientNode, new KVCommand.Put<>(syncKey, syncValue));
        
        // Wait for sync to complete - the new node should have the complete state
        await().atMost(TIMEOUT.multipliedBy(2))
               .pollInterval(Duration.ofSeconds(1))
               .until(() -> {
                   var newNodeState = cluster.stores().get(newNode).snapshot();
                   
                   if (newNodeState.size() < beforeSize) {
                       log.info("New node has {} entries, waiting for full sync of {} entries", 
                                newNodeState.size(), beforeSize);
                       return false;
                   }
                   
                   // Check sync key and a sample of initial data
                   if (!syncValue.equals(newNodeState.get(syncKey))) {
                       return false;
                   }
                   
                   for (int i = 0; i < initialCommands; i += 10) {
                       var key = "initial-key-" + i;
                       var expectedValue = "value-" + i;

                       if (!expectedValue.equals(newNodeState.get(key))) {
                           return false;
                       }
                   }
                   
                   return true;
               });
        
        log.info("New node successfully synced state");
        
        // Submit additional commands and verify a new node participates
        int additionalCommands = 20;
        for (int i = 0; i < additionalCommands; i++) {
            var key = "post-add-key-" + i;
            var value = "post-value-" + i;
            
            // Submit via the new node to verify it can accept commands
            if (i % 2 == 0) {
                cluster.submitAndWait(newNode, new KVCommand.Put<>(key, value));
            } else {
                cluster.submitAndWait(clientNode, new KVCommand.Put<>(key, value));
            }
            
            // Verify all nodes have this value
            final int index = i;
            await().atMost(TIMEOUT)
                   .until(() -> cluster.allNodesHaveValue("post-add-key-" + index, "post-value-" + index));
        }
        
        log.info("Verified new node participates in consensus");

        // Final verification - check state consistency across all nodes, including the new one
        var allNodes = new ArrayList<>(cluster.ids());
        allNodes.add(newNode);

        var referenceState = cluster.stores().get(clientNode).snapshot();
        
        for (var nodeId : allNodes) {
            var nodeState = cluster.stores().get(nodeId).snapshot();
            assertEquals(referenceState.size(), nodeState.size(), 
                       "Node " + nodeId + " should have same number of entries as reference");
                       
            // Check a sample of keys
            for (int i = 0; i < initialCommands; i += 10) {
                var key = "initial-key-" + i;
                assertEquals(referenceState.get(key), nodeState.get(key),
                           "Node " + nodeId + " should have same value for " + key);
            }
            
            for (int i = 0; i < additionalCommands; i++) {
                var key = "post-add-key-" + i;
                assertEquals(referenceState.get(key), nodeState.get(key),
                           "Node " + nodeId + " should have same value for " + key);
            }
        }
    }

    /**
     * Test 5.2: Remove node from active cluster
     * Assertion: Remaining nodes continue; quorum size decreases
     */
    @Test
    void removeNodeFromActiveCluster() {
        log.info("Starting remove node from active cluster test");
        
        // Establish some initial state
        var clientNode = cluster.getFirst();
        var nodeToRemove = cluster.ids().get(INITIAL_CLUSTER_SIZE - 1); // Last node in the cluster
        
        // Submit initial commands
        int initialCommands = 50;
        for (int i = 0; i < initialCommands; i++) {
            cluster.submitAndWait(clientNode, new KVCommand.Put<>("pre-remove-key-" + i, "value-" + i));
        }
        
        log.info("Initial state established with {} commands", initialCommands);
        
        // Verify the initial state across all nodes
        await().atMost(TIMEOUT)
               .until(() -> cluster.allNodesHaveValue("pre-remove-key-" + (initialCommands - 1), 
                                                     "value-" + (initialCommands - 1)));
        
        // Remove a node
        log.info("Removing node: {}", nodeToRemove);
        cluster.disconnect(nodeToRemove);

        // Submit additional commands after removal
        int postRemoveCommands = 20;
        for (int i = 0; i < postRemoveCommands; i++) {
            var key = "post-remove-key-" + i;
            var value = "post-value-" + i;
            
            cluster.submitAndWait(clientNode, new KVCommand.Put<>(key, value));
            
            // Verify all remaining nodes have this value
            final int index = i;
            await().atMost(TIMEOUT)
                   .until(() -> {
                       for (NodeId nodeId : cluster.ids()) {
                           if (nodeId.equals(nodeToRemove)) continue; // Skip the removed node
                           
                           String val = cluster.stores().get(nodeId).snapshot().get("post-remove-key-" + index);
                           if (!("post-value-" + index).equals(val)) {
                               return false;
                           }
                       }
                       return true;
                   });
        }
        
        log.info("Cluster continues to operate after node removal");
        
        // Verify the removed node doesn't have the new keys
        var removedNodeState = cluster.stores().get(nodeToRemove).snapshot();

        for (int i = 0; i < postRemoveCommands; i++) {
            var key = "post-remove-key-" + i;
            assertNull(removedNodeState.get(key), 
                     "Removed node should not have key: " + key);
        }
        
        // Verify all remaining nodes have identical state
        var referenceNode = cluster.ids().get(0);
        if (referenceNode.equals(nodeToRemove)) {
            referenceNode = cluster.ids().get(1);
        }

        var referenceState = cluster.stores().get(referenceNode).snapshot();
        
        for (var nodeId : cluster.ids()) {
            if (nodeId.equals(nodeToRemove)) {
                continue;
            }

            var nodeState = cluster.stores().get(nodeId).snapshot();
            assertEquals(referenceState.size(), nodeState.size(),
                       "Node " + nodeId + " should have same number of entries as reference");
            
            // Check all keys
            for (var key : referenceState.keySet()) {
                assertEquals(referenceState.get(key), nodeState.get(key),
                           "Node " + nodeId + " should have same value for " + key);
            }
        }
    }

    /**
     * Test 5.3: Replace primary (two nodes removed, two nodes added)
     * Assertion: Cluster state preserved; consensus maintained
     */
    @Test
    void replacePrimaryNodes() {
        log.info("Starting replace primary nodes test: {}", cluster.network());
        
        // For this test, we'll:
        // 1. Establish baseline state
        // 2. Remove two nodes, including the first one (primary)
        // 3. Add two new nodes
        // 4. Verify consensus continues and the state is preserved
        
        // 1. Establish the baseline state
        var originalPrimary = cluster.getFirst();
        var secondToRemove = cluster.ids().get(1);
        
        int initialCommands = 50;
        for (int i = 0; i < initialCommands; i++) {
            cluster.submitAndWait(originalPrimary, new KVCommand.Put<>("baseline-key-" + i, "value-" + i));
        }
        
        log.info("Baseline state established with {} commands", initialCommands);
        
        // Get a reference remaining node that will survive the reconfiguration
        var survivingNode = cluster.ids().get(2); // third node
        
        // 2. Remove two nodes including the primary
        log.info("Removing primary node {} and second node {}", originalPrimary, secondToRemove);
        cluster.disconnect(originalPrimary);
        cluster.disconnect(secondToRemove);

        // 3. Add two new nodes
        var newNode1 = NodeId.nodeId("new-node-1");
        var newNode2 = NodeId.nodeId("new-node-2");
        
        log.info("Adding two new nodes: {} and {}", newNode1, newNode2);
        
        // Add the first new node
        cluster.addNewNode(newNode1);
        cluster.awaitNode(newNode1);
        
        // Add the second new node
        cluster.addNewNode(newNode2);
        cluster.awaitNode(newNode2);
        
        // Trigger state sync by submitting a new command through the surviving node
        log.info("Triggering state sync via remaining node: {}", survivingNode);
        cluster.submitAndWait(survivingNode, new KVCommand.Put<>("sync-key", "sync-value"));
        
        // 4. Verify the new nodes have synced and participate correctly
        await().atMost(TIMEOUT.multipliedBy(2))
               .pollInterval(Duration.ofSeconds(1))
               .until(() -> {
                   // Check sync key is present on new nodes
                   if (!cluster.allNodesHaveValue("sync-key", "sync-value")) {
                       return false;
                   }
                   
                   // Check a sample of baseline keys
                   for (int i = 0; i < initialCommands; i += 10) {
                       if (!cluster.allNodesHaveValue("baseline-key-" + i, "value-" + i)) {
                           return false;
                       }
                   }
                   
                   return true;
               });
        
        log.info("Verified new nodes synced successfully");
        
        // Verify consensus continues with a new configuration by submitting commands
        // through one of the new nodes
        int postReconfigCommands = 20;
        var commandLatch = new CountDownLatch(postReconfigCommands);
        var successCounter = new AtomicInteger(0);
        
        for (int i = 0; i < postReconfigCommands; i++) {
            var key = "post-reconfig-key-" + i;
            var value = "post-value-" + i;
            
            // Alternate between the two new nodes for submissions
            var submitNode = (i % 2 == 0) ? newNode1 : newNode2;
            
            cluster.engines().get(submitNode)
                   .apply(List.of(new KVCommand.Put<>(key, value)))
                   .await(timeSpan(10).seconds())
                   .onSuccess(_ -> {
                       successCounter.incrementAndGet();
                       commandLatch.countDown();
                   })
                   .onFailure(cause -> {
                       log.error("Command failed on new node: {}", cause.message());
                       commandLatch.countDown();
                   });
        }
        
        try {
            boolean completed = commandLatch.await(60, TimeUnit.SECONDS);
            assertTrue(completed, "All post-reconfiguration commands should complete");
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for commands to complete");
        }
        
        assertEquals(postReconfigCommands, successCounter.get(), 
                    "All post-reconfiguration commands should succeed");
        
        // Final verification - check state consistency across all connected nodes
        var connectedNodes = new ArrayList<NodeId>();
        connectedNodes.add(survivingNode);
        connectedNodes.add(cluster.ids().get(3)); // Another original survivor
        connectedNodes.add(cluster.ids().get(4)); // Another original survivor
        connectedNodes.add(newNode1);
        connectedNodes.add(newNode2);

        var referenceState = cluster.stores().get(survivingNode).snapshot();
        
        for (var nodeId : connectedNodes) {
            var nodeState = cluster.stores().get(nodeId).snapshot();
            
            assertEquals(referenceState.size(), nodeState.size(), 
                        "Node " + nodeId + " should have same state size");
            
            // Check baseline keys
            for (int i = 0; i < initialCommands; i++) {
                var key = "baseline-key-" + i;
                assertEquals(referenceState.get(key), nodeState.get(key), 
                           "Node " + nodeId + " should have same value for " + key);
            }
            
            // Check post-reconfiguration keys
            for (int i = 0; i < postReconfigCommands; i++) {
                var key = "post-reconfig-key-" + i;
                assertEquals(referenceState.get(key), nodeState.get(key),
                           "Node " + nodeId + " should have same value for " + key);
            }
        }
        
        log.info("Successfully verified cluster state consistency after primary replacement");
    }

    /**
     * Test 5.4: Majority replacement (f+1 nodes replaced)
     * Assertion: Full state transfer; operation continues
     */
    @Test
    void majorityReplacement() {
        log.info("Starting majority replacement test");
        
        // For a 5-node cluster, f = 2, so we'll replace f+1 = 3 nodes (a majority)
        int nodesToReplace = (INITIAL_CLUSTER_SIZE - 1) / 2 + 1; // f+1
        log.info("Will replace {} out of {} nodes (f+1 where f={})", 
                nodesToReplace, INITIAL_CLUSTER_SIZE, (INITIAL_CLUSTER_SIZE - 1) / 2);
        
        // Establish the baseline state
        var clientNode = cluster.getFirst();
        
        int initialCommands = 50;
        for (int i = 0; i < initialCommands; i++) {
            cluster.submitAndWait(clientNode, new KVCommand.Put<>("majority-key-" + i, "value-" + i));
        }
        
        log.info("Baseline state established with {} commands", initialCommands);
        
        // Select nodes to replace
        var nodesToRemove = new ArrayList<NodeId>();

        for (int i = 0; i < nodesToReplace; i++) {
            nodesToRemove.add(cluster.ids().get(i));
        }
        
        // Get a surviving node ID for later reference
        var survivingNode = cluster.ids().get(nodesToReplace); // First node not being replaced
        
        // Remove nodes one by one, maintaining quorum until the last one
        log.info("Removing nodes: {}", nodesToRemove);
        
        for (int idx = 0; idx < nodesToRemove.size() - 1; idx++) {
            final int i = idx;

            var nodeToRemove = nodesToRemove.get(i);
            cluster.disconnect(nodeToRemove);
            
            // After each disconnect, still submit a command to ensure the cluster is working
            var key = "interim-key-" + i;
            var value = "interim-value-" + i;
            
            cluster.submitAndWait(survivingNode, new KVCommand.Put<>(key, value));
            
            // Verify commands still go through
            await().atMost(TIMEOUT)
                   .until(() -> {
                       for (var nodeId : cluster.ids()) {
                           if (nodesToRemove.subList(0, i + 1).contains(nodeId)) {
                               continue; // Skip removed node
                           }

                           var val = cluster.stores().get(nodeId).snapshot().get("interim-key-" + i);

                           if (!("interim-value-" + i).equals(val)) {
                               return false;
                           }
                       }
                       return true;
                   });
            
            log.info("Cluster still operational after removing {} of {} nodes", i + 1, nodesToRemove.size());
        }
        
        // Now add replacement nodes before removing the last one to prevent losing quorum
        var newNodes = new ArrayList<NodeId>();

        for (int i = 0; i < nodesToReplace; i++) {
            var newNode = NodeId.nodeId("replacement-node-" + (i + 1));
            newNodes.add(newNode);
            
            log.info("Adding replacement node: {}", newNode);
            cluster.addNewNode(newNode);
            cluster.awaitNode(newNode);
            
            // After adding each new node, submit a command to help trigger state sync
            var key = "new-node-key-" + i;
            var value = "new-node-value-" + i;
            
            cluster.submitAndWait(survivingNode, new KVCommand.Put<>(key, value));
            
            // This ensures each new node participates in consensus before adding the next
            final int index = i;
            await().atMost(TIMEOUT)
                   .until(() -> cluster.allNodesHaveValue("new-node-key-" + index, "new-node-value-" + index));
            
            log.info("New node {} synced and participating", newNode);
        }
        
        // Now remove the last original node
        var lastNodeToRemove = nodesToRemove.getLast();
        log.info("Removing final original node: {}", lastNodeToRemove);
        cluster.disconnect(lastNodeToRemove);

        // At this point, we have a completely different majority of nodes
        // Let's verify they can still reach consensus
        
        int finalCommands = 20;
        for (int i = 0; i < finalCommands; i++) {
            var key = "post-majority-key-" + i;
            var value = "post-majority-value-" + i;
            
            // Submit each command through a different new node to verify that all nodes are participating
            var submitNode = newNodes.get(i % newNodes.size());
            
            cluster.submitAndWait(submitNode, new KVCommand.Put<>(key, value));
            
            // Verify all connected nodes have the value
            final int index = i;
            await().atMost(TIMEOUT)
                   .until(() -> {
                       // Get all currently connected nodes
                       var connectedNodes = cluster.network().connectedNodes();
                       
                       for (var nodeId : connectedNodes) {
                           var val = cluster.stores().get(nodeId).snapshot().get("post-majority-key-" + index);
                           if (!("post-majority-value-" + index).equals(val)) {
                               return false;
                           }
                       }
                       return true;
                   });
        }
        
        log.info("Successfully validated consensus with a new majority");

        // Final verification - check complete state consistency across all connected nodes
        var connectedNodes = cluster.network().connectedNodes();
        var referenceState = cluster.stores().get(survivingNode).snapshot();

        for (var nodeId : connectedNodes) {
            if (nodeId.equals(survivingNode)) {
                continue; // Skip reference node
            }

            var nodeState = cluster.stores().get(nodeId).snapshot();

            assertEquals(referenceState.size(), nodeState.size(),
                       "Node " + nodeId + " should have same state size as reference");

            // Check all keys match
            for (var key : referenceState.keySet()) {
                assertEquals(referenceState.get(key), nodeState.get(key),
                           "Node " + nodeId + " should have same value for " + key);
            }
        }
        
        log.info("Successfully verified complete state transfer with majority replacement");
    }
}
