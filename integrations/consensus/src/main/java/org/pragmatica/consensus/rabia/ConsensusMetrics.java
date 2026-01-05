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

import org.pragmatica.consensus.NodeId;

/// Metrics collection interface for consensus engine observability.
///
/// Implementations can integrate with monitoring systems like Micrometer.
/// Use [#noop()] for a no-op implementation when metrics are not needed.
public interface ConsensusMetrics {
    /// Record a decision being made.
    ///
    /// @param nodeId     The node making the decision
    /// @param phase      The phase number
    /// @param stateValue The decided value (V0 or V1)
    /// @param durationNs Duration from phase start to decision in nanoseconds
    void recordDecision(NodeId nodeId, Phase phase, StateValue stateValue, long durationNs);

    /// Record a proposal being sent or received.
    ///
    /// @param nodeId The node sending the proposal
    /// @param phase  The phase number
    void recordProposal(NodeId nodeId, Phase phase);

    /// Record a round 1 vote.
    ///
    /// @param nodeId     The voting node
    /// @param phase      The phase number
    /// @param stateValue The voted value
    void recordVoteRound1(NodeId nodeId, Phase phase, StateValue stateValue);

    /// Record a round 2 vote.
    ///
    /// @param nodeId     The voting node
    /// @param phase      The phase number
    /// @param stateValue The voted value
    void recordVoteRound2(NodeId nodeId, Phase phase, StateValue stateValue);

    /// Record a synchronization attempt.
    ///
    /// @param nodeId  The node requesting sync
    /// @param success Whether sync was successful
    void recordSyncAttempt(NodeId nodeId, boolean success);

    /// Update the pending batch count gauge.
    ///
    /// @param nodeId The node
    /// @param count  Current number of pending batches
    void updatePendingBatches(NodeId nodeId, int count);

    /// Returns a no-op implementation that does nothing.
    static ConsensusMetrics noop() {
        return NoopMetrics.INSTANCE;
    }

    /// No-op implementation for when metrics are disabled.
    enum NoopMetrics implements ConsensusMetrics {
        INSTANCE;
        @Override
        public void recordDecision(NodeId nodeId, Phase phase, StateValue stateValue, long durationNs) {}
        @Override
        public void recordProposal(NodeId nodeId, Phase phase) {}
        @Override
        public void recordVoteRound1(NodeId nodeId, Phase phase, StateValue stateValue) {}
        @Override
        public void recordVoteRound2(NodeId nodeId, Phase phase, StateValue stateValue) {}
        @Override
        public void recordSyncAttempt(NodeId nodeId, boolean success) {}
        @Override
        public void updatePendingBatches(NodeId nodeId, int count) {}
    }
}
