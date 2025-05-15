package org.pragmatica.cluster.net;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.topology.ip.TopologyConfig;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.message.MessageRouter;
import org.pragmatica.net.NodeAddress;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.cluster.net.NodeId.randomNodeId;
import static org.pragmatica.cluster.net.NodeInfo.nodeInfo;
import static org.pragmatica.cluster.topology.ip.TcpTopologyManager.tcpTopologyManager;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class TcpTopologyManagerTest {
    private final MessageRouter router = MessageRouter.messageRouter();

    private TopologyManager topologyManager;
    private NodeId nodeId1;
    private NodeId nodeId2;
    private NodeId nodeId3;
    private NodeInfo nodeInfo1;
    private NodeInfo nodeInfo2;
    private InetSocketAddress socketAddress1;
    private InetSocketAddress socketAddress2;
    private InetSocketAddress socketAddress3;

    @BeforeEach
    void setUp() {
        nodeId1 = randomNodeId();
        nodeId2 = randomNodeId();
        nodeId3 = randomNodeId();

        socketAddress1 = new InetSocketAddress("localhost", 8080);
        socketAddress2 = new InetSocketAddress("127.0.0.1", 8082);
        socketAddress3 = new InetSocketAddress("127.0.0.1", 8083);

        nodeInfo1 = nodeInfo(nodeId1, NodeAddress.nodeAddress(socketAddress1));
        nodeInfo2 = nodeInfo(nodeId2, NodeAddress.nodeAddress(socketAddress2));

        var config = new TopologyConfig(nodeId1, timeSpan(100).hours(), TimeSpan.timeSpan(10).seconds(), List.of(nodeInfo1, nodeInfo2));

        topologyManager = tcpTopologyManager(config, router);
    }

    @Test
    void getReturnsNoneForNonExistentNode() {
        assertEquals(Option.none(), topologyManager.get(nodeId3));
    }

    @Test
    void getReturnsCorrectValueForExistingNodeId() {
        assertEquals(Option.some(nodeInfo1), topologyManager.get(nodeId1));
        assertEquals(Option.some(nodeInfo2), topologyManager.get(nodeId2));
    }

    @Test
    void reverseLookupReturnsNoneForNonExistentAddress() {
        assertEquals(Option.none(), topologyManager.reverseLookup(socketAddress3));
    }

    @Test
    void reverseLookupReturnsExistingAddress() {
        assertEquals(Option.some(nodeId1), topologyManager.reverseLookup(socketAddress1));
        assertEquals(Option.some(nodeId2), topologyManager.reverseLookup(socketAddress2));
    }

    @Test
    void clusterSizeReturnsCorrectSize() {
        assertEquals(2, topologyManager.clusterSize());
    }
}