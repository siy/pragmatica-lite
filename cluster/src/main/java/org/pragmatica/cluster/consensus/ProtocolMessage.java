package org.pragmatica.cluster.consensus;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.message.Message;

/// Marker interface for all protocol messages
public interface ProtocolMessage extends Message.Wired {
    NodeId sender();
}
