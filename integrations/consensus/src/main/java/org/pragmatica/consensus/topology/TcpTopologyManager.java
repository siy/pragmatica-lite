package org.pragmatica.consensus.topology;

import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.net.NetworkManagementOperation;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.SharedScheduler;
import org.pragmatica.lang.utils.TimeSource;
import org.pragmatica.messaging.MessageReceiver;
import org.pragmatica.messaging.MessageRouter;
import org.pragmatica.net.tcp.NodeAddress;
import org.pragmatica.net.tcp.TlsConfig;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Topology manager for TCP/IP networks.
public interface TcpTopologyManager extends TopologyManager {
    @MessageReceiver
    void reconcile(NetworkManagementOperation.ConnectedNodesList connectedNodesList);

    @MessageReceiver
    void handleAddNodeMessage(TopologyManagementMessage.AddNode message);

    @MessageReceiver
    void handleRemoveNodeMessage(TopologyManagementMessage.RemoveNode removeNode);

    @MessageReceiver
    void handleDiscoverNodesMessage(TopologyManagementMessage.DiscoverNodes discoverNodes);

    @MessageReceiver
    void handleMergeNodesMessage(TopologyManagementMessage.DiscoveredNodes discoveredNodes);

    @MessageReceiver
    void handleConnectionFailed(NetworkManagementOperation.ConnectionFailed connectionFailed);

    @MessageReceiver
    void handleConnectionEstablished(NetworkManagementOperation.ConnectionEstablished connectionEstablished);

    static TcpTopologyManager tcpTopologyManager(TopologyConfig config, MessageRouter router) {
        return tcpTopologyManager(config, router, TimeSource.system());
    }

