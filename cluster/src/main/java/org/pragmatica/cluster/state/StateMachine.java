package org.pragmatica.cluster.state;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.message.MessageRouter;

import java.util.List;

/// Generalized state machine which can be replicated across cluster.
public interface StateMachine<T extends Command> {
    /// Process a command and update the machine's state.
    /// The command must be immutable and its execution must be deterministic.
    ///
    /// @param command The command to process
    <R> R process(T command);

    @SuppressWarnings("unchecked")
    default <R> List<R> process(List<T> commands) {
        return commands.stream().map(command -> (R) process(command))
                       .toList();
    }

    /// Create a snapshot of the current state machine state.
    /// The snapshot should be serializable and should capture the complete state.
    ///
    /// @return A Result containing the serialized state snapshot
    Result<byte[]> makeSnapshot();

    /// Restore the machine's state from a snapshot.
    /// This should completely replace the current state with the state from the snapshot.
    ///
    /// @return A Result indicating the success or failure of the restoration
    Result<Unit> restoreSnapshot(byte[] snapshot);

    /// Reset state machine to its initial state
    void reset();

    /// Configure message routing
    void configure(MessageRouter.MutableRouter router);
}
