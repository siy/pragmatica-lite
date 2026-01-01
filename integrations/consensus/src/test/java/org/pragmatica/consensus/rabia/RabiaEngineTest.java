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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.ConsensusErrors;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.ProtocolMessage;
import org.pragmatica.consensus.StateMachine;
import org.pragmatica.consensus.net.ClusterNetwork;
import org.pragmatica.consensus.net.NetworkManagementOperation;
import org.pragmatica.consensus.net.NetworkMessage;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Synchronous.*;
import org.pragmatica.consensus.topology.QuorumStateNotification;
import org.pragmatica.consensus.topology.TopologyManager;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.tcp.NodeAddress;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.consensus.NodeId.nodeId;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class RabiaEngineTest {

    record TestCommand(String value) implements Command {}

    private static final NodeId NODE_1 = nodeId("node-1");
    private static final NodeId NODE_2 = nodeId("node-2");
    private static final NodeId NODE_3 = nodeId("node-3");
    private static final int CLUSTER_SIZE = 3;

    private TestTopologyManager topologyManager;
    private TestClusterNetwork network;
    private TestStateMachine stateMachine;
    private RabiaEngine<TestCommand> engine;

    @BeforeEach
    void setUp() {
        topologyManager = new TestTopologyManager(NODE_1, CLUSTER_SIZE);
        network = new TestClusterNetwork();
        stateMachine = new TestStateMachine();
        engine = new RabiaEngine<>(topologyManager, network, stateMachine, ProtocolConfig.testConfig());
    }

    @AfterEach
    void tearDown() {
        engine.stop().await();
    }

    private void activateEngine() throws InterruptedException {
        engine.quorumState(QuorumStateNotification.ESTABLISHED);
        // Wait for sync to occur and send quorum sync responses
        Thread.sleep(150); // Allow sync request to be sent
        // Send sync responses from other nodes
        engine.processSyncResponse(new SyncResponse<>(NODE_2, RabiaPersistence.SavedState.empty()));
        engine.processSyncResponse(new SyncResponse<>(NODE_3, RabiaPersistence.SavedState.empty()));
        Thread.sleep(50); // Allow activation to complete
    }

    @Nested
    class CommandSubmission {

        @Test
        void submit_fails_when_engine_inactive() {
            var result = engine.apply(List.of(new TestCommand("test"))).await();

            assertThat(result.isFailure()).isTrue();
            result.onFailure(cause ->
                assertThat(cause).isInstanceOf(ConsensusErrors.NodeInactive.class)
            );
        }

        @Test
        void submit_fails_for_empty_command_list() throws InterruptedException {
            activateEngine();

            var result = engine.apply(List.<TestCommand>of()).await();

            assertThat(result.isFailure()).isTrue();
            result.onFailure(cause ->
                assertThat(cause).isInstanceOf(ConsensusErrors.CommandBatchIsEmpty.class)
            );
        }
    }

    @Nested
    class QuorumHandling {

        @Test
        void disconnection_resets_engine_state() throws InterruptedException {
            activateEngine();

            engine.quorumState(QuorumStateNotification.DISAPPEARED);
            Thread.sleep(50);

            var result = engine.apply(List.of(new TestCommand("test"))).await();
            assertThat(result.isFailure()).isTrue();
        }
    }

    @Nested
    class MessageHandling {

        @Test
        void sync_request_broadcast_on_quorum_established() throws InterruptedException {
            engine.quorumState(QuorumStateNotification.ESTABLISHED);
            Thread.sleep(150);

            var hasSyncRequest = network.getMessages().stream()
                .anyMatch(m -> m instanceof RabiaProtocolMessage.Asynchronous.SyncRequest);
            assertThat(hasSyncRequest).isTrue();
        }

        @Test
        void ignores_proposals_when_inactive() {
            var batch = Batch.batch(List.of(new TestCommand("test")));
            var propose = new Propose<>(NODE_2, new Phase(1), batch);

            engine.processPropose(propose);
            // No exception should be thrown
        }

        @Test
        void ignores_votes_when_inactive() {
            var vote = new VoteRound1(NODE_2, new Phase(1), StateValue.V1);

            engine.processVoteRound1(vote);
            // No exception should be thrown
        }

        @Test
        void ignores_decisions_when_inactive() {
            var decision = new Decision<>(NODE_2, new Phase(1), StateValue.V1, Batch.<TestCommand>emptyBatch());

            engine.processDecision(decision);
            // No exception should be thrown
        }
    }

    @Nested
    class ProtocolFlow {

        @Test
        void processes_complete_round_with_v1_decision() throws InterruptedException {
            activateEngine();
            network.clearMessages();

            var batch = Batch.batch(List.of(new TestCommand("test-cmd")));

            // Simulate receiving proposals from all nodes (same batch)
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch));
            engine.processPropose(new Propose<>(NODE_2, Phase.ZERO, batch));
            Thread.sleep(50); // Allow processing

            // Check that round 1 vote was broadcast
            var hasVoteRound1 = network.getMessages().stream()
                .anyMatch(m -> m instanceof VoteRound1);
            assertThat(hasVoteRound1).isTrue();

            // Simulate round 1 votes from other nodes
            engine.processVoteRound1(new VoteRound1(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound1(new VoteRound1(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(50);

            // Simulate round 2 votes
            engine.processVoteRound2(new VoteRound2(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound2(new VoteRound2(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(50);

            // Check that a decision was broadcast
            var hasDecision = network.getMessages().stream()
                .anyMatch(m -> m instanceof Decision);
            assertThat(hasDecision).isTrue();
        }

        @Test
        void handles_v0_decision_for_conflicting_proposals() throws InterruptedException {
            activateEngine();
            network.clearMessages();

            var batch1 = Batch.batch(List.of(new TestCommand("cmd1")));
            var batch2 = Batch.batch(List.of(new TestCommand("cmd2")));

            // Simulate conflicting proposals
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch1));
            engine.processPropose(new Propose<>(NODE_2, Phase.ZERO, batch2));
            Thread.sleep(50);

            // When proposals don't agree, expect V0 votes
            var vote = network.getMessages().stream()
                .filter(m -> m instanceof VoteRound1)
                .map(m -> (VoteRound1) m)
                .findFirst();

            assertThat(vote).isPresent();
            assertThat(vote.get().stateValue()).isEqualTo(StateValue.V0);
        }
    }

    // ==================== Stub Implementations ====================

    static class TestTopologyManager implements TopologyManager {
        private final NodeInfo self;
        private final int clusterSize;

        TestTopologyManager(NodeId selfId, int clusterSize) {
            this.self = NodeInfo.nodeInfo(selfId, NodeAddress.nodeAddress("localhost", 5000));
            this.clusterSize = clusterSize;
        }

        @Override
        public NodeInfo self() {
            return self;
        }

        @Override
        public Option<NodeInfo> get(NodeId id) {
            return Option.option(NodeInfo.nodeInfo(id, NodeAddress.nodeAddress("localhost", 5000)));
        }

        @Override
        public int clusterSize() {
            return clusterSize;
        }

        @Override
        public Option<NodeId> reverseLookup(SocketAddress socketAddress) {
            return Option.empty();
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public TimeSpan pingInterval() {
            return timeSpan(1).seconds();
        }
    }

    static class TestClusterNetwork implements ClusterNetwork {
        private final List<ProtocolMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public <M extends ProtocolMessage> void broadcast(M message) {
            messages.add(message);
        }

        @Override
        public void connect(NetworkManagementOperation.ConnectNode connectNode) {}

        @Override
        public void disconnect(NetworkManagementOperation.DisconnectNode disconnectNode) {}

        @Override
        public void listNodes(NetworkManagementOperation.ListConnectedNodes listConnectedNodes) {}

        @Override
        public void handlePing(NetworkMessage.Ping ping) {}

        @Override
        public void handlePong(NetworkMessage.Pong pong) {}

        @Override
        public <M extends ProtocolMessage> void send(NodeId nodeId, M message) {
            messages.add(message);
        }

        @Override
        public Promise<Unit> start() {
            return Promise.success(Unit.unit());
        }

        @Override
        public Promise<Unit> stop() {
            return Promise.success(Unit.unit());
        }

        List<ProtocolMessage> getMessages() {
            return Collections.unmodifiableList(messages);
        }

        void clearMessages() {
            messages.clear();
        }
    }

    static class TestStateMachine implements StateMachine<TestCommand> {
        private final List<TestCommand> processedCommands = new CopyOnWriteArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <R> R process(TestCommand command) {
            processedCommands.add(command);
            return (R) ("result:" + command.value());
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
            processedCommands.clear();
        }

        List<TestCommand> getProcessedCommands() {
            return Collections.unmodifiableList(processedCommands);
        }
    }
}
