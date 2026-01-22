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

/**
 * Sealed interface representing protocol actions from weak_mvc.ivy spec.
 */
public sealed interface ProtocolAction<C extends Command> {
    record InitialProposal<C extends Command>(NodeId node, Batch<C> value) implements ProtocolAction<C> {}

    record InitialVote1<C extends Command>(NodeId node, StateValue vote) implements ProtocolAction<C> {}

    record PhaseRound1<C extends Command>(NodeId node, Phase phase, StateValue vote) implements ProtocolAction<C> {}

    record PhaseRound2<C extends Command>(NodeId node, Phase phase, StateValue vote) implements ProtocolAction<C> {}

    record DecideFullVal<C extends Command>(NodeId node, Phase phase, Batch<C> value) implements ProtocolAction<C> {}

    record DecideFullNoval<C extends Command>(NodeId node, Phase phase) implements ProtocolAction<C> {}

    record DecisionBc<C extends Command>(NodeId node, Phase phase, StateValue value) implements ProtocolAction<C> {}

    record CoinFlip<C extends Command>(Phase phase, StateValue value) implements ProtocolAction<C> {}
}
