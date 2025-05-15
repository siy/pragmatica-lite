package org.pragmatica.cluster.consensus;

import org.pragmatica.cluster.state.Command;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.List;

/// Generalized Consensus API
public interface Consensus<T extends ProtocolMessage, C extends Command> {
    /// Entry point for all protocol messages received from the network
    void processMessage(T message);

    /// Attempts to submit a batch of commands for the replicated state machine.
    /// Attempt may fail (return `false`) if the node is dormant (not yet active or there is no quorum).
    <R> Promise<List<R>> apply(List<C> commands);

    Promise<Unit> start();

    Promise<Unit> stop();
}
