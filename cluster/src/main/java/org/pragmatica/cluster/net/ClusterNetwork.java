package org.pragmatica.cluster.net;

import org.pragmatica.cluster.consensus.ProtocolMessage;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/// Generalized Network API
public interface ClusterNetwork {
    /// Broadcast a message to all nodes in the cluster
    /// Note that actual implementation may just send messages directly, not necessarily use broadcasting in
    /// underlying protocol.
    <M extends ProtocolMessage> void broadcast(M message);

    /// Send a message to a specific node
    <M extends ProtocolMessage> void send(NodeId nodeId, M message);

    /// Start the network.
    Promise<Unit> start();

    /// Stop the network.
    Promise<Unit> stop();
}
