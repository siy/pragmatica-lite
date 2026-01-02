/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.consensus.rabia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.StateMachine;
import org.pragmatica.consensus.net.NetworkManagementOperation;
import org.pragmatica.consensus.net.NetworkMessage;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.consensus.net.netty.NettyClusterNetwork;
import org.pragmatica.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Synchronous.SyncResponse;
import org.pragmatica.consensus.topology.QuorumStateNotification;
import org.pragmatica.consensus.topology.TcpTopologyManager;
import org.pragmatica.consensus.topology.TopologyConfig;
import org.pragmatica.consensus.topology.TopologyManagementMessage;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.messaging.MessageRouter;
import org.pragmatica.messaging.MessageRouter.MutableRouter;
import org.pragmatica.net.tcp.NodeAddress;
import org.pragmatica.serialization.fury.FuryDeserializer;
import org.pragmatica.serialization.fury.FurySerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.pragmatica.consensus.NodeId.nodeId;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Performance tests for Rabia consensus using real TCP networking.
 * All nodes run in the same process but communicate over localhost TCP.
 */
class RabiaNetworkPerformanceTest {

    record TestCommand(int id) implements Command {}

    private static final AtomicInteger portCounter = new AtomicInteger(16000);
    private static final int CLUSTER_SIZE = 5;
    private static final int WARMUP_ROUNDS = 20;
    private static final int BENCHMARK_ROUNDS = 100;

    private int basePort;
    private List<NetworkNode> nodes;
    private CountDownLatch decisionLatch;
    private AtomicInteger decisionsReached;

    @BeforeEach
    void setUp() throws Exception {
        basePort = portCounter.getAndAdd(CLUSTER_SIZE + 10);
        nodes = new ArrayList<>();
        decisionsReached = new AtomicInteger(0);

        // Create node info list for all nodes
        var nodeInfos = new ArrayList<NodeInfo>();
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            var id = nodeId("node-" + i);
            var port = basePort + i;
            nodeInfos.add(NodeInfo.nodeInfo(id, NodeAddress.nodeAddress("127.0.0.1", port)));
        }

        // Create all nodes
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            var id = nodeId("node-" + i);
            var node = new NetworkNode(id, nodeInfos, this::onDecision);
            nodes.add(node);
        }

        // Start all nodes
        Promise.allOf(nodes.stream().map(NetworkNode::start).toList()).await();
        Thread.sleep(500);

        // Activate all engines
        for (var node : nodes) {
            node.activateEngine();
        }
        Thread.sleep(500);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (var node : nodes) {
            node.stop();
        }
        Thread.sleep(100);
    }

    private void onDecision() {
        decisionsReached.incrementAndGet();
        if (decisionLatch != null) {
            decisionLatch.countDown();
        }
    }

    @Test
    void single_proposer_network_throughput() throws Exception {

        var proposer = nodes.getFirst();

        // Warmup
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            decisionLatch = new CountDownLatch(CLUSTER_SIZE);
            proposer.submitCommands(List.of(new TestCommand(i)));
            if (!decisionLatch.await(2, TimeUnit.SECONDS)) {
                System.out.println("Warmup timeout at round " + i + ", decisions: " + decisionsReached.get());
                break;
            }
        }

        decisionsReached.set(0);

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            decisionLatch = new CountDownLatch(CLUSTER_SIZE);
            proposer.submitCommands(List.of(new TestCommand(WARMUP_ROUNDS + i)));
            if (!decisionLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("Timeout waiting for decision " + i);
            }
        }
        long endTime = System.nanoTime();

        printResults("Single Proposer (TCP)", startTime, endTime, BENCHMARK_ROUNDS);
    }

    @Test
    void round_robin_proposers_network_throughput() throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            decisionLatch = new CountDownLatch(CLUSTER_SIZE);
            var proposer = nodes.get(i % CLUSTER_SIZE);
            proposer.submitCommands(List.of(new TestCommand(i)));
            decisionLatch.await(5, TimeUnit.SECONDS);
        }

        decisionsReached.set(0);

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            decisionLatch = new CountDownLatch(CLUSTER_SIZE);
            var proposer = nodes.get(i % CLUSTER_SIZE);
            proposer.submitCommands(List.of(new TestCommand(WARMUP_ROUNDS + i)));
            if (!decisionLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("Timeout waiting for decision " + i);
            }
        }
        long endTime = System.nanoTime();

        printResults("Round-Robin Proposers (TCP)", startTime, endTime, BENCHMARK_ROUNDS);
    }

    @Test
    void concurrent_proposers_network_throughput() throws Exception {
        // Warmup - all nodes propose different commands
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            decisionLatch = new CountDownLatch(CLUSTER_SIZE);
            for (int j = 0; j < CLUSTER_SIZE; j++) {
                nodes.get(j).submitCommands(List.of(new TestCommand(i * 100 + j)));
            }
            decisionLatch.await(5, TimeUnit.SECONDS);
        }

        decisionsReached.set(0);

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            decisionLatch = new CountDownLatch(CLUSTER_SIZE);
            for (int j = 0; j < CLUSTER_SIZE; j++) {
                nodes.get(j).submitCommands(List.of(new TestCommand((WARMUP_ROUNDS + i) * 100 + j)));
            }
            if (!decisionLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("Timeout waiting for decision " + i);
            }
        }
        long endTime = System.nanoTime();

        printResults("Concurrent Proposers (TCP)", startTime, endTime, BENCHMARK_ROUNDS);
    }

    private void printResults(String scenario, long startNanos, long endNanos, int rounds) {
        double durationMs = (endNanos - startNanos) / 1_000_000.0;
        double durationSec = durationMs / 1000.0;
        double opsPerSec = rounds / durationSec;
        double avgLatencyMs = durationMs / rounds;

        System.out.println();
        System.out.println("=== " + scenario + " ===");
        System.out.println("Cluster size:     " + CLUSTER_SIZE + " nodes");
        System.out.println("Rounds:           " + rounds);
        System.out.println("Decisions:        " + decisionsReached.get());
        System.out.println("Total time:       " + String.format("%.2f", durationMs) + " ms");
        System.out.println("Throughput:       " + String.format("%.0f", opsPerSec) + " decisions/sec");
        System.out.println("Avg latency:      " + String.format("%.2f", avgLatencyMs) + " ms/decision");
    }

    // ==================== Network Node Implementation ====================

    static class NetworkNode {
        final NodeId nodeId;
        private final List<NodeInfo> allNodes;
        private final Runnable onDecision;
        private final FurySerializer serializer;
        private final FuryDeserializer deserializer;
        private final MutableRouter router;
        private RabiaEngine<TestCommand> engine;
        private NettyClusterNetwork network;
        private TcpTopologyManager topologyManager;

        NetworkNode(NodeId nodeId, List<NodeInfo> allNodes, Runnable onDecision) {
            this.nodeId = nodeId;
            this.allNodes = allNodes;
            this.onDecision = onDecision;
            this.serializer = FurySerializer.furySerializer(this::registerClasses);
            this.deserializer = FuryDeserializer.furyDeserializer(this::registerClasses);
            this.router = MessageRouter.mutable();
        }

        private void registerClasses(Consumer<Class<?>> register) {
            register.accept(RabiaProtocolMessage.Synchronous.Propose.class);
            register.accept(RabiaProtocolMessage.Synchronous.VoteRound1.class);
            register.accept(RabiaProtocolMessage.Synchronous.VoteRound2.class);
            register.accept(RabiaProtocolMessage.Synchronous.Decision.class);
            register.accept(RabiaProtocolMessage.Synchronous.SyncResponse.class);
            register.accept(RabiaProtocolMessage.Asynchronous.SyncRequest.class);
            register.accept(RabiaProtocolMessage.Asynchronous.NewBatch.class);
            register.accept(NetworkMessage.Ping.class);
            register.accept(NetworkMessage.Pong.class);
            register.accept(SavedState.class);
            register.accept(Batch.class);
            register.accept(BatchId.class);
            register.accept(CorrelationId.class);
            register.accept(Phase.class);
            register.accept(StateValue.class);
            register.accept(NodeId.class);
            register.accept(TestCommand.class);
            register.accept(ArrayList.class);
        }

        @SuppressWarnings("unchecked")
        Promise<Unit> start() {
            // Create topology config
            var config = new TopologyConfig(
                nodeId,
                timeSpan(100).hours(),  // Long reconciliation interval for tests
                timeSpan(10).seconds(), // Ping interval
                allNodes
            );

            // Create topology manager and wire up routes
            topologyManager = TcpTopologyManager.tcpTopologyManager(config, router);

            // Wire up topology manager message routes
            router.addRoute(TopologyManagementMessage.AddNode.class, topologyManager::handleAddNodeMessage);
            router.addRoute(TopologyManagementMessage.RemoveNode.class, topologyManager::handleRemoveNodeMessage);
            router.addRoute(TopologyManagementMessage.DiscoverNodes.class, topologyManager::handleDiscoverNodesMessage);
            router.addRoute(TopologyManagementMessage.DiscoveredNodes.class, topologyManager::handleMergeNodesMessage);
            router.addRoute(NetworkManagementOperation.ConnectedNodesList.class, topologyManager::reconcile);

            // Create network
            network = new NettyClusterNetwork(topologyManager, serializer, deserializer, router);

            // Wire up network message routes
            router.addRoute(NetworkManagementOperation.ConnectNode.class, network::connect);
            router.addRoute(NetworkManagementOperation.DisconnectNode.class, network::disconnect);
            router.addRoute(NetworkManagementOperation.ListConnectedNodes.class, network::listNodes);
            router.addRoute(NetworkMessage.Ping.class, network::handlePing);
            router.addRoute(NetworkMessage.Pong.class, network::handlePong);

            // Create engine
            engine = new RabiaEngine<>(topologyManager, network, new SimpleStateMachine(), ProtocolConfig.testConfig());

            // Wire up engine message routes
            router.addRoute(RabiaProtocolMessage.Synchronous.Propose.class,
                            msg -> engine.processPropose((RabiaProtocolMessage.Synchronous.Propose<TestCommand>) msg));
            router.addRoute(RabiaProtocolMessage.Synchronous.VoteRound1.class, engine::processVoteRound1);
            router.addRoute(RabiaProtocolMessage.Synchronous.VoteRound2.class, engine::processVoteRound2);
            router.addRoute(RabiaProtocolMessage.Synchronous.Decision.class, msg -> {
                engine.processDecision((RabiaProtocolMessage.Synchronous.Decision<TestCommand>) msg);
                onDecision.run();
            });
            router.addRoute(RabiaProtocolMessage.Synchronous.SyncResponse.class,
                            msg -> engine.processSyncResponse((SyncResponse<TestCommand>) msg));
            router.addRoute(RabiaProtocolMessage.Asynchronous.SyncRequest.class, engine::handleSyncRequest);
            router.addRoute(RabiaProtocolMessage.Asynchronous.NewBatch.class,
                            msg -> engine.handleNewBatch((RabiaProtocolMessage.Asynchronous.NewBatch<TestCommand>) msg));
            router.addRoute(QuorumStateNotification.class, engine::quorumState);

            // Start network first, then topology manager (order matters!)
            return network.start()
                          .onSuccessRun(topologyManager::start);
        }

        void activateEngine() throws InterruptedException {
            Thread.sleep(200); // Allow connections to establish

            // Send sync responses from all other nodes to activate
            for (var nodeInfo : allNodes) {
                if (!nodeInfo.id().equals(nodeId)) {
                    engine.processSyncResponse(new SyncResponse<>(nodeInfo.id(), SavedState.empty()));
                }
            }
            Thread.sleep(100); // Allow activation to complete
        }

        void submitCommands(List<TestCommand> commands) {
            engine.handleSubmit(new RabiaEngineIO.SubmitCommands<>(commands));
        }

        void stop() {
            if (engine != null) {
                engine.stop().await();
            }
            if (topologyManager != null) {
                topologyManager.stop();
            }
            if (network != null) {
                network.stop().await();
            }
        }

        static class SimpleStateMachine implements StateMachine<TestCommand> {
            private final List<TestCommand> processed = new CopyOnWriteArrayList<>();

            @Override
            @SuppressWarnings("unchecked")
            public <R> R process(TestCommand command) {
                processed.add(command);
                return (R) ("result:" + command.id());
            }

            @Override
            public Result<byte[]> makeSnapshot() {
                return Result.success(new byte[0]);
            }

            @Override
            public Result<Unit> restoreSnapshot(byte[] snapshot) {
                return Result.success(Unit.unit());
            }

            @Override
            public void reset() {
                processed.clear();
            }
        }
    }
}
