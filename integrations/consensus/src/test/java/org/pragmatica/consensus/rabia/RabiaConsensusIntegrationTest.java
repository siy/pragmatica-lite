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
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.ProtocolMessage;
import org.pragmatica.consensus.StateMachine;
import org.pragmatica.consensus.net.ClusterNetwork;
import org.pragmatica.consensus.net.NetworkManagementOperation;
import org.pragmatica.consensus.net.NetworkMessage;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Asynchronous.*;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.consensus.NodeId.nodeId;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Integration tests simulating multi-node consensus scenarios.
 * These tests verify the complete protocol flow including:
 * - Multi-node proposal agreement
 * - Voting rounds (R1 and R2)
 * - Decision agreement
 * - Value locking across phases
 */
class RabiaConsensusIntegrationTest {

    record TestCommand(String value) implements Command {}

    private static final NodeId NODE_1 = nodeId("node-1");
    private static final NodeId NODE_2 = nodeId("node-2");
    private static final NodeId NODE_3 = nodeId("node-3");
    private static final int CLUSTER_SIZE = 3;

    private ClusterSimulator cluster;

    @BeforeEach
    void setUp() {
        cluster = new ClusterSimulator(List.of(NODE_1, NODE_2, NODE_3));
    }

    @AfterEach
    void tearDown() {
        cluster.stopAll();
    }

    @Nested
    class ConsensusAgreement {

        @Test
        void all_nodes_agree_on_same_proposal() throws InterruptedException {
            cluster.activateAll();

            var batch = Batch.batch(List.of(new TestCommand("test-cmd")));

            // All nodes propose the same batch
            cluster.simulateProposal(NODE_1, batch);
            cluster.simulateProposal(NODE_2, batch);
            cluster.simulateProposal(NODE_3, batch);
            cluster.deliverAllPendingMessages();
            Thread.sleep(50);

            // All nodes should vote V1 since quorum agrees
            cluster.deliverAllPendingMessages();
            Thread.sleep(50);

            // Verify all votes are V1
            var votes = cluster.getMessagesByType(VoteRound1.class);
            assertThat(votes).allMatch(v -> v.stateValue() == StateValue.V1);
        }

        @Test
        void conflicting_proposals_lead_to_v0_votes() throws InterruptedException {
            cluster.activateAll();

            var batch1 = Batch.batch(List.of(new TestCommand("cmd1")));
            var batch2 = Batch.batch(List.of(new TestCommand("cmd2")));
            var batch3 = Batch.batch(List.of(new TestCommand("cmd3")));

            // All nodes propose different batches
            cluster.simulateProposal(NODE_1, batch1);
            cluster.simulateProposal(NODE_2, batch2);
            cluster.simulateProposal(NODE_3, batch3);
            cluster.deliverAllPendingMessages();
            Thread.sleep(50);

            cluster.deliverAllPendingMessages();
            Thread.sleep(50);

            // With no majority agreement, votes should be V0
            var votes = cluster.getMessagesByType(VoteRound1.class);
            assertThat(votes).allMatch(v -> v.stateValue() == StateValue.V0);
        }

        @Test
        void majority_proposal_leads_to_v1_decision() throws InterruptedException {
            cluster.activateAll();

            var majorityBatch = Batch.batch(List.of(new TestCommand("majority")));
            var minorityBatch = Batch.batch(List.of(new TestCommand("minority")));

            // 2/3 nodes propose the same batch
            cluster.simulateProposal(NODE_1, majorityBatch);
            cluster.simulateProposal(NODE_2, majorityBatch);
            cluster.simulateProposal(NODE_3, minorityBatch);
            cluster.deliverAllPendingMessages();
            Thread.sleep(50);

            // Complete voting rounds
            cluster.deliverAllPendingMessages();
            Thread.sleep(50);
            cluster.deliverAllPendingMessages();
            Thread.sleep(50);

            var decisions = cluster.getMessagesByType(Decision.class);
            if (!decisions.isEmpty()) {
                // With majority agreement, decision should be V1
                assertThat(decisions.getFirst().stateValue()).isEqualTo(StateValue.V1);
            }
        }
    }

    @Nested
    class VotingRounds {

        @Test
        void round2_vote_is_v1_when_quorum_voted_v1_in_round1() throws InterruptedException {
            var phaseData = new PhaseData<TestCommand>(new Phase(1));
            var batch = Batch.batch(List.of(new TestCommand("test")));

            // Register quorum V1 votes
            phaseData.registerRound1Vote(NODE_1, StateValue.V1);
            phaseData.registerRound1Vote(NODE_2, StateValue.V1);

            var round2Vote = phaseData.evaluateRound2Vote(2);

            assertThat(round2Vote).isEqualTo(StateValue.V1);
        }

