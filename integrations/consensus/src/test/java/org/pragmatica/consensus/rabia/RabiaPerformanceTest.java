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
import org.junit.jupiter.api.Tag;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.pragmatica.consensus.NodeId.nodeId;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Performance tests for Rabia consensus.
 * Measures throughput in two scenarios:
 * 1. Single proposer (leader-like pattern)
 * 2. Multiple proposers (contention pattern)
 */
@Tag("Benchmark")
class RabiaPerformanceTest {

    record TestCommand(int id) implements Command {}

    private static final int CLUSTER_SIZE = 5;
    private static final int WARMUP_ROUNDS = 100;
    private static final int BENCHMARK_ROUNDS = 1000;

    private List<NodeId> nodeIds;
    private PerformanceCluster cluster;

    @BeforeEach
    void setUp() {
        nodeIds = new ArrayList<>();
        for (int i = 1; i <= CLUSTER_SIZE; i++) {
            nodeIds.add(nodeId("node-" + i));
        }
        cluster = new PerformanceCluster(nodeIds);
    }

    @AfterEach
    void tearDown() {
        cluster.stopAll();
    }

    @Test
    void single_proposer_throughput() throws InterruptedException {
        cluster.activateAll();
        var proposer = nodeIds.getFirst();

        // Warmup
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            runConsensusRound(proposer, i);
        }
        cluster.resetCounters();

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            runConsensusRound(proposer, WARMUP_ROUNDS + i);
        }
        long endTime = System.nanoTime();

        printResults("Single Proposer", startTime, endTime, BENCHMARK_ROUNDS);
    }

    @Test
    void multiple_proposers_throughput() throws InterruptedException {
        cluster.activateAll();

        // Warmup
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            var proposer = nodeIds.get(i % CLUSTER_SIZE);
            runConsensusRound(proposer, i);
        }
        cluster.resetCounters();

        // Benchmark - round-robin proposers
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            var proposer = nodeIds.get(i % CLUSTER_SIZE);
            runConsensusRound(proposer, WARMUP_ROUNDS + i);
        }
        long endTime = System.nanoTime();

        printResults("Multiple Proposers (round-robin)", startTime, endTime, BENCHMARK_ROUNDS);
    }

    @Test
    void concurrent_proposers_throughput() throws InterruptedException {
        cluster.activateAll();

        // Warmup
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            runConcurrentConsensusRound(i);
        }
        cluster.resetCounters();

        // Benchmark - all nodes propose simultaneously
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ROUNDS; i++) {
            runConcurrentConsensusRound(WARMUP_ROUNDS + i);
        }
        long endTime = System.nanoTime();

        printResults("Concurrent Proposers (all nodes)", startTime, endTime, BENCHMARK_ROUNDS);
    }

    private void runConsensusRound(NodeId proposer, int commandId) {
        var batch = Batch.batch(List.of(new TestCommand(commandId)));
        var phase = new Phase(commandId);

        // All nodes propose the same batch (simulating leader replication)
        for (var nodeId : nodeIds) {
            var propose = new Propose<>(nodeId, phase, batch);
            cluster.broadcastToAll(propose);
        }

        // Process all messages until stable (proposals -> R1 -> R2 -> decision)
        for (int i = 0; i < 4; i++) {
            cluster.processAllPendingMessages();
        }
    }

    private void runConcurrentConsensusRound(int commandId) {
        var phase = new Phase(commandId);

        // All nodes propose different batches
        for (int i = 0; i < nodeIds.size(); i++) {
            var proposer = nodeIds.get(i);
            var batch = Batch.batch(List.of(new TestCommand(commandId * 100 + i)));
            var propose = new Propose<>(proposer, phase, batch);
            cluster.broadcastToAll(propose);
        }

        // Process all messages until stable
        for (int i = 0; i < 4; i++) {
            cluster.processAllPendingMessages();
        }
    }

    private void printResults(String scenario, long startNanos, long endNanos, int rounds) {
        double durationMs = (endNanos - startNanos) / 1_000_000.0;
        double durationSec = durationMs / 1000.0;
        double opsPerSec = rounds / durationSec;
        double avgLatencyUs = (endNanos - startNanos) / 1000.0 / rounds;

        System.out.println();
        System.out.println("=== " + scenario + " ===");
        System.out.println("Cluster size:     " + CLUSTER_SIZE + " nodes");
        System.out.println("Rounds:           " + rounds);
        System.out.println("Total time:       " + String.format("%.2f", durationMs) + " ms");
        System.out.println("Throughput:       " + String.format("%.0f", opsPerSec) + " decisions/sec");
        System.out.println("Avg latency:      " + String.format("%.2f", avgLatencyUs) + " µs/decision");
        System.out.println("Messages sent:    " + cluster.getTotalMessagesSent());
        System.out.println("Decisions made:   " + cluster.getTotalDecisions());
    }

    // ==================== Performance-Optimized Cluster ====================

    static class PerformanceCluster {
        private final Map<Phase, PhaseData<TestCommand>> phaseDataMap = new ConcurrentHashMap<>();
        private final List<NodeId> nodeIds;
        private final int quorumSize;
        private final int fPlusOne;
        private final AtomicLong messagesSent = new AtomicLong();
        private final AtomicInteger decisions = new AtomicInteger();
        private final List<ProtocolMessage> pendingMessages = new ArrayList<>();

        PerformanceCluster(List<NodeId> nodeIds) {
            this.nodeIds = nodeIds;
            this.quorumSize = nodeIds.size() / 2 + 1;
            this.fPlusOne = nodeIds.size() - quorumSize + 1;
        }

        void activateAll() {
            // No-op for direct simulation
        }

        void stopAll() {
            phaseDataMap.clear();
        }

        void resetCounters() {
            messagesSent.set(0);
            decisions.set(0);
        }

        long getTotalMessagesSent() {
            return messagesSent.get();
        }

        int getTotalDecisions() {
            return decisions.get();
        }

        @SuppressWarnings("unchecked")
        void broadcastToAll(ProtocolMessage message) {
            messagesSent.addAndGet(nodeIds.size());
            pendingMessages.add(message);
        }

        void processAllPendingMessages() {
            var toProcess = new ArrayList<>(pendingMessages);
            pendingMessages.clear();

            for (var message : toProcess) {
                processMessage(message);
            }
        }

        @SuppressWarnings("unchecked")
        private void processMessage(ProtocolMessage message) {
            switch (message) {
                case Propose<?> propose -> handlePropose((Propose<TestCommand>) propose);
                case VoteRound1 vote -> handleVoteRound1(vote);
                case VoteRound2 vote -> handleVoteRound2(vote);
                case Decision<?> decision -> handleDecision((Decision<TestCommand>) decision);
                default -> {}
            }
        }

        private void handlePropose(Propose<TestCommand> propose) {
            var phaseData = getOrCreatePhaseData(propose.phase());

            // Register proposal from sender
            phaseData.registerProposal(propose.sender(), propose.value());

            // Each node checks if it can vote
            for (var nodeId : nodeIds) {
                if (!phaseData.hasVotedRound1(nodeId) && phaseData.hasQuorumProposals(quorumSize)) {
                    var vote = phaseData.evaluateInitialVote(nodeId, quorumSize);
                    phaseData.registerRound1Vote(nodeId, vote.stateValue());
                    messagesSent.addAndGet(nodeIds.size());
                    pendingMessages.add(vote);
                }
            }
        }

        private void handleVoteRound1(VoteRound1 vote) {
            var phaseData = getOrCreatePhaseData(vote.phase());
            phaseData.registerRound1Vote(vote.sender(), vote.stateValue());

            // Each node checks if it can proceed to round 2
            for (var nodeId : nodeIds) {
                if (!phaseData.hasVotedRound2(nodeId) && phaseData.hasRound1MajorityVotes(quorumSize)) {
                    var round2Vote = phaseData.evaluateRound2Vote(quorumSize);
                    phaseData.registerRound2Vote(nodeId, round2Vote);
                    messagesSent.addAndGet(nodeIds.size());
                    pendingMessages.add(new VoteRound2(nodeId, vote.phase(), round2Vote));
                }
            }
        }

        private void handleVoteRound2(VoteRound2 vote) {
            var phaseData = getOrCreatePhaseData(vote.phase());
            phaseData.registerRound2Vote(vote.sender(), vote.stateValue());

            // Check if any node can make a decision
            if (!phaseData.isDecided() && phaseData.hasRound2MajorityVotes(quorumSize)) {
                var decision = phaseData.processRound2Completion(nodeIds.getFirst(), fPlusOne, quorumSize);
                if (phaseData.tryMarkDecided()) {
                    decisions.incrementAndGet();
                    messagesSent.addAndGet(nodeIds.size());
                    pendingMessages.add(decision);
                }
            }
        }

        private void handleDecision(Decision<TestCommand> decision) {
            var phaseData = getOrCreatePhaseData(decision.phase());
            phaseData.tryMarkDecided();
        }

        private PhaseData<TestCommand> getOrCreatePhaseData(Phase phase) {
            return phaseDataMap.computeIfAbsent(phase, PhaseData::new);
        }
    }
}
