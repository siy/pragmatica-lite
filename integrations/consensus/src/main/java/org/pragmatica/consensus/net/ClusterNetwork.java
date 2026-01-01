/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.consensus.net;

import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.ProtocolMessage;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.messaging.MessageReceiver;

/**
 * Generalized Network API for cluster communication.
 * <p>
 * <b>Implementation contract:</b> All methods must be exception-safe.
 * Network failures should be logged internally, not thrown to callers.
 * The consensus protocol handles message loss through timeouts and retries.
 */
public interface ClusterNetwork {
    /**
     * Broadcast a message to all nodes in the cluster.
     * <p>
     * Note that actual implementation may just send messages directly,
     * not necessarily use broadcasting in underlying protocol.
     * <p>
     * Implementations must not throw exceptions. Failed sends should be
     * logged and silently ignored - the protocol handles message loss.
     */
    <M extends ProtocolMessage> void broadcast(M message);

    @MessageReceiver
    void connect(NetworkManagementOperation.ConnectNode connectNode);

    @MessageReceiver
    void disconnect(NetworkManagementOperation.DisconnectNode disconnectNode);

    @MessageReceiver
    void listNodes(NetworkManagementOperation.ListConnectedNodes listConnectedNodes);

    @MessageReceiver
    void handlePing(NetworkMessage.Ping ping);

    @MessageReceiver
    void handlePong(NetworkMessage.Pong pong);

    /**
     * Send a message to a specific node.
     * <p>
     * Implementations must not throw exceptions. Failed sends should be
     * logged and silently ignored - the protocol handles message loss.
     */
    <M extends ProtocolMessage> void send(NodeId nodeId, M message);

    /**
     * Start the network.
     */
    Promise<Unit> start();

    /**
     * Stop the network.
     */
    Promise<Unit> stop();
}
