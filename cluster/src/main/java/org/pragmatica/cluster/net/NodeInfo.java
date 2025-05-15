package org.pragmatica.cluster.net;

import org.pragmatica.net.NodeAddress;

/// Node information: ID and address.
public interface NodeInfo {
    NodeId id();

    NodeAddress address();

    /// Create new node information.
    static NodeInfo nodeInfo(NodeId id, NodeAddress address) {
        record nodeInfo(NodeId id, NodeAddress address) implements NodeInfo {}

        return new nodeInfo(id, address);
    }
}
