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
import org.pragmatica.messaging.Message;

import java.util.List;

/// Notifications for topology change: node added/removed/down.
public sealed interface TopologyChangeNotification extends Message.Local {
    /// The node which added or removed. For the NodeDown notification, this is the own ID of the node.
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
