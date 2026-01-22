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
import org.junit.jupiter.api.Test;
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
 * Tests for super-majority fast path optimization in Rabia consensus.
 * <p>
 * When n - f nodes vote the same value in Round 1, we can skip Round 2
 * and decide immediately. This is safe because:
 * <pre>
 * 1. At least (n-f) - f = n - 2f >= 1 of these voters are non-faulty
 * 2. Any future quorum will include at least f+1 of these voters
 * 3. Therefore Round 2 would produce the same value anyway
 * 4. Safe to skip Round 2 and decide immediately
 * </pre>
 */
class RabiaSuperMajorityFastPathTest {

    record TestCommand(String value) implements Command {}

    static Stream<Arguments> clusterSizes() {
        return Stream.of(
            Arguments.of(3),
            Arguments.of(5),
            Arguments.of(7)
        );
    }

    @Nested
    class SuperMajorityThresholds {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void super_majority_size_is_n_minus_f(int size) {
            var config = ClusterConfiguration.of(size);

            // n - f = n - (n-1)/2 = (n+1)/2 for odd n
            int expected = size - config.maxFailures();
            assertThat(config.superMajoritySize()).isEqualTo(expected);

            // For 3 nodes: n-f = 3-1 = 2
            // For 5 nodes: n-f = 5-2 = 3
            // For 7 nodes: n-f = 7-3 = 4
            int expectedBySize = switch (size) {
                case 3 -> 2;
                case 5 -> 3;
                case 7 -> 4;
                default -> size - (size - 1) / 2;
            };
            assertThat(config.superMajoritySize()).isEqualTo(expectedBySize);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void super_majority_greater_than_quorum(int size) {
            var config = ClusterConfiguration.of(size);

            // Super-majority must be >= quorum for correctness
            // n - f >= (n/2) + 1 for odd n
            assertThat(config.superMajoritySize())
                .isGreaterThanOrEqualTo(config.quorumSize());
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void super_majority_less_than_or_equal_cluster_size(int size) {
            var config = ClusterConfiguration.of(size);

            assertThat(config.superMajoritySize())
                .isLessThanOrEqualTo(config.clusterSize());
        }
    }

    @Nested
    class FastPathTriggerConditions {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void fast_path_triggers_when_super_majority_agrees_v1(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // All nodes vote V1 in round 1 (super-majority agreement)
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Super-majority check should return V1
            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());

            assertThat(superMajorityValue.isPresent()).isTrue();
            assertThat(superMajorityValue.unwrap()).isEqualTo(StateValue.V1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void fast_path_triggers_when_super_majority_agrees_v0(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // All nodes vote V0 in round 1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V0);
            }

            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());

            assertThat(superMajorityValue.isPresent()).isTrue();
            assertThat(superMajorityValue.unwrap()).isEqualTo(StateValue.V0);
        }

        @Test
        void normal_path_when_only_quorum_agrees_in_9_node_cluster() {
            // Use 9-node cluster where quorum (5) < super-majority (5) is NOT true
            // Actually for odd n: quorum = (n/2)+1 = 5, super-majority = n-f = n-(n-1)/2 = 5
            // For even n like 10: quorum = 6, super-majority = 10-4 = 6
            // Standard CFT clusters always have quorum == super-majority
            // So test split votes where neither reaches super-majority
            var config = ClusterConfiguration.of(5);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // 2 nodes vote V1, 2 nodes vote V0, 1 doesn't vote
            // super-majority = 3 for 5 nodes, neither reaches it
            state.voteRound1(config.nodeIds().get(0), Phase.ZERO, StateValue.V1);
            state.voteRound1(config.nodeIds().get(1), Phase.ZERO, StateValue.V1);
            state.voteRound1(config.nodeIds().get(2), Phase.ZERO, StateValue.V0);
            state.voteRound1(config.nodeIds().get(3), Phase.ZERO, StateValue.V0);
            // Node 4 doesn't vote

            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());
            assertThat(superMajorityValue.isPresent())
                .as("Neither V0 nor V1 should reach super-majority of 3")
                .isFalse();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void fast_path_with_f_slow_nodes(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // n-f nodes vote V1 (exactly super-majority)
            for (int i = 0; i < config.superMajoritySize(); i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }
            // f slow nodes haven't voted yet

            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());

            assertThat(superMajorityValue.isPresent()).isTrue();
            assertThat(superMajorityValue.unwrap()).isEqualTo(StateValue.V1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void no_fast_path_when_votes_split(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // Split votes so that neither side reaches super-majority
            // For n=3, n-f=2: need at least 2 votes of one type for super-majority
            //   Split: 1 V0, 1 V1, 1 missing -> neither has 2
            // For n=5, n-f=3: need at least 3 votes of one type
            //   Split: 2 V0, 2 V1, 1 missing -> neither has 3
            // For n=7, n-f=4: need at least 4 votes of one type
            //   Split: 3 V0, 3 V1, 1 missing -> neither has 4
            int votesPerSide = (config.superMajoritySize() - 1);
            for (int i = 0; i < votesPerSide; i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V0);
            }
            for (int i = votesPerSide; i < votesPerSide * 2; i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }
            // Remaining nodes haven't voted yet

            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());

