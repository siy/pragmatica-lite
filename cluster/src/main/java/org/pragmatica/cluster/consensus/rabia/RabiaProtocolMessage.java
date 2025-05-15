package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.ProtocolMessage;
import org.pragmatica.cluster.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.Command;

/// Message types for the Rabia consensus protocol.
public sealed interface RabiaProtocolMessage extends ProtocolMessage {
    sealed interface Synchronous extends RabiaProtocolMessage {
        /// Initial proposal from a node.
        record Propose<C extends Command>(NodeId sender, Phase phase, Batch<C> value)
                implements Synchronous {}

        /// Round 1 vote message.
        record VoteRound1(NodeId sender, Phase phase, StateValue stateValue)
                implements Synchronous {}

        /// Round 2 vote message.
        record VoteRound2(NodeId sender, Phase phase, StateValue stateValue)
                implements Synchronous {}

        /// Decision broadcast message.
        record Decision<C extends Command>(
                NodeId sender,
                Phase phase,
                StateValue stateValue,
                Batch<C> value)
                implements Synchronous {}

        /// State synchronization response.
        record SyncResponse<C extends Command>(NodeId sender, SavedState<C> state) implements Synchronous {}
    }

    sealed interface Asynchronous extends RabiaProtocolMessage {
        /// State synchronization request.
        record SyncRequest(NodeId sender) implements Asynchronous {}

        /// Distribute a new batch
        record NewBatch<C extends Command>(NodeId sender, Batch<C> batch) implements Asynchronous {}
    }
}
