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

import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.ProtocolMessage;
import org.pragmatica.consensus.rabia.RabiaPersistence.SavedState;

/**
 * Message types for the Rabia consensus protocol.
 */
public sealed interface RabiaProtocolMessage extends ProtocolMessage {
    /**
     * Synchronous protocol messages (part of the consensus rounds).
     */
    sealed interface Synchronous extends RabiaProtocolMessage {
        /**
         * Initial proposal from a node.
         */
        record Propose<C extends Command>(NodeId sender, Phase phase, Batch<C> value)
        implements Synchronous {}

        /**
         * Round 1 vote message.
         */
        record VoteRound1(NodeId sender, Phase phase, StateValue stateValue)
        implements Synchronous {}

        /**
         * Round 2 vote message.
         */
        record VoteRound2(NodeId sender, Phase phase, StateValue stateValue)
        implements Synchronous {}

        /**
         * Decision broadcast message.
         */
        record Decision<C extends Command>(NodeId sender,
                                           Phase phase,
                                           StateValue stateValue,
                                           Batch<C> value)
        implements Synchronous {}

        /**
         * State synchronization response.
         */
        record SyncResponse<C extends Command>(NodeId sender, SavedState<C> state) implements Synchronous {}
    }

    /**
     * Asynchronous protocol messages (outside consensus rounds).
     */
    sealed interface Asynchronous extends RabiaProtocolMessage {
        /**
         * State synchronization request.
         */
        record SyncRequest(NodeId sender) implements Asynchronous {}

        /**
         * Distribute a new batch to all nodes.
         */
        record NewBatch<C extends Command>(NodeId sender, Batch<C> batch) implements Asynchronous {}
    }
}
