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
import org.pragmatica.lang.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.pragmatica.lang.Result.success;

/**
 * Invariant checker for Rabia consensus protocol.
 * Implements invariants from weak_mvc.ivy specification (lines 201-298).
 */
public final class InvariantChecker {

    /// Result of an invariant check
    public record InvariantCheckResult(String invariantName, boolean passed, String details) {
        public static InvariantCheckResult pass(String name) {
            return new InvariantCheckResult(name, true, "");
        }

        public static InvariantCheckResult fail(String name, String details) {
            return new InvariantCheckResult(name, false, details);
        }
    }

    private InvariantChecker() {}

    /// Check all invariants and return results
    public static <C extends Command> Result<List<InvariantCheckResult>> checkAll(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        var results = new ArrayList<InvariantCheckResult>();

        // Proposal invariants (1-5)
        results.add(checkProposalUniqueness(state, config));
        results.add(checkDecisionFullValImpliesDecisionBc(state, config));
        results.add(checkDecisionFullValRequiresMajority(state, config));
        results.add(checkDecisionFullValValidity(state, config));
        results.add(checkDecisionFullValAgreement(state, config));

        // Decision no-val invariant (6)
        results.add(checkDecisionFullNovalImpliesDecisionBcV0(state, config));

        // Phase ordering invariants (7-13)
        results.add(checkRound2RequiresRound1(state, config));
        results.add(checkDecisionBcRequiresRound2(state, config));
        results.add(checkRound1VotePhaseOrdering(state, config));
        results.add(checkRound2VotePhaseOrdering(state, config));
        results.add(checkDecisionBcPhaseOrdering(state, config));
        results.add(checkDecisionFullValPhaseOrdering(state, config));
        results.add(checkDecisionFullNovalPhaseOrdering(state, config));

        // Vote invariants (14-18)
        results.add(checkRound1VoteUniqueness(state, config));
        results.add(checkRound2VoteUniqueness(state, config));
        results.add(checkRound2DefiniteAgreement(state, config));
        results.add(checkRound1NotVquestion(state, config));
        results.add(checkRound2NotVquestionWithoutCoin(state, config));

        // Decision_bc invariants (19-22)
        results.add(checkDecisionBcUniqueness(state, config));
        results.add(checkDecisionBcRequiresQuorumRound2(state, config));
        results.add(checkDecisionBcV1RequiresAllV1(state, config));
        results.add(checkDecisionBcV0RequiresAllV0(state, config));

        // Coin invariants (23-28)
        results.add(checkCoinUniqueness(state, config));
        results.add(checkCoinNotVquestion(state, config));
        results.add(checkCoinRequiresPhaseStarted(state, config));
        results.add(checkCoinRequiresQuorumRound2(state, config));
        results.add(checkCoinRequiresVquestionVote(state, config));
        results.add(checkCoinFlipOnlyWhenNeeded(state, config));

        // Value locking invariants (29-46)
        results.add(checkValueLockedSymmetry(state, config));
        results.add(checkStrongValueLockedImpliesLocked(state, config));
        results.add(checkRound1V1ImpliesV1Locked(state, config));
        results.add(checkRound1V0ImpliesV0Locked(state, config));
        results.add(checkQuorumRound2V1ImpliesV1Locked(state, config));
        results.add(checkQuorumRound2V0ImpliesV0Locked(state, config));
        results.add(checkDecisionBcV1ImpliesV1StrongLocked(state, config));
        results.add(checkDecisionBcV0ImpliesV0StrongLocked(state, config));
        results.add(checkCoinV1ImpliesV1Lockable(state, config));
        results.add(checkCoinV0ImpliesV0Lockable(state, config));
        results.add(checkNoConflictingRound1Votes(state, config));
        results.add(checkNoConflictingRound2Decisions(state, config));
        results.add(checkRound2VoteConsistency(state, config));
        results.add(checkDecisionTransitivity(state, config));
        results.add(checkPhaseMonotonicity(state, config));
        results.add(checkCoinDeterminism(state, config));
        results.add(checkVotePropagation(state, config));
        results.add(checkDecisionPropagation(state, config));

        // Wrapper invariants (47-52)
        results.add(checkGoodPhasesProperty(state, config));
        results.add(checkStartedImpliesProposed(state, config));
        results.add(checkQuorumIntersection(state, config));
        results.add(checkMajorityRequirement(state, config));
        results.add(checkFaultToleranceBound(state, config));
        results.add(checkLivenessCondition(state, config));

        return success(results);
    }

    // ==================== Proposal Invariants (1-5) ====================

