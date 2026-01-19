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
import org.pragmatica.consensus.rabia.helper.ClusterConfiguration;

import java.util.HashSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for quorum intersection properties from weak_mvc.ivy specification (lines 17-29).
 * <p>
 * These axioms are foundational to Rabia's safety guarantees:
 * <pre>
 * axiom forall Q1:set_majority, Q2:set_majority. exists N:node.
 *       member_maj(N, Q1) & member_maj(N, Q2)
 *
 * axiom forall Q1:set_majority, Q2:set_f_plus_1.
 *       exists N:node. member_maj(N, Q1) & member_fp1(N, Q2)
 * </pre>
 */
class RabiaQuorumIntersectionTest {

    static Stream<Arguments> clusterSizes() {
        return Stream.of(
            Arguments.of(3),
            Arguments.of(5),
            Arguments.of(7),
            Arguments.of(9)
        );
    }

    @Nested
    class MajorityIntersection {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaQuorumIntersectionTest#clusterSizes")
        void any_two_majorities_intersect(int size) {
            var config = ClusterConfiguration.of(size);
            var quorumPairs = config.quorumPairs();

            for (var pair : quorumPairs) {
                var q1 = pair.get(0);
                var q2 = pair.get(1);

                var intersection = new HashSet<>(q1);
                intersection.retainAll(q2);

                assertThat(intersection)
                    .as("Quorums %s and %s must intersect", q1, q2)
                    .isNotEmpty();
            }
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaQuorumIntersectionTest#clusterSizes")
        void intersection_enables_vote_propagation(int size) {
            var config = ClusterConfiguration.of(size);

            // Any two majorities share at least one node
            // This ensures conflicting decisions cannot happen
            var q1 = new HashSet<>(config.nodeIds().subList(0, config.quorumSize()));
            var q2 = new HashSet<>(config.nodeIds().subList(
                config.clusterSize() - config.quorumSize(),
                config.clusterSize()));

            var intersection = new HashSet<>(q1);
            intersection.retainAll(q2);

            // With n=2f+1, quorum=f+1, intersection is at least 1
            assertThat(intersection.size()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    class MajorityFPlusOneIntersection {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaQuorumIntersectionTest#clusterSizes")
        void majority_and_f_plus_one_intersect(int size) {
            var config = ClusterConfiguration.of(size);
            var majorities = config.allQuorums();
            var fPlusOneSets = config.allFPlusOneSets();

            for (var maj : majorities) {
                for (var fp1 : fPlusOneSets) {
                    var intersection = new HashSet<>(maj);
                    intersection.retainAll(fp1);

                    assertThat(intersection)
                        .as("Majority %s and f+1 set %s must intersect", maj, fp1)
                        .isNotEmpty();
                }
            }
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaQuorumIntersectionTest#clusterSizes")
        void f_plus_one_v1_votes_guarantee_decision_propagation(int size) {
            var config = ClusterConfiguration.of(size);

            // f+1 nodes voted V1
            var v1Voters = new HashSet<>(config.nodeIds().subList(0, config.fPlusOne()));

            // Any majority quorum must contain at least one V1 voter
            for (var quorum : config.allQuorums()) {
                var intersection = new HashSet<>(quorum);
                intersection.retainAll(v1Voters);

                assertThat(intersection)
                    .as("Majority must contain V1 voter")
                    .isNotEmpty();
            }
        }
    }

    @Nested
    class QuorumBasedDecisionSafety {

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaQuorumIntersectionTest#clusterSizes")
        void round2_non_question_requires_quorum_round1(int size) {
            var config = ClusterConfiguration.of(size);

            // This is a property test: verifying the relationship
            // Any two majorities must overlap, so conflicting
            // non-VQUESTION votes in round 2 are impossible
            //
            // If quorum Q1 voted V0 in round 1 (enabling V0 vote in round 2)
            // and quorum Q2 voted V1 in round 1 (enabling V1 vote in round 2)
            // Q1 and Q2 must share a node, but that node can only vote once
            // Hence conflicting round 2 votes are impossible

            assertThat(config.quorumSize() * 2).isGreaterThan(config.clusterSize());
        }

        @ParameterizedTest
        @MethodSource("org.pragmatica.consensus.rabia.spec.RabiaQuorumIntersectionTest#clusterSizes")
        void decision_bc_requires_f_plus_one_round2(int size) {
            var config = ClusterConfiguration.of(size);

            // n = 2f + 1, so f = (n-1)/2
            // f+1 = (n-1)/2 + 1 = (n+1)/2
            // This ensures: even with f failures, at least one correct node voted

            int f = config.maxFailures();
            assertThat(config.fPlusOne()).isEqualTo(f + 1);
            assertThat(config.fPlusOne()).isLessThanOrEqualTo(config.clusterSize());
        }
    }
}
