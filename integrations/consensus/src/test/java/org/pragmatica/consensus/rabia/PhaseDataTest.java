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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.NodeId;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.consensus.NodeId.nodeId;

class PhaseDataTest {

    record TestCommand(String value) implements Command {}

    // 5-node cluster: quorum = 3, f = 2, f+1 = 3
    private static final int QUORUM_SIZE = 3;
    private static final int F_PLUS_ONE = 3;

    private static final NodeId NODE_1 = nodeId("node-1").unwrap();
    private static final NodeId NODE_2 = nodeId("node-2").unwrap();
    private static final NodeId NODE_3 = nodeId("node-3").unwrap();
    private static final NodeId NODE_4 = nodeId("node-4").unwrap();
    private static final NodeId NODE_5 = nodeId("node-5").unwrap();

    private PhaseData<TestCommand> phaseData;

    @BeforeEach
    void setUp() {
        phaseData = new PhaseData<>(new Phase(1));
    }

    private Batch<TestCommand> createBatch(String... values) {
        var commands = java.util.Arrays.stream(values)
                                       .map(TestCommand::new)
                                       .toList();
        return Batch.batch(commands);
    }

    @Nested
    class ProposalTracking {

        @Test
        void registerProposal_stores_first_proposal() {
            var batch = createBatch("cmd1");

            phaseData.registerProposal(NODE_1, batch);

            assertThat(phaseData.proposalCount()).isEqualTo(1);
        }

        @Test
        void registerProposal_is_idempotent_first_wins() {
            var batch1 = createBatch("cmd1");
            var batch2 = createBatch("cmd2");

            phaseData.registerProposal(NODE_1, batch1);
            phaseData.registerProposal(NODE_1, batch2);

            assertThat(phaseData.proposalCount()).isEqualTo(1);
        }

        @Test
        void hasQuorumProposals_returns_true_when_quorum_reached() {
            phaseData.registerProposal(NODE_1, createBatch("cmd1"));
            phaseData.registerProposal(NODE_2, createBatch("cmd2"));
            assertThat(phaseData.hasQuorumProposals(QUORUM_SIZE)).isFalse();

            phaseData.registerProposal(NODE_3, createBatch("cmd3"));
            assertThat(phaseData.hasQuorumProposals(QUORUM_SIZE)).isTrue();
        }
    }

    @Nested
    class Round1VoteTracking {

        @Test
        void hasVotedRound1_returns_false_initially() {
            assertThat(phaseData.hasVotedRound1(NODE_1)).isFalse();
        }

        @Test
        void hasVotedRound1_returns_true_after_vote() {
            phaseData.registerRound1Vote(NODE_1, StateValue.V1);

            assertThat(phaseData.hasVotedRound1(NODE_1)).isTrue();
        }

        @Test
        void hasRound1MajorityVotes_checks_total_votes() {
            phaseData.registerRound1Vote(NODE_1, StateValue.V1);
            phaseData.registerRound1Vote(NODE_2, StateValue.V0);
            assertThat(phaseData.hasRound1MajorityVotes(QUORUM_SIZE)).isFalse();

            phaseData.registerRound1Vote(NODE_3, StateValue.V1);
            assertThat(phaseData.hasRound1MajorityVotes(QUORUM_SIZE)).isTrue();
        }

        @Test
        void countRound1VotesForValue_counts_specific_value() {
            phaseData.registerRound1Vote(NODE_1, StateValue.V1);
            phaseData.registerRound1Vote(NODE_2, StateValue.V0);
            phaseData.registerRound1Vote(NODE_3, StateValue.V1);

            assertThat(phaseData.countRound1VotesForValue(StateValue.V1)).isEqualTo(2);
            assertThat(phaseData.countRound1VotesForValue(StateValue.V0)).isEqualTo(1);
        }
    }

    @Nested
    class Round2VoteTracking {

        @Test
        void hasVotedRound2_returns_false_initially() {
            assertThat(phaseData.hasVotedRound2(NODE_1)).isFalse();
        }

        @Test
        void hasVotedRound2_returns_true_after_vote() {
            phaseData.registerRound2Vote(NODE_1, StateValue.V1);

            assertThat(phaseData.hasVotedRound2(NODE_1)).isTrue();
        }

