package org.pragmatica.cluster.leader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.leader.LeaderNotification.LeaderChange;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.lang.Option;
import org.pragmatica.message.MessageRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.cluster.leader.LeaderNotification.leaderChange;
import static org.pragmatica.cluster.topology.TopologyChangeNotification.nodeAdded;
import static org.pragmatica.cluster.topology.TopologyChangeNotification.nodeRemoved;
import static org.pragmatica.message.MessageRouter.messageRouter;

class LeaderManagerTest {
    record Watcher<T>(List<T> collected) {
        public void watch(T notification) {
            collected.add(notification);
        }
    }

    private final NodeId self = NodeId.randomNodeId();
    private final List<NodeId> nodes = List.of(NodeId.randomNodeId(), self, NodeId.randomNodeId());

    private final MessageRouter router = messageRouter();
    private final Watcher<LeaderNotification> watcher = new Watcher<>(new ArrayList<>());

    @BeforeEach
    void setUp() {
        watcher.collected().clear();
        router.addRoute(LeaderChange.class, watcher::watch);

        // It still does it work, but we don't have to keep reference to it
        LeaderManager.leaderManager(self, router);
    }

    @Test
    void nodesAddedThenQuorumReached() {
        var expected = simulateClusterStart();

        // When quorum disappears, we should see disappearance of the leader
        expected.add(leaderChange(Option.none(), false));
        router.route(QuorumStateNotification.DISAPPEARED);

        assertEquals(expected, watcher.collected());
    }

    @Test
    void nodesAddedAndRemovedLeaderStaysStable() {
        var expected = simulateClusterStart();

        sendNodeAdded(NodeId.randomNodeId());
        sendNodeRemoved(nodes.getLast());

        // When quorum disappears, we should see disappearance of the leader
        expected.add(leaderChange(Option.none(), false));
        router.route(QuorumStateNotification.DISAPPEARED);

        assertEquals(expected, watcher.collected());
    }

    @Test
    void leaderRemovedAndReplacedWithNewOne() {
        var expected = simulateClusterStart();

        sendNodeRemoved(self);
        // Here should appear new leader, which is not a local node
        expected.add(leaderChange(Option.option(nodes.getFirst()), false));

        // When quorum disappears, we should see disappearance of the leader
        expected.add(leaderChange(Option.none(), false));
        router.route(QuorumStateNotification.DISAPPEARED);

        assertEquals(expected, watcher.collected());
    }

    private void sendNodeAdded(NodeId nodeId) {
        var topology = Stream.concat(nodes.stream(), Stream.of(nodeId))
                             .sorted()
                             .toList();

        router.route(nodeAdded(nodeId, topology));
    }

    private void sendNodeRemoved(NodeId nodeId) {
        var topology = nodes.stream()
                            .filter(id -> !id.equals(nodeId))
                            .sorted()
                            .toList();

        router.route(nodeRemoved(nodeId, topology));
    }

    private List<LeaderNotification> simulateClusterStart() {
        var expected = new ArrayList<LeaderNotification>();
        var list = new ArrayList<NodeId>();
        for (var nodeId : nodes) {
            list.add(nodeId);

            var topology = list.stream().sorted().toList();

            if(nodeId.equals(nodes.getLast())) {
                // When quorum will be reached, we should see the current state of
                // the leader selection
                expected.add(leaderChange(Option.option(topology.getFirst()),
                                          self.equals(topology.getFirst())));

                router.route(QuorumStateNotification.ESTABLISHED);
            }

            router.route(nodeAdded(nodeId, topology));
        }

        return expected;
    }
}