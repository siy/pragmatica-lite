package org.pragmatica.cluster.net.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.pragmatica.cluster.consensus.ProtocolMessage;
import org.pragmatica.cluster.net.*;
import org.pragmatica.cluster.net.NetworkManagementOperation.ListConnectedNodes;
import org.pragmatica.cluster.net.NetworkMessage.Ping;
import org.pragmatica.cluster.net.NetworkMessage.Pong;
import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.Serializer;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.cluster.topology.TopologyChangeNotification;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.utils.SharedScheduler;
import org.pragmatica.message.Message;
import org.pragmatica.message.MessageRouter;
import org.pragmatica.net.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.pragmatica.cluster.net.NetworkManagementOperation.ConnectNode;
import static org.pragmatica.cluster.net.NetworkManagementOperation.DisconnectNode;
import static org.pragmatica.cluster.net.netty.NettyClusterNetwork.ViewChangeOperation.*;

/**
 * Manages network connections between nodes using Netty.
 */
public class NettyClusterNetwork implements ClusterNetwork {
    private static final Logger log = LoggerFactory.getLogger(NettyClusterNetwork.class);
    private static final double SCALE = 0.3d;

    private static final int LENGTH_FIELD_LEN = 4;
    private static final int INITIAL_BYTES_TO_STRIP = LENGTH_FIELD_LEN;

    private final NodeInfo self;
    private final Map<NodeId, Channel> peerLinks = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final TopologyManager topologyManager;
    private final Supplier<List<ChannelHandler>> handlers;
    private final MessageRouter router;
    private final AtomicReference<Server> server = new AtomicReference<>();

    enum ViewChangeOperation {
        ADD, REMOVE, SHUTDOWN
    }

    public NettyClusterNetwork(TopologyManager topologyManager,
                               Serializer serializer,
                               Deserializer deserializer,
                               MessageRouter router) {
        this.self = topologyManager.self();
        this.topologyManager = topologyManager;
        this.router = router;
        this.handlers = () -> List.of(
                new LengthFieldBasedFrameDecoder(1048576, 0, LENGTH_FIELD_LEN, 0, INITIAL_BYTES_TO_STRIP),
                new LengthFieldPrepender(LENGTH_FIELD_LEN),
                new Decoder(deserializer),
                new Encoder(serializer),
                new Handler(this::peerConnected, this::peerDisconnected, router::route));

        router.addRoute(ConnectNode.class, this::connect);
        router.addRoute(DisconnectNode.class, this::disconnect);
        router.addRoute(ListConnectedNodes.class, this::listNodes);

        router.addRoute(Ping.class, this::handlePing);
        router.addRoute(Pong.class, this::handlePong);

        schedulePing();
    }

    private void schedulePing() {
        SharedScheduler.schedule(this::pingNodes, topologyManager.pingInterval().randomize(SCALE));
    }

    private Option<NodeId> randomNode() {
        if (!isRunning.get() || peerLinks.isEmpty()) {
            return Option.none();
        }

        var ids = new ArrayList<>(peerLinks.keySet());
        Collections.shuffle(ids);

        return Option.some(ids.getFirst());
    }

    private void pingNodes() {
        randomNode()
                .onPresent(peerId -> sendToChannel(peerId, new Ping(self.id()), peerLinks.get(peerId)));

        schedulePing();
    }

    private void handlePong(Pong pong) {
        log.debug("Node {} received pong from {}", self, pong.sender());
    }

    private void handlePing(Ping ping) {
        log.debug("Node {} received ping from {}", self.id(), ping.sender());
        sendToChannel(ping.sender(), new Pong(self.id()), peerLinks.get(ping.sender()));
    }

    private void listNodes(ListConnectedNodes listConnectedNodes) {
        router.route(new NetworkManagementOperation.ConnectedNodesList(List.copyOf(peerLinks.keySet())));
    }

    private void peerConnected(Channel channel) {
        topologyManager.reverseLookup(channel.remoteAddress())
                       .orElse(() -> topologyManager.reverseLookup(channel.localAddress()))
                       .onPresent(nodeId -> peerLinks.put(nodeId, channel))
                       .onPresent(nodeId -> log.debug("Node {} connected", nodeId))
                       .onPresent(nodeId -> processViewChange(ADD, nodeId))
                       .onEmpty(() -> log.warn("Unknown node {}, disconnecting", channel.remoteAddress()))
                       .onEmpty(() -> channel.close()
                                             .addListener(_ -> log.info("Host {} disconnected",
                                                                        channel.remoteAddress())));
    }

    private void peerDisconnected(Channel channel) {
        topologyManager.reverseLookup(channel.remoteAddress())
                       .onPresent(peerLinks::remove)
                       .onPresent(nodeId -> processViewChange(REMOVE, nodeId))
                       .onPresent(nodeId -> log.debug("Node {} diconnected", nodeId));
    }