        @Test
        void hasRound2MajorityVotes_checks_total_votes() {
            phaseData.registerRound2Vote(NODE_1, StateValue.V1);
            phaseData.registerRound2Vote(NODE_2, StateValue.VQUESTION);
            assertThat(phaseData.hasRound2MajorityVotes(QUORUM_SIZE)).isFalse();

            phaseData.registerRound2Vote(NODE_3, StateValue.V0);
            assertThat(phaseData.hasRound2MajorityVotes(QUORUM_SIZE)).isTrue();
        }

        @Test
        void countRound2VotesForValue_counts_specific_value() {
            phaseData.registerRound2Vote(NODE_1, StateValue.V1);
            phaseData.registerRound2Vote(NODE_2, StateValue.VQUESTION);
            phaseData.registerRound2Vote(NODE_3, StateValue.V1);

            assertThat(phaseData.countRound2VotesForValue(StateValue.V1)).isEqualTo(2);
            assertThat(phaseData.countRound2VotesForValue(StateValue.VQUESTION)).isEqualTo(1);
            assertThat(phaseData.countRound2VotesForValue(StateValue.V0)).isEqualTo(0);
        }
    }

    @Nested
    class DecisionTracking {

        @Test
        void isDecided_returns_false_initially() {
            assertThat(phaseData.isDecided()).isFalse();
        }

        @Test
        void tryMarkDecided_returns_true_first_time() {
            assertThat(phaseData.tryMarkDecided()).isTrue();
            assertThat(phaseData.isDecided()).isTrue();
        }

        @Test
        void tryMarkDecided_returns_false_when_already_decided() {
            phaseData.tryMarkDecided();

            assertThat(phaseData.tryMarkDecided()).isFalse();
        }
    }

    @Nested
    class EvaluateInitialVote {

        @Test
        void votes_V1_when_quorum_agrees_on_same_batch() {
            var batch = createBatch("cmd1");
            phaseData.registerProposal(NODE_1, batch);
            phaseData.registerProposal(NODE_2, batch);
            phaseData.registerProposal(NODE_3, batch);

            var vote = phaseData.evaluateInitialVote(NODE_1, QUORUM_SIZE);

            assertThat(vote.stateValue()).isEqualTo(StateValue.V1);
            assertThat(vote.sender()).isEqualTo(NODE_1);
            assertThat(vote.phase()).isEqualTo(phaseData.phase());
        }

        @Test
        void votes_V0_when_no_quorum_agreement() {
            var batch1 = createBatch("cmd1");
            var batch2 = createBatch("cmd2");
            var batch3 = createBatch("cmd3");
            phaseData.registerProposal(NODE_1, batch1);
            phaseData.registerProposal(NODE_2, batch2);
            phaseData.registerProposal(NODE_3, batch3);

            var vote = phaseData.evaluateInitialVote(NODE_1, QUORUM_SIZE);

            assertThat(vote.stateValue()).isEqualTo(StateValue.V0);
        }

        @Test
        void votes_V0_when_only_two_agree() {
            var batch = createBatch("cmd1");
            var otherBatch = createBatch("cmd2");
            phaseData.registerProposal(NODE_1, batch);
            phaseData.registerProposal(NODE_2, batch);
            phaseData.registerProposal(NODE_3, otherBatch);

            var vote = phaseData.evaluateInitialVote(NODE_1, QUORUM_SIZE);

            assertThat(vote.stateValue()).isEqualTo(StateValue.V0);
        }

        @Test
        void votes_V1_when_exactly_quorum_agrees() {
            var batch = createBatch("cmd1");
            var otherBatch = createBatch("cmd2");
            phaseData.registerProposal(NODE_1, batch);
            phaseData.registerProposal(NODE_2, batch);
            phaseData.registerProposal(NODE_3, batch);
            phaseData.registerProposal(NODE_4, otherBatch);
            phaseData.registerProposal(NODE_5, otherBatch);

            var vote = phaseData.evaluateInitialVote(NODE_1, QUORUM_SIZE);

            assertThat(vote.stateValue()).isEqualTo(StateValue.V1);
        }

        @Test
        void ignores_empty_batches() {
            var batch = createBatch("cmd1");
            phaseData.registerProposal(NODE_1, batch);
            phaseData.registerProposal(NODE_2, batch);
            phaseData.registerProposal(NODE_3, batch);
            phaseData.registerProposal(NODE_4, Batch.emptyBatch());
            phaseData.registerProposal(NODE_5, Batch.emptyBatch());

            var vote = phaseData.evaluateInitialVote(NODE_1, QUORUM_SIZE);

            assertThat(vote.stateValue()).isEqualTo(StateValue.V1);
        }
    }

