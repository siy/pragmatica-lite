package org.pragmatica.cluster.consensus.rabia.infrastructure;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.NodeInfo;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.io.TimeSpan;

import java.net.SocketAddress;

public record TestTopologyManager(int clusterSize, NodeInfo self) implements TopologyManager {
    @Override
    public Option<NodeInfo> get(NodeId id) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Option<NodeId> reverseLookup(SocketAddress socketAddress) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public TimeSpan pingInterval() {
        return TimeSpan.timeSpan(1).seconds();
    }
}
