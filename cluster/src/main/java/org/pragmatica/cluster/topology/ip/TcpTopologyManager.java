package org.pragmatica.cluster.topology.ip;

import org.pragmatica.cluster.net.*;
import org.pragmatica.cluster.topology.TopologyManagementMessage;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.SharedScheduler;
import org.pragmatica.message.MessageRouter;
import org.pragmatica.net.NodeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/// Topology manager for TCP/IP networks.
public interface TcpTopologyManager extends TopologyManager {
    void start();

    void stop();

    static TcpTopologyManager tcpTopologyManager(TopologyConfig config, MessageRouter router) {
        record Manager(Map<NodeId, NodeInfo> nodesById, Map<NodeAddress, NodeId> nodeIdsByAddress,
                       MessageRouter router, TopologyConfig config, AtomicBoolean active) implements
                TcpTopologyManager {

            private static final Logger log = LoggerFactory.getLogger(TcpTopologyManager.class);

            Manager(Map<NodeId, NodeInfo> nodesById,
                    Map<NodeAddress, NodeId> nodeIdsByAddress,
                    MessageRouter router,
                    TopologyConfig config,
                    AtomicBoolean active) {
                this.config = config;
                this.router = router;
                this.nodesById = nodesById;
                this.nodeIdsByAddress = nodeIdsByAddress;
                this.active = active;

                config().coreNodes().forEach(this::addNode);

                router.addRoute(TopologyManagementMessage.AddNode.class, this::handleAddNodeMessage)
                      .addRoute(TopologyManagementMessage.RemoveNode.class, this::handleRemoveNodeMessage)
                      .addRoute(TopologyManagementMessage.DiscoverNodes.class, this::handleDiscoverNodesMessage)
                      .addRoute(TopologyManagementMessage.DiscoveredNodes.class, this::handleMergeNodesMessage)
                      .addRoute(NetworkManagementOperation.ConnectedNodesList.class, this::reconcile);

                log.trace("Topology manager {} initialized with {} nodes", config.self(), config.coreNodes());

                SharedScheduler.scheduleAtFixedRate(this::initReconcile, config.reconciliationInterval());
            }

            private void initReconcile() {
                if (active.get()) {
                    router().route(new NetworkManagementOperation.ListConnectedNodes());
                } else if (nodesById().isEmpty()) {
                    config().coreNodes().forEach(this::addNode);
                }
            }

            private void reconcile(NetworkManagementOperation.ConnectedNodesList connectedNodesList) {
                var snapshot = new HashSet<>(nodesById.keySet());

                connectedNodesList.connected()
                                  .forEach(snapshot::remove);

                snapshot.forEach(this::requestConnection);
            }

            private void handleAddNodeMessage(TopologyManagementMessage.AddNode message) {
                addNode(message.nodeInfo());
            }

            private void handleRemoveNodeMessage(TopologyManagementMessage.RemoveNode removeNode) {
                removeNode(removeNode.nodeId());
            }

            private void handleDiscoverNodesMessage(TopologyManagementMessage.DiscoverNodes discoverNodes) {
                router().route(new TopologyManagementMessage.DiscoveredNodes(List.copyOf(nodesById.values())));
            }

            private void handleMergeNodesMessage(TopologyManagementMessage.DiscoveredNodes discoveredNodes) {
                discoveredNodes.nodeInfos().forEach(this::addNode);
            }

            private void addNode(NodeInfo nodeInfo) {
                // To avoid reliance on the networking layer behavior, adding is done
                // atomically and the command to establish the connection is sent only once.
                if (nodesById().putIfAbsent(nodeInfo.id(), nodeInfo) == null) {
                    nodeIdsByAddress().putIfAbsent(nodeInfo.address(), nodeInfo.id());
                    requestConnection(nodeInfo.id());
                }
            }

            private void requestConnection(NodeId id) {
                router().route(new NetworkManagementOperation.ConnectNode(id));
            }

            private void removeNode(NodeId nodeId) {
                // To avoid reliance on the networking layer behavior, removing is done
                // atomically and command to drop the connection is sent only once.
                nodesById().computeIfPresent(nodeId, (_, value) -> {
                    nodeIdsByAddress.remove(value.address());

                    router().route(new NetworkManagementOperation.DisconnectNode(nodeId));
                    return null;
                });
            }

            @Override
            public Option<NodeInfo> get(NodeId id) {
                return Option.option(nodesById.get(id));
            }

            @Override
            public int clusterSize() {
                return nodesById.size();
            }

            @Override
            public Option<NodeId> reverseLookup(SocketAddress socketAddress) {
                return (socketAddress instanceof InetSocketAddress inet)
                        ? Option.option(nodeIdsByAddress.get(NodeAddress.nodeAddress(inet)))
                        : Option.empty();
            }

            @Override
            public void start() {
                if (active().compareAndSet(false, true)) {
                    log.trace("Starting topology manager at {}", config.self());
                    initReconcile();
                }
            }

            @Override
            public void stop() {
                active().set(false);
            }

            @Override
            public NodeInfo self() {
                return nodesById().get(config.self());
            }

            @Override
            public TimeSpan pingInterval() {
                return config().pingInterval();
            }
        }

        return new Manager(new ConcurrentHashMap<>(),
                           new ConcurrentHashMap<>(),
                           router,
                           config,
                           new AtomicBoolean(false));
    }
}