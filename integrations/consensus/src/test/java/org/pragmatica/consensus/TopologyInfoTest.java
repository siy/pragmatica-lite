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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopologyInfoTest {

    @Test
    void quorumSize_is_majority() {
        var topology = TopologyInfo.topologyInfo(NodeId.nodeId("node-1"), 3);

        assertThat(topology.quorumSize()).isEqualTo(2); // 3/2 + 1 = 2
    }

    @Test
    void quorumSize_for_five_nodes() {
        var topology = TopologyInfo.topologyInfo(NodeId.nodeId("node-1"), 5);

        assertThat(topology.quorumSize()).isEqualTo(3); // 5/2 + 1 = 3
    }

    @Test
    void fPlusOne_is_calculated_correctly() {
        var topology = TopologyInfo.topologyInfo(NodeId.nodeId("node-1"), 3);

        // f+1 = clusterSize - quorumSize + 1 = 3 - 2 + 1 = 2
        assertThat(topology.fPlusOne()).isEqualTo(2);
    }

    @Test
    void fPlusOne_for_five_nodes() {
        var topology = TopologyInfo.topologyInfo(NodeId.nodeId("node-1"), 5);

        // f+1 = 5 - 3 + 1 = 3
        assertThat(topology.fPlusOne()).isEqualTo(3);
    }

    @Test
    void self_returns_node_id() {
        var nodeId = NodeId.nodeId("test-node");
        var topology = TopologyInfo.topologyInfo(nodeId, 3);

        assertThat(topology.self()).isEqualTo(nodeId);
    }
}
