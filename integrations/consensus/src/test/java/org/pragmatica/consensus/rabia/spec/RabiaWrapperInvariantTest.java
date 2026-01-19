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

package org.pragmatica.consensus.rabia.spec;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.rabia.Batch;
import org.pragmatica.consensus.rabia.Phase;
import org.pragmatica.consensus.rabia.StateValue;
import org.pragmatica.consensus.rabia.helper.ClusterConfiguration;
import org.pragmatica.consensus.rabia.helper.ClusterState;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Rabia protocol wrapper invariants from weak_mvc.ivy specification (conjectures 47-52).
 * <p>
 * These invariants cover phase goodness and started predicates as defined in the ind_defs isolate
 * (lines 305-385 of weak_mvc.ivy).
 * <p>
 * Derived predicates:
 * <ul>
 *   <li>lt(P1, P2) = le(P1, P2) & P1 ~= P2</li>
 *   <li>started(P) = exists N, V. vote_rnd1(N, P, V)</li>
 *   <li>good(P) = started(P) & (forall P0. lt(P0, P) -> started(P0)) &
 *       (forall P0, V0. lt(P0, P) & started(P) & ((exists N. decision_bc(N, P0, V0)) |
 *       state_value_locked(P0, V0)) -> state_value_locked(P, V0))</li>
 * </ul>
 */
class RabiaWrapperInvariantTest {

    record TestCommand(String value) implements Command {}

    static Stream<Arguments> clusterSizes() {
        return Stream.of(
            Arguments.of(3),
            Arguments.of(5),
            Arguments.of(7)
        );
    }

    /// Check if phase P is "started" per spec: exists N, V. vote_rnd1(N, P, V)
    private boolean isStarted(ClusterState<TestCommand> state, Phase phase) {
        return state.isStarted(phase);
    }

    /// Check if phase P is "good" per spec definition:
    /// good(P) = started(P) &
    ///           (forall P0. lt(P0, P) -> started(P0)) &
    ///           (forall P0, V0. lt(P0, P) & started(P) & decision_or_locked(P0, V0) -> locked(P, V0))
    private boolean isGood(ClusterState<TestCommand> state, Phase phase) {
        if (!isStarted(state, phase)) {
            return false;
        }

        // All preceding phases must be started
        for (long p = 0; p < phase.value(); p++) {
            if (!isStarted(state, new Phase(p))) {
                return false;
            }
        }

        // Value locking propagation: if value was locked/decided in earlier phase,
        // it should remain lockable in current phase
        for (long p = 0; p < phase.value(); p++) {
            var priorPhase = new Phase(p);
            if (isStarted(state, priorPhase)) {
                // Check if V1 was decided or locked in prior phase
                if (hasDecisionBcForValue(state, priorPhase, StateValue.V1) ||
                    state.isValueLocked(priorPhase, StateValue.V1)) {
                    // V1 should remain locked in current phase
                    if (!state.isValueLocked(phase, StateValue.V1)) {
                        return false;
                    }
                }
                // Check if V0 was decided or locked in prior phase
                if (hasDecisionBcForValue(state, priorPhase, StateValue.V0) ||
                    state.isValueLocked(priorPhase, StateValue.V0)) {
                    if (!state.isValueLocked(phase, StateValue.V0)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean hasDecisionBcForValue(ClusterState<TestCommand> state, Phase phase, StateValue value) {
        for (var node : state.getAllNodes()) {
            var decisions = state.getDecisionsBc(node);
            if (decisions.containsKey(phase) && decisions.get(phase) == value) {
                return true;
            }
        }
        return false;
    }

    @Nested
    class GoodPhaseInvariants {

        // [47] good_succ_good: good(P) & succ(P, Psucc) & started(Psucc) -> good(Psucc)
        // If phase P is good and its successor Psucc is started, then Psucc is also good
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void good_succ_good(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Start phase 0 with all nodes voting V1
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Phase 0 should be good (started, no prior phases)
            assertThat(isGood(state, Phase.ZERO)).isTrue();

            // Start phase 1 (successor of phase 0)
            var phase1 = new Phase(1);
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase1, StateValue.V1);
            }

            // If phase 0 is good and phase 1 is started, phase 1 should be good
            assertThat(isStarted(state, phase1)).isTrue();
            assertThat(isGood(state, phase1)).isTrue();
        }

        // [48] good_zero: started(zero) -> good(zero)
        // Phase zero is good whenever it is started (no prior phases to check)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void good_zero(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Phase ZERO is not started initially
            assertThat(isStarted(state, Phase.ZERO)).isFalse();

            // Start phase ZERO by having a node vote
            var node = config.nodeIds().getFirst();
            state.propose(node, batch);
            state.voteRound1(node, Phase.ZERO, StateValue.V1);

            // Now phase ZERO is started
            assertThat(isStarted(state, Phase.ZERO)).isTrue();

            // Phase ZERO should be good (no prior phases to satisfy)
            assertThat(isGood(state, Phase.ZERO)).isTrue();
        }
    }

    @Nested
    class DecisionStartedInvariants {

        // [50] decision_bc_started: decision_bc(N,P,V2) -> started(P)
        // If a node has a decision_bc for phase P, then phase P must be started
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void decision_bc_started(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // To make a valid decision_bc, we need the phase to be started first
            // Start phase by having nodes vote in round 1
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, phase, StateValue.V1);
            }

            // Have quorum vote in round 2
            for (int i = 0; i < config.quorumSize(); i++) {
                state.voteRound2(config.nodeIds().get(i), phase, StateValue.V1);
            }

            // Make decision
            var decider = config.nodeIds().getFirst();
            state.decideBc(decider, phase, StateValue.V1);

            // Verify invariant: if decision_bc exists, phase must be started
            assertThat(state.hasDecisionBc(decider, phase)).isTrue();
            assertThat(isStarted(state, phase)).isTrue();
        }
    }

