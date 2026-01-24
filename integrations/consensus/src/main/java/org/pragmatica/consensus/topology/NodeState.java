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

package org.pragmatica.consensus.topology;

import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.lang.Option;

import java.time.Instant;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

/// State of a node in the topology, tracking health and connection attempts.
///
/// @param info             The node information
/// @param health           Current health status
/// @param failedAttempts   Number of consecutive failed connection attempts
/// @param lastAttempt      Timestamp of the last connection attempt
/// @param nextAttemptAfter Earliest time when next connection attempt is allowed
public record NodeState(NodeInfo info,
                        NodeHealth health,
                        int failedAttempts,
                        Instant lastAttempt,
                        Option<Instant> nextAttemptAfter) {
    /// Creates a healthy node state.
    public static NodeState healthy(NodeInfo info, Instant now) {
        return new NodeState(info, NodeHealth.HEALTHY, 0, now, none());
    }

    /// Creates a suspected node state with backoff.
    public static NodeState suspected(NodeInfo info, int failedAttempts, Instant lastAttempt, Instant nextAttempt) {
        return new NodeState(info, NodeHealth.SUSPECTED, failedAttempts, lastAttempt, some(nextAttempt));
    }

    /// Creates a disabled node state.
    public static NodeState disabled(NodeInfo info, int failedAttempts, Instant lastAttempt) {
        return new NodeState(info, NodeHealth.DISABLED, failedAttempts, lastAttempt, none());
    }

    /// Checks if a connection attempt can be made at the given time.
    /// Returns true for HEALTHY nodes and for SUSPECTED nodes if now > nextAttemptAfter.
    /// Returns false for DISABLED nodes.
    public boolean canAttemptConnection(Instant now) {
        return switch (health) {
            case HEALTHY -> true;
            case SUSPECTED -> nextAttemptAfter.map(next -> now.isAfter(next) || now.equals(next))
                                              .or(true);
            case DISABLED -> false;
        };
    }

    /// Checks if the node is active (HEALTHY or SUSPECTED).
    public boolean isActive() {
        return health == NodeHealth.HEALTHY || health == NodeHealth.SUSPECTED;
    }
}
