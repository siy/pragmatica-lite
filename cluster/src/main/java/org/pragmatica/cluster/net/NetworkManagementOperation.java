package org.pragmatica.cluster.net;

import org.pragmatica.message.Message;

import java.util.List;

public sealed interface NetworkManagementOperation extends Message.Local {
    record ConnectNode(NodeId node) implements NetworkManagementOperation {}

    record DisconnectNode(NodeId nodeId) implements NetworkManagementOperation {}

    record ListConnectedNodes() implements NetworkManagementOperation {}

    record ConnectedNodesList(List<NodeId> connected) implements NetworkManagementOperation {}
}
