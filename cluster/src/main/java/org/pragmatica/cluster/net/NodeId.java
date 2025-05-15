package org.pragmatica.cluster.net;

import org.pragmatica.utility.IdGenerator;

/// Cluster node ID
public record NodeId(String id) implements Comparable<NodeId> {
    @Override
    public int compareTo(NodeId o) {
        return id().compareTo(o.id());
    }

    /// Create new node ID from the given string.
    public static NodeId nodeId(String id) {
        return new NodeId(id);
    }

    /// Automatically generate unique node ID.
    public static NodeId randomNodeId() {
        return nodeId(IdGenerator.generate("node"));
    }
}