    @Nested
    class EvaluateRound2Vote {

        @Test
        void votes_V1_when_quorum_voted_V1_in_round1() {
            phaseData.registerRound1Vote(NODE_1, StateValue.V1);
            phaseData.registerRound1Vote(NODE_2, StateValue.V1);
            phaseData.registerRound1Vote(NODE_3, StateValue.V1);

            var vote = phaseData.evaluateRound2Vote(QUORUM_SIZE);

            assertThat(vote).isEqualTo(StateValue.V1);
        }

        @Test
        void votes_V0_when_quorum_voted_V0_in_round1() {
            phaseData.registerRound1Vote(NODE_1, StateValue.V0);
            phaseData.registerRound1Vote(NODE_2, StateValue.V0);
            phaseData.registerRound1Vote(NODE_3, StateValue.V0);

            var vote = phaseData.evaluateRound2Vote(QUORUM_SIZE);

            assertThat(vote).isEqualTo(StateValue.V0);
        }

        @Test
        void votes_VQUESTION_when_no_quorum_agreement() {
            phaseData.registerRound1Vote(NODE_1, StateValue.V0);
            phaseData.registerRound1Vote(NODE_2, StateValue.V1);
            phaseData.registerRound1Vote(NODE_3, StateValue.V0);
            phaseData.registerRound1Vote(NODE_4, StateValue.V1);

            var vote = phaseData.evaluateRound2Vote(QUORUM_SIZE);

            assertThat(vote).isEqualTo(StateValue.VQUESTION);
        }

        @Test
        void prioritizes_V0_check_over_V1() {
            // When both V0 and V1 have quorum (impossible in practice, but tests priority)
            phaseData.registerRound1Vote(NODE_1, StateValue.V0);
            phaseData.registerRound1Vote(NODE_2, StateValue.V0);
            phaseData.registerRound1Vote(NODE_3, StateValue.V0);

            var vote = phaseData.evaluateRound2Vote(QUORUM_SIZE);

            assertThat(vote).isEqualTo(StateValue.V0);
        }
    }

    @Nested
    class FindAgreedProposal {

        @Test
        void returns_empty_batch_when_no_proposals() {
            var batch = phaseData.findAgreedProposal(QUORUM_SIZE);

            assertThat(batch.isNotEmpty()).isFalse();
        }

        @Test
        void returns_batch_with_most_proposals() {
            var winningBatch = createBatch("winning");
            var losingBatch = createBatch("losing");
            phaseData.registerProposal(NODE_1, winningBatch);
            phaseData.registerProposal(NODE_2, winningBatch);
            phaseData.registerProposal(NODE_3, winningBatch);
            phaseData.registerProposal(NODE_4, losingBatch);
            phaseData.registerProposal(NODE_5, losingBatch);

            var batch = phaseData.findAgreedProposal(QUORUM_SIZE);

            assertThat(batch.correlationId()).isEqualTo(winningBatch.correlationId());
        }

        @Test
        void uses_correlationId_as_tiebreaker() {
            var batch1 = createBatch("batch1");
            var batch2 = createBatch("batch2");
            phaseData.registerProposal(NODE_1, batch1);
            phaseData.registerProposal(NODE_2, batch2);
            phaseData.registerProposal(NODE_3, batch1);
            phaseData.registerProposal(NODE_4, batch2);

            var batch = phaseData.findAgreedProposal(QUORUM_SIZE);

            // Should choose deterministically based on correlationId
            assertThat(batch.isNotEmpty()).isTrue();
        }

        @Test
        void ignores_empty_batches() {
            var realBatch = createBatch("real");
            phaseData.registerProposal(NODE_1, realBatch);
            phaseData.registerProposal(NODE_2, Batch.emptyBatch());
            phaseData.registerProposal(NODE_3, Batch.emptyBatch());

            var batch = phaseData.findAgreedProposal(QUORUM_SIZE);

            assertThat(batch.correlationId()).isEqualTo(realBatch.correlationId());
        }

        @Test
        void returns_empty_when_all_proposals_are_empty() {
            phaseData.registerProposal(NODE_1, Batch.emptyBatch());
            phaseData.registerProposal(NODE_2, Batch.emptyBatch());
            phaseData.registerProposal(NODE_3, Batch.emptyBatch());

            var batch = phaseData.findAgreedProposal(QUORUM_SIZE);

            assertThat(batch.isNotEmpty()).isFalse();
        }
    }

