package org.pragmatica.cluster.topology;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.NodeInfo;
import org.pragmatica.message.Message;

import java.util.List;

/// Notification related to topology change and discovery
sealed public interface TopologyManagementMessage extends Message.Local {
    record AddNode(NodeInfo nodeInfo) implements TopologyManagementMessage {}

    record RemoveNode(NodeId nodeId) implements TopologyManagementMessage {}

    record DiscoverNodes(NodeId sender) implements TopologyManagementMessage {}

    record DiscoveredNodes(List<NodeInfo> nodeInfos) implements TopologyManagementMessage {}
}
