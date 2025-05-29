package org.pragmatica.cluster.consensus.rabia.infrastructure;

import org.pragmatica.cluster.consensus.rabia.ProtocolConfig;
import org.pragmatica.cluster.consensus.rabia.RabiaEngine;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.cluster.net.local.LocalNetwork.FaultInjector;
import org.pragmatica.cluster.node.rabia.CustomClasses;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification;
import org.pragmatica.cluster.state.kvstore.KVStore;
import org.pragmatica.lang.Promise;
import org.pragmatica.message.MessageRouter;
import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.Serializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.cluster.net.NodeId.randomNodeId;
import static org.pragmatica.cluster.net.NodeInfo.nodeInfo;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;
import static org.pragmatica.net.NodeAddress.nodeAddress;
import static org.pragmatica.net.serialization.binary.fury.FuryDeserializer.furyDeserializer;
import static org.pragmatica.net.serialization.binary.fury.FurySerializer.furySerializer;

/// Holds a small Rabia cluster wired over a single LocalNetwork.
public class TestCluster {
    private final LocalNetwork network;
    private final List<NodeId> ids = new ArrayList<>();
    private final Map<NodeId, RabiaEngine<KVCommand>> engines = new LinkedHashMap<>();
    private final Map<NodeId, KVStore<String, String>> stores = new LinkedHashMap<>();
    private final Map<NodeId, MessageRouter> routers = new LinkedHashMap<>();
    private final Serializer serializer = furySerializer(CustomClasses::configure);
    private final Deserializer deserializer = furyDeserializer(CustomClasses::configure);
    private final int size;

    public TestCluster(int size) {
        this.size = size;
        var topologyManager = new TestTopologyManager(size, nodeInfo(randomNodeId(), nodeAddress("localhost", 8090)));
        network = new LocalNetwork(topologyManager, routers, new FaultInjector());

        // create nodes
        for (int i = 1; i <= size; i++) {
            var id = NodeId.nodeId("node-" + i);
            ids.add(id);
            addNewNode(id);
        }
        network.start();
    }

    public Map<NodeId, RabiaEngine<KVCommand>> engines() {
        return engines;
    }

    public Map<NodeId, KVStore<String, String>> stores() {
        return stores;
    }

    public Map<NodeId, MessageRouter> routers() {
        return routers;
    }

    public NodeId getFirst() {
        return ids.getFirst();
    }

    public List<NodeId> ids() {
        return ids;
    }

    public LocalNetwork network() {
        return network;
    }

    public void disconnect(NodeId id) {
        network.disconnect(id);
    }

    public void addNewNode(NodeId id) {
        var router = MessageRouter.messageRouter();
        var store = new KVStore<String, String>(router, serializer, deserializer);
        var topologyManager = new TestTopologyManager(size, nodeInfo(id, nodeAddress("localhost", 8090)));
        var engine = new RabiaEngine<>(topologyManager, network, store, router, ProtocolConfig.testConfig());

        network.addNode(id, engine::processMessage);
        stores.put(id, store);
        engines.put(id, engine);
        routers.put(id, router);

        var stateChangePrinter = new StateChangePrinter(id);
        router.addRoute(KVStoreNotification.ValuePut.class, stateChangePrinter::accept);
    }

    public void awaitNode(NodeId nodeId) {
        engines.get(nodeId)
               .start()
               .await(timeSpan(10).seconds())
               .onFailure(cause -> fail("Failed to start " + nodeId.id() + " " + cause));
    }

    public void awaitStart() {
        var promises = engines.values()
                              .stream()
                              .map(RabiaEngine::start)
                              .toList();

        Promise.allOf(promises)
               .await(timeSpan(10).seconds())
               .onFailureRun(() -> fail("Failed to start all nodes within 10 seconds"));
    }

    public void submitAndWait(NodeId nodeId, KVCommand command) {
        engines.get(nodeId)
               .apply(List.of(command))
               .await(timeSpan(10).seconds())
               .onFailure(cause -> fail("Failed to apply command: (a, 1): " + cause));
    }

    public boolean allNodesHaveValue(String k1, String v1) {
        return network.connectedNodes().stream()
                      .allMatch(id -> v1.equals(stores.get(id).snapshot().get(k1)));
    }
}
