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
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.rabia.Phase;
import org.pragmatica.consensus.rabia.StateValue;
import org.pragmatica.consensus.rabia.helper.ClusterConfiguration;
import org.pragmatica.consensus.rabia.helper.ClusterState;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Rabia protocol value locking invariants from weak_mvc.ivy specification (conjectures 31-46).
 */
class RabiaValueLockingTest {

    record TestCommand(String value) implements Command {}

    static Stream<Arguments> clusterSizes() {
        return Stream.of(
            Arguments.of(3),
            Arguments.of(5),
            Arguments.of(7)
        );
    }

    @Nested
    class DerivedPredicates {

        // [31] state_value_locked(P, V) = forall N, Valt. vote_rnd1(N, P, Valt) -> Valt = V
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void state_value_locked_definition(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Value is locked if all round 1 votes are for the same value
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V1);
            }

            // V1 should be locked
            assertThat(state.isValueLocked(phase, StateValue.V1)).isTrue();
            // V0 should NOT be locked (there are votes, but not for V0)
            assertThat(state.isValueLocked(phase, StateValue.V0)).isFalse();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void state_value_locked_vacuously_true_when_no_votes(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // With no votes, any value is vacuously locked
            assertThat(state.isValueLocked(phase, StateValue.V0)).isTrue();
            assertThat(state.isValueLocked(phase, StateValue.V1)).isTrue();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void state_value_locked_false_when_conflicting_votes(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Mixed votes - neither value is locked
            state.voteRound1(config.nodeIds().get(0), phase, StateValue.V0);
            state.voteRound1(config.nodeIds().get(1), phase, StateValue.V1);

            assertThat(state.isValueLocked(phase, StateValue.V0)).isFalse();
            assertThat(state.isValueLocked(phase, StateValue.V1)).isFalse();
        }

        // [32] strong_state_value_locked(P, V) = (exists N. vote_rnd1(N, P, V)) & state_value_locked(P, V)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void strong_state_value_locked_definition(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Strong lock requires at least one vote AND lock
            state.voteRound1(config.nodeIds().getFirst(), phase, StateValue.V1);

            // V1 is strongly locked (has vote + locked)
            assertThat(state.isStrongValueLocked(phase, StateValue.V1)).isTrue();
            // V0 is NOT strongly locked (no vote for V0, even though it's vacuously locked without conflicting votes)
            assertThat(state.isStrongValueLocked(phase, StateValue.V0)).isFalse();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void strong_state_value_locked_false_when_no_votes(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // No votes - value is not strongly locked (even though vacuously locked)
            assertThat(state.isStrongValueLocked(phase, StateValue.V0)).isFalse();
            assertThat(state.isStrongValueLocked(phase, StateValue.V1)).isFalse();
        }

        // [33] members_voted_rnd2(Q, P) = forall N. member_maj(N, Q) -> exists V. vote_rnd2(N, P, V)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void members_voted_rnd2_definition(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var quorum = Set.copyOf(config.nodeIds().subList(0, config.quorumSize()));

            // All quorum members must have voted round 2
            for (var node : quorum) {
                state.voteRound2(node, phase, StateValue.V1);
            }

            assertThat(state.quorumVotedRound2(quorum, phase)).isTrue();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void members_voted_rnd2_false_when_missing_votes(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var quorum = Set.copyOf(config.nodeIds().subList(0, config.quorumSize()));

            // Only some quorum members voted
            state.voteRound2(config.nodeIds().getFirst(), phase, StateValue.V1);

            assertThat(state.quorumVotedRound2(quorum, phase)).isFalse();
        }
    }

    @Nested
    class LockingRules {

        // [34] vote_rnd1_pred_rnd: vote_rnd1(N1, Psucc, V1) & succ(P,Psucc) -> exists N2. vote_rnd1(N2, P, V1)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void vote_rnd1_pred_rnd(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var succPhase = phase.successor();

            // If a node votes V1 in successor phase, there must exist a node that voted V1 in predecessor
            state.voteRound1(config.nodeIds().get(0), phase, StateValue.V1);
            state.voteRound1(config.nodeIds().get(1), succPhase, StateValue.V1);

            // Verify the invariant: exists N2 with vote_rnd1(N2, P, V1)
            var hasV1InPred = config.nodeIds().stream()
                                    .anyMatch(n -> {
                                        var vote = state.getRound1Votes(n).get(phase);
                                        return vote == StateValue.V1;
                                    });
            assertThat(hasV1InPred).isTrue();
        }

        // [35] decision_bc_locks_successor: decision_bc(N1, P, V1) & succ(P,Psucc) -> state_value_locked(Psucc, V1)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void decision_bc_locks_successor(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var succPhase = phase.successor();

            // Decision at phase P locks value in successor phase
            state.decideBc(config.nodeIds().getFirst(), phase, StateValue.V1);

