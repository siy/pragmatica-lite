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

package org.pragmatica.consensus.rabia.helper;

import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.rabia.Batch;
import org.pragmatica.consensus.rabia.Phase;
import org.pragmatica.consensus.rabia.StateValue;
import org.pragmatica.lang.Option;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks global cluster state mirroring Ivy relations for invariant checking.
 */
public final class ClusterState<C extends Command> {
    private final Map<NodeId, Option<Batch<C>>> proposals = new HashMap<>();
    private final Map<NodeId, Map<Phase, StateValue>> round1Votes = new HashMap<>();
    private final Map<NodeId, Map<Phase, StateValue>> round2Votes = new HashMap<>();
    private final Map<NodeId, Map<Phase, StateValue>> decisionsBc = new HashMap<>();
    private final Map<NodeId, Map<Phase, Batch<C>>> decisionsFullVal = new HashMap<>();
    private final Map<NodeId, Set<Phase>> decisionsFullNoval = new HashMap<>();
    private final Map<Phase, StateValue> coinFlips = new HashMap<>();
    private final Map<NodeId, Option<Phase>> nodePhases = new HashMap<>();

    public ClusterState(List<NodeId> nodes) {
        for (var node : nodes) {
            proposals.put(node, Option.none());
            round1Votes.put(node, new HashMap<>());
            round2Votes.put(node, new HashMap<>());
            decisionsBc.put(node, new HashMap<>());
            decisionsFullVal.put(node, new HashMap<>());
            decisionsFullNoval.put(node, new HashSet<>());
            nodePhases.put(node, Option.none());
        }
    }

    // Mutators matching Ivy protocol actions
    public void propose(NodeId node, Batch<C> value) {
        proposals.put(node, Option.some(value));
    }

    public void voteRound1(NodeId node, Phase phase, StateValue value) {
        round1Votes.get(node).put(phase, value);
    }

    public void voteRound2(NodeId node, Phase phase, StateValue value) {
        round2Votes.get(node).put(phase, value);
    }

    public void decideBc(NodeId node, Phase phase, StateValue value) {
        decisionsBc.get(node).put(phase, value);
    }

    public void decideFullVal(NodeId node, Phase phase, Batch<C> value) {
        decisionsFullVal.get(node).put(phase, value);
    }

    public void decideFullNoval(NodeId node, Phase phase) {
        decisionsFullNoval.get(node).add(phase);
    }

    public void setCoin(Phase phase, StateValue value) {
        coinFlips.put(phase, value);
    }

    public void setInPhase(NodeId node, Phase phase) {
        nodePhases.put(node, Option.some(phase));
    }

    public void clearPhase(NodeId node) {
        nodePhases.put(node, Option.none());
    }

    // Accessors for invariant checking
    public Option<Batch<C>> getProposal(NodeId node) {
        return proposals.get(node);
    }

    public Map<Phase, StateValue> getRound1Votes(NodeId node) {
        return round1Votes.get(node);
    }

    public Map<Phase, StateValue> getRound2Votes(NodeId node) {
        return round2Votes.get(node);
    }

    public Map<Phase, StateValue> getDecisionsBc(NodeId node) {
        return decisionsBc.get(node);
    }

    public Map<Phase, Batch<C>> getDecisionsFullVal(NodeId node) {
        return decisionsFullVal.get(node);
    }

    public Set<Phase> getDecisionsFullNoval(NodeId node) {
        return decisionsFullNoval.get(node);
    }

    public Option<StateValue> getCoin(Phase phase) {
        return Option.option(coinFlips.get(phase));
    }

    public Option<Phase> getNodePhase(NodeId node) {
        return nodePhases.get(node);
    }

    public Set<NodeId> getAllNodes() {
        return proposals.keySet();
    }

    // Helpers for invariant checks
    public boolean hasVotedRound1(NodeId node, Phase phase) {
        return round1Votes.get(node).containsKey(phase);
    }

    public boolean hasVotedRound2(NodeId node, Phase phase) {
        return round2Votes.get(node).containsKey(phase);
    }

    public boolean hasDecisionBc(NodeId node, Phase phase) {
        return decisionsBc.get(node).containsKey(phase);
    }

    public int countRound1VotesForValue(Phase phase, StateValue value) {
        return (int) round1Votes.values().stream()
                                .filter(m -> m.containsKey(phase) && m.get(phase) == value)
                                .count();
    }

    public int countRound2VotesForValue(Phase phase, StateValue value) {
        return (int) round2Votes.values().stream()
                                .filter(m -> m.containsKey(phase) && m.get(phase) == value)
                                .count();
    }

    public Set<NodeId> nodesWithRound2VoteForValue(Phase phase, StateValue value) {
        var result = new HashSet<NodeId>();
        for (var entry : round2Votes.entrySet()) {
            if (entry.getValue().containsKey(phase) && entry.getValue().get(phase) == value) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public boolean isStarted(Phase phase) {
        return round1Votes.values().stream()
                          .anyMatch(m -> m.containsKey(phase));
    }

    /// Derived predicate: state_value_locked per spec
    /// A value is locked if no node has voted for a different value in round 1
    public boolean isValueLocked(Phase phase, StateValue value) {
        for (var nodeVotes : round1Votes.values()) {
            if (nodeVotes.containsKey(phase) && nodeVotes.get(phase) != value) {
                return false;
            }
        }
        return true; // vacuously true if no votes
    }

    /// Derived predicate: strong_state_value_locked per spec
    /// A value is strongly locked if at least one node voted for it and the value is locked
    public boolean isStrongValueLocked(Phase phase, StateValue value) {
        boolean hasVote = round1Votes.values().stream()
                                     .anyMatch(m -> m.containsKey(phase) && m.get(phase) == value);
        return hasVote && isValueLocked(phase, value);
    }

    /// Derived predicate: members_voted_rnd2 per spec
    public boolean quorumVotedRound2(Set<NodeId> quorum, Phase phase) {
        return quorum.stream().allMatch(n -> hasVotedRound2(n, phase));
    }

    /// Get all nodes that have voted in round 1 for a given phase
    public Set<NodeId> nodesWithRound1Vote(Phase phase) {
        var result = new HashSet<NodeId>();
        for (var entry : round1Votes.entrySet()) {
            if (entry.getValue().containsKey(phase)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /// Get all nodes that have voted in round 2 for a given phase
    public Set<NodeId> nodesWithRound2Vote(Phase phase) {
        var result = new HashSet<NodeId>();
        for (var entry : round2Votes.entrySet()) {
            if (entry.getValue().containsKey(phase)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /// Get all phases that have been started
    public Set<Phase> allStartedPhases() {
        var result = new HashSet<Phase>();
        for (var nodeVotes : round1Votes.values()) {
            result.addAll(nodeVotes.keySet());
        }
        return result;
    }

    /// Get all phases where decisions have been made
    public Set<Phase> allDecidedPhases() {
        var result = new HashSet<Phase>();
        for (var nodeDecisions : decisionsBc.values()) {
            result.addAll(nodeDecisions.keySet());
        }
        return result;
    }

    /// Check if any node has proposed
    public boolean anyNodeProposed() {
        return proposals.values().stream().anyMatch(Option::isPresent);
    }

    /// Get all proposed values
    public Set<Batch<C>> allProposedValues() {
        var result = new HashSet<Batch<C>>();
        for (var proposal : proposals.values()) {
            proposal.onPresent(result::add);
        }
        return result;
    }
}
