package org.pragmatica.cluster.consensus.rabia;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.ConsensusErrors;
import org.pragmatica.cluster.consensus.rabia.infrastructure.TestCluster;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class RabiaIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(RabiaIntegrationTest.class);

    @Test
    void threeNodeCluster_agreesAndPropagates() {
        var c = new TestCluster(3);

        c.awaitStart();

        c.engines()
         .get(c.getFirst())
         .apply(List.of(new KVCommand.Put<>("k1", "v1")))
         .await(timeSpan(10).seconds())
         .onSuccess(_ -> log.info("Successfully applied command: (k1, v1)"))
         .onFailure(cause -> fail("Failed to apply command: (k1, v1): " + cause));

        // await all three having it
        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> c.allNodesHaveValue("k1", "v1"));

        // submit on node2
        c.engines().get(c.ids().get(1))
         .apply(List.of(new KVCommand.Put<>("k2", "v2")))
         .await(timeSpan(10).seconds())
         .onSuccess(_ -> log.info("Successfully applied command: (k2, v2)"))
         .onFailure(cause -> fail("Failed to apply command: (k2, v2): " + cause));

        Awaitility.await()
                  .atMost(2, TimeUnit.SECONDS)
                  .until(() -> c.allNodesHaveValue("k2", "v2"));
    }

    @Test
    void fiveNodeCluster_withFailures_andSnapshotJoin() {
        var c = new TestCluster(5);

        c.awaitStart();

        c.submitAndWait(c.getFirst(), new KVCommand.Put<>("a", "1"));

        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> c.allNodesHaveValue("a", "1"));

        // fail node1
        c.disconnect(c.getFirst());

        // still quorum on 4 nodes: put b->2
        c.submitAndWait(c.ids().get(1), new KVCommand.Put<>("b", "2"));

        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> c.allNodesHaveValue("b", "2"));

        // fail node2
        c.disconnect(c.ids().get(1));

        // still quorum on 3 nodes: put c->3
        c.submitAndWait(c.ids().get(2), new KVCommand.Put<>("c", "3"));

        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> c.allNodesHaveValue("c", "3"));

        // fail node3 → only 2 left, quorum=3 ⇒ no new entries
        c.disconnect(c.ids().get(2));
        var beforeSize = c.stores().get(c.ids().get(3)).snapshot().size();

        c.engines()
         .get(c.ids().get(3))
         .apply(List.of(new KVCommand.Put<>("d", "4")))
         .await(timeSpan(2).seconds())
         .onSuccess(_ -> fail("Should not be successful"))
         .onFailure(cause -> assertEquals(ConsensusErrors.nodeInactive(c.ids().get(3)), cause));

        Awaitility.await()
                  .during(Duration.ofSeconds(1))
                  .atMost(10, TimeUnit.SECONDS)
                  .untilAsserted(() -> assertEquals(beforeSize, c.stores().get(c.ids().get(3)).snapshot().size()));

        // bring up node-6 as a replacement
        var node6 = NodeId.nodeId("node-6");
        c.addNewNode(node6);
        c.awaitNode(node6);

        // node-6 should eventually have all values: a,b,c
        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> {
                      var mem = c.stores().get(node6).snapshot();
                      return "1".equals(mem.get("a"))
                              && "2".equals(mem.get("b"))
                              && "3".equals(mem.get("c"));
                  });

        // now nodes 4,5,6 form a quorum of 3: put e->5
        c.submitAndWait(node6, new KVCommand.Put<>("e", "5"));

        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .until(() -> c.allNodesHaveValue("e", "5"));
    }
}