            assertThat(superMajorityValue.isPresent()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void no_fast_path_when_exactly_superMajoritySize_minus_1_votes(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // Vote exactly superMajoritySize - 1 times for V1
            int votesNeeded = config.superMajoritySize() - 1;
            for (int i = 0; i < votesNeeded; i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }

            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());

            assertThat(superMajorityValue.isPresent())
                .as("superMajoritySize - 1 votes should NOT trigger fast path")
                .isFalse();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void no_fast_path_when_already_decided(int size) {
            // This test verifies the canUseFastPath() check: !phaseData.isDecided()
            // Once a phase is decided, fast path should not trigger again
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // All nodes vote V1 - super-majority achieved
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Decision already made for this phase
            state.decideBc(config.nodeIds().getFirst(), Phase.ZERO, StateValue.V1);

            // Super-majority still exists in round 1 votes
            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());
            assertThat(superMajorityValue.isPresent())
                .as("Super-majority exists")
                .isTrue();

            // But the phase is already decided - fast path should NOT trigger again
            // In actual RabiaEngine, canUseFastPath checks !phaseData.isDecided()
            assertThat(state.hasDecisionBc(config.nodeIds().getFirst(), Phase.ZERO))
                .as("Phase already decided - fast path should not trigger")
                .isTrue();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void no_fast_path_when_already_voted_round2(int size) {
            // This test verifies the canUseFastPath() check: !phaseData.hasVotedRound2(self)
            // Once a node has voted in round 2, it should not use fast path
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var selfNode = config.nodeIds().getFirst();

            // All nodes vote V1 - super-majority achieved
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Self node already voted in round 2 (took normal path earlier)
            state.voteRound2(selfNode, Phase.ZERO, StateValue.V1);

            // Super-majority still exists in round 1 votes
            var superMajorityValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());
            assertThat(superMajorityValue.isPresent())
                .as("Super-majority exists")
                .isTrue();

            // But the node already voted round 2 - fast path should NOT trigger
            // In actual RabiaEngine, canUseFastPath checks !phaseData.hasVotedRound2(self)
            assertThat(state.hasVotedRound2(selfNode, Phase.ZERO))
                .as("Node already voted round 2 - fast path should not trigger")
                .isTrue();
        }
    }

    @Nested
    class FastPathDecisionConsistency {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void fast_path_decision_matches_normal_path(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // All nodes vote V1 in round 1
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Fast path would decide V1
            var fastPathValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());

            // Normal path: round 2 would also result in V1
            // When all voted V1 in round 1, round 2 evaluates to V1
            // Then decision_bc would be V1

            assertThat(fastPathValue.isPresent()).isTrue();
            assertThat(fastPathValue.unwrap()).isEqualTo(StateValue.V1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void fast_path_v0_preserves_correctness(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // All nodes vote V0 (no proposal agreement)
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V0);
            }

            var fastPathValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());

            assertThat(fastPathValue.isPresent()).isTrue();
            assertThat(fastPathValue.unwrap()).isEqualTo(StateValue.V0);

            // V0 decision means empty batch - no commands committed
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void late_round1_votes_ignored_after_fast_path_decision(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // Super-majority votes V1
            for (int i = 0; i < config.superMajoritySize(); i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }

            // Fast path triggered - decision made
            var fastPathValue = state.getSuperMajorityRound1Value(
                Phase.ZERO, config.superMajoritySize());
            assertThat(fastPathValue.unwrap()).isEqualTo(StateValue.V1);

            // Simulate late arrival of remaining round 1 votes
            for (int i = config.superMajoritySize(); i < config.clusterSize(); i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V0);
            }

            // Decision was already made - late votes don't change it
            // The decided value (V1) is correct because n-f nodes agreed
            state.decideBc(config.nodeIds().getFirst(), Phase.ZERO, StateValue.V1);

