package org.pragmatica.cluster.net;

import org.pragmatica.message.Message;

public sealed interface NetworkMessage extends Message.Wired {
    /// Ping - test connection request
    record Ping(NodeId sender) implements NetworkMessage {}

    /// Pong - test connection response
    record Pong(NodeId sender) implements NetworkMessage {}
}