    @Nested
    class VoteTraceInvariants {

        // [51] vote_rnd2_implies_rnd1: vote_rnd2(N,P,V) & V ~= vquestion -> exists N2. vote_rnd1(N2, P, V)
        // If a node votes a definite value (V0 or V1) in round 2, some node must have voted
        // that same value in round 1
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void vote_rnd2_implies_rnd1(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Have majority vote V1 in round 1
            for (int i = 0; i < config.quorumSize(); i++) {
                var node = config.nodeIds().get(i);
                state.propose(node, batch);
                state.voteRound1(node, phase, StateValue.V1);
            }

            // A node can now vote V1 in round 2 (because it saw majority V1 in round 1)
            var voter = config.nodeIds().getFirst();
            state.voteRound2(voter, phase, StateValue.V1);

            // Verify: if node voted V1 in round 2, there must exist a node that voted V1 in round 1
            var round2Vote = state.getRound2Votes(voter).get(phase);
            if (round2Vote != StateValue.VQUESTION) {
                assertThat(state.countRound1VotesForValue(phase, round2Vote))
                    .as("A definite round 2 vote requires matching round 1 votes")
                    .isGreaterThan(0);
            }
        }

        // Test for VQUESTION vote - no requirement on round 1
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void vote_rnd2_vquestion_allowed_without_matching_rnd1(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Split vote: some V0, some V1 - neither has majority
            int half = config.clusterSize() / 2;
            for (int i = 0; i < half; i++) {
                var node = config.nodeIds().get(i);
                state.propose(node, batch);
                state.voteRound1(node, phase, StateValue.V1);
            }
            for (int i = half; i < config.clusterSize(); i++) {
                var node = config.nodeIds().get(i);
                state.propose(node, batch);
                state.voteRound1(node, phase, StateValue.V0);
            }

            // A node can vote VQUESTION in round 2 (no majority in round 1)
            var voter = config.nodeIds().getFirst();
            state.voteRound2(voter, phase, StateValue.VQUESTION);

            // VQUESTION vote has no requirement on matching round 1 value
            var round2Vote = state.getRound2Votes(voter).get(phase);
            assertThat(round2Vote).isEqualTo(StateValue.VQUESTION);
        }

        // [52] decision_bc_implies_vote_rnd1: decision_bc(N,P,V) -> exists N2. vote_rnd1(N2, P, V)
        // If a node decides value V, some node must have voted V in round 1
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void decision_bc_implies_vote_rnd1(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // All nodes vote V1 in round 1
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, phase, StateValue.V1);
            }

            // Quorum votes V1 in round 2
            for (int i = 0; i < config.quorumSize(); i++) {
                state.voteRound2(config.nodeIds().get(i), phase, StateValue.V1);
            }

