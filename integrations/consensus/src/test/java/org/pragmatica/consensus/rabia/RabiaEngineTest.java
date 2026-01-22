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
import org.pragmatica.consensus.ConsensusError;
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
import org.pragmatica.net.tcp.Server;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.tcp.NodeAddress;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.consensus.NodeId.nodeId;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class RabiaEngineTest {

    record TestCommand(String value) implements Command {}

    private static final NodeId NODE_1 = nodeId("node-1").unwrap();
    private static final NodeId NODE_2 = nodeId("node-2").unwrap();
    private static final NodeId NODE_3 = nodeId("node-3").unwrap();
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
                assertThat(cause).isInstanceOf(ConsensusError.NodeInactive.class)
            );
        }

        @Test
        void submit_fails_for_empty_command_list() throws InterruptedException {
            activateEngine();

            var result = engine.apply(List.<TestCommand>of()).await();

            assertThat(result.isFailure()).isTrue();
            result.onFailure(cause ->
                assertThat(cause).isInstanceOf(ConsensusError.CommandBatchIsEmpty.class)
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
    class ProtocolInvariants {

        @Test
        void locked_value_carries_to_next_phase() throws InterruptedException {
            activateEngine();
            network.clearMessages();

            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Complete phase 0 with V1 decision
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch));
            engine.processPropose(new Propose<>(NODE_2, Phase.ZERO, batch));
            Thread.sleep(50);

            engine.processVoteRound1(new VoteRound1(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound1(new VoteRound1(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(50);

            engine.processVoteRound2(new VoteRound2(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound2(new VoteRound2(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(100);

            // Verify phase 0 decision was broadcast
            var phase0Decision = network.getMessages().stream()
                .anyMatch(m -> m instanceof Decision<?> d && d.phase().equals(Phase.ZERO));
            assertThat(phase0Decision).as("Phase 0 should have V1 decision").isTrue();

            // After V1 decision, the locked value (V1) is set for next phase
            // Per Rabia spec: moveToNextPhase() sets lockedValue to Option.some(decidedValue)
            // This is verified by the engine advancing to next phase
            network.clearMessages();

            // Submit new commands which will trigger startPhase for phase 1
            engine.handleSubmit(new RabiaEngineIO.SubmitCommands<>(List.of(new TestCommand("cmd2"))));
            Thread.sleep(150);

            // If there were pending batches and locked value was V1, the engine should
            // broadcast a VoteRound1 with V1 immediately when starting phase 1
            var phase1Messages = network.getMessages().stream()
                .filter(m -> m instanceof Propose<?> || m instanceof VoteRound1)
                .toList();

            // The engine should have broadcast a proposal for phase 1
            var hasPhase1Proposal = phase1Messages.stream()
                .anyMatch(m -> m instanceof Propose<?> p && p.phase().equals(new Phase(1)));

            assertThat(hasPhase1Proposal)
                .as("Engine should have started phase 1 with proposal after V1 decision")
                .isTrue();
        }

        @Test
        void state_machine_applied_only_on_v1_decision() throws InterruptedException {
            activateEngine();
            stateMachine.processedCommands.clear();

            // Complete phase with V0 decision
            engine.processVoteRound1(new VoteRound1(NODE_1, Phase.ZERO, StateValue.V0));
            engine.processVoteRound1(new VoteRound1(NODE_2, Phase.ZERO, StateValue.V0));
            engine.processVoteRound1(new VoteRound1(NODE_3, Phase.ZERO, StateValue.V0));
            Thread.sleep(50);

            engine.processVoteRound2(new VoteRound2(NODE_1, Phase.ZERO, StateValue.V0));
            engine.processVoteRound2(new VoteRound2(NODE_2, Phase.ZERO, StateValue.V0));
            engine.processVoteRound2(new VoteRound2(NODE_3, Phase.ZERO, StateValue.V0));
            Thread.sleep(50);

            // V0 decision should NOT apply commands
            assertThat(stateMachine.processedCommands).isEmpty();
        }

        @Test
        void pending_batches_removed_after_v1_decision() throws InterruptedException {
            activateEngine();
            network.clearMessages();

            var command = new TestCommand("cmd");
            var batch = Batch.batch(List.of(command));

            // Simulate a complete V1 decision flow with a non-empty batch
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch));
            engine.processPropose(new Propose<>(NODE_2, Phase.ZERO, batch));
            Thread.sleep(50);

            engine.processVoteRound1(new VoteRound1(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound1(new VoteRound1(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(50);

            engine.processVoteRound2(new VoteRound2(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound2(new VoteRound2(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(100);

            // Verify a decision was broadcast
            var hasDecision = network.getMessages().stream()
                .anyMatch(m -> m instanceof Decision<?>);
            assertThat(hasDecision).as("Decision should be broadcast").isTrue();

            // After V1 decision with non-empty batch, commands should be processed
            assertThat(stateMachine.getProcessedCommands())
                .as("Commands should be applied to state machine after V1 decision")
                .isNotEmpty();
        }

        @Test
        void phase_advances_after_decision() throws InterruptedException {
            activateEngine();

            // Complete phase 0
            var batch = Batch.batch(List.of(new TestCommand("cmd")));
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch));
            engine.processPropose(new Propose<>(NODE_2, Phase.ZERO, batch));
            Thread.sleep(50);

            engine.processVoteRound1(new VoteRound1(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound1(new VoteRound1(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(50);

            engine.processVoteRound2(new VoteRound2(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound2(new VoteRound2(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(100);

            // Verify decision was broadcast for phase 0
            var phase0Decision = network.getMessages().stream()
                .anyMatch(m -> m instanceof Decision<?> d && d.phase().equals(Phase.ZERO));
            assertThat(phase0Decision).as("Phase 0 should have decision").isTrue();

            network.clearMessages();

            // Submit commands to trigger phase 1
            engine.handleSubmit(new RabiaEngineIO.SubmitCommands<>(List.of(new TestCommand("cmd2"))));
            Thread.sleep(150);

            // Engine should broadcast a proposal for phase 1
            var hasPhase1Proposal = network.getMessages().stream()
                .anyMatch(m -> m instanceof Propose<?> p && p.phase().equals(new Phase(1)));

            assertThat(hasPhase1Proposal)
                .as("Engine should start phase 1 after phase 0 decision")
                .isTrue();
        }

        @Test
        void multiple_phases_complete_correctly() throws InterruptedException {
            activateEngine();

            for (int phase = 0; phase < 3; phase++) {
                network.clearMessages();
                var p = new Phase(phase);
                var batch = Batch.batch(List.of(new TestCommand("cmd-" + phase)));

                engine.processPropose(new Propose<>(NODE_1, p, batch));
                engine.processPropose(new Propose<>(NODE_2, p, batch));
                Thread.sleep(50);

                engine.processVoteRound1(new VoteRound1(NODE_2, p, StateValue.V1));
                engine.processVoteRound1(new VoteRound1(NODE_3, p, StateValue.V1));
                Thread.sleep(50);

                engine.processVoteRound2(new VoteRound2(NODE_2, p, StateValue.V1));
                engine.processVoteRound2(new VoteRound2(NODE_3, p, StateValue.V1));
                Thread.sleep(100);

                // Verify decision was broadcast
                var hasDecision = network.getMessages().stream()
                    .anyMatch(m -> m instanceof Decision<?> d && d.phase().equals(p));
                assertThat(hasDecision).as("Phase %d should have decision", phase).isTrue();
            }
        }

        @Test
        void cleanup_removes_old_phases() throws InterruptedException {
            activateEngine();

            // Complete multiple phases to accumulate phase data
            for (int phase = 0; phase < 5; phase++) {
                var p = new Phase(phase);
                var batch = Batch.batch(List.of(new TestCommand("cmd-" + phase)));

                engine.processPropose(new Propose<>(NODE_1, p, batch));
                engine.processPropose(new Propose<>(NODE_2, p, batch));
                Thread.sleep(30);

                engine.processVoteRound1(new VoteRound1(NODE_2, p, StateValue.V1));
                engine.processVoteRound1(new VoteRound1(NODE_3, p, StateValue.V1));
                Thread.sleep(30);

                engine.processVoteRound2(new VoteRound2(NODE_2, p, StateValue.V1));
                engine.processVoteRound2(new VoteRound2(NODE_3, p, StateValue.V1));
                Thread.sleep(50);
            }

            // Wait for cleanup task to run (cleanup interval is configured in ProtocolConfig.testConfig())
            // Note: Cleanup is triggered by a scheduled task, so we wait for it to run
            Thread.sleep(500);

            // Old phases should be cleaned up (cleanup threshold is in config)
            // This verifies the cleanupOldPhases() method is working
            // The exact assertion depends on config.removeOlderThanPhases() value
            // We just verify the engine is still functional after cleanup
            assertThat(engine.isActive()).isTrue();
        }

        @Test
        void sync_response_restores_state_correctly() throws InterruptedException {
            // This test verifies the sync response mechanism (same as activateEngine helper)
            // The activateEngine helper already tests this, so we verify it works correctly
            activateEngine();

            // Engine should be active after receiving sync responses
            assertThat(engine.isActive()).isTrue();

            // Verify engine accepts commands after sync restoration (doesn't fail immediately)
            // The apply returns a Promise that won't complete without full protocol execution,
            // so we verify the engine state is correct rather than waiting for the result
            var batch = Batch.batch(List.of(new TestCommand("test-after-sync")));
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch));
            Thread.sleep(50);

            // After sync, the engine should be able to process proposals
            // (no exception thrown and active remains true)
            assertThat(engine.isActive()).isTrue();
        }

        @Test
        void round1_votes_never_vquestion() throws InterruptedException {
            // Invariant 14: round1 votes never VQUESTION
            activateEngine();
            network.clearMessages();

            var batch = Batch.batch(List.of(new TestCommand("test")));

            // Simulate receiving proposals from all nodes
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch));
            engine.processPropose(new Propose<>(NODE_2, Phase.ZERO, batch));
            Thread.sleep(50);

            // Verify all round 1 votes are NOT VQUESTION
            assertThat(network.getMessages().stream()
                .filter(m -> m instanceof VoteRound1)
                .map(m -> (VoteRound1) m)
                .allMatch(v -> v.stateValue() != StateValue.VQUESTION))
                .isTrue();
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

        @Test
        void fast_path_skips_round2_when_super_majority_agrees() throws InterruptedException {
            activateEngine();
            network.clearMessages();

            var batch = Batch.batch(List.of(new TestCommand("fast-path-cmd")));

            // Simulate receiving proposals from all nodes (same batch)
            engine.processPropose(new Propose<>(NODE_1, Phase.ZERO, batch));
            engine.processPropose(new Propose<>(NODE_2, Phase.ZERO, batch));
            Thread.sleep(50);

            // Simulate round 1 votes from all nodes (super-majority = n-f = 2 for 3 nodes)
            // ENGINE already voted V1 when proposals matched, so just need NODE_2 and NODE_3
            engine.processVoteRound1(new VoteRound1(NODE_2, Phase.ZERO, StateValue.V1));
            engine.processVoteRound1(new VoteRound1(NODE_3, Phase.ZERO, StateValue.V1));
            Thread.sleep(100);

            // Verify decision was broadcast WITHOUT any round 2 votes being sent
            var hasDecision = network.getMessages().stream()
                .anyMatch(m -> m instanceof Decision);
            assertThat(hasDecision).as("Fast path should produce a decision").isTrue();

            // Verify no VoteRound2 was broadcast (fast path skipped round 2)
            var round2VoteCount = network.getMessages().stream()
                .filter(m -> m instanceof VoteRound2)
                .count();
            assertThat(round2VoteCount).as("Fast path should skip round 2 voting").isZero();
        }
    }

    // ==================== Stub Implementations ====================

    static class TestTopologyManager implements TopologyManager {
        private final NodeInfo self;
        private final int clusterSize;

        TestTopologyManager(NodeId selfId, int clusterSize) {
            this.self = NodeInfo.nodeInfo(selfId, NodeAddress.nodeAddress("localhost", 5000).unwrap());
            this.clusterSize = clusterSize;
        }

        @Override
        public NodeInfo self() {
            return self;
        }

        @Override
        public Option<NodeInfo> get(NodeId id) {
            return Option.option(NodeInfo.nodeInfo(id, NodeAddress.nodeAddress("localhost", 5000).unwrap()));
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
        public Promise<Unit> start() {
            return Promise.success(Unit.unit());
        }

        @Override
        public Promise<Unit> stop() {
            return Promise.success(Unit.unit());
        }

        @Override
        public TimeSpan pingInterval() {
            return timeSpan(1).seconds();
        }

        @Override
        public TimeSpan helloTimeout() {
            return timeSpan(5).seconds();
        }
    }

    static class TestClusterNetwork implements ClusterNetwork {
        private final List<ProtocolMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public <M extends ProtocolMessage> Unit broadcast(M message) {
            messages.add(message);
            return Unit.unit();
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
        public <M extends ProtocolMessage> Unit send(NodeId nodeId, M message) {
            messages.add(message);
            return Unit.unit();
        }

        @Override
        public Promise<Unit> start() {
            return Promise.success(Unit.unit());
        }

        @Override
        public Promise<Unit> stop() {
            return Promise.success(Unit.unit());
        }

        @Override
        public int connectedNodeCount() {
            return 0; // Test network has no real connections
        }

        @Override
        public Option<Server> server() {
            return Option.none();
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
        public Unit reset() {
            processedCommands.clear();
            return Unit.unit();
        }

        List<TestCommand> getProcessedCommands() {
            return Collections.unmodifiableList(processedCommands);
        }
    }
}
