package org.pragmatica.cluster.net.local;

import org.pragmatica.cluster.consensus.ProtocolMessage;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage;
import org.pragmatica.cluster.net.ClusterNetwork;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.message.MessageRouter;
import org.pragmatica.utility.Sleep;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Local network implementation suitable for testing purposes
public class LocalNetwork implements ClusterNetwork {
    public enum FaultType {
        MESSAGE_LOSS,
        MESSAGE_DELAY,
        MESSAGE_DUPLICATE,
        MESSAGE_REORDER,
        NODE_CRASH,
        NODE_PARTITION,
        NODE_BYZANTINE
    }

    private final Map<NodeId, Consumer<RabiaProtocolMessage>> nodes = new ConcurrentHashMap<>();
    private final TopologyManager topologyManager;
    private final Map<NodeId, List<NodeId>> partitions = new ConcurrentHashMap<>();
    private final Executor executor = Executors.newFixedThreadPool(5);
    private final MessageRouter router;
    private FaultInjector faultInjector;

    public LocalNetwork(TopologyManager topologyManager, MessageRouter router, FaultInjector faultInjector) {
        this.topologyManager = topologyManager;
        this.router = router;
        this.faultInjector = faultInjector;
    }

    public List<NodeId> connectedNodes() {
        return List.copyOf(nodes.keySet());
    }

    @Override
    public <M extends ProtocolMessage> void broadcast(M message) {
        nodes.keySet()
             .forEach(nodeId -> send(nodeId, message));
    }

    @Override
    public <M extends ProtocolMessage> void send(NodeId nodeId, M message) {
        // For Byzantine behavior - only check if it's a Byzantine node if we can get the sender
        var sender = message.sender();
        if (sender != null && faultInjector.isFaultyNode(sender, FaultType.NODE_BYZANTINE)) {
            // Handle Byzantine behavior - for now, just drop the message
            return;
        }

        if (nodes.containsKey(nodeId)) {
            processWithFaultInjection(nodeId, (RabiaProtocolMessage) message);
        }
    }

    public void disconnect(NodeId nodeId) {
        nodes.remove(nodeId);
        if (nodes.size() == topologyManager.quorumSize() - 1) {
            router.route(QuorumStateNotification.DISAPPEARED);
        }
    }

    @Override
    public Promise<Unit> start() {
        return Promise.unitPromise();
    }

    @Override
    public Promise<Unit> stop() {
        return Promise.unitPromise();
    }

    public void addNode(NodeId nodeId, Consumer<RabiaProtocolMessage> listener) {
        // Use processWithFaultInjection to wrap the listener
        nodes.put(nodeId, listener);
        if (nodes.size() == topologyManager.quorumSize()) {
            router.route(QuorumStateNotification.ESTABLISHED);
        }
    }

    // Method to process messages with fault injection
    protected <M extends RabiaProtocolMessage> void processWithFaultInjection(NodeId destination, M message) {
        var sender = message.sender();

        // Check for the node crash
        if (sender != null && faultInjector.isFaultyNode(destination, FaultType.NODE_CRASH)) {
            return; // Dropped
        }

        // Check for node partition
        if (sender != null && partitions.containsKey(sender) &&
                partitions.get(sender).contains(destination)) {
            return; // Dropped due to partition
        }

        // Check for message loss
        if (faultInjector.shouldDropMessage()) {
            return; // Dropped
        }

        // Check for message delay
        if (faultInjector.shouldDelayMessage()) {
            Sleep.sleep(faultInjector.messageDelay());
        }

        // Process the message
        var listener = nodes.get(destination);

        if (listener == null) {
            return;
        }

        executor.execute(() -> {
            listener.accept(message);

            // Check for message duplication
            if (faultInjector.shouldDuplicateMessage()) {
                listener.accept(message);
            }
        });
    }

    // Network partition management
    public void createPartition(Collection<NodeId> group1, Collection<NodeId> group2) {
        for (var node1 : group1) {
            for (var node2 : group2) {
                partitions.computeIfAbsent(node1, _ -> new ArrayList<>()).add(node2);
                partitions.computeIfAbsent(node2, _ -> new ArrayList<>()).add(node1);
            }
        }
    }

    public void healPartitions() {
        partitions.clear();
    }

    public FaultInjector getFaultInjector() {
        return faultInjector;
    }

    public void setFaultInjector(FaultInjector faultInjector) {
        this.faultInjector = faultInjector;
    }

    // Fault injector implementation
    public static class FaultInjector {
        private final Map<FaultType, Boolean> activeFaults = new EnumMap<>(FaultType.class);
        private final Map<NodeId, Set<FaultType>> nodeSpecificFaults = new ConcurrentHashMap<>();
        private final Random random = new Random();
        private double messageLossRate = 0.0;
        private TimeSpan messageDelay = TimeSpan.timeSpan(0).millis();

        public FaultInjector() {
            Stream.of(FaultType.values())
                  .forEach(type -> activeFaults.put(type, false));
        }

        public void setFault(FaultType type, boolean active) {
            activeFaults.put(type, active);
        }

        public void setNodeFault(NodeId nodeId, FaultType type, boolean active) {
            nodeSpecificFaults.computeIfAbsent(nodeId, _ -> EnumSet.noneOf(FaultType.class));
            if (active) {
                nodeSpecificFaults.get(nodeId).add(type);
            } else {
                nodeSpecificFaults.get(nodeId).remove(type);
            }
        }

        public void setMessageLossRate(double rate) {
            this.messageLossRate = Math.max(0.0, Math.min(1.0, rate));
        }

        public void messageDelay(TimeSpan delay) {
            this.messageDelay = delay;
        }

        public boolean shouldDropMessage() {
            return activeFaults.get(FaultType.MESSAGE_LOSS) && random.nextDouble() < messageLossRate;
        }

        public boolean shouldDelayMessage() {
            return activeFaults.get(FaultType.MESSAGE_DELAY);
        }

        public TimeSpan messageDelay() {
            return messageDelay;
        }

        public boolean shouldDuplicateMessage() {
            return activeFaults.get(FaultType.MESSAGE_DUPLICATE);
        }

        public boolean isFaultyNode(NodeId nodeId, FaultType type) {
            return nodeSpecificFaults.containsKey(nodeId) &&
                    nodeSpecificFaults.get(nodeId).contains(type);
        }

        public void clearAllFaults() {
            for (FaultType type : FaultType.values()) {
                activeFaults.put(type, false);
            }
            nodeSpecificFaults.clear();
            messageLossRate = 0.0;
            messageDelay = TimeSpan.timeSpan(0).millis();
        }
    }
}
