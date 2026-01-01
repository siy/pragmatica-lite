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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToByteEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.ProtocolMessage;
import org.pragmatica.consensus.StateMachine;
import org.pragmatica.consensus.net.ClusterNetwork;
import org.pragmatica.consensus.net.NetworkManagementOperation;
import org.pragmatica.consensus.net.NetworkMessage;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Synchronous.SyncResponse;
import org.pragmatica.consensus.topology.QuorumStateNotification;
import org.pragmatica.consensus.topology.TopologyManager;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.tcp.NodeAddress;
import org.pragmatica.net.tcp.Server;
import org.pragmatica.serialization.fury.FuryDeserializer;
import org.pragmatica.serialization.fury.FurySerializer;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

        // Create node ID to port mapping for all nodes
        var nodePortMap = new ConcurrentHashMap<NodeId, Integer>();
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            nodePortMap.put(nodeId("node-" + i), basePort + i);
        }

        // Create all nodes
        for (int i = 0; i < CLUSTER_SIZE; i++) {
            var id = nodeId("node-" + i);
            var port = basePort + i;
            var node = new NetworkNode(id, port, CLUSTER_SIZE, nodePortMap, this::onDecision);
            nodes.add(node);
        }

        // Start all servers
        Promise.allOf(nodes.stream().map(NetworkNode::start).toList()).await();
        Thread.sleep(200);

        // Connect all nodes to each other
        for (var node : nodes) {
            node.connectToAllPeers();
        }
        Thread.sleep(300);

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
        final int port;
        private final int clusterSize;
        private final Map<NodeId, Integer> nodePortMap;
        private final Runnable onDecision;
        private final Map<NodeId, Channel> connections = new ConcurrentHashMap<>();
        private final FurySerializer serializer;
        private final FuryDeserializer deserializer;
        private Server server;
        private RabiaEngine<TestCommand> engine;
        private TcpClusterNetwork network;

        NetworkNode(NodeId nodeId, int port, int clusterSize, Map<NodeId, Integer> nodePortMap, Runnable onDecision) {
            this.nodeId = nodeId;
            this.port = port;
            this.clusterSize = clusterSize;
            this.nodePortMap = nodePortMap;
            this.onDecision = onDecision;
            this.serializer = FurySerializer.furySerializer(this::registerClasses);
            this.deserializer = FuryDeserializer.furyDeserializer(this::registerClasses);
        }

        private void registerClasses(Consumer<Class<?>> register) {
            register.accept(RabiaProtocolMessage.Synchronous.Propose.class);
            register.accept(RabiaProtocolMessage.Synchronous.VoteRound1.class);
            register.accept(RabiaProtocolMessage.Synchronous.VoteRound2.class);
            register.accept(RabiaProtocolMessage.Synchronous.Decision.class);
            register.accept(RabiaProtocolMessage.Synchronous.SyncResponse.class);
            register.accept(RabiaProtocolMessage.Asynchronous.SyncRequest.class);
            register.accept(RabiaProtocolMessage.Asynchronous.NewBatch.class);
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

        Promise<Unit> start() {
            network = new TcpClusterNetwork();
            var topology = new SimpleTopologyManager(nodeId, clusterSize, nodePortMap);
            engine = new RabiaEngine<>(topology, network, new SimpleStateMachine(), ProtocolConfig.testConfig());

            return Server.server("node-" + port, port, this::createHandlers)
                         .map(s -> {
                             this.server = s;
                             return Unit.unit();
                         });
        }

        void connectToAllPeers() {
            var connectionPromises = new ArrayList<Promise<Unit>>();
            for (var entry : nodePortMap.entrySet()) {
                if (!entry.getKey().equals(nodeId)) {
                    var peerPort = entry.getValue();
                    var peerId = entry.getKey();
                    var promise = server.connectTo(NodeAddress.nodeAddress("127.0.0.1", peerPort))
                          .map(ch -> {
                              connections.put(peerId, ch);
                              return Unit.unit();
                          })
                          .onFailure(e -> System.err.println("Failed to connect to " + peerId + ": " + e));
                    connectionPromises.add(promise);
                }
            }
            // Wait for all connections
            Promise.allOf(connectionPromises).await();
        }

        void activateEngine() throws InterruptedException {
            // Trigger quorum established
            engine.quorumState(QuorumStateNotification.ESTABLISHED);
            Thread.sleep(100); // Allow sync request to be sent

            // Send sync responses from all other nodes to activate
            for (var otherId : nodePortMap.keySet()) {
                if (!otherId.equals(nodeId)) {
                    engine.processSyncResponse(new SyncResponse<>(otherId, SavedState.empty()));
                }
            }
            Thread.sleep(100); // Allow activation to complete
        }

        void submitCommands(List<TestCommand> commands) {
            engine.handleSubmit(new RabiaEngineIO.SubmitCommands<>(commands));
        }

        boolean isActive() {
            return engine.isActive();
        }

        void stop() {
            if (engine != null) {
                engine.stop().await();
            }
            if (server != null) {
                server.stop(() -> Promise.success(Unit.unit())).await();
            }
            connections.values().forEach(Channel::close);
            connections.clear();
        }

        private List<ChannelHandler> createHandlers() {
            return List.of(
                new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4),
                new LengthFieldPrepender(4),
                new MessageEncoder(),
                new MessageDecoder()
            );
        }

        class TcpClusterNetwork implements ClusterNetwork {
            @Override
            public <M extends ProtocolMessage> void broadcast(M message) {
                for (var entry : connections.entrySet()) {
                    var channel = entry.getValue();
                    if (channel != null && channel.isActive()) {
                        channel.writeAndFlush(message);
                    }
                }
            }

            @Override
            public <M extends ProtocolMessage> void send(NodeId targetId, M message) {
                var channel = connections.get(targetId);
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(message);
                }
            }

            @Override
            public void connect(NetworkManagementOperation.ConnectNode c) {}
            @Override
            public void disconnect(NetworkManagementOperation.DisconnectNode d) {}
            @Override
            public void listNodes(NetworkManagementOperation.ListConnectedNodes l) {}
            @Override
            public void handlePing(NetworkMessage.Ping p) {}
            @Override
            public void handlePong(NetworkMessage.Pong p) {}
            @Override
            public Promise<Unit> start() { return Promise.success(Unit.unit()); }
            @Override
            public Promise<Unit> stop() { return Promise.success(Unit.unit()); }
        }

        class MessageEncoder extends MessageToByteEncoder<ProtocolMessage> {
            @Override
            protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, ByteBuf out) {
                serializer.write(out, msg);
            }
        }

        class MessageDecoder extends ChannelInboundHandlerAdapter {
            @Override
            @SuppressWarnings("unchecked")
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof ByteBuf buf) {
                    try {
                        var message = deserializer.<ProtocolMessage>read(buf);
                        routeMessage(message);
                    } finally {
                        buf.release();
                    }
                }
            }

            @SuppressWarnings("unchecked")
            private void routeMessage(ProtocolMessage message) {
                switch (message) {
                    case RabiaProtocolMessage.Synchronous.Propose<?> p ->
                        engine.processPropose((RabiaProtocolMessage.Synchronous.Propose<TestCommand>) p);
                    case RabiaProtocolMessage.Synchronous.VoteRound1 v ->
                        engine.processVoteRound1(v);
                    case RabiaProtocolMessage.Synchronous.VoteRound2 v ->
                        engine.processVoteRound2(v);
                    case RabiaProtocolMessage.Synchronous.Decision<?> d -> {
                        engine.processDecision((RabiaProtocolMessage.Synchronous.Decision<TestCommand>) d);
                        onDecision.run();
                    }
                    case RabiaProtocolMessage.Synchronous.SyncResponse<?> s ->
                        engine.processSyncResponse((SyncResponse<TestCommand>) s);
                    case RabiaProtocolMessage.Asynchronous.SyncRequest s ->
                        engine.handleSyncRequest(s);
                    case RabiaProtocolMessage.Asynchronous.NewBatch<?> b ->
                        engine.handleNewBatch((RabiaProtocolMessage.Asynchronous.NewBatch<TestCommand>) b);
                    default -> {}
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                System.err.println("Channel error on " + nodeId + ": " + cause.getMessage());
            }
        }

        static class SimpleTopologyManager implements TopologyManager {
            private final NodeInfo self;
            private final int clusterSize;
            private final Map<NodeId, Integer> nodePortMap;

            SimpleTopologyManager(NodeId selfId, int clusterSize, Map<NodeId, Integer> nodePortMap) {
                var port = nodePortMap.getOrDefault(selfId, 5000);
                this.self = NodeInfo.nodeInfo(selfId, NodeAddress.nodeAddress("127.0.0.1", port));
                this.clusterSize = clusterSize;
                this.nodePortMap = nodePortMap;
            }

            @Override
            public NodeInfo self() { return self; }

            @Override
            public Option<NodeInfo> get(NodeId id) {
                var port = nodePortMap.getOrDefault(id, 5000);
                return Option.option(NodeInfo.nodeInfo(id, NodeAddress.nodeAddress("127.0.0.1", port)));
            }

            @Override
            public int clusterSize() { return clusterSize; }

            @Override
            public Option<NodeId> reverseLookup(SocketAddress addr) { return Option.empty(); }

            @Override
            public void start() {}

            @Override
            public void stop() {}

            @Override
            public TimeSpan pingInterval() { return timeSpan(1).seconds(); }
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