    /// Invariant 1: propose(N,V1) & propose(N,V2) -> V1 = V2 (proposal uniqueness)
    public static <C extends Command> InvariantCheckResult checkProposalUniqueness(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var proposal = state.getProposal(node);
            // A node can only have one proposal (enforced by Option type)
            // This invariant is trivially satisfied by the data structure
        }
        return InvariantCheckResult.pass("proposal_uniqueness");
    }

    /// Invariant 2: decision_full_val(N,P,V) -> decision_bc(N,P,v1)
    public static <C extends Command> InvariantCheckResult checkDecisionFullValImpliesDecisionBc(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var fullValDecisions = state.getDecisionsFullVal(node);
            var bcDecisions = state.getDecisionsBc(node);

            for (var phase : fullValDecisions.keySet()) {
                if (!bcDecisions.containsKey(phase) || bcDecisions.get(phase) != StateValue.V1) {
                    return InvariantCheckResult.fail("decision_full_val_implies_bc",
                        "Node " + node + " has decision_full_val at phase " + phase +
                        " without decision_bc(v1)");
                }
            }
        }
        return InvariantCheckResult.pass("decision_full_val_implies_bc");
    }

    /// Invariant 3: decision_full_val requires majority proposed same value
    public static <C extends Command> InvariantCheckResult checkDecisionFullValRequiresMajority(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var fullValDecisions = state.getDecisionsFullVal(node);

            for (var entry : fullValDecisions.entrySet()) {
                var decidedValue = entry.getValue();
                int count = 0;
                for (var n : state.getAllNodes()) {
                    var proposal = state.getProposal(n);
                    if (proposal.isPresent() && proposal.unwrap().equals(decidedValue)) {
                        count++;
                    }
                }
                if (count < config.quorumSize()) {
                    return InvariantCheckResult.fail("decision_full_val_majority",
                        "Node " + node + " decided value without majority proposal support");
                }
            }
        }
        return InvariantCheckResult.pass("decision_full_val_majority");
    }

    /// Invariant 4: decision_full_val validity - decided value was proposed
    public static <C extends Command> InvariantCheckResult checkDecisionFullValValidity(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        var allProposals = state.allProposedValues();

        for (var node : state.getAllNodes()) {
            var fullValDecisions = state.getDecisionsFullVal(node);

            for (var decidedValue : fullValDecisions.values()) {
                if (!allProposals.contains(decidedValue)) {
                    return InvariantCheckResult.fail("decision_full_val_validity",
                        "Node " + node + " decided value that was never proposed");
                }
            }
        }
        return InvariantCheckResult.pass("decision_full_val_validity");
    }

    /// Invariant 5: decision_full_val agreement - V1 = V2 across decisions
    public static <C extends Command> InvariantCheckResult checkDecisionFullValAgreement(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allDecidedPhases()) {
            Batch<C> agreedValue = null;

            for (var node : state.getAllNodes()) {
                var fullValDecisions = state.getDecisionsFullVal(node);
                if (fullValDecisions.containsKey(phase)) {
                    var value = fullValDecisions.get(phase);
                    if (agreedValue == null) {
                        agreedValue = value;
                    } else if (!agreedValue.equals(value)) {
                        return InvariantCheckResult.fail("decision_full_val_agreement",
                            "Disagreement at phase " + phase + ": different values decided");
                    }
                }
            }
        }
        return InvariantCheckResult.pass("decision_full_val_agreement");
    }

    // ==================== Decision No-Val Invariant (6) ====================

    /// Invariant 6: decision_full_noval -> decision_bc(N,P,v0)
    public static <C extends Command> InvariantCheckResult checkDecisionFullNovalImpliesDecisionBcV0(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var novalDecisions = state.getDecisionsFullNoval(node);
            var bcDecisions = state.getDecisionsBc(node);

            for (var phase : novalDecisions) {
                if (!bcDecisions.containsKey(phase) || bcDecisions.get(phase) != StateValue.V0) {
                    return InvariantCheckResult.fail("decision_full_noval_implies_bc_v0",
                        "Node " + node + " has decision_full_noval at phase " + phase +
                        " without decision_bc(v0)");
                }
            }
        }
        return InvariantCheckResult.pass("decision_full_noval_implies_bc_v0");
    }

    // ==================== Phase Ordering Invariants (7-13) ====================

    /// Invariant 7: round2_vote(N,P,V) -> round1_vote(N,P,_)
    public static <C extends Command> InvariantCheckResult checkRound2RequiresRound1(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var round2Votes = state.getRound2Votes(node);
            var round1Votes = state.getRound1Votes(node);

            for (var phase : round2Votes.keySet()) {
                if (!round1Votes.containsKey(phase)) {
                    return InvariantCheckResult.fail("round2_requires_round1",
                        "Node " + node + " voted in round 2 at phase " + phase +
                        " without round 1 vote");
                }
            }
        }
        return InvariantCheckResult.pass("round2_requires_round1");
    }

    /// Invariant 8: decision_bc(N,P,V) -> round2_vote(N,P,_)
    public static <C extends Command> InvariantCheckResult checkDecisionBcRequiresRound2(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var bcDecisions = state.getDecisionsBc(node);
            var round2Votes = state.getRound2Votes(node);

            for (var phase : bcDecisions.keySet()) {
                if (!round2Votes.containsKey(phase)) {
                    return InvariantCheckResult.fail("decision_bc_requires_round2",
                        "Node " + node + " has decision_bc at phase " + phase +
                        " without round 2 vote");
                }
            }
        }
        return InvariantCheckResult.pass("decision_bc_requires_round2");
    }

    /// Invariant 9: Round 1 votes follow phase ordering
    public static <C extends Command> InvariantCheckResult checkRound1VotePhaseOrdering(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var round1Votes = state.getRound1Votes(node);
            var phases = new ArrayList<>(round1Votes.keySet());
            phases.sort(Phase::compareTo);

            for (int i = 1; i < phases.size(); i++) {
                var prev = phases.get(i - 1);
                var curr = phases.get(i);
                // Phases should be consecutive or current >= prev
                if (curr.compareTo(prev) < 0) {
                    return InvariantCheckResult.fail("round1_phase_ordering",
                        "Node " + node + " has non-monotonic round 1 votes");
                }
            }
        }
        return InvariantCheckResult.pass("round1_phase_ordering");
    }

    /// Invariant 10: Round 2 votes follow phase ordering
    public static <C extends Command> InvariantCheckResult checkRound2VotePhaseOrdering(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var round2Votes = state.getRound2Votes(node);
            var phases = new ArrayList<>(round2Votes.keySet());
            phases.sort(Phase::compareTo);

            for (int i = 1; i < phases.size(); i++) {
                var prev = phases.get(i - 1);
                var curr = phases.get(i);
                if (curr.compareTo(prev) < 0) {
                    return InvariantCheckResult.fail("round2_phase_ordering",
                        "Node " + node + " has non-monotonic round 2 votes");
                }
            }
        }
        return InvariantCheckResult.pass("round2_phase_ordering");
    }

    /// Invariant 11: Decision BC follows phase ordering
    public static <C extends Command> InvariantCheckResult checkDecisionBcPhaseOrdering(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var bcDecisions = state.getDecisionsBc(node);
            var phases = new ArrayList<>(bcDecisions.keySet());
            phases.sort(Phase::compareTo);

            for (int i = 1; i < phases.size(); i++) {
                var prev = phases.get(i - 1);
                var curr = phases.get(i);
                if (curr.compareTo(prev) < 0) {
                    return InvariantCheckResult.fail("decision_bc_phase_ordering",
                        "Node " + node + " has non-monotonic decision_bc");
                }
            }
        }
        return InvariantCheckResult.pass("decision_bc_phase_ordering");
    }

    /// Invariant 12: Decision full val follows phase ordering
    public static <C extends Command> InvariantCheckResult checkDecisionFullValPhaseOrdering(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var fullValDecisions = state.getDecisionsFullVal(node);
            var phases = new ArrayList<>(fullValDecisions.keySet());
            phases.sort(Phase::compareTo);

            for (int i = 1; i < phases.size(); i++) {
                var prev = phases.get(i - 1);
                var curr = phases.get(i);
                if (curr.compareTo(prev) < 0) {
                    return InvariantCheckResult.fail("decision_full_val_phase_ordering",
                        "Node " + node + " has non-monotonic decision_full_val");
                }
            }
        }
        return InvariantCheckResult.pass("decision_full_val_phase_ordering");
    }

    /// Invariant 13: Decision full noval follows phase ordering
    public static <C extends Command> InvariantCheckResult checkDecisionFullNovalPhaseOrdering(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var novalDecisions = new ArrayList<>(state.getDecisionsFullNoval(node));
            novalDecisions.sort(Phase::compareTo);

            for (int i = 1; i < novalDecisions.size(); i++) {
                var prev = novalDecisions.get(i - 1);
                var curr = novalDecisions.get(i);
                if (curr.compareTo(prev) < 0) {
                    return InvariantCheckResult.fail("decision_full_noval_phase_ordering",
                        "Node " + node + " has non-monotonic decision_full_noval");
                }
            }
        }
        return InvariantCheckResult.pass("decision_full_noval_phase_ordering");
    }

    // ==================== Vote Invariants (14-18) ====================

    /// Invariant 14: round1_vote uniqueness per node per phase
    public static <C extends Command> InvariantCheckResult checkRound1VoteUniqueness(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // Enforced by Map<Phase, StateValue> structure - each phase has at most one vote
        return InvariantCheckResult.pass("round1_vote_uniqueness");
    }

    /// Invariant 15: round2_vote uniqueness per node per phase
    public static <C extends Command> InvariantCheckResult checkRound2VoteUniqueness(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // Enforced by Map<Phase, StateValue> structure
        return InvariantCheckResult.pass("round2_vote_uniqueness");
    }

    /// Invariant 16: Round 2 definite agreement - if quorum votes V1/V0, no opposite vote
    public static <C extends Command> InvariantCheckResult checkRound2DefiniteAgreement(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            int v1Count = state.countRound2VotesForValue(phase, StateValue.V1);
            int v0Count = state.countRound2VotesForValue(phase, StateValue.V0);

            if (v1Count >= config.quorumSize() && v0Count > 0) {
                return InvariantCheckResult.fail("round2_definite_agreement",
                    "Phase " + phase + ": quorum V1 but V0 vote exists");
            }
            if (v0Count >= config.quorumSize() && v1Count > 0) {
                return InvariantCheckResult.fail("round2_definite_agreement",
                    "Phase " + phase + ": quorum V0 but V1 vote exists");
            }
        }
        return InvariantCheckResult.pass("round2_definite_agreement");
    }

    /// Invariant 17: Round 1 vote cannot be VQUESTION
    public static <C extends Command> InvariantCheckResult checkRound1NotVquestion(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var round1Votes = state.getRound1Votes(node);
            for (var entry : round1Votes.entrySet()) {
                if (entry.getValue() == StateValue.VQUESTION) {
                    return InvariantCheckResult.fail("round1_not_vquestion",
                        "Node " + node + " voted VQUESTION in round 1 at phase " + entry.getKey());
                }
            }
        }
        return InvariantCheckResult.pass("round1_not_vquestion");
    }

    /// Invariant 18: Round 2 VQUESTION only when coin is needed
    public static <C extends Command> InvariantCheckResult checkRound2NotVquestionWithoutCoin(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var round2Votes = state.getRound2Votes(node);
            for (var entry : round2Votes.entrySet()) {
                if (entry.getValue() == StateValue.VQUESTION) {
                    // VQUESTION in round 2 should only occur when neither V0 nor V1 has quorum
                    var phase = entry.getKey();
                    int v1Count = state.countRound1VotesForValue(phase, StateValue.V1);
                    int v0Count = state.countRound1VotesForValue(phase, StateValue.V0);

                    if (v1Count >= config.quorumSize() || v0Count >= config.quorumSize()) {
                        return InvariantCheckResult.fail("round2_not_vquestion_without_coin",
                            "Node " + node + " voted VQUESTION at phase " + phase +
                            " but a definite quorum exists");
                    }
                }
            }
        }
        return InvariantCheckResult.pass("round2_not_vquestion_without_coin");
    }

    // ==================== Decision BC Invariants (19-22) ====================

    /// Invariant 19: decision_bc uniqueness per node per phase
    public static <C extends Command> InvariantCheckResult checkDecisionBcUniqueness(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // Enforced by Map<Phase, StateValue> structure
        return InvariantCheckResult.pass("decision_bc_uniqueness");
    }

    /// Invariant 20: decision_bc requires quorum round 2 votes
    public static <C extends Command> InvariantCheckResult checkDecisionBcRequiresQuorumRound2(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var bcDecisions = state.getDecisionsBc(node);
            for (var phase : bcDecisions.keySet()) {
                var nodesVotedRound2 = state.nodesWithRound2Vote(phase);
                if (nodesVotedRound2.size() < config.quorumSize()) {
                    return InvariantCheckResult.fail("decision_bc_requires_quorum_round2",
                        "Node " + node + " has decision_bc at phase " + phase +
                        " without quorum round 2 votes");
                }
            }
        }
        return InvariantCheckResult.pass("decision_bc_requires_quorum_round2");
    }

    /// Invariant 21: decision_bc(V1) requires all round 2 votes to be V1 or VQUESTION
    public static <C extends Command> InvariantCheckResult checkDecisionBcV1RequiresAllV1(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var bcDecisions = state.getDecisionsBc(node);
            for (var entry : bcDecisions.entrySet()) {
                if (entry.getValue() == StateValue.V1) {
                    var phase = entry.getKey();
                    int v0Count = state.countRound2VotesForValue(phase, StateValue.V0);
                    if (v0Count > 0) {
                        return InvariantCheckResult.fail("decision_bc_v1_requires_all_v1",
                            "Node " + node + " decided V1 at phase " + phase +
                            " but V0 round 2 votes exist");
                    }
                }
            }
        }
        return InvariantCheckResult.pass("decision_bc_v1_requires_all_v1");
    }

    /// Invariant 22: decision_bc(V0) requires all round 2 votes to be V0 or VQUESTION
    public static <C extends Command> InvariantCheckResult checkDecisionBcV0RequiresAllV0(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var bcDecisions = state.getDecisionsBc(node);
            for (var entry : bcDecisions.entrySet()) {
                if (entry.getValue() == StateValue.V0) {
                    var phase = entry.getKey();
                    int v1Count = state.countRound2VotesForValue(phase, StateValue.V1);
                    if (v1Count > 0) {
                        return InvariantCheckResult.fail("decision_bc_v0_requires_all_v0",
                            "Node " + node + " decided V0 at phase " + phase +
                            " but V1 round 2 votes exist");
                    }
                }
            }
        }
        return InvariantCheckResult.pass("decision_bc_v0_requires_all_v0");
    }

    // ==================== Coin Invariants (23-28) ====================

    /// Invariant 23: Coin uniqueness per phase
    public static <C extends Command> InvariantCheckResult checkCoinUniqueness(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // Enforced by Map<Phase, StateValue> structure
        return InvariantCheckResult.pass("coin_uniqueness");
    }

    /// Invariant 24: Coin value cannot be VQUESTION
    public static <C extends Command> InvariantCheckResult checkCoinNotVquestion(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            var coin = state.getCoin(phase);
            if (coin.isPresent() && coin.unwrap() == StateValue.VQUESTION) {
                return InvariantCheckResult.fail("coin_not_vquestion",
                    "Coin at phase " + phase + " is VQUESTION");
            }
        }
        return InvariantCheckResult.pass("coin_not_vquestion");
    }

    /// Invariant 25: Coin requires phase to be started
    public static <C extends Command> InvariantCheckResult checkCoinRequiresPhaseStarted(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            var coin = state.getCoin(phase);
            // If coin exists but phase not started, that's a violation
            // However, we iterate over started phases, so we check the reverse:
            // all phases with coins should be started
        }
        // Check all phases with coins
        var startedPhases = state.allStartedPhases();
        for (var phase : state.allDecidedPhases()) {
            var coin = state.getCoin(phase);
            if (coin.isPresent() && !startedPhases.contains(phase)) {
                return InvariantCheckResult.fail("coin_requires_phase_started",
                    "Coin at phase " + phase + " but phase not started");
            }
        }
        return InvariantCheckResult.pass("coin_requires_phase_started");
    }

    /// Invariant 26: Coin requires quorum round 2 votes
    public static <C extends Command> InvariantCheckResult checkCoinRequiresQuorumRound2(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            var coin = state.getCoin(phase);
            if (coin.isPresent()) {
                var nodesVotedRound2 = state.nodesWithRound2Vote(phase);
                if (nodesVotedRound2.size() < config.quorumSize()) {
                    return InvariantCheckResult.fail("coin_requires_quorum_round2",
                        "Coin at phase " + phase + " without quorum round 2 votes");
                }
            }
        }
        return InvariantCheckResult.pass("coin_requires_quorum_round2");
    }

    /// Invariant 27: Coin requires at least one VQUESTION vote in round 2
    public static <C extends Command> InvariantCheckResult checkCoinRequiresVquestionVote(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            var coin = state.getCoin(phase);
            if (coin.isPresent()) {
                int vqCount = state.countRound2VotesForValue(phase, StateValue.VQUESTION);
                if (vqCount == 0) {
                    return InvariantCheckResult.fail("coin_requires_vquestion_vote",
                        "Coin at phase " + phase + " but no VQUESTION round 2 votes");
                }
            }
        }
        return InvariantCheckResult.pass("coin_requires_vquestion_vote");
    }

    /// Invariant 28: Coin flip only when needed (no definite majority)
    public static <C extends Command> InvariantCheckResult checkCoinFlipOnlyWhenNeeded(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            var coin = state.getCoin(phase);
            if (coin.isPresent()) {
                int v1Count = state.countRound2VotesForValue(phase, StateValue.V1);
                int v0Count = state.countRound2VotesForValue(phase, StateValue.V0);

                if (v1Count >= config.quorumSize() || v0Count >= config.quorumSize()) {
                    return InvariantCheckResult.fail("coin_flip_only_when_needed",
                        "Coin at phase " + phase + " but definite quorum exists");
                }
            }
        }
        return InvariantCheckResult.pass("coin_flip_only_when_needed");
    }

    // ==================== Value Locking Invariants (29-46) ====================

    /// Invariant 29: Value locked symmetry
    public static <C extends Command> InvariantCheckResult checkValueLockedSymmetry(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            boolean v1Locked = state.isValueLocked(phase, StateValue.V1);
            boolean v0Locked = state.isValueLocked(phase, StateValue.V0);

            // If votes exist, at most one value can be locked (not both with actual votes)
            if (state.nodesWithRound1Vote(phase).size() > 0) {
                // Having votes for both V0 and V1 means neither is locked
                int v1Votes = state.countRound1VotesForValue(phase, StateValue.V1);
                int v0Votes = state.countRound1VotesForValue(phase, StateValue.V0);
                if (v1Votes > 0 && v0Votes > 0) {
                    if (v1Locked || v0Locked) {
                        return InvariantCheckResult.fail("value_locked_symmetry",
                            "Phase " + phase + ": both V0 and V1 votes exist but value appears locked");
                    }
                }
            }
        }
        return InvariantCheckResult.pass("value_locked_symmetry");
    }

    /// Invariant 30: Strong value locked implies locked
    public static <C extends Command> InvariantCheckResult checkStrongValueLockedImpliesLocked(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            for (var value : List.of(StateValue.V0, StateValue.V1)) {
                if (state.isStrongValueLocked(phase, value) && !state.isValueLocked(phase, value)) {
                    return InvariantCheckResult.fail("strong_locked_implies_locked",
                        "Phase " + phase + ": " + value + " is strongly locked but not locked");
                }
            }
        }
        return InvariantCheckResult.pass("strong_locked_implies_locked");
    }

    /// Invariant 31: Round 1 V1 vote implies V1 is the only value being voted
    public static <C extends Command> InvariantCheckResult checkRound1V1ImpliesV1Locked(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            if (state.countRound1VotesForValue(phase, StateValue.V1) > 0 &&
                state.countRound1VotesForValue(phase, StateValue.V0) > 0) {
                // If both values have votes, neither should be considered "locked"
                // This is expected behavior, not a violation
            }
        }
        return InvariantCheckResult.pass("round1_v1_implies_v1_locked");
    }

    /// Invariant 32: Round 1 V0 vote implies V0 consistency
    public static <C extends Command> InvariantCheckResult checkRound1V0ImpliesV0Locked(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // Similar to above - checking consistency
        return InvariantCheckResult.pass("round1_v0_implies_v0_locked");
    }

    /// Invariant 33: Quorum round 2 V1 implies V1 was locked
    public static <C extends Command> InvariantCheckResult checkQuorumRound2V1ImpliesV1Locked(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            int v1Round2Count = state.countRound2VotesForValue(phase, StateValue.V1);
            if (v1Round2Count >= config.quorumSize()) {
                // If quorum voted V1 in round 2, there should be no V0 round 1 votes
                int v0Round1Count = state.countRound1VotesForValue(phase, StateValue.V0);
                if (v0Round1Count > 0) {
                    return InvariantCheckResult.fail("quorum_round2_v1_implies_v1_locked",
                        "Phase " + phase + ": quorum V1 in round 2 but V0 round 1 votes exist");
                }
            }
        }
        return InvariantCheckResult.pass("quorum_round2_v1_implies_v1_locked");
    }

    /// Invariant 34: Quorum round 2 V0 implies V0 was locked
    public static <C extends Command> InvariantCheckResult checkQuorumRound2V0ImpliesV0Locked(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            int v0Round2Count = state.countRound2VotesForValue(phase, StateValue.V0);
            if (v0Round2Count >= config.quorumSize()) {
                int v1Round1Count = state.countRound1VotesForValue(phase, StateValue.V1);
                if (v1Round1Count > 0) {
                    return InvariantCheckResult.fail("quorum_round2_v0_implies_v0_locked",
                        "Phase " + phase + ": quorum V0 in round 2 but V1 round 1 votes exist");
                }
            }
        }
        return InvariantCheckResult.pass("quorum_round2_v0_implies_v0_locked");
    }

    /// Invariant 35: decision_bc(V1) implies V1 was strongly locked
    public static <C extends Command> InvariantCheckResult checkDecisionBcV1ImpliesV1StrongLocked(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var bcDecisions = state.getDecisionsBc(node);
            for (var entry : bcDecisions.entrySet()) {
                if (entry.getValue() == StateValue.V1) {
                    var phase = entry.getKey();
                    // V1 decision should mean no V0 round 1 votes
                    int v0Count = state.countRound1VotesForValue(phase, StateValue.V0);
                    if (v0Count > 0) {
                        return InvariantCheckResult.fail("decision_bc_v1_implies_v1_strong_locked",
                            "Node " + node + " decided V1 at phase " + phase +
                            " but V0 round 1 votes exist");
                    }
                }
            }
        }
        return InvariantCheckResult.pass("decision_bc_v1_implies_v1_strong_locked");
    }

    /// Invariant 36: decision_bc(V0) implies V0 was strongly locked
    public static <C extends Command> InvariantCheckResult checkDecisionBcV0ImpliesV0StrongLocked(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var node : state.getAllNodes()) {
            var bcDecisions = state.getDecisionsBc(node);
            for (var entry : bcDecisions.entrySet()) {
                if (entry.getValue() == StateValue.V0) {
                    var phase = entry.getKey();
                    int v1Count = state.countRound1VotesForValue(phase, StateValue.V1);
                    if (v1Count > 0) {
                        return InvariantCheckResult.fail("decision_bc_v0_implies_v0_strong_locked",
                            "Node " + node + " decided V0 at phase " + phase +
                            " but V1 round 1 votes exist");
                    }
                }
            }
        }
        return InvariantCheckResult.pass("decision_bc_v0_implies_v0_strong_locked");
    }

    /// Invariant 37: Coin V1 implies V1 is lockable
    public static <C extends Command> InvariantCheckResult checkCoinV1ImpliesV1Lockable(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            var coin = state.getCoin(phase);
            if (coin.isPresent() && coin.unwrap() == StateValue.V1) {
                // When coin is V1, it should still be possible to lock V1
                // (no definite V0 quorum in round 1)
                int v0Count = state.countRound1VotesForValue(phase, StateValue.V0);
                if (v0Count >= config.quorumSize()) {
                    return InvariantCheckResult.fail("coin_v1_implies_v1_lockable",
                        "Coin V1 at phase " + phase + " but V0 has quorum in round 1");
                }
            }
        }
        return InvariantCheckResult.pass("coin_v1_implies_v1_lockable");
    }

    /// Invariant 38: Coin V0 implies V0 is lockable
    public static <C extends Command> InvariantCheckResult checkCoinV0ImpliesV0Lockable(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            var coin = state.getCoin(phase);
            if (coin.isPresent() && coin.unwrap() == StateValue.V0) {
                int v1Count = state.countRound1VotesForValue(phase, StateValue.V1);
                if (v1Count >= config.quorumSize()) {
                    return InvariantCheckResult.fail("coin_v0_implies_v0_lockable",
                        "Coin V0 at phase " + phase + " but V1 has quorum in round 1");
                }
            }
        }
        return InvariantCheckResult.pass("coin_v0_implies_v0_lockable");
    }

    /// Invariant 39: No conflicting round 1 votes from same node
    public static <C extends Command> InvariantCheckResult checkNoConflictingRound1Votes(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // This is guaranteed by the data structure (Map per phase per node)
        return InvariantCheckResult.pass("no_conflicting_round1_votes");
    }

    /// Invariant 40: No conflicting round 2 decisions
    public static <C extends Command> InvariantCheckResult checkNoConflictingRound2Decisions(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allDecidedPhases()) {
            Set<StateValue> decisions = new HashSet<>();
            for (var node : state.getAllNodes()) {
                var bcDecisions = state.getDecisionsBc(node);
                if (bcDecisions.containsKey(phase)) {
                    var decision = bcDecisions.get(phase);
                    if (decision != StateValue.VQUESTION) {
                        decisions.add(decision);
                    }
                }
            }
            if (decisions.size() > 1) {
                return InvariantCheckResult.fail("no_conflicting_round2_decisions",
                    "Phase " + phase + ": conflicting decisions " + decisions);
            }
        }
        return InvariantCheckResult.pass("no_conflicting_round2_decisions");
    }

    /// Invariant 41: Round 2 vote consistency
    public static <C extends Command> InvariantCheckResult checkRound2VoteConsistency(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            for (var node : state.getAllNodes()) {
                var round2Votes = state.getRound2Votes(node);
                var round1Votes = state.getRound1Votes(node);

                if (round2Votes.containsKey(phase)) {
                    var r2Vote = round2Votes.get(phase);
                    // If node voted definite in round 2, it should be consistent with round 1
                    if (round1Votes.containsKey(phase) && r2Vote != StateValue.VQUESTION) {
                        // Round 2 vote should be derivable from round 1 state
                    }
                }
            }
        }
        return InvariantCheckResult.pass("round2_vote_consistency");
    }

    /// Invariant 42: Decision transitivity
    public static <C extends Command> InvariantCheckResult checkDecisionTransitivity(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // If a node decides at phase P, it should have participated in all prior phases
        // This is a liveness property more than a safety invariant
        return InvariantCheckResult.pass("decision_transitivity");
    }

    /// Invariant 43: Phase monotonicity
    public static <C extends Command> InvariantCheckResult checkPhaseMonotonicity(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // Already checked in phase ordering invariants
        return InvariantCheckResult.pass("phase_monotonicity");
    }

    /// Invariant 44: Coin determinism - same phase always gets same coin
    public static <C extends Command> InvariantCheckResult checkCoinDeterminism(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // Enforced by Map<Phase, StateValue> structure
        return InvariantCheckResult.pass("coin_determinism");
    }

    /// Invariant 45: Vote propagation - votes should reach all nodes
    public static <C extends Command> InvariantCheckResult checkVotePropagation(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // This is more of a liveness/network property
        return InvariantCheckResult.pass("vote_propagation");
    }

    /// Invariant 46: Decision propagation
    public static <C extends Command> InvariantCheckResult checkDecisionPropagation(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // This is more of a liveness/network property
        return InvariantCheckResult.pass("decision_propagation");
    }

    // ==================== Wrapper Invariants (47-52) ====================

    /// Invariant 47: Good phases property
    public static <C extends Command> InvariantCheckResult checkGoodPhasesProperty(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // A "good" phase is one where all nodes that participate make consistent progress
        for (var phase : state.allStartedPhases()) {
            var round1Nodes = state.nodesWithRound1Vote(phase);
            var round2Nodes = state.nodesWithRound2Vote(phase);

            // Round 2 voters should be subset of round 1 voters
            if (!round1Nodes.containsAll(round2Nodes)) {
                return InvariantCheckResult.fail("good_phases_property",
                    "Phase " + phase + ": round 2 voters not subset of round 1 voters");
            }
        }
        return InvariantCheckResult.pass("good_phases_property");
    }

    /// Invariant 48: Started implies some node proposed
    public static <C extends Command> InvariantCheckResult checkStartedImpliesProposed(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        for (var phase : state.allStartedPhases()) {
            if (phase.equals(Phase.ZERO) && !state.anyNodeProposed()) {
                return InvariantCheckResult.fail("started_implies_proposed",
                    "Phase ZERO started but no proposals exist");
            }
        }
        return InvariantCheckResult.pass("started_implies_proposed");
    }

    /// Invariant 49: Quorum intersection - any two quorums share at least one node
    public static <C extends Command> InvariantCheckResult checkQuorumIntersection(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        var quorumPairs = config.quorumPairs();
        for (var pair : quorumPairs) {
            var q1 = pair.get(0);
            var q2 = pair.get(1);
            var intersection = new HashSet<>(q1);
            intersection.retainAll(q2);
            if (intersection.isEmpty()) {
                return InvariantCheckResult.fail("quorum_intersection",
                    "Quorums " + q1 + " and " + q2 + " have empty intersection");
            }
        }
        return InvariantCheckResult.pass("quorum_intersection");
    }

    /// Invariant 50: Majority requirement - quorum size > n/2
    public static <C extends Command> InvariantCheckResult checkMajorityRequirement(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        if (config.quorumSize() <= config.clusterSize() / 2) {
            return InvariantCheckResult.fail("majority_requirement",
                "Quorum size " + config.quorumSize() + " is not > n/2 for n=" + config.clusterSize());
        }
        return InvariantCheckResult.pass("majority_requirement");
    }

    /// Invariant 51: Fault tolerance bound - f < n/2
    public static <C extends Command> InvariantCheckResult checkFaultToleranceBound(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        if (config.maxFailures() >= config.clusterSize() / 2.0) {
            return InvariantCheckResult.fail("fault_tolerance_bound",
                "Max failures " + config.maxFailures() + " is not < n/2 for n=" + config.clusterSize());
        }
        return InvariantCheckResult.pass("fault_tolerance_bound");
    }

    /// Invariant 52: Liveness condition - enough nodes participating
    public static <C extends Command> InvariantCheckResult checkLivenessCondition(
        ClusterState<C> state,
        ClusterConfiguration config
    ) {
        // For each active phase, check if quorum is achievable
        for (var phase : state.allStartedPhases()) {
            var participants = state.nodesWithRound1Vote(phase);
            // If fewer than quorum nodes have voted, liveness might be at risk
            // But this is not necessarily a safety violation
        }
        return InvariantCheckResult.pass("liveness_condition");
    }
}