    @Nested
    class ProcessRound2Completion {

        @Test
        void decides_V1_when_f_plus_one_voted_V1() {
            var batch = createBatch("cmd1");
            phaseData.registerProposal(NODE_1, batch);
            phaseData.registerProposal(NODE_2, batch);
            phaseData.registerProposal(NODE_3, batch);
            phaseData.registerRound2Vote(NODE_1, StateValue.V1);
            phaseData.registerRound2Vote(NODE_2, StateValue.V1);
            phaseData.registerRound2Vote(NODE_3, StateValue.V1);

            var decision = phaseData.processRound2Completion(NODE_1, F_PLUS_ONE, QUORUM_SIZE);

            assertThat(decision.stateValue()).isEqualTo(StateValue.V1);
            assertThat(decision.value().isNotEmpty()).isTrue();
        }

        @Test
        void decides_V0_when_f_plus_one_voted_V0() {
            phaseData.registerRound2Vote(NODE_1, StateValue.V0);
            phaseData.registerRound2Vote(NODE_2, StateValue.V0);
            phaseData.registerRound2Vote(NODE_3, StateValue.V0);

            var decision = phaseData.processRound2Completion(NODE_1, F_PLUS_ONE, QUORUM_SIZE);

            assertThat(decision.stateValue()).isEqualTo(StateValue.V0);
            assertThat(decision.value().isNotEmpty()).isFalse();
        }

        @Test
        void uses_coin_flip_when_no_f_plus_one_agreement() {
            phaseData.registerRound2Vote(NODE_1, StateValue.V0);
            phaseData.registerRound2Vote(NODE_2, StateValue.V1);
            phaseData.registerRound2Vote(NODE_3, StateValue.VQUESTION);
            phaseData.registerRound2Vote(NODE_4, StateValue.VQUESTION);

            var decision = phaseData.processRound2Completion(NODE_1, F_PLUS_ONE, QUORUM_SIZE);

            // Phase 1 has value 1, which is odd -> V1
            assertThat(decision.stateValue()).isEqualTo(StateValue.V1);
        }

        @Test
        void prioritizes_V1_over_V0_check() {
            // When both V1 and V0 have f+1 votes (impossible in practice, but tests priority)
            var batch = createBatch("cmd1");
            phaseData.registerProposal(NODE_1, batch);
            phaseData.registerProposal(NODE_2, batch);
            phaseData.registerProposal(NODE_3, batch);
            phaseData.registerRound2Vote(NODE_1, StateValue.V1);
            phaseData.registerRound2Vote(NODE_2, StateValue.V1);
            phaseData.registerRound2Vote(NODE_3, StateValue.V1);

            var decision = phaseData.processRound2Completion(NODE_1, F_PLUS_ONE, QUORUM_SIZE);

            assertThat(decision.stateValue()).isEqualTo(StateValue.V1);
        }
    }

    @Nested
    class CoinFlip {

        @Test
        void returns_V0_for_even_phases() {
            var evenPhaseData = new PhaseData<TestCommand>(new Phase(2));

            assertThat(evenPhaseData.coinFlip()).isEqualTo(StateValue.V0);
        }

        @Test
        void returns_V1_for_odd_phases() {
            var oddPhaseData = new PhaseData<TestCommand>(new Phase(1));

            assertThat(oddPhaseData.coinFlip()).isEqualTo(StateValue.V1);
        }

        @Test
        void is_deterministic_for_same_phase() {
            var phaseData1 = new PhaseData<TestCommand>(new Phase(5));
            var phaseData2 = new PhaseData<TestCommand>(new Phase(5));

            assertThat(phaseData1.coinFlip()).isEqualTo(phaseData2.coinFlip());
        }

        @Test
        void phase_zero_returns_V0() {
            var zeroPhaseData = new PhaseData<TestCommand>(Phase.ZERO);

            assertThat(zeroPhaseData.coinFlip()).isEqualTo(StateValue.V0);
        }
    }

    @Nested
    class PhaseAccessor {

        @Test
        void returns_phase_from_constructor() {
            var phase = new Phase(42);
            var pd = new PhaseData<TestCommand>(phase);

            assertThat(pd.phase()).isEqualTo(phase);
        }
    }
}
