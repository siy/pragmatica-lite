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

package org.pragmatica.dht;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

/// Configuration for the distributed hash table.
///
/// @param replicationFactor number of copies of each piece of data (including primary).
///                          Use 0 for full replication (all nodes store everything).
/// @param writeQuorum       minimum number of successful writes for operation to succeed
/// @param readQuorum        minimum number of successful reads for operation to succeed
public record DHTConfig(int replicationFactor, int writeQuorum, int readQuorum) {
    /// Full replication marker - all nodes store all data.
    public static final int FULL_REPLICATION = 0;

    private static final Cause INVALID_REPLICATION = Causes.cause("replicationFactor must be >= 0 (0 = full replication)");
    private static final Cause INVALID_WRITE_QUORUM = Causes.cause("writeQuorum must be between 1 and replicationFactor");
    private static final Cause INVALID_READ_QUORUM = Causes.cause("readQuorum must be between 1 and replicationFactor");

    /// Default configuration: 3 replicas, quorum of 2 for both reads and writes.
    public static final DHTConfig DEFAULT = new DHTConfig(3, 2, 2);

    /// Single-node configuration for testing.
    public static final DHTConfig SINGLE_NODE = new DHTConfig(1, 1, 1);

    /// Full replication configuration - all nodes store all data.
    /// Read/write quorum of 1 since any node has all data.
    public static final DHTConfig FULL = new DHTConfig(FULL_REPLICATION, 1, 1);

    /// Create a DHT configuration with validation.
    public static Result<DHTConfig> dhtConfig(int replicationFactor, int writeQuorum, int readQuorum) {
        if (replicationFactor < 0) {
            return INVALID_REPLICATION.result();
        }
        if (replicationFactor > 0) {
            if (writeQuorum < 1 || writeQuorum > replicationFactor) {
                return INVALID_WRITE_QUORUM.result();
            }
            if (readQuorum < 1 || readQuorum > replicationFactor) {
                return INVALID_READ_QUORUM.result();
            }
        }
        return Result.success(new DHTConfig(replicationFactor, writeQuorum, readQuorum));
    }

    /// Create a config with the given replication factor and majority quorum.
    /// Use 0 for full replication.
    /// Returns the pre-defined FULL config for full replication, otherwise calculates majority quorum.
    public static Result<DHTConfig> withReplication(int replicationFactor) {
        if (replicationFactor == FULL_REPLICATION) {
            return Result.success(FULL);
        }
        int quorum = (replicationFactor / 2) + 1;
        return dhtConfig(replicationFactor, quorum, quorum);
    }

    /// Check if this is full replication mode (all nodes store everything).
    public boolean isFullReplication() {
        return replicationFactor == FULL_REPLICATION;
    }

    /// Check if reads and writes overlap (strong consistency guarantee).
    /// R + W > N ensures that any read will see the most recent write.
    /// Full replication is always strongly consistent.
    public boolean isStronglyConsistent() {
        return isFullReplication() || readQuorum + writeQuorum > replicationFactor;
    }

    /// Get effective replication factor for a given cluster size.
    /// For full replication, returns cluster size. Otherwise returns configured value.
    public int effectiveReplicationFactor(int clusterSize) {
        return isFullReplication()
               ? clusterSize
               : Math.min(replicationFactor, clusterSize);
    }
}
