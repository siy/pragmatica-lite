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

import org.pragmatica.lang.Cause;

/**
 * Error types for consensus operations.
 */
public sealed interface ConsensusErrors extends Cause {
    record CommandBatchIsEmpty() implements ConsensusErrors {
        @Override
        public String message() {
            return "Command batch is empty";
        }
    }

    record NodeInactive(NodeId nodeId) implements ConsensusErrors {
        @Override
        public String message() {
            return "Node " + nodeId.id() + " is inactive";
        }
    }

    record SnapshotFailed(String reason) implements ConsensusErrors {
        @Override
        public String message() {
            return "Snapshot failed: " + reason;
        }
    }

    record RestoreFailed(String reason) implements ConsensusErrors {
        @Override
        public String message() {
            return "Restore failed: " + reason;
        }
    }

    static ConsensusErrors commandBatchIsEmpty() {
        return new CommandBatchIsEmpty();
    }

    static ConsensusErrors nodeInactive(NodeId nodeId) {
        return new NodeInactive(nodeId);
    }

    static ConsensusErrors snapshotFailed(String reason) {
        return new SnapshotFailed(reason);
    }

    static ConsensusErrors restoreFailed(String reason) {
        return new RestoreFailed(reason);
    }
}