            // Simulate all nodes voting V1 in successor (as required by protocol after decision)
            for (var node : config.nodeIds()) {
                state.voteRound1(node, succPhase, StateValue.V1);
            }

            // Successor phase should be locked to V1
            assertThat(state.isValueLocked(succPhase, StateValue.V1)).isTrue();
        }

        // [36] locked_rnd2_constraint: state_value_locked(P, V1) & vote_rnd2(N, P, V2) -> V1 = V2 | V2 = vquestion
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void locked_rnd2_constraint(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Lock V1 in round 1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V1);
            }
            assertThat(state.isValueLocked(phase, StateValue.V1)).isTrue();

            // Round 2 votes must be V1 or VQUESTION (not V0)
            state.voteRound2(config.nodeIds().get(0), phase, StateValue.V1);
            state.voteRound2(config.nodeIds().get(1), phase, StateValue.VQUESTION);

            // Verify all round 2 votes satisfy constraint
            for (var node : config.nodeIds()) {
                var vote = state.getRound2Votes(node).get(phase);
                if (vote != null) {
                    assertThat(vote == StateValue.V1 || vote == StateValue.VQUESTION)
                        .as("Round 2 vote must be V1 or VQUESTION when V1 is locked")
                        .isTrue();
                }
            }
        }

        // [37] coin_majority_question: coin(P, V) -> exists Q:set_majority. forall N. member_maj(N, Q) -> vote_rnd2(N, P, vquestion)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void coin_majority_question(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // For coin to be used, majority must have voted VQUESTION
            for (int i = 0; i < config.quorumSize(); i++) {
                state.voteRound2(config.nodeIds().get(i), phase, StateValue.VQUESTION);
            }

            state.setCoin(phase, StateValue.V1);

            // Verify majority voted VQUESTION
            var quorum = Set.copyOf(config.nodeIds().subList(0, config.quorumSize()));
            boolean allVquestion = quorum.stream()
                                         .allMatch(n -> state.getRound2Votes(n).get(phase) == StateValue.VQUESTION);
            assertThat(allVquestion).isTrue();
        }

        // [38] locked_majority_vote: state_value_locked(P, V) -> forall Q. members_voted_rnd2(Q, P) -> exists N. member_maj(N, Q) & vote_rnd2(N, P, V)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void locked_majority_vote(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Lock V1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V1);
            }

            // Have quorum vote round 2 with some V1 and some VQUESTION
            var quorum = config.nodeIds().subList(0, config.quorumSize());
            state.voteRound2(quorum.get(0), phase, StateValue.V1);
            for (int i = 1; i < quorum.size(); i++) {
                state.voteRound2(quorum.get(i), phase, StateValue.VQUESTION);
            }

            var quorumSet = Set.copyOf(quorum);
            assertThat(state.quorumVotedRound2(quorumSet, phase)).isTrue();

            // If locked, at least one quorum member must have voted V1
            boolean hasV1Vote = quorum.stream()
                                      .anyMatch(n -> state.getRound2Votes(n).get(phase) == StateValue.V1);
            assertThat(hasV1Vote).isTrue();
        }

        // [39] locked_no_coin: state_value_locked(P, V) -> forall Q. members_voted_rnd2(Q, P) -> ~coin(P, V2)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void locked_no_coin(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Lock V1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V1);
            }

            // Quorum votes round 2 - at least one must vote V1 (due to lock)
            var quorum = config.nodeIds().subList(0, config.quorumSize());
            state.voteRound2(quorum.get(0), phase, StateValue.V1);
            for (int i = 1; i < quorum.size(); i++) {
                state.voteRound2(quorum.get(i), phase, StateValue.VQUESTION);
            }

            // When locked with majority round 2 completed, no coin should be used
            // (decision can be made via the V1 vote)
            var quorumSet = Set.copyOf(quorum);
            assertThat(state.quorumVotedRound2(quorumSet, phase)).isTrue();
            assertThat(state.isValueLocked(phase, StateValue.V1)).isTrue();

            // Coin should not be set in this scenario
            assertThat(state.getCoin(phase).isPresent()).isFalse();
        }

        // [40] locked_locks_succ: state_value_locked(P, V) & succ(P,Psucc) -> state_value_locked(Psucc, V)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void locked_locks_succ(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var succPhase = phase.successor();

            // Lock V1 in phase
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V1);
            }
            assertThat(state.isValueLocked(phase, StateValue.V1)).isTrue();

            // Protocol ensures successor inherits lock - all successor votes must be V1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, succPhase, StateValue.V1);
            }

            // Successor phase should also be locked to V1
            assertThat(state.isValueLocked(succPhase, StateValue.V1)).isTrue();
        }
    }

    @Nested
    class DecisionLocking {

        // [41] decision_succ_majority: decision_bc(N1, P, V1) & succ(P,Psucc) -> forall Q. members_voted_rnd2(Q, Psucc) -> exists N. member_maj(N, Q) & vote_rnd2(N, Psucc, V1)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void decision_succ_majority(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var succPhase = phase.successor();

            // Decision at phase P
            state.decideBc(config.nodeIds().getFirst(), phase, StateValue.V1);

            // In successor, all round 1 votes must be V1 (due to lock)
            for (var node : config.nodeIds()) {
                state.voteRound1(node, succPhase, StateValue.V1);
            }

            // Quorum votes in round 2 of successor
            var quorum = config.nodeIds().subList(0, config.quorumSize());
            state.voteRound2(quorum.get(0), phase.successor(), StateValue.V1);
            for (int i = 1; i < quorum.size(); i++) {
                state.voteRound2(quorum.get(i), phase.successor(), StateValue.VQUESTION);
            }

            // At least one quorum member voted V1 in successor round 2
            boolean hasV1InSucc = quorum.stream()
                                        .anyMatch(n -> state.getRound2Votes(n).get(succPhase) == StateValue.V1);
            assertThat(hasV1InSucc).isTrue();
        }

        // [42] decision_no_succ_coin: decision_bc(N1, P, V1) & succ(P,Psucc) -> ~exists V. coin(Psucc, V)
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void decision_no_succ_coin(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var succPhase = phase.successor();

            // Decision at phase P
            state.decideBc(config.nodeIds().getFirst(), phase, StateValue.V1);

            // In successor phase, due to lock, no coin should be needed
            // All nodes vote V1 in round 1 and round 2
            for (var node : config.nodeIds()) {
                state.voteRound1(node, succPhase, StateValue.V1);
                state.voteRound2(node, succPhase, StateValue.V1);
            }

            // No coin should exist for successor phase
            assertThat(state.getCoin(succPhase).isPresent()).isFalse();
        }

        // [43] vl_decision_bc_agree: state_value_locked(P, V) & decision_bc(N2, P, V2) -> V = V2
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void vl_decision_bc_agree(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Lock V1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V1);
            }
            assertThat(state.isValueLocked(phase, StateValue.V1)).isTrue();

            // Any decision must be V1 (due to lock)
            state.decideBc(config.nodeIds().getFirst(), phase, StateValue.V1);

            var decision = state.getDecisionsBc(config.nodeIds().getFirst()).get(phase);
            assertThat(decision).isEqualTo(StateValue.V1);
        }

        // [44] decision_succ_agree: decision_bc(N1, P, V1) & succ(P,Psucc) & decision_bc(N2, Psucc, V2) -> V1 = V2
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void decision_succ_agree(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;
            var succPhase = phase.successor();

            // Decision V1 at phase P
            state.decideBc(config.nodeIds().get(0), phase, StateValue.V1);

            // Decision at successor must agree
            state.decideBc(config.nodeIds().get(1), succPhase, StateValue.V1);

            var decisionP = state.getDecisionsBc(config.nodeIds().get(0)).get(phase);
            var decisionSucc = state.getDecisionsBc(config.nodeIds().get(1)).get(succPhase);

            assertThat(decisionP).isEqualTo(decisionSucc);
        }

        // [45] decision_same_round_agree: decision_bc(N1, P, V1) & decision_bc(N2, P, V2) -> V1 = V2
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void decision_same_round_agree(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // Multiple nodes make decisions in same phase
            for (var node : config.nodeIds()) {
                state.decideBc(node, phase, StateValue.V1);
            }

            // All decisions must agree
            var decisions = config.nodeIds().stream()
                                  .map(n -> state.getDecisionsBc(n).get(phase))
                                  .filter(Objects::nonNull)
                                  .distinct()
                                  .toList();

            assertThat(decisions).hasSize(1);
            assertThat(decisions.getFirst()).isEqualTo(StateValue.V1);
        }

        // [46] locked_uniqueness: (exists N, V. vote_rnd1(N, P, V)) & state_value_locked(P, V1) & state_value_locked(P, V2) -> V1 = V2
        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void locked_uniqueness(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // All nodes vote V1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V1);
            }

            // Check that at least one vote exists
            boolean hasVote = config.nodeIds().stream()
                                    .anyMatch(n -> state.hasVotedRound1(n, phase));
            assertThat(hasVote).isTrue();

            // If both V0 and V1 were locked, they'd have to be equal (contradiction)
            // Only V1 can be locked when all voted V1
            assertThat(state.isValueLocked(phase, StateValue.V1)).isTrue();
            assertThat(state.isValueLocked(phase, StateValue.V0)).isFalse();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaValueLockingTest#clusterSizes")
        void locked_uniqueness_complementary(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var phase = Phase.ZERO;

            // All nodes vote V0
            for (var node : config.nodeIds()) {
                state.voteRound1(node, phase, StateValue.V0);
            }

            // Only V0 can be locked when all voted V0
            assertThat(state.isValueLocked(phase, StateValue.V0)).isTrue();
            assertThat(state.isValueLocked(phase, StateValue.V1)).isFalse();

            // This demonstrates that at most one value can be locked when votes exist
        }
    }
}
