package org.pragmatica.cluster.leader;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.cluster.topology.TopologyChangeNotification.NodeAdded;
import org.pragmatica.cluster.topology.TopologyChangeNotification.NodeDown;
import org.pragmatica.cluster.topology.TopologyChangeNotification.NodeRemoved;
import org.pragmatica.message.MessageRouter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.pragmatica.cluster.leader.LeaderNotification.leaderChange;
import static org.pragmatica.lang.Option.option;

/// Leader manager is responsible for choosing which node is the leader.
/// Although consensus is leaderless, it is often necessary to have a single
/// source of truth for the cluster management. The leader node can be used for this purpose.
public interface LeaderManager {
    static LeaderManager leaderManager(NodeId self, MessageRouter router) {
        record leaderManager(NodeId self, MessageRouter router, AtomicBoolean active,
                             AtomicReference<NodeId> currentLeader) implements LeaderManager {
            public void nodeAdded(NodeAdded nodeAdded) {
                tryElect(nodeAdded.topology().getFirst());
            }

            public void nodeRemoved(NodeRemoved nodeRemoved) {
                // Should not happen, but better safe than sorry.
                if (nodeRemoved.topology().isEmpty()) {
                    return;
                }

                tryElect(nodeRemoved.topology().getFirst());
            }

            public void nodeDown(NodeDown nodeDown) {
                currentLeader().set(null);
                stop();
            }

            private void tryElect(NodeId candidate) {
                var oldLeader = currentLeader().get();

                // Potential leader change. Implicitly handles initial election.
                if (currentLeader().compareAndSet(oldLeader, candidate)) {
                    if (!candidate.equals(oldLeader)) {
                        notifyLeaderChange();
                    }
                }
            }

            private void notifyLeaderChange() {
                if (active().get()) {
                    var nodeId = currentLeader().get();
                    router().route(leaderChange(option(nodeId), self.equals(nodeId)));
                }
            }

            public void watchQuorumState(QuorumStateNotification quorumState) {
                switch (quorumState) {
                    case ESTABLISHED -> start();
                    case DISAPPEARED -> stop();
                }
            }

            private void start() {
                active.set(true);
                notifyLeaderChange();
            }

            private void stop() {
                currentLeader().set(null);
                notifyLeaderChange();
                active.set(false);
            }
        }

        var manager = new leaderManager(self, router, new AtomicBoolean(false), new AtomicReference<>());

        router.addRoute(NodeAdded.class, manager::nodeAdded);
        router.addRoute(NodeRemoved.class, manager::nodeRemoved);
        router.addRoute(NodeDown.class, manager::nodeDown);
        router.addRoute(QuorumStateNotification.class, manager::watchQuorumState);

        return manager;
    }
}
