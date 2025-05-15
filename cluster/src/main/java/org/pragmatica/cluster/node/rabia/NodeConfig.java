package org.pragmatica.cluster.node.rabia;

import org.pragmatica.cluster.consensus.rabia.ProtocolConfig;
import org.pragmatica.cluster.topology.ip.TopologyConfig;

public interface NodeConfig {
    ProtocolConfig protocol();

    TopologyConfig topology();

    static NodeConfig nodeConfig(ProtocolConfig protocol, TopologyConfig topology) {
        record nodeConfig(ProtocolConfig protocol, TopologyConfig topology) implements NodeConfig {}

        return new nodeConfig(protocol, topology);
    }
}
