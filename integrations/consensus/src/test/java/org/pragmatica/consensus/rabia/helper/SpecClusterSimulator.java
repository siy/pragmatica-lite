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
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.List;
import java.util.Set;

/**
 * Deterministic model-checking style simulator for Rabia consensus protocol.
 * Tracks cluster state, validates invariants after each action, and provides
 * query methods matching Ivy spec predicates.
 */
public final class SpecClusterSimulator<C extends Command> {

    /// Error types for simulator operations
    public sealed interface SimulatorError extends Cause {
        record InvariantViolation(String invariantName, String details) implements SimulatorError {
            @Override
            public String message() {
                return "Invariant violation [" + invariantName + "]: " + details;
            }
        }

        record InvalidAction(String action, String reason) implements SimulatorError {
            @Override
            public String message() {
                return "Invalid action [" + action + "]: " + reason;
            }
        }

        record NodeNotFound(NodeId nodeId) implements SimulatorError {
            @Override
            public String message() {
                return "Node not found: " + nodeId.id();
            }
        }
    }

    private final ClusterConfiguration config;
    private final ClusterState<C> state;
    private final VotingHistoryRecorder<C> history;
    private boolean strictMode = true;

    public SpecClusterSimulator(ClusterConfiguration config) {
        this.config = config;
        this.state = new ClusterState<>(config.nodeIds());
        this.history = new VotingHistoryRecorder<>();
    }

    /// Create simulator with standard 3-node configuration
    public static <C extends Command> SpecClusterSimulator<C> threeNodeCluster() {
        return new SpecClusterSimulator<>(ClusterConfiguration.threeNodes());
    }

    /// Create simulator with 5-node configuration
    public static <C extends Command> SpecClusterSimulator<C> fiveNodeCluster() {
        return new SpecClusterSimulator<>(ClusterConfiguration.fiveNodes());
    }

    /// Enable or disable strict invariant checking (default: enabled)
    public SpecClusterSimulator<C> withStrictMode(boolean strict) {
        this.strictMode = strict;
        return this;
    }

    // ==================== Protocol Actions (matching Ivy spec) ====================

    /// Initial proposal from a node
    public Result<SpecClusterSimulator<C>> initialProposal(NodeId node, Batch<C> value) {
        return validateNode(node)
            .flatMap(_ -> validateNoExistingProposal(node))
            .map(_ -> {
                state.propose(node, value);
                history.recordProposal(node, value);
                return this;
            })
            .flatMap(this::checkInvariants);
    }

    /// Initial round 1 vote (Phase ZERO)
    public Result<SpecClusterSimulator<C>> initialVote1(NodeId node, StateValue vote) {
        return phaseRound1(node, Phase.ZERO, vote);
    }

    /// Round 1 vote at specified phase
    public Result<SpecClusterSimulator<C>> phaseRound1(NodeId node, Phase phase, StateValue vote) {
        return validateNode(node)
            .flatMap(_ -> validateRound1Vote(node, phase, vote))
            .map(_ -> {
                state.voteRound1(node, phase, vote);
                history.recordRound1Vote(node, phase, vote);
                return this;
            })
            .flatMap(this::checkInvariants);
    }

    /// Round 2 vote at specified phase
    public Result<SpecClusterSimulator<C>> phaseRound2(NodeId node, Phase phase, StateValue vote) {
        return validateNode(node)
            .flatMap(_ -> validateRound2Vote(node, phase))
            .map(_ -> {
                state.voteRound2(node, phase, vote);
                history.recordRound2Vote(node, phase, vote);
                return this;
            })
            .flatMap(this::checkInvariants);
    }

    /// Decision with value (decision_full_val)
    public Result<SpecClusterSimulator<C>> decideBcFullVal(NodeId node, Phase phase, Batch<C> value) {
        return validateNode(node)
            .flatMap(_ -> validateDecision(node, phase))
            .map(_ -> {
                state.decideBc(node, phase, StateValue.V1);
                state.decideFullVal(node, phase, value);
                history.recordDecision(node, phase, StateValue.V1);
                return this;
            })
            .flatMap(this::checkInvariants);
    }

    /// Decision without value (decision_full_noval)
    public Result<SpecClusterSimulator<C>> decideBcFullNoval(NodeId node, Phase phase) {
        return validateNode(node)
            .flatMap(_ -> validateDecision(node, phase))
            .map(_ -> {
                state.decideBc(node, phase, StateValue.V0);
                state.decideFullNoval(node, phase);
                history.recordDecision(node, phase, StateValue.V0);
                return this;
            })
            .flatMap(this::checkInvariants);
    }

    /// Coin flip for a phase
    public Result<SpecClusterSimulator<C>> coinFlip(Phase phase, StateValue value) {
        return validateCoinFlip(phase, value)
            .map(_ -> {
                state.setCoin(phase, value);
                history.recordCoinFlip(phase, value);
                return this;
            })
            .flatMap(this::checkInvariants);
    }