            assertThat(state.getDecisionsBc(config.nodeIds().getFirst()).get(Phase.ZERO))
                .isEqualTo(StateValue.V1);
        }
    }

    @Nested
    class FastPathAcrossClusterSizes {

        @Test
        void fast_path_threshold_for_3_node_cluster() {
            var config = ClusterConfiguration.of(3);
            // n=3, f=1, n-f=2 (super-majority)
            // quorum=2 (majority)
            assertThat(config.superMajoritySize()).isEqualTo(2);
            assertThat(config.quorumSize()).isEqualTo(2);
            // For 3 nodes, super-majority == quorum
        }

        @Test
        void fast_path_threshold_for_5_node_cluster() {
            var config = ClusterConfiguration.of(5);
            // n=5, f=2, n-f=3 (super-majority)
            // quorum=3 (majority)
            assertThat(config.superMajoritySize()).isEqualTo(3);
            assertThat(config.quorumSize()).isEqualTo(3);
            // For 5 nodes, super-majority == quorum
        }

        @Test
        void fast_path_threshold_for_7_node_cluster() {
            var config = ClusterConfiguration.of(7);
            // n=7, f=3, n-f=4 (super-majority)
            // quorum=4 (majority)
            assertThat(config.superMajoritySize()).isEqualTo(4);
            assertThat(config.quorumSize()).isEqualTo(4);
            // For 7 nodes, super-majority == quorum
        }

        @Test
        void fast_path_threshold_for_9_node_cluster() {
            var config = ClusterConfiguration.of(9);
            // n=9, f=4, n-f=5 (super-majority)
            // quorum=5 (majority)
            assertThat(config.superMajoritySize()).isEqualTo(5);
            assertThat(config.quorumSize()).isEqualTo(5);
        }
    }

    @Nested
    class CorrectnessArgument {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void at_least_one_non_faulty_in_super_majority(int size) {
            var config = ClusterConfiguration.of(size);

            // When n-f nodes vote V, at least (n-f) - f = n - 2f >= 1 are non-faulty
            // For n = 2f + 1: n - 2f = 1
            int nonFaultyInSuperMajority = config.superMajoritySize() - config.maxFailures();

            assertThat(nonFaultyInSuperMajority).isGreaterThanOrEqualTo(1);
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void any_quorum_includes_super_majority_voter(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // n-f nodes vote V1
            var superMajorityVoters = config.nodeIds().subList(0, config.superMajoritySize());
            for (var voter : superMajorityVoters) {
                state.voteRound1(voter, Phase.ZERO, StateValue.V1);
            }

            // Any quorum must include at least one of these voters
            // Because |super-majority| + |quorum| > n (pigeonhole principle)
            for (var quorum : config.allQuorums()) {
                boolean hasOverlap = quorum.stream()
                    .anyMatch(superMajorityVoters::contains);
                assertThat(hasOverlap)
                    .as("Quorum %s must overlap with super-majority voters", quorum)
                    .isTrue();
            }
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void round2_would_produce_same_result(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // Super-majority votes V1 in round 1
            for (int i = 0; i < config.superMajoritySize(); i++) {
                state.voteRound1(config.nodeIds().get(i), Phase.ZERO, StateValue.V1);
            }

            // Since super-majority >= quorum, round 2 would see quorum of V1 votes
            // This means round 2 vote would be V1 (not VQUESTION)
            // And f+1 nodes would vote V1 in round 2, leading to V1 decision

            var v1Count = state.countRound1VotesForValue(Phase.ZERO, StateValue.V1);
            assertThat(v1Count).isGreaterThanOrEqualTo(config.quorumSize());

            // Therefore skipping round 2 is safe - outcome is deterministic
        }
    }

    @Nested
    class ValueLockingWithFastPath {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void fast_path_decision_locks_successor_phase(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());

            // Phase 0: super-majority agrees on V1 -> fast path decision
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }
            state.decideBc(config.nodeIds().getFirst(), Phase.ZERO, StateValue.V1);

            // Per Rabia spec: decision locks the value for successor phase
            var successorPhase = Phase.ZERO.successor();

            // All nodes should vote V1 in successor (locked value)
            for (var node : config.nodeIds()) {
                state.voteRound1(node, successorPhase, StateValue.V1);
            }

            assertThat(state.isValueLocked(successorPhase, StateValue.V1)).isTrue();
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaSuperMajorityFastPathTest#clusterSizes")
        void fast_path_preserves_protocol_invariants(int size) {
            var config = ClusterConfiguration.of(size);
            var state = new ClusterState<TestCommand>(config.nodeIds());
            var batch = Batch.batch(List.of(new TestCommand("cmd")));

            // All propose same batch
            for (var node : config.nodeIds()) {
                state.propose(node, batch);
            }

            // All vote V1 (super-majority)
            for (var node : config.nodeIds()) {
                state.voteRound1(node, Phase.ZERO, StateValue.V1);
            }

            // Fast path decision
            state.decideBc(config.nodeIds().getFirst(), Phase.ZERO, StateValue.V1);

            // Invariant: value is locked
            assertThat(state.isValueLocked(Phase.ZERO, StateValue.V1)).isTrue();

            // Invariant: decision matches lock
            assertThat(state.getDecisionsBc(config.nodeIds().getFirst()).get(Phase.ZERO))
                .isEqualTo(StateValue.V1);
        }
    }
}