    static TcpTopologyManager tcpTopologyManager(TopologyConfig config, MessageRouter router, TimeSource timeSource) {
        record Manager(Map<NodeId, NodeState> nodeStatesById,
                       Map<NodeAddress, NodeId> nodeIdsByAddress,
                       MessageRouter router,
                       TopologyConfig config,
                       TimeSource timeSource,
                       AtomicBoolean active) implements TcpTopologyManager {
            private static final Logger log = LoggerFactory.getLogger(TcpTopologyManager.class);

            Manager(Map<NodeId, NodeState> nodeStatesById,
                    Map<NodeAddress, NodeId> nodeIdsByAddress,
                    MessageRouter router,
                    TopologyConfig config,
                    TimeSource timeSource,
                    AtomicBoolean active) {
                this.config = config;
                this.router = router;
                this.nodeStatesById = nodeStatesById;
                this.nodeIdsByAddress = nodeIdsByAddress;
                this.timeSource = timeSource;
                this.active = active;
                config().coreNodes()
                      .forEach(this::addNode);
                log.trace("Topology manager {} initialized with {} nodes", config.self(), config.coreNodes());
                SharedScheduler.scheduleAtFixedRate(this::initReconcile, config.reconciliationInterval());
            }

            private Instant now() {
                return Instant.ofEpochSecond(0, timeSource.nanoTime());
            }

            private void initReconcile() {
                if (active.get()) {
                    router().route(new NetworkManagementOperation.ListConnectedNodes());
                } else if (nodeStatesById().isEmpty()) {
                    config().coreNodes()
                          .forEach(this::addNode);
                }
            }

            @Override
            public void reconcile(NetworkManagementOperation.ConnectedNodesList connectedNodesList) {
                var snapshot = new HashSet<>(nodeStatesById.keySet());
                connectedNodesList.connected()
                                  .forEach(snapshot::remove);
                snapshot.forEach(this::requestConnectionIfEligible);
            }

            private void requestConnectionIfEligible(NodeId id) {
                Option.option(nodeStatesById.get(id))
                      .filter(state -> state.canAttemptConnection(now()))
                      .onPresent(_ -> requestConnection(id));
            }

            @Override
            public void handleAddNodeMessage(TopologyManagementMessage.AddNode message) {
                addNode(message.nodeInfo());
            }

            @Override
            public void handleRemoveNodeMessage(TopologyManagementMessage.RemoveNode removeNode) {
                removeNode(removeNode.nodeId());
            }

            @Override
            public void handleDiscoverNodesMessage(TopologyManagementMessage.DiscoverNodes discoverNodes) {
                var nodeInfos = nodeStatesById.values()
                                              .stream()
                                              .map(NodeState::info)
                                              .toList();
                router().route(new TopologyManagementMessage.DiscoveredNodes(nodeInfos));
            }

            @Override
            public void handleMergeNodesMessage(TopologyManagementMessage.DiscoveredNodes discoveredNodes) {
                discoveredNodes.nodeInfos()
                               .forEach(this::addNode);
            }

            @Override
            public void handleConnectionFailed(NetworkManagementOperation.ConnectionFailed connectionFailed) {
                var nodeId = connectionFailed.nodeId();
                Option.option(nodeStatesById.get(nodeId))
                      .onPresent(state -> processConnectionFailure(state,
                                                                   connectionFailed.cause()));
            }

            private void processConnectionFailure(NodeState state, Throwable cause) {
                var nodeId = state.info()
                                  .id();
                var newAttempts = state.failedAttempts() + 1;
                var backoff = config.backoff();
                var now = now();
                if (backoff.shouldDisable(newAttempts)) {
                    var disabledState = NodeState.disabled(state.info(), newAttempts, now);
                    nodeStatesById.put(nodeId, disabledState);
                    log.warn("Node {} disabled after {} failed attempts: {}", nodeId, newAttempts, cause.getMessage());
                    router.route(TopologyChangeNotification.nodeDisabled(nodeId, activeTopology()));
                    checkAutoReset();
                } else {
                    var delay = backoff.backoffStrategy()
                                       .nextTimeout(newAttempts);
                    var nextAttempt = now.plusNanos(delay.nanos());
                    var suspectedState = NodeState.suspected(state.info(), newAttempts, now, nextAttempt);
                    nodeStatesById.put(nodeId, suspectedState);
                    log.debug("Node {} connection failed (attempt {}), next attempt after {}: {}",
                              nodeId,
                              newAttempts,
                              delay,
                              cause.getMessage());
                }
            }

            private void checkAutoReset() {
                var disabledCount = (int) nodeStatesById.values()
                                                       .stream()
                                                       .filter(s -> s.health() == NodeHealth.DISABLED)
                                                       .count();
                if (disabledCount >= maxFailures()) {
                    log.warn("Too many disabled nodes ({}), resetting all to healthy for cluster liveness",
                             disabledCount);
                    resetAllDisabledNodes();
                }
            }

            private void resetAllDisabledNodes() {
                var now = now();
                nodeStatesById.replaceAll((id, state) -> state.health() == NodeHealth.DISABLED
                                                         ? NodeState.healthy(state.info(), now)
                                                         : state);
                router.route(TopologyChangeNotification.allNodesReset(activeTopology()));
            }

            @Override
            public void handleConnectionEstablished(NetworkManagementOperation.ConnectionEstablished connectionEstablished) {
                var nodeId = connectionEstablished.nodeId();
                Option.option(nodeStatesById.get(nodeId))
                      .onPresent(state -> processConnectionEstablished(state));
            }

            private void processConnectionEstablished(NodeState state) {
                var nodeId = state.info()
                                  .id();
                var wasDisabled = state.health() == NodeHealth.DISABLED;
                var healthyState = NodeState.healthy(state.info(), now());
                nodeStatesById.put(nodeId, healthyState);
                if (wasDisabled) {
                    log.info("Node {} reactivated after successful connection", nodeId);
                    router.route(TopologyChangeNotification.nodeReactivated(nodeId, activeTopology()));
                } else if (state.health() == NodeHealth.SUSPECTED) {
                    log.debug("Node {} recovered from suspected state", nodeId);
                }
            }

            private void addNode(NodeInfo nodeInfo) {
                var now = now();
                var initialState = NodeState.healthy(nodeInfo, now);
                // To avoid reliance on the networking layer behavior, adding is done
                // atomically and the command to establish the connection is sent only once.
                Option.option(nodeStatesById().putIfAbsent(nodeInfo.id(),
                                                           initialState))
                      .onEmpty(() -> {
                                   nodeIdsByAddress().putIfAbsent(nodeInfo.address(),
                                                                  nodeInfo.id());
                                   // Only request connection if topology manager is active (router is ready)
                if (active().get()) {
                                       requestConnection(nodeInfo.id());
                                   }
                               });
            }

            private void requestConnection(NodeId id) {
                router().route(new NetworkManagementOperation.ConnectNode(id));
            }

            private void removeNode(NodeId nodeId) {
                // To avoid reliance on the networking layer behavior, removing is done
                // atomically and command to drop the connection is sent only once.
                Option.option(nodeStatesById().remove(nodeId))
                      .onPresent(state -> {
                                     nodeIdsByAddress.remove(state.info()
                                                                  .address());
                                     router().route(new NetworkManagementOperation.DisconnectNode(nodeId));
                                 });
            }

            @Override
            public Option<NodeInfo> get(NodeId id) {
                return Option.option(nodeStatesById.get(id))
                             .map(NodeState::info);
            }

            @Override
            public Option<NodeState> getState(NodeId id) {
                return Option.option(nodeStatesById.get(id));
            }

            @Override
            public List<NodeId> activeTopology() {
                return nodeStatesById.values()
                                     .stream()
                                     .filter(NodeState::isActive)
                                     .map(state -> state.info()
                                                        .id())
                                     .sorted()
                                     .toList();
            }

            @Override
            public List<NodeId> fullTopology() {
                return nodeStatesById.keySet()
                                     .stream()
                                     .sorted()
                                     .toList();
            }

            @Override
            public int clusterSize() {
                return nodeStatesById.size();
            }

            @Override
            public int activeClusterSize() {
                return (int) nodeStatesById.values()
                                          .stream()
                                          .filter(NodeState::isActive)
                                          .count();
            }

            @Override
            public Option<NodeId> reverseLookup(SocketAddress socketAddress) {
                return (socketAddress instanceof InetSocketAddress inet)
                       ? NodeAddress.nodeAddress(inet)
                                    .option()
                                    .flatMap(addr -> Option.option(nodeIdsByAddress.get(addr)))
                       : Option.empty();
            }

            @Override
            public Promise<Unit> start() {
                if (active().compareAndSet(false, true)) {
                    log.trace("Starting topology manager at {}", config.self());
                    initReconcile();
                }
                return Promise.success(Unit.unit());
            }

            @Override
            public Promise<Unit> stop() {
                active().set(false);
                return Promise.success(Unit.unit());
            }

            @Override
            public NodeInfo self() {
                return nodeStatesById().get(config.self())
                                     .info();
            }

            @Override
            public TimeSpan pingInterval() {
                return config().pingInterval();
            }

            @Override
            public TimeSpan helloTimeout() {
                return config().helloTimeout();
            }

            @Override
            public Option<TlsConfig> tls() {
                return config().tls();
            }
        }
        return new Manager(new ConcurrentHashMap<>(),
                           new ConcurrentHashMap<>(),
                           router,
                           config,
                           timeSource,
                           new AtomicBoolean(false));
    }
}
