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

import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Synchronous.Decision;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Synchronous.VoteRound1;
import org.pragmatica.lang.Option;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.pragmatica.consensus.rabia.Batch.emptyBatch;

/// Data structure to hold all state related to a specific consensus phase.
///
/// This class tracks proposals, round 1 votes, round 2 votes, and decision state
/// for a single phase of the Rabia consensus protocol.
///
/// @param <C> Command type
final class PhaseData<C extends Command> {
    private final Phase phase;
    private final Map<NodeId, Batch<C>> proposals = new ConcurrentHashMap<>();
    private final Map<NodeId, StateValue> round1Votes = new ConcurrentHashMap<>();
    private final Map<NodeId, StateValue> round2Votes = new ConcurrentHashMap<>();
    private final AtomicBoolean decided = new AtomicBoolean(false);

    PhaseData(Phase phase) {
        this.phase = phase;
    }

    Phase phase() {
        return phase;
    }

    // ==================== Intent-Revealing API ====================
    /// Registers a proposal from a node. Idempotent - first proposal wins.
    void registerProposal(NodeId node, Batch<C> batch) {
        proposals.putIfAbsent(node, batch);
    }

    /// Checks if a node has already voted in round 1.
    boolean hasVotedRound1(NodeId node) {
        return round1Votes.containsKey(node);
    }

    /// Registers a round 1 vote from a node.
    void registerRound1Vote(NodeId node, StateValue value) {
        round1Votes.put(node, value);
    }

    /// Checks if a node has already voted in round 2.
    boolean hasVotedRound2(NodeId node) {
        return round2Votes.containsKey(node);
    }

    /// Registers a round 2 vote from a node.
    void registerRound2Vote(NodeId node, StateValue value) {
        round2Votes.put(node, value);
    }

    /// Checks if a decision has been made for this phase.
    boolean isDecided() {
        return decided.get();
    }

    /// Attempts to mark this phase as decided. Returns true if successful
    /// (was not already decided), false if already decided.
    boolean tryMarkDecided() {
        return decided.compareAndSet(false, true);
    }

    /// Returns the number of proposals collected.
    int proposalCount() {
        return proposals.size();
    }

    /// Checks if we have collected proposals from a majority of nodes.
    boolean hasQuorumProposals(int quorumSize) {
        return proposals.size() >= quorumSize;
    }

    // ==================== Voting Logic ====================
    /// Checks if we have collected votes from a majority of nodes in round 1.
    boolean hasRound1MajorityVotes(int quorumSize) {
        return round1Votes.size() >= quorumSize;
    }

    /// Checks if we have collected votes from a majority of nodes in round 2.
    boolean hasRound2MajorityVotes(int quorumSize) {
        return round2Votes.size() >= quorumSize;
    }

    /// Finds the agreed proposal when a V1 decision is made.
    /// Returns the batch that has the most proposals (quorum support expected),
    /// with deterministic tiebreaker by correlationId for consistency across nodes.
    Batch<C> findAgreedProposal(int quorumSize) {
        if (proposals.isEmpty()) {
            return emptyBatch();
        }
        // Group batches by correlationId and count
        var batchesByCorrelationId = proposals.values()
                                              .stream()
                                              .filter(Batch::isNotEmpty)
                                              .collect(Collectors.groupingBy(Batch::correlationId));
        // Find the correlationId with most proposals (should have quorum for V1 decision)
        // Use correlationId as tiebreaker for determinism across nodes
        return batchesByCorrelationId.entrySet()
                                     .stream()
                                     .max(Comparator.<Map.Entry<CorrelationId, List<Batch<C>>>> comparingInt(e -> e.getValue()
                                                                                                                   .size())
                                                    .thenComparing(e -> e.getKey()
                                                                         .id()))
                                     .map(e -> e.getValue()
                                                .getFirst())
                                     .orElse(emptyBatch());
    }

    /// Evaluates the initial round 1 vote based on collected proposals.
    /// Per Rabia spec: vote V1 if a majority of nodes proposed the same batch, else V0.
    ///
    /// This should only be called after hasQuorumProposals() returns true.
    VoteRound1 evaluateInitialVote(NodeId self, int quorumSize) {
        // Count proposals by correlationId to find if any batch has quorum support
        var countByCorrelationId = proposals.values()
                                            .stream()
                                            .filter(Batch::isNotEmpty)
                                            .collect(Collectors.groupingBy(Batch::correlationId,
                                                                           Collectors.counting()));
        // Check if any correlationId has quorum support
        boolean hasQuorumAgreement = countByCorrelationId.values()
                                                         .stream()
                                                         .anyMatch(count -> count >= quorumSize);
        var stateValue = hasQuorumAgreement
                         ? StateValue.V1
                         : StateValue.V0;
        return new VoteRound1(self, phase, stateValue);
    }

    /// Evaluates the round 2 vote based on round 1 voting results.
    /// Per Rabia spec: if majority voted same value, vote that; else vote VQUESTION.
    StateValue evaluateRound2Vote(int quorumSize) {
        for (var value : List.of(StateValue.V0, StateValue.V1)) {
            if (countRound1VotesForValue(value) >= quorumSize) {
                return value;
            }
        }
        return StateValue.VQUESTION;
    }

    /// Counts round 1 votes for a specific state value.
    int countRound1VotesForValue(StateValue value) {
        return (int) round1Votes.values()
                               .stream()
                               .filter(v -> v == value)
                               .count();
    }

    /// Checks if we have a super-majority agreement on a single value in Round 1.
    /// If true, we can skip Round 2 and decide immediately (fast path).
    ///
    /// @param superMajoritySize the n - f threshold
    /// @return the agreed value if super-majority exists, empty otherwise
    Option<StateValue> getSuperMajorityRound1Value(int superMajoritySize) {
        for (var value : List.of(StateValue.V0, StateValue.V1)) {
            if (countRound1VotesForValue(value) >= superMajoritySize) {
                return Option.some(value);
            }
        }
        return Option.none();
    }

    /// Counts round 2 votes for a specific state value.
    int countRound2VotesForValue(StateValue value) {
        return (int) round2Votes.values()
                               .stream()
                               .filter(v -> v == value)
                               .count();
    }

    /// Processes round 2 completion and determines the decision.
    /// Per Rabia spec:
    /// 1. If f+1 nodes voted V1, decide V1
    /// 2. If f+1 nodes voted V0, decide V0
    /// 3. Otherwise, use deterministic coin flip
    Decision<C> processRound2Completion(NodeId self, int fPlusOneSize, int quorumSize) {
        if (countRound2VotesForValue(StateValue.V1) >= fPlusOneSize) {
            return new Decision<>(self, phase, StateValue.V1, findAgreedProposal(quorumSize));
        }
        if (countRound2VotesForValue(StateValue.V0) >= fPlusOneSize) {
            return new Decision<>(self, phase, StateValue.V0, emptyBatch());
        }
        var decision = coinFlip();
        var batch = decision == StateValue.V1
                    ? findAgreedProposal(quorumSize)
                    : Batch.<C>emptyBatch();
        return new Decision<>(self, phase, decision, batch);
    }

    /// Gets a deterministic coin flip value for a phase.
    /// Must be deterministic across all nodes for consensus correctness.
    /// Uses bit-based check to avoid Math.abs(Long.MIN_VALUE) returning negative.
    StateValue coinFlip() {
        long seed = phase.value();
        return (seed & 1) == 0
               ? StateValue.V0
               : StateValue.V1;
    }
}
