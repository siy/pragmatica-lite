package org.pragmatica.cluster.topology;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.message.Message;

import java.util.List;

/// Notifications for topology change: node added/removed/down.
public sealed interface TopologyChangeNotification extends Message.Local {
    /// The node which added or removed. For the `NodeDown` notification, this is the own ID of the node.
    NodeId nodeId();

    /// Ordered list of the connected nodes in the cluster. The topology returned by this method
    /// is the topology AFTER the change, i.e., current state of the topology as the node knows it.
    List<NodeId> topology();

    /// Node added notification. Sent when a new node joins the cluster.
    record NodeAdded(NodeId nodeId, List<NodeId> topology) implements TopologyChangeNotification {}

    /// Node removed notification. Sent when a node leaves the cluster.
    record NodeRemoved(NodeId nodeId, List<NodeId> topology) implements TopologyChangeNotification {}

    /// Node down notification. Send when the current node goes down. Topology is always empty in this notification.
    record NodeDown(NodeId nodeId, List<NodeId> topology) implements TopologyChangeNotification {}

    static NodeAdded nodeAdded(NodeId nodeId, List<NodeId> changedView) {
        return new NodeAdded(nodeId, changedView);
    }

    static NodeRemoved nodeRemoved(NodeId nodeId, List<NodeId> changedView) {
        return new NodeRemoved(nodeId, changedView);
    }

    static NodeDown nodeDown(NodeId nodeId) {
        return new NodeDown(nodeId, List.of());
    }
}
