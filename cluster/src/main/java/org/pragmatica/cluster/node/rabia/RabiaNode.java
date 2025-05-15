package org.pragmatica.cluster.node.rabia;

import org.pragmatica.cluster.consensus.rabia.RabiaEngine;
import org.pragmatica.cluster.leader.LeaderManager;
import org.pragmatica.cluster.net.ClusterNetwork;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.cluster.topology.ip.TcpTopologyManager;
import org.pragmatica.cluster.net.netty.NettyClusterNetwork;
import org.pragmatica.cluster.node.ClusterNode;
import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.Serializer;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.cluster.state.StateMachine;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.message.MessageRouter;

import java.util.List;

public interface RabiaNode<C extends Command> extends ClusterNode<C> {

    @SuppressWarnings("unused")
    MessageRouter router();

    static <C extends Command> RabiaNode<C> rabiaNode(NodeConfig config,
                                                      MessageRouter router,
                                                      StateMachine<C> stateMachine,
                                                      Serializer serializer,
                                                      Deserializer deserializer) {
        record rabiaNode<C extends Command>(NodeConfig config,
                                            MessageRouter router,
                                            StateMachine<C> stateMachine,
                                            ClusterNetwork network,
                                            TopologyManager topologyManager,
                                            RabiaEngine<C> consensus,
                                            LeaderManager leaderManager) implements RabiaNode<C> {
            @Override
            public NodeId self() {
                return config().topology().self();
            }

            @Override
            public Promise<Unit> start() {
                return network().start()
                                .onSuccessRunAsync(topologyManager()::start)
                                .flatMap(consensus()::start);
            }

            @Override
            public Promise<Unit> stop() {
                return consensus().stop()
                                  .onResultRun(topologyManager()::stop)
                                  .flatMap(network()::stop);
            }

            @Override
            public <R> Promise<List<R>> apply(List<C> commands) {
                return consensus().apply(commands);
            }
        }

        var topologyManager = TcpTopologyManager.tcpTopologyManager(config.topology(), router);
        var leaderManager = LeaderManager.leaderManager(config.topology().self(), router);
        var network = new NettyClusterNetwork(topologyManager, serializer, deserializer, router);
        var consensus = new RabiaEngine<>(topologyManager, network, stateMachine,
                                          router, config.protocol());

        return new rabiaNode<>(config, router, stateMachine, network, topologyManager, consensus, leaderManager);
    }
}
