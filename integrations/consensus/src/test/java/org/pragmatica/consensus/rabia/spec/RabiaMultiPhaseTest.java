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
 * Tests for multi-phase protocol behavior and failure scenarios.
 */
class RabiaMultiPhaseTest {

    record TestCommand(String value) implements Command {}

    static Stream<Arguments> clusterSizes() {
        return Stream.of(
            Arguments.of(3),
            Arguments.of(5),
            Arguments.of(7)
        );
    }

    @Nested
    class PhaseProgression {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void single_phase_happy_path_v1(int size) {
            // All nodes propose same value -> V1 decision
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // All propose same batch
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
            }

            // All vote V1 in round 1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // All vote V1 in round 2
            for (var node : config.nodeIds()) {
                state.voteRound2(node, Phase.ZERO, StateValue.V1);
            }

            // Decision should be V1
            var node = config.nodeIds().getFirst();
            state.decideBc(node, Phase.ZERO, StateValue.V1);

            assertThat(state.getDecisionsBc(node).get(Phase.ZERO))
                .isEqualTo(StateValue.V1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void single_phase_conflict_v0(int size) {
            // Different proposals -> no majority -> V0 decision
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // Each node proposes different batch
            for (int i = 0; i < config.nodeIds().size(); i++) {
                var batch = Batch.batch(List.of(new TestCommand("cmd-" + i)));
                state.propose(config.nodeIds().get(i), batch);
            }

            // All vote V0 in round 1 (no agreement)
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V0);
            }

            // All vote V0 in round 2
            for (var node : config.nodeIds()) {
                state.voteRound2(node, Phase.ZERO, StateValue.V0);
            }

            var node = config.nodeIds().getFirst();
            state.decideBc(node, Phase.ZERO, StateValue.V0);

            assertThat(state.getDecisionsBc(node).get(Phase.ZERO))
                .isEqualTo(StateValue.V0);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void multi_phase_consecutive_v1_decisions(int size) {
            // Multiple phases, each with V1 decision
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            for (int phase = 0; phase < 3; phase++) {
                var p = new Phase(phase);
                var batch = Batch.batch(List.of(new TestCommand("cmd-" + phase)));

                for (var node : config.nodeIds()) {
                    state.propose(node, batch);
                    state.voteRound1(node, p, StateValue.V1);
                    state.voteRound2(node, p, StateValue.V1);
                }

                state.decideBc(config.nodeIds().getFirst(), p, StateValue.V1);
            }

            // All phases decided V1
            for (int phase = 0; phase < 3; phase++) {
                assertThat(state.getDecisionsBc(config.nodeIds().getFirst()).get(new Phase(phase)))
                    .isEqualTo(StateValue.V1);
            }
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void multi_phase_mixed_v0_v1_decisions(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var decider = config.nodeIds().getFirst();

            // Phase 0: V0 decision
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V0);
                state.voteRound2(node, Phase.ZERO, StateValue.V0);
            }
            state.decideBc(decider, Phase.ZERO, StateValue.V0);

