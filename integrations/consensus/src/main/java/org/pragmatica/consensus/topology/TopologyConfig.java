package org.pragmatica.consensus.topology;

import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.tcp.TlsConfig;

import java.util.List;

/// Configuration for cluster topology management.
///
/// @param self                   This node's ID
/// @param reconciliationInterval How often to reconcile cluster state
/// @param pingInterval           How often to ping other nodes
/// @param helloTimeout           Timeout for Hello handshake on new connections
/// @param coreNodes              Initial cluster members
/// @param tls                    TLS configuration for cluster communication (empty for plain TCP)
public record TopologyConfig(NodeId self,
                             TimeSpan reconciliationInterval,
                             TimeSpan pingInterval,
                             TimeSpan helloTimeout,
                             List<NodeInfo> coreNodes,
                             Option<TlsConfig> tls) {
    public TopologyConfig {
        coreNodes = List.copyOf(coreNodes);
    }

    private static final TimeSpan DEFAULT_HELLO_TIMEOUT = TimeSpan.timeSpan(5)
                                                                 .seconds();

    /// Create TopologyConfig without TLS and default hello timeout.
    public TopologyConfig(NodeId self,
                          TimeSpan reconciliationInterval,
                          TimeSpan pingInterval,
                          List<NodeInfo> coreNodes) {
        this(self, reconciliationInterval, pingInterval, DEFAULT_HELLO_TIMEOUT, coreNodes, Option.empty());
    }
}