            // Node decides V1
            var decider = config.nodeIds().getFirst();
            state.decideBc(decider, phase, StateValue.V1);

            // Verify: if decision_bc(V1), then some node voted V1 in round 1
            var decision = state.getDecisionsBc(decider).get(phase);
            assertThat(state.countRound1VotesForValue(phase, decision))
                .as("Decision requires matching round 1 votes")
                .isGreaterThan(0);
        }

        // Test decision V0 path
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void decision_bc_v0_implies_vote_rnd1_v0(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // All nodes vote V0 in round 1
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, phase, StateValue.V0);
            }

            // Quorum votes V0 in round 2
            for (int i = 0; i < config.quorumSize(); i++) {
                state.voteRound2(config.nodeIds().get(i), phase, StateValue.V0);
            }

            // Node decides V0
            var decider = config.nodeIds().getFirst();
            state.decideBc(decider, phase, StateValue.V0);

            // Verify: if decision_bc(V0), then some node voted V0 in round 1
            var decision = state.getDecisionsBc(decider).get(phase);
            assertThat(decision).isEqualTo(StateValue.V0);
            assertThat(state.countRound1VotesForValue(phase, StateValue.V0))
                .as("V0 decision requires V0 round 1 votes")
                .isGreaterThan(0);
        }
    }

    @Nested
    class PhaseSequenceInvariants {

        // Test that good phases maintain sequential property across multiple phases
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void good_phases_sequential(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Start phases 0, 1, 2 in sequence
            for (int phaseNum = 0; phaseNum <= 2; phaseNum++) {
                var phase = new Phase(phaseNum);
                for (var node : config.nodeIds()) {
                    state.propose(node, batch);
                    state.voteRound1(node, phase, StateValue.V1);
                }
            }

            // All phases should be good since they are sequential and consistent
            assertThat(isGood(state, Phase.ZERO)).isTrue();
            assertThat(isGood(state, new Phase(1))).isTrue();
            assertThat(isGood(state, new Phase(2))).isTrue();
        }

        // Test that skipping a phase breaks the "good" property
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void skipped_phase_breaks_good(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Start phase 0
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Skip phase 1, directly start phase 2
            var phase2 = new Phase(2);
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase2, StateValue.V1);
            }

            // Phase 0 is good (no prior phases)
            assertThat(isGood(state, Phase.ZERO)).isTrue();

            // Phase 1 is NOT started
            assertThat(isStarted(state, new Phase(1))).isFalse();

            // Phase 2 is started but NOT good (phase 1 is not started)
            assertThat(isStarted(state, phase2)).isTrue();
            assertThat(isGood(state, phase2)).isFalse();
        }
    }

    @Nested
    class ValueLockingPropagation {

        // Test value locking propagates across good phases
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void value_locked_propagates(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Phase 0: all nodes vote V1 - V1 is locked
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }
            assertThat(state.isValueLocked(Phase.ZERO, StateValue.V1)).isTrue();

            // Phase 1: all nodes still vote V1 - V1 remains locked
            var phase1 = new Phase(1);
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase1, StateValue.V1);
            }
            assertThat(state.isValueLocked(phase1, StateValue.V1)).isTrue();

            // Both phases are good
            assertThat(isGood(state, Phase.ZERO)).isTrue();
            assertThat(isGood(state, phase1)).isTrue();
        }

        // Test decision in prior phase affects goodness
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaWrapperInvariantTest#clusterSizes")
        void decision_in_prior_phase_affects_goodness(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Phase 0: decide V1
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
                state.voteRound2(node, Phase.ZERO, StateValue.V1);
            }
            state.decideBc(config.nodeIds().getFirst(), Phase.ZERO, StateValue.V1);

            // Phase 1: must also lock V1 to be good
            var phase1 = new Phase(1);
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase1, StateValue.V1);
            }

            // Phase 1 is good because V1 decision in phase 0 and V1 locked in phase 1
            assertThat(hasDecisionBcForValue(state, Phase.ZERO, StateValue.V1)).isTrue();
            assertThat(state.isValueLocked(phase1, StateValue.V1)).isTrue();
            assertThat(isGood(state, phase1)).isTrue();
        }
    }
}
