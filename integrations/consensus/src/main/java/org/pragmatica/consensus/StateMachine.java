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

package org.pragmatica.consensus;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.List;

/**
 * Replicated state machine interface.
 * Implementations must ensure deterministic command execution.
 *
 * @param <C> Command type
 */
public interface StateMachine<C extends Command> {
    /**
     * Process a command and update the machine's state.
     * The command must be immutable and its execution must be deterministic.
     *
     * @param command The command to process
     * @return The result of processing the command
     */
    <R> R process(C command);

    /**
     * Process multiple commands in order.
     */
    @SuppressWarnings("unchecked")
    default <R> List<R> process(List<C> commands) {
        return commands.stream()
                       .map(command -> (R) process(command))
                       .toList();
    }

    /**
     * Create a snapshot of the current state machine state.
     * The snapshot should be serializable and capture the complete state.
     *
     * @return A Result containing the serialized state snapshot
     */
    Result<byte[]> makeSnapshot();

    /**
     * Restore the machine's state from a snapshot.
     * This should completely replace the current state with the state from the snapshot.
     *
     * @return A Result indicating success or failure
     */
    Result<Unit> restoreSnapshot(byte[] snapshot);

    /**
     * Reset state machine to its initial state.
     */
    void reset();
}