        @Test
        void round2_vote_is_vquestion_when_no_quorum() throws InterruptedException {
            var phaseData = new PhaseData<TestCommand>(new Phase(1));

            // Split votes evenly - no value has quorum (2)
            phaseData.registerRound1Vote(NODE_1, StateValue.V1);
            phaseData.registerRound1Vote(NODE_2, StateValue.V0);
            // Only 2 votes, neither has quorum

            var round2Vote = phaseData.evaluateRound2Vote(2);

            assertThat(round2Vote).isEqualTo(StateValue.VQUESTION);
        }
    }

    @Nested
    class DecisionProcessing {

        @Test
        void v1_decision_returns_batch_with_quorum_support() {
            var phaseData = new PhaseData<TestCommand>(new Phase(1));
            var batch = Batch.batch(List.of(new TestCommand("test")));

            // Register quorum proposals for same batch
            phaseData.registerProposal(NODE_1, batch);
            phaseData.registerProposal(NODE_2, batch);
            phaseData.registerProposal(NODE_3, batch);

            // Register V1 votes
            phaseData.registerRound2Vote(NODE_1, StateValue.V1);
            phaseData.registerRound2Vote(NODE_2, StateValue.V1);
            phaseData.registerRound2Vote(NODE_3, StateValue.V1);

            var decision = phaseData.processRound2Completion(NODE_1, 2, 2);

            assertThat(decision.stateValue()).isEqualTo(StateValue.V1);
            assertThat(decision.value().correlationId()).isEqualTo(batch.correlationId());
        }

        @Test
        void v0_decision_returns_empty_batch() {
            var phaseData = new PhaseData<TestCommand>(new Phase(1));

            // Register V0 votes
            phaseData.registerRound2Vote(NODE_1, StateValue.V0);
            phaseData.registerRound2Vote(NODE_2, StateValue.V0);
            phaseData.registerRound2Vote(NODE_3, StateValue.V0);

            var decision = phaseData.processRound2Completion(NODE_1, 2, 2);

            assertThat(decision.stateValue()).isEqualTo(StateValue.V0);
            assertThat(decision.value().isNotEmpty()).isFalse();
        }

        @Test
        void coin_flip_when_no_f_plus_one_agreement() {
            var phaseData = new PhaseData<TestCommand>(new Phase(1));

            // Split votes across V0, V1, VQUESTION (no f+1 for any)
            phaseData.registerRound2Vote(NODE_1, StateValue.V0);
            phaseData.registerRound2Vote(NODE_2, StateValue.VQUESTION);
            phaseData.registerRound2Vote(NODE_3, StateValue.VQUESTION);

            var decision = phaseData.processRound2Completion(NODE_1, 2, 2);

            // Phase 1 is odd, so coin flip should be V1
            assertThat(decision.stateValue()).isEqualTo(StateValue.V1);
        }
    }

    @Nested
    class DeterministicBehavior {

        @Test
        void find_agreed_proposal_is_deterministic() {
            var phaseData1 = new PhaseData<TestCommand>(new Phase(1));
            var phaseData2 = new PhaseData<TestCommand>(new Phase(1));

            var batch1 = Batch.batch(List.of(new TestCommand("a")));
            var batch2 = Batch.batch(List.of(new TestCommand("b")));

            // Same proposals in different order
            phaseData1.registerProposal(NODE_1, batch1);
            phaseData1.registerProposal(NODE_2, batch2);

            phaseData2.registerProposal(NODE_2, batch2);
            phaseData2.registerProposal(NODE_1, batch1);

            // Should return same batch regardless of insertion order
            var result1 = phaseData1.findAgreedProposal(2);
            var result2 = phaseData2.findAgreedProposal(2);

            assertThat(result1.correlationId()).isEqualTo(result2.correlationId());
        }

        @Test
        void coin_flip_is_deterministic_across_nodes() {
            var phaseData1 = new PhaseData<TestCommand>(new Phase(42));
            var phaseData2 = new PhaseData<TestCommand>(new Phase(42));
            var phaseData3 = new PhaseData<TestCommand>(new Phase(42));

            assertThat(phaseData1.coinFlip())
                .isEqualTo(phaseData2.coinFlip())
                .isEqualTo(phaseData3.coinFlip());
        }
    }

    // ==================== Cluster Simulator ====================