    // ==================== Query Methods (matching Ivy spec predicates) ====================

    /// Check if value is locked at phase (no conflicting votes)
    public boolean isValueLocked(Phase phase, StateValue value) {
        return state.isValueLocked(phase, value);
    }

    /// Check if value is strongly locked at phase (locked and at least one vote for it)
    public boolean isStrongValueLocked(Phase phase, StateValue value) {
        return state.isStrongValueLocked(phase, value);
    }

    /// Get round 1 vote for node at phase
    public Option<StateValue> getRound1Vote(NodeId node, Phase phase) {
        return Option.option(state.getRound1Votes(node).get(phase));
    }

    /// Get round 2 vote for node at phase
    public Option<StateValue> getRound2Vote(NodeId node, Phase phase) {
        return Option.option(state.getRound2Votes(node).get(phase));
    }

    /// Get decision for node at phase
    public Option<StateValue> getDecisionBc(NodeId node, Phase phase) {
        return Option.option(state.getDecisionsBc(node).get(phase));
    }

    /// Get decided value for node at phase
    public Option<Batch<C>> getDecisionFullVal(NodeId node, Phase phase) {
        return Option.option(state.getDecisionsFullVal(node).get(phase));
    }

    /// Check if node has decided no-value at phase
    public boolean hasDecisionFullNoval(NodeId node, Phase phase) {
        return state.getDecisionsFullNoval(node).contains(phase);
    }

    /// Get coin value for phase
    public Option<StateValue> getCoin(Phase phase) {
        return state.getCoin(phase);
    }

    /// Check if phase has been started (any round 1 vote exists)
    public boolean isPhaseStarted(Phase phase) {
        return state.isStarted(phase);
    }

    /// Check if quorum has voted in round 2 for phase
    public boolean quorumVotedRound2(Phase phase) {
        return state.nodesWithRound2Vote(phase).size() >= config.quorumSize();
    }

    /// Count round 1 votes for value at phase
    public int countRound1Votes(Phase phase, StateValue value) {
        return state.countRound1VotesForValue(phase, value);
    }

    /// Count round 2 votes for value at phase
    public int countRound2Votes(Phase phase, StateValue value) {
        return state.countRound2VotesForValue(phase, value);
    }

    /// Get all nodes that voted in round 2 for value at phase
    public Set<NodeId> nodesWithRound2VoteFor(Phase phase, StateValue value) {
        return state.nodesWithRound2VoteForValue(phase, value);
    }

    /// Get proposal for node
    public Option<Batch<C>> getProposal(NodeId node) {
        return state.getProposal(node);
    }

    /// Get all nodes
    public List<NodeId> getAllNodes() {
        return config.nodeIds();
    }

    /// Get cluster configuration
    public ClusterConfiguration getConfig() {
        return config;
    }

    /// Get current cluster state (for inspection)
    public ClusterState<C> getState() {
        return state;
    }

    /// Get history recorder
    public VotingHistoryRecorder<C> getHistory() {
        return history;
    }

    /// Format current state as string for debugging
    public String formatState() {
        var sb = new StringBuilder();
        sb.append("Cluster State:\n");
        sb.append("  Configuration: ").append(config.clusterSize()).append(" nodes, ")
          .append("quorum=").append(config.quorumSize()).append("\n");

        sb.append("\n  Proposals:\n");
        for (var node : config.nodeIds()) {
            state.getProposal(node).onPresent(b ->
                sb.append("    ").append(node.id()).append(": ").append(b.id().id()).append("\n")
            );
        }

        sb.append("\n  Phases:\n");
        for (var phase : state.allStartedPhases()) {
            sb.append("    Phase ").append(phase.value()).append(":\n");
            sb.append("      Round1: V1=").append(state.countRound1VotesForValue(phase, StateValue.V1))
              .append(", V0=").append(state.countRound1VotesForValue(phase, StateValue.V0)).append("\n");
            sb.append("      Round2: V1=").append(state.countRound2VotesForValue(phase, StateValue.V1))
              .append(", V0=").append(state.countRound2VotesForValue(phase, StateValue.V0))
              .append(", V?=").append(state.countRound2VotesForValue(phase, StateValue.VQUESTION)).append("\n");
            state.getCoin(phase).onPresent(c ->
                sb.append("      Coin: ").append(c).append("\n")
            );
        }

        return sb.toString();
    }

    // ==================== Validation Helpers ====================

    private Result<NodeId> validateNode(NodeId node) {
        if (!config.nodeIds().contains(node)) {
            return new SimulatorError.NodeNotFound(node).result();
        }
        return Result.success(node);
    }