    @Override
    public Promise<Unit> start() {
        if (isRunning.compareAndSet(false, true)) {
            return Server.server("NettyClusterNetwork", self.address().port(), handlers)
                         .onSuccess(NettyClusterNetwork.this.server::set)
                         .onFailure(_ -> isRunning.set(false))
                         .mapToUnit();
        }

        return Promise.unitPromise();
    }

    @Override
    public Promise<Unit> stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Stopping {}: notifying view change", server.get().name());
            processViewChange(SHUTDOWN, self.id());

            return server.get()
                         .stop(() -> {
                             log.info("Stopping {}: closing peer connections", server.get().name());

                             var promises = new ArrayList<Promise<Unit>>();
                             for (var link : peerLinks.values()) {
                                 var promise = Promise.<Unit>promise();
                                 link.close()
                                     .addListener(future -> {
                                         if (future.isSuccess()) {
                                             promise.succeed(Unit.unit());
                                         } else {
                                             promise.fail(Causes.fromThrowable(future.cause()));
                                         }
                                     });
                                 promises.add(promise);
                             }
                             return Promise.allOf(promises)
                                           .mapToUnit();
                         });
        }

        return Promise.unitPromise();
    }

    private void connect(ConnectNode connectNode) {
        if (!isRunning.get()) {
            log.error("Attempt to connect {} while node is not running", connectNode.node());
            return;
        }
        topologyManager.get(connectNode.node())
                       .onPresent(this::connectPeer)
                       .onEmpty(() -> log.error("Unknown {}", connectNode.node()));
    }

    private void connectPeer(NodeInfo nodeInfo) {
        var peerId = nodeInfo.id();

        if (peerLinks.containsKey(peerId)) {
            log.warn("Node {} already connected", peerId);
            return;
        }

        // Executed asynchronously to avoid blocking the main thread
        server.get()
              .connectTo(nodeInfo.address())
              .onSuccess(channel -> {
                  peerLinks.put(peerId, channel);
                  processViewChange(ADD, peerId);
              })
              .onFailure(cause -> log.warn("Node {} failed to connect to {}: {}", peerId, nodeInfo.id(), cause));
    }

    private void disconnect(DisconnectNode disconnectNode) {
        var channel = peerLinks.remove(disconnectNode.nodeId());

        if (channel == null) {
            return;
        }

        processViewChange(REMOVE, disconnectNode.nodeId());

        // Executed asynchronously to avoid blocking the main thread
        channel.close()
               .addListener(future -> {
                   if (future.isSuccess()) {
                       log.info("Node {} disconnected from node {}", self.id(), disconnectNode.nodeId());
                   } else {
                       log.warn("Node {} failed to disconnect from node {}: ",
                                self.id(),
                                disconnectNode.nodeId(),
                                future.cause());
                   }
               });
    }

    @Override
    public <M extends ProtocolMessage> void send(NodeId peerId, M message) {
        sendToChannel(peerId, message, peerLinks.get(peerId));
    }

    private <M extends Message.Wired> void sendToChannel(NodeId peerId, M message, Channel channel) {
        if (channel == null) {
            log.warn("Node {} is not connected", peerId);
            return;
        }

        if (!channel.isActive()) {
            peerLinks.remove(peerId);
            processViewChange(REMOVE, peerId);
            log.warn("Node {} is not active", peerId);
            return;
        }

//        log.info("Node {} sending message {} to {}", self.id(), message, peerId);

        channel.writeAndFlush(message);
    }

    @Override
    public <M extends ProtocolMessage> void broadcast(M message) {
        peerLinks.forEach((peerId, channel) -> sendToChannel(peerId, message, channel));
    }

    private void processViewChange(ViewChangeOperation operation, NodeId peerId) {
        var viewChange = switch (operation) {
            case ADD -> {
                if ((peerLinks.size() + 1) == topologyManager.quorumSize()) {
                    router.route(QuorumStateNotification.ESTABLISHED);
                }
                yield TopologyChangeNotification.nodeAdded(peerId, currentView());
            }
            case REMOVE -> {
                if ((peerLinks.size() + 1) == topologyManager.quorumSize() - 1) {
                    router.route(QuorumStateNotification.DISAPPEARED);
                }
                yield TopologyChangeNotification.nodeRemoved(peerId, currentView());
            }
            case SHUTDOWN -> {
                router.route(QuorumStateNotification.DISAPPEARED);
                yield TopologyChangeNotification.nodeDown(peerId);
            }
        };

        router.route(viewChange);
    }

    private List<NodeId> currentView() {
        return peerLinks.keySet()
                        .stream()
                        .sorted()
                        .toList();
    }
}
