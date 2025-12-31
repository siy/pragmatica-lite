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

package org.pragmatica.consensus.topology;

import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.net.NodeInfo;
import org.pragmatica.messaging.Message;

import java.util.List;

/**
 * Notification related to topology change and discovery.
 */
sealed public interface TopologyManagementMessage extends Message.Local {
    record AddNode(NodeInfo nodeInfo) implements TopologyManagementMessage {}

    record RemoveNode(NodeId nodeId) implements TopologyManagementMessage {}

    record DiscoverNodes(NodeId sender) implements TopologyManagementMessage {}

    record DiscoveredNodes(List<NodeInfo> nodeInfos) implements TopologyManagementMessage {}
}
