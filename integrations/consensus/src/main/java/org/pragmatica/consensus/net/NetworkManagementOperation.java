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
import org.pragmatica.messaging.Message;

import java.util.List;

public sealed interface NetworkManagementOperation extends Message.Local {
    record ConnectNode(NodeId node) implements NetworkManagementOperation {}

    record DisconnectNode(NodeId nodeId) implements NetworkManagementOperation {}

    record ListConnectedNodes() implements NetworkManagementOperation {}

    record ConnectedNodesList(List<NodeId> connected) implements NetworkManagementOperation {}

    /// Notification that a connection attempt to a node has failed.
    record ConnectionFailed(NodeId nodeId, Throwable cause) implements NetworkManagementOperation {}

    /// Notification that a connection to a node has been established.
    record ConnectionEstablished(NodeId nodeId) implements NetworkManagementOperation {}
}
