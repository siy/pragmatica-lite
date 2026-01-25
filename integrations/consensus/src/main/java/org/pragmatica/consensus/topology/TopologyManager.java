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

import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.tcp.TlsConfig;

import java.net.SocketAddress;
import java.util.List;

/// Representation of our knowledge about the cluster structure: known nodes and cluster/quorum size.
/// Note that this is not a representation of the actual cluster topology.
public interface TopologyManager {
    /// This node information.
    NodeInfo self();

    /// Retrieve information about the node.
    Option<NodeInfo> get(NodeId id);

    /// Returns the configured fixed cluster size used for quorum calculations.
    /// This value is set at startup and can be dynamically updated via SetClusterSize message.
    /// Using a fixed cluster size prevents split-brain resurrection scenarios.
    int clusterSize();

    /// The quorum size (majority) for the cluster.
    default int quorumSize() {
        return clusterSize() / 2 + 1;
    }

    /// Gets the f+1 size for the cluster (where f is the maximum number of failures).
    default int fPlusOne() {
        return clusterSize() - quorumSize() + 1;
    }

    /// Gets the maximum number of failures the cluster can tolerate.
    default int maxFailures() {
        return (clusterSize() - 1) / 2;
    }

    /// Returns the super-majority size (n - f) for fast path optimization.
    /// When this many nodes agree in Round 1, we can skip Round 2.
    default int superMajoritySize() {
        return clusterSize() - maxFailures();
    }

    /// Mapping from IP address (host and port) to node ID.
    Option<NodeId> reverseLookup(SocketAddress socketAddress);

    Promise<Unit> start();

    Promise<Unit> stop();

    TimeSpan pingInterval();

    /// Timeout for Hello handshake on new connections.
    TimeSpan helloTimeout();

    /// TLS configuration for cluster communication (empty for plain TCP).
    default Option<TlsConfig> tls() {
        return Option.empty();
    }

    /// Retrieve the state of a node by ID.
    Option<NodeState> getState(NodeId id);

    /// Returns the list of all node IDs in the topology.
    List<NodeId> topology();
}