    private Result<NodeId> validateNoExistingProposal(NodeId node) {
        if (state.getProposal(node).isPresent()) {
            return new SimulatorError.InvalidAction("initialProposal",
                "Node " + node.id() + " already has a proposal").result();
        }
        return Result.success(node);
    }

    private Result<NodeId> validateRound1Vote(NodeId node, Phase phase, StateValue vote) {
        if (vote == StateValue.VQUESTION) {
            return new SimulatorError.InvalidAction("phaseRound1",
                "Round 1 vote cannot be VQUESTION").result();
        }
        if (state.hasVotedRound1(node, phase)) {
            return new SimulatorError.InvalidAction("phaseRound1",
                "Node " + node.id() + " already voted in round 1 at phase " + phase.value()).result();
        }
        return Result.success(node);
    }

    private Result<NodeId> validateRound2Vote(NodeId node, Phase phase) {
        if (!state.hasVotedRound1(node, phase)) {
            return new SimulatorError.InvalidAction("phaseRound2",
                "Node " + node.id() + " must vote in round 1 before round 2").result();
        }
        if (state.hasVotedRound2(node, phase)) {
            return new SimulatorError.InvalidAction("phaseRound2",
                "Node " + node.id() + " already voted in round 2 at phase " + phase.value()).result();
        }
        return Result.success(node);
    }

    private Result<NodeId> validateDecision(NodeId node, Phase phase) {
        if (!state.hasVotedRound2(node, phase)) {
            return new SimulatorError.InvalidAction("decide",
                "Node " + node.id() + " must vote in round 2 before deciding").result();
        }
        if (state.hasDecisionBc(node, phase)) {
            return new SimulatorError.InvalidAction("decide",
                "Node " + node.id() + " already has a decision at phase " + phase.value()).result();
        }
        return Result.success(node);
    }

    private Result<Phase> validateCoinFlip(Phase phase, StateValue value) {
        if (value == StateValue.VQUESTION) {
            return new SimulatorError.InvalidAction("coinFlip",
                "Coin value cannot be VQUESTION").result();
        }
        if (state.getCoin(phase).isPresent()) {
            return new SimulatorError.InvalidAction("coinFlip",
                "Phase " + phase.value() + " already has a coin flip").result();
        }
        if (!state.isStarted(phase)) {
            return new SimulatorError.InvalidAction("coinFlip",
                "Phase " + phase.value() + " has not started").result();
        }
        return Result.success(phase);
    }

    private Result<SpecClusterSimulator<C>> checkInvariants(SpecClusterSimulator<C> simulator) {
        if (!strictMode) {
            return Result.success(simulator);
        }

        return InvariantChecker.checkAll(state, config)
                               .flatMap(results -> {
                                   for (var result : results) {
                                       if (!result.passed()) {
                                           return new SimulatorError.InvariantViolation(
                                               result.invariantName(),
                                               result.details()
                                           ).result();
                                       }
                                   }
                                   return Result.success(simulator);
                               });
    }

    // ==================== Bulk Operations for Testing ====================

    /// Execute a sequence of protocol actions
    public Result<SpecClusterSimulator<C>> executeActions(List<ProtocolAction<C>> actions) {
        Result<SpecClusterSimulator<C>> result = Result.success(this);
        for (var action : actions) {
            result = result.flatMap(sim -> executeAction(sim, action));
        }
        return result;
    }

    private Result<SpecClusterSimulator<C>> executeAction(SpecClusterSimulator<C> sim, ProtocolAction<C> action) {
        return switch (action) {
            case ProtocolAction.InitialProposal<C> a -> sim.initialProposal(a.node(), a.value());
            case ProtocolAction.InitialVote1<C> a -> sim.initialVote1(a.node(), a.vote());
            case ProtocolAction.PhaseRound1<C> a -> sim.phaseRound1(a.node(), a.phase(), a.vote());
            case ProtocolAction.PhaseRound2<C> a -> sim.phaseRound2(a.node(), a.phase(), a.vote());
            case ProtocolAction.DecideFullVal<C> a -> sim.decideBcFullVal(a.node(), a.phase(), a.value());
            case ProtocolAction.DecideFullNoval<C> a -> sim.decideBcFullNoval(a.node(), a.phase());
            case ProtocolAction.DecisionBc<C> a -> {
                // DecisionBc is handled through DecideFullVal or DecideFullNoval
                yield new SimulatorError.InvalidAction("DecisionBc",
                    "DecisionBc should use DecideFullVal or DecideFullNoval").result();
            }
            case ProtocolAction.CoinFlip<C> a -> sim.coinFlip(a.phase(), a.value());
        };
    }

    /// Reset simulator to initial state
    public SpecClusterSimulator<C> reset() {
        return new SpecClusterSimulator<C>(config).withStrictMode(strictMode);
    }
}
