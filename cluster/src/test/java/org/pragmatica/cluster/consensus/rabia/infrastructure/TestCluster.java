package org.pragmatica.cluster.consensus.rabia.infrastructure;

import org.pragmatica.cluster.consensus.rabia.ProtocolConfig;
import org.pragmatica.cluster.consensus.rabia.RabiaEngine;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.cluster.net.local.LocalNetwork.FaultInjector;
import org.pragmatica.cluster.node.rabia.CustomClasses;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.cluster.state.kvstore.*;
import org.pragmatica.lang.Promise;
import org.pragmatica.message.MessageRouter;
import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.Serializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Asynchronous;
import static org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Synchronous;
import static org.pragmatica.cluster.net.NodeId.randomNodeId;
import static org.pragmatica.cluster.net.NodeInfo.nodeInfo;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;
import static org.pragmatica.net.NodeAddress.nodeAddress;
import static org.pragmatica.net.serialization.binary.fury.FuryDeserializer.furyDeserializer;
import static org.pragmatica.net.serialization.binary.fury.FurySerializer.furySerializer;

/// Holds a small Rabia cluster wired over a single LocalNetwork.
public class TestCluster {
    public record StringKey(String key) implements StructuredKey {
        @Override
        public boolean matches(StructuredPattern pattern) {
            return false;
        }

        public static StringKey key(String key) {
            return new StringKey(key);
        }

        public static void register(Consumer<Class<?>> consumer) {
            consumer.accept(StringKey.class);
        }
    }

    private final LocalNetwork network;
    private final List<NodeId> ids = new ArrayList<>();
    private final Map<NodeId, RabiaEngine<KVCommand<StringKey>>> engines = new LinkedHashMap<>();
    private final Map<NodeId, KVStore<StringKey, String>> stores = new LinkedHashMap<>();
    private final Map<NodeId, MessageRouter.MutableRouter> routers = new LinkedHashMap<>();
    private final Serializer serializer = furySerializer(CustomClasses::configure, StringKey::register);
    private final Deserializer deserializer = furyDeserializer(CustomClasses::configure, StringKey::register);
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

    public Map<NodeId, RabiaEngine<KVCommand<StringKey>>> engines() {
        return engines;
    }

    public Map<NodeId, KVStore<StringKey, String>> stores() {
        return stores;
    }

    public Map<NodeId, MessageRouter.MutableRouter> routers() {
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
        var router = MessageRouter.mutable();
        var store = new KVStore<StringKey, String>(router, serializer, deserializer);
        var topologyManager = new TestTopologyManager(size, nodeInfo(id, nodeAddress("localhost", 8090)));
        var engine = new RabiaEngine<>(topologyManager, network, store, ProtocolConfig.testConfig());

        store.configure(router);
        topologyManager.configure(router);
        engine.configure(router);

        network.addNode(id, createHandler(engine));
        stores.put(id, store);
        engines.put(id, engine);
        routers.put(id, router);

        var stateChangePrinter = new StateChangePrinter(id);
        router.addRoute(KVStoreNotification.ValuePut.class, stateChangePrinter::accept);
    }

    @SuppressWarnings("unchecked")
    private static <C extends Command> Consumer<RabiaProtocolMessage> createHandler(RabiaEngine<C> engine) {
        return message -> {
            switch (message) {
                case Synchronous.Propose<?> propose -> engine.processPropose((Synchronous.Propose<C>) propose);
                case Synchronous.VoteRound1 voteRnd1 -> engine.processVoteRound1(voteRnd1);
                case Synchronous.VoteRound2 voteRnd2 -> engine.processVoteRound2(voteRnd2);
                case Synchronous.Decision<?> decision -> engine.processDecision((Synchronous.Decision<C>) decision);
                case Synchronous.SyncResponse<?> syncResponse -> engine.processSyncResponse((Synchronous.SyncResponse<C>) syncResponse);
                case Asynchronous.SyncRequest syncRequest -> engine.handleSyncRequest(syncRequest);
                case Asynchronous.NewBatch<?> newBatch -> engine.handleNewBatch(newBatch);
            }
        };
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

    public void submitAndWait(NodeId nodeId, KVCommand<StringKey> command) {
        engines.get(nodeId)
               .apply(List.of(command))
               .await(timeSpan(10).seconds())
               .onFailure(cause -> fail("Failed to apply command: (a, 1): " + cause));
    }

    public boolean allNodesHaveValue(StringKey k1, String v1) {
        return network.connectedNodes().stream()
                      .allMatch(id -> v1.equals(stores.get(id).snapshot().get(k1)));
    }
}