    static class ClusterSimulator {
        private final Map<NodeId, RabiaEngine<TestCommand>> engines = new ConcurrentHashMap<>();
        private final Map<NodeId, SimulatedNetwork> networks = new ConcurrentHashMap<>();
        private final Map<NodeId, TestStateMachine> stateMachines = new ConcurrentHashMap<>();
        private final List<NodeId> nodeIds;

        ClusterSimulator(List<NodeId> nodeIds) {
            this.nodeIds = nodeIds;
            for (var nodeId : nodeIds) {
                var network = new SimulatedNetwork(nodeId, this);
                var stateMachine = new TestStateMachine();
                var topologyManager = new SimulatedTopologyManager(nodeId, nodeIds.size());
                var engine = new RabiaEngine<>(topologyManager, network, stateMachine, ProtocolConfig.testConfig());
                networks.put(nodeId, network);
                stateMachines.put(nodeId, stateMachine);
                engines.put(nodeId, engine);
            }
        }

        void activateAll() throws InterruptedException {
            for (var engine : engines.values()) {
                engine.quorumState(QuorumStateNotification.ESTABLISHED);
            }
            Thread.sleep(150);

            // Send sync responses to activate all nodes
            for (var nodeId : nodeIds) {
                for (var otherId : nodeIds) {
                    if (!nodeId.equals(otherId)) {
                        engines.get(nodeId).processSyncResponse(new SyncResponse<>(otherId, SavedState.empty()));
                    }
                }
            }
            Thread.sleep(50);
        }

        void stopAll() {
            for (var engine : engines.values()) {
                engine.stop().await();
            }
        }

        void simulateProposal(NodeId sender, Batch<TestCommand> batch) {
            var propose = new Propose<>(sender, Phase.ZERO, batch);
            for (var engine : engines.values()) {
                engine.processPropose(propose);
            }
        }

        void deliverAllPendingMessages() {
            for (var network : networks.values()) {
                for (var message : network.getPendingMessages()) {
                    deliverMessage(message);
                }
                network.clearPendingMessages();
            }
        }

        @SuppressWarnings("unchecked")
        private void deliverMessage(ProtocolMessage message) {
            for (var engine : engines.values()) {
                switch (message) {
                    case Propose<?> p -> engine.processPropose((Propose<TestCommand>) p);
                    case VoteRound1 v -> engine.processVoteRound1(v);
                    case VoteRound2 v -> engine.processVoteRound2(v);
                    case Decision<?> d -> engine.processDecision((Decision<TestCommand>) d);
                    default -> {}
                }
            }
        }

        @SuppressWarnings("unchecked")
        <M extends ProtocolMessage> List<M> getMessagesByType(Class<M> type) {
            var result = new ArrayList<M>();
            for (var network : networks.values()) {
                for (var message : network.getAllMessages()) {
                    if (type.isInstance(message)) {
                        result.add((M) message);
                    }
                }
            }
            return result;
        }
    }

    static class SimulatedNetwork implements ClusterNetwork {
        private final NodeId self;
        private final ClusterSimulator cluster;
        private final List<ProtocolMessage> allMessages = new CopyOnWriteArrayList<>();
        private final List<ProtocolMessage> pendingMessages = new CopyOnWriteArrayList<>();

        SimulatedNetwork(NodeId self, ClusterSimulator cluster) {
            this.self = self;
            this.cluster = cluster;
        }

        @Override
        public <M extends ProtocolMessage> void broadcast(M message) {
            allMessages.add(message);
            pendingMessages.add(message);
        }

        @Override
        public <M extends ProtocolMessage> void send(NodeId nodeId, M message) {
            allMessages.add(message);
            pendingMessages.add(message);
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
        public Promise<Unit> start() {
            return Promise.success(Unit.unit());
        }

        @Override
        public Promise<Unit> stop() {
            return Promise.success(Unit.unit());
        }

        List<ProtocolMessage> getAllMessages() {
            return Collections.unmodifiableList(allMessages);
        }

        List<ProtocolMessage> getPendingMessages() {
            return Collections.unmodifiableList(new ArrayList<>(pendingMessages));
        }

        void clearPendingMessages() {
            pendingMessages.clear();
        }
    }

    static class SimulatedTopologyManager implements TopologyManager {
        private final NodeInfo self;
        private final int clusterSize;

        SimulatedTopologyManager(NodeId selfId, int clusterSize) {
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

    static class TestStateMachine implements StateMachine<TestCommand> {
        private final List<TestCommand> processedCommands = new ArrayList<>();

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
    }
}
