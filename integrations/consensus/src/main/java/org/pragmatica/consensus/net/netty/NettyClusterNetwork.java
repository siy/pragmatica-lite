package org.pragmatica.consensus.net.netty;

import org.pragmatica.consensus.ProtocolMessage;
import org.pragmatica.consensus.net.ClusterNetwork;
import org.pragmatica.consensus.net.NetworkManagementOperation;
import org.pragmatica.consensus.net.NetworkManagementOperation.ListConnectedNodes;
import org.pragmatica.consensus.net.NetworkMessage.Hello;
import org.pragmatica.consensus.net.NetworkMessage.Ping;
import org.pragmatica.consensus.net.NetworkMessage.Pong;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.consensus.topology.QuorumStateNotification;
import org.pragmatica.consensus.topology.TopologyChangeNotification;
import org.pragmatica.consensus.topology.TopologyManager;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.utils.SharedScheduler;
import org.pragmatica.messaging.Message;
import org.pragmatica.messaging.MessageRouter;
import org.pragmatica.net.tcp.Server;
import org.pragmatica.net.tcp.ServerConfig;
import org.pragmatica.serialization.Deserializer;
import org.pragmatica.serialization.Serializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.consensus.net.NetworkManagementOperation.ConnectNode;
import static org.pragmatica.consensus.net.NetworkManagementOperation.DisconnectNode;
import static org.pragmatica.consensus.net.netty.NettyClusterNetwork.ViewChangeOperation.*;

/// Manages network connections between nodes using Netty.
public class NettyClusterNetwork implements ClusterNetwork {
    private static final Logger log = LoggerFactory.getLogger(NettyClusterNetwork.class);
    private static final double SCALE = 0.3d;

    private static final int LENGTH_FIELD_LEN = 4;
    private static final int INITIAL_BYTES_TO_STRIP = LENGTH_FIELD_LEN;

    private final NodeInfo self;
    private final Map<NodeId, Channel> peerLinks = new ConcurrentHashMap<>();
    private final Map<Channel, NodeId> channelToNodeId = new ConcurrentHashMap<>();
    private final Set<Channel> pendingChannels = ConcurrentHashMap.newKeySet();
    private final Map<Channel, ScheduledFuture<?>> helloTimeouts = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final TopologyManager topologyManager;
    private final Supplier<List<ChannelHandler>> handlers;
    private final MessageRouter router;
    private final AtomicReference<Server> server = new AtomicReference<>();

    enum ViewChangeOperation {
        ADD,
        REMOVE,
        SHUTDOWN
    }

    public NettyClusterNetwork(TopologyManager topologyManager,
                               Serializer serializer,
                               Deserializer deserializer,
                               MessageRouter router) {
        this(topologyManager, serializer, deserializer, router, List.of());
    }

    public NettyClusterNetwork(TopologyManager topologyManager,
                               Serializer serializer,
                               Deserializer deserializer,
                               MessageRouter router,
                               List<ChannelHandler> additionalHandlers) {
        this.self = topologyManager.self();
        this.topologyManager = topologyManager;
        this.router = router;
        this.handlers = () -> {
            var result = new ArrayList<ChannelHandler>();
            result.add(new LengthFieldBasedFrameDecoder(1048576, 0, LENGTH_FIELD_LEN, 0, INITIAL_BYTES_TO_STRIP));
            result.add(new LengthFieldPrepender(LENGTH_FIELD_LEN));
            result.add(new Decoder(deserializer));
            result.add(new Encoder(serializer));
            result.addAll(additionalHandlers);
            result.add(new Handler(this::peerConnected, this::peerDisconnected, this::handleHello, router::route));
            return result;
        };
        schedulePing();
    }

    private void schedulePing() {
        SharedScheduler.schedule(this::pingNodes,
                                 topologyManager.pingInterval()
                                                .randomize(SCALE));
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
        randomNode().onPresent(peerId -> sendToChannel(peerId, new Ping(self.id()), peerLinks.get(peerId)));
        schedulePing();
    }

    @Override
    public void handlePing(Ping ping) {
        log.debug("Node {} received ping from {}", self.id(), ping.sender());
        sendToChannel(ping.sender(),
                      new Pong(self.id()),
                      peerLinks.get(ping.sender()));
    }

    @Override
    public void handlePong(Pong pong) {
        log.debug("Node {} received pong from {}", self, pong.sender());
    }

    @Override
    public void listNodes(ListConnectedNodes listConnectedNodes) {
        router.route(new NetworkManagementOperation.ConnectedNodesList(List.copyOf(peerLinks.keySet())));
    }

    private void peerConnected(Channel channel) {
        pendingChannels.add(channel);
        channel.writeAndFlush(new Hello(self.id()));
        scheduleHelloTimeout(channel);
        log.debug("Channel active, sent Hello and waiting for response: {}", channel.remoteAddress());
    }