            // Phase 1: V1 decision
            var phase1 = new Phase(1);
            var batch = Batch.batch(List.of(new TestCommand("cmd")));
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, phase1, StateValue.V1);
                state.voteRound2(node, phase1, StateValue.V1);
            }
            state.decideBc(decider, phase1, StateValue.V1);

            assertThat(state.getDecisionsBc(decider).get(Phase.ZERO)).isEqualTo(StateValue.V0);
            assertThat(state.getDecisionsBc(decider).get(phase1)).isEqualTo(StateValue.V1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void value_locking_across_phases(int size) {
            // Test value locking persists across phases
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // First phase decides V1
            var batch = Batch.batch(List.of(new TestCommand("locked-cmd")));
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
                state.voteRound2(node, Phase.ZERO, StateValue.V1);
            }
            state.decideBc(config.nodeIds().getFirst(), Phase.ZERO, StateValue.V1);

            // Verify value is locked in successor phase
            var successorPhase = new Phase(1);
            assertThat(state.isValueLocked(successorPhase, StateValue.V1)).isTrue();
        }
    }

    @Nested
    class FailureScenarios {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void f_failures_before_proposal(int size) {
            // f nodes fail before proposing - remaining nodes still form quorum
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // Only quorum nodes propose
            for (int i = 0; i < config.quorumSize(); i++) {
                state.propose(config.nodeIds().get(i), batch);
            }

            // Remaining f nodes "failed" - didn't propose
            assertThat(config.nodeIds().stream()
                .filter(n -> state.getProposal(n).isPresent())
                .count())
                .isEqualTo(config.quorumSize());
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void f_failures_after_round1(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // All vote round 1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Only quorum vote round 2 (f failed)
            for (int i = 0; i < config.quorumSize(); i++) {
                state.voteRound2(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }

            // Should still have quorum for round 2
            assertThat(state.countRound2VotesForValue(Phase.ZERO, StateValue.V1))
                .isGreaterThanOrEqualTo(config.quorumSize());
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void f_failures_after_round2(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // All complete round 1 and round 2
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
                state.voteRound2(node, Phase.ZERO, StateValue.V1);
            }

            // Only f+1 nodes make decision (others "failed")
            for (int i = 0; i < config.fPlusOne(); i++) {
                state.decideBc(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }

            // Decisions still consistent
            var decisions = config.nodeIds().stream()
                .filter(n -> state.hasDecisionBc(n, Phase.ZERO))
                .map(n -> state.getDecisionsBc(n).get(Phase.ZERO))
                .distinct()
                .toList();

            assertThat(decisions).hasSize(1);
            assertThat(decisions.getFirst()).isEqualTo(StateValue.V1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void exactly_quorum_nodes_available(int size) {
            // Exactly quorum nodes are available - minimum for progress
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            var availableNodes = config.nodeIds().subList(0, config.quorumSize());

            for (var node : availableNodes) {
                state.propose(node, batch);
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
                state.voteRound2(node, Phase.ZERO, StateValue.V1);
            }

            state.decideBc(availableNodes.getFirst(), Phase.ZERO, StateValue.V1);

            // Should succeed with exactly quorum
            assertThat(state.hasDecisionBc(availableNodes.getFirst(), Phase.ZERO)).isTrue();
        }
    }

    @Nested
    class CoinFlipScenarios {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void coin_flip_when_round2_split(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // Split round 1 votes (no majority)
            int half = config.clusterSize() / 2;
            for (int i = 0; i < half; i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V0);
            }
            for (int i = half; i < config.clusterSize(); i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }

            // Round 2 votes are VQUESTION (no majority in round 1)
            for (var node : config.nodeIds()) {
                state.voteRound2(node, Phase.ZERO, StateValue.VQUESTION);
            }

            // Coin flip needed - simulate deterministic coin based on phase
            var coin = RabiaMultiPhaseTest.coinFlip(Phase.ZERO);
            state.setCoin(Phase.ZERO, coin);

            assertThat(coin).isIn(StateValue.V0, StateValue.V1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void deterministic_coin_across_nodes(int size) {
            // All nodes must get same coin flip for same phase
            var config = ClusterConfiguration.of(size);

            var phase = new Phase(42);
            var coins = config.nodeIds().stream()
                .map(_ -> RabiaMultiPhaseTest.coinFlip(phase))
                .distinct()
                .toList();

            // All nodes should get same coin value
            assertThat(coins).hasSize(1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaMultiPhaseTest#clusterSizes")
        void no_coin_when_f_plus_one_agree(int size) {
            // When f+1 agree on non-VQUESTION, no coin flip needed
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // f+1 vote V1 in round 2
            for (int i = 0; i < config.fPlusOne(); i++) {
                state.voteRound2(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }

            // Decision can be made without coin
            assertThat(state.countRound2VotesForValue(Phase.ZERO, StateValue.V1))
                .isGreaterThanOrEqualTo(config.fPlusOne());

            // Coin should not be set
            assertThat(state.getCoin(Phase.ZERO).isPresent()).isFalse();
        }
    }

    /// Deterministic coin flip matching PhaseData.coinFlip() implementation.
    /// Must be deterministic across all nodes for consensus correctness.
    private static StateValue coinFlip(Phase phase) {
        long seed = phase.value();
        return (Math.abs(seed) % 2 == 0)
               ? StateValue.V0
               : StateValue.V1;
    }
}
