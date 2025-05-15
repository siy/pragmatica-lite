package org.pragmatica.node;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.ProtocolConfig;
import org.pragmatica.cluster.net.NodeInfo;
import org.pragmatica.cluster.node.rabia.CustomClasses;
import org.pragmatica.cluster.topology.ip.TopologyConfig;
import org.pragmatica.cluster.node.ClusterNode;
import org.pragmatica.cluster.node.rabia.RabiaNode;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.pragmatica.cluster.state.kvstore.KVStore;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.net.NodeAddress.nodeAddress;
import static org.pragmatica.cluster.net.NodeId.nodeId;
import static org.pragmatica.cluster.net.NodeInfo.nodeInfo;
import static org.pragmatica.cluster.node.rabia.NodeConfig.nodeConfig;
import static org.pragmatica.cluster.node.rabia.RabiaNode.rabiaNode;
import static org.pragmatica.net.serialization.binary.fury.FuryDeserializer.furyDeserializer;
import static org.pragmatica.net.serialization.binary.fury.FurySerializer.furySerializer;

class RabiaNodeNettyIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(RabiaNodeNettyIntegrationTest.class);

    private static final int CLUSTER_SIZE = 5;
    private static final int BASE_PORT = 3040;
    private static final List<NodeInfo> NODES = List.of(
            nodeInfo(nodeId("node-1"), nodeAddress("localhost", BASE_PORT)),
            nodeInfo(nodeId("node-2"), nodeAddress("localhost", BASE_PORT + 1)),
            nodeInfo(nodeId("node-3"), nodeAddress("localhost", BASE_PORT + 2)),
            nodeInfo(nodeId("node-4"), nodeAddress("localhost", BASE_PORT + 3)),
            nodeInfo(nodeId("node-5"), nodeAddress("localhost", BASE_PORT + 4)));
    private static final TimeSpan RECONCILE_INTERVAL = TimeSpan.timeSpan(5).seconds();
    private static final TimeSpan PING_INTERVAL = TimeSpan.timeSpan(100).seconds();
    private static final TimeSpan AWAIT_TIMEOUT = TimeSpan.timeSpan(10).seconds();
    private static final Duration AWAIT_DURATION = Duration.ofSeconds(10);

    private final List<RabiaNode<KVCommand>> nodes = new ArrayList<>();
    private final List<KVStore<String, String>> stores = new ArrayList<>();
    private final List<MessageRouter> routers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        var protocolConfig = ProtocolConfig.testConfig();
        var serializer = furySerializer(CustomClasses::configure);
        var deserializer = furyDeserializer(CustomClasses::configure);
        var configuredNodes = NODES.subList(0, CLUSTER_SIZE);

        for (int i = 0; i < CLUSTER_SIZE; i++) {
            var topologyConfig = new TopologyConfig(NODES.get(i).id(),
                                                    RECONCILE_INTERVAL,
                                                    PING_INTERVAL,
                                                    configuredNodes);
            var router = MessageRouter.messageRouter();

            routers.add(router);
            var store = new KVStore<String, String>(serializer, deserializer);

            stores.add(store);

            var node = rabiaNode(nodeConfig(protocolConfig, topologyConfig),
                                 router, store, serializer, deserializer);
            nodes.add(node);
        }
        // Start all nodes
        var promises = nodes.stream()
                            .map(ClusterNode::start)
                            .toList();

        Promise.allOf(promises)
               .await(AWAIT_TIMEOUT)
               .onFailureRun(() -> fail("Failed to start all nodes within timeout"))
               .onSuccess(_ -> log.info("All nodes started successfully"));
    }

    @AfterEach
    void tearDown() {
        nodes.forEach(node -> node.stop().await(AWAIT_TIMEOUT));
    }

//    @Disabled("Serializer crash")
    @Test
    void happyPath_allNodesAgreeOnPutGetRemove() {
        // Put values via each node
        var list = new ArrayList<Promise<List<Object>>>();

        for (int i = 0; i < CLUSTER_SIZE; i++) {
            String key = "key-" + i;
            String value = "value-" + i;

            list.add(nodes.get(i)
                          .apply(List.of(new KVCommand.Put<>(key, value))));
        }

        Promise.allOf(list)
               .await(AWAIT_TIMEOUT)
               .onFailure(v -> fail("Failed to put values to state machine within timeout: " + v))
               .onSuccess(_ -> log.info("All nodes have been put successfully"));

        for (int i = 0; i < CLUSTER_SIZE; i++) {
            var store = stores.get(i);

            log.info("Store {}, content {}", i, store.snapshot());
        }

        // Await all nodes have all keys
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            var key = "key-" + i;
            var value = "value-" + i;

            await().atMost(AWAIT_DURATION)
                   .until(() -> stores.stream()
                                      .allMatch(
                                              store -> value.equals(store.snapshot().get(key))));
        }

        // Remove a key via one node
        nodes.getFirst()
             .apply(List.of(new KVCommand.Remove<>("key-0")))
             .await(AWAIT_TIMEOUT);

        // Await all nodes have removed the key
        await().atMost(AWAIT_DURATION)
               .until(() -> stores.stream().noneMatch(store -> store.snapshot().containsKey("key-0")));
    }

    @Disabled("May have issues with restarting node on the same port")
    @Test
    void nodeCrashAndRecovery_catchesUpWithCluster() {
        // Put initial value
        nodes.getFirst()
             .apply(List.of(new KVCommand.Put<>("crash-key", "v0"))).await(AWAIT_TIMEOUT);

        await().atMost(AWAIT_DURATION)
               .until(() -> stores.stream().allMatch(store -> "v0".equals(store.snapshot().get("crash-key"))));

        // Stop node 1 (index 1)
        nodes.get(1)
             .stop()
             .await(AWAIT_TIMEOUT);

        // Put new values while node 1 is down
        for (int i = 1; i <= 3; i++) {
            var key = "crash-key-" + i;
            var value = "v" + i;

            nodes.getFirst()
                 .apply(List.of(new KVCommand.Put<>(key, value))).await(AWAIT_TIMEOUT);
        }
        // Await all live nodes have the new values
        for (int i = 1; i <= 3; i++) {
            String key = "crash-key-" + i;
            String value = "v" + i;

            await().atMost(AWAIT_DURATION)
                   .until(() -> stores.stream()
                                      .filter(s -> !nodes.get(stores.indexOf(s))
                                                         .self()
                                                         .id()
                                                         .equals(nodes.get(1).self().id()))
                                      .allMatch(store -> value.equals(store.snapshot().get(key))));
        }
        // Restart node 1
        nodes.get(1).start().await(AWAIT_TIMEOUT);
        // Await node 1 catches up
        for (int i = 1; i <= 3; i++) {
            var key = "crash-key-" + i;
            var value = "v" + i;

            await().atMost(AWAIT_DURATION)
                   .until(() -> value.equals(stores.get(1).snapshot().get(key)));
        }
    }
} 
