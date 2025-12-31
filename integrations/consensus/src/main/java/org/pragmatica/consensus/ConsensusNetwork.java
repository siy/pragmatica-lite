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

package org.pragmatica.consensus;

/**
 * Network abstraction for consensus protocol communication.
 * Implementations handle the actual network transport.
 */
public interface ConsensusNetwork {
    /**
     * Broadcast a message to all nodes in the cluster.
     */
    <M extends ProtocolMessage> void broadcast(M message);

    /**
     * Send a message to a specific node.
     */
    <M extends ProtocolMessage> void send(NodeId nodeId, M message);
}