    private void scheduleHelloTimeout(Channel channel) {
        var timeout = SharedScheduler.schedule(() -> onHelloTimeout(channel), topologyManager.helloTimeout());
        helloTimeouts.put(channel, timeout);
    }

    private void onHelloTimeout(Channel channel) {
        helloTimeouts.remove(channel);
        if (pendingChannels.remove(channel)) {
            log.warn("Hello timeout for channel {}, closing", channel.remoteAddress());
            channel.close();
        }
    }

    private void handleHello(Hello hello, Channel channel) {
        var timeout = helloTimeouts.remove(channel);
        if (timeout != null) {
            timeout.cancel(false);
        }
        if (!pendingChannels.remove(channel)) {
            log.debug("Received Hello from {} on already established channel", hello.sender());
            return;
        }
        if (topologyManager.get(hello.sender())
                           .isEmpty()) {
            log.warn("Received Hello from unknown node {}, closing", hello.sender());
            channel.close();
            return;
        }
        var existing = peerLinks.putIfAbsent(hello.sender(), channel);
        if (existing != null) {
            log.debug("Duplicate connection from {}, closing new channel", hello.sender());
            channel.close();
            return;
        }
        channelToNodeId.put(channel, hello.sender());
        processViewChange(ADD, hello.sender());
        log.debug("Node {} connected via Hello handshake", hello.sender());
    }

    private void peerDisconnected(Channel channel) {
        helloTimeouts.remove(channel);
        pendingChannels.remove(channel);
        var nodeId = channelToNodeId.remove(channel);
        if (nodeId != null && peerLinks.remove(nodeId, channel)) {
            processViewChange(REMOVE, nodeId);
            log.debug("Node {} disconnected", nodeId);
        }
    }

    @Override
    public Promise<Unit> start() {
        if (isRunning.compareAndSet(false, true)) {
            var serverConfig = ServerConfig.serverConfig("NettyClusterNetwork",
                                                         self.address()
                                                             .port());
            // Apply TLS for both incoming (server) and outgoing (client) connections
            var effectiveConfig = topologyManager.tls()
                                                 .map(tls -> serverConfig.withTls(tls)
                                                                         .withClientTls(tls))
                                                 .or(serverConfig);
            return Server.server(effectiveConfig, handlers)
                         .onSuccess(NettyClusterNetwork.this.server::set)
                         .onFailure(_ -> isRunning.set(false))
                         .mapToUnit();
        }
        return Promise.unitPromise();
    }

    @Override
    public Promise<Unit> stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Stopping {}: notifying view change",
                     server.get()
                           .name());
            processViewChange(SHUTDOWN, self.id());
            return server.get()
                         .stop(this::onStop);
        }
        return Promise.unitPromise();
    }

    private Promise<Unit> onStop() {
        log.info("Stopping {}: closing peer connections",
                 server.get()
                       .name());
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
    }

    @Override
    public void connect(ConnectNode connectNode) {
        if (server.get() == null) {
            log.error("Attempt to connect {} while node is not running", connectNode.node());
            return;
        }
        if (connectNode.node()
                       .equals(self.id())) {
            return;
        }
        topologyManager.get(connectNode.node())
                       .onPresent(this::connectPeer)
                       .onEmpty(() -> log.error("Unknown {}",
                                                connectNode.node()));
    }

    private void connectPeer(NodeInfo nodeInfo) {
        var peerId = nodeInfo.id();
        if (peerLinks.containsKey(peerId)) {
            return;
        }
        server.get()
              .connectTo(nodeInfo.address())
              .onFailure(cause -> log.warn("Failed to connect to {}: {}", peerId, cause));
    }

    @Override
    public void disconnect(DisconnectNode disconnectNode) {
        var channel = peerLinks.remove(disconnectNode.nodeId());
        if (channel == null) {
            return;
        }
        channelToNodeId.remove(channel);
        processViewChange(REMOVE, disconnectNode.nodeId());
        channel.close()
               .addListener(future -> {
                                if (future.isSuccess()) {
                                    log.info("Node {} disconnected from node {}",
                                             self.id(),
                                             disconnectNode.nodeId());
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
            channelToNodeId.remove(channel);
            processViewChange(REMOVE, peerId);
            log.warn("Node {} is not active", peerId);
            return;
        }
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
        // Include self in the view so leader election considers all nodes
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(self.id()),
                                              peerLinks.keySet()
                                                       .stream())
                   .sorted()
                   .toList();
    }

    @Override
    public int connectedNodeCount() {
        return peerLinks.size();
    }

    @Override
    public Option<Server> server() {
        return Option.option(server.get());
    }
}
