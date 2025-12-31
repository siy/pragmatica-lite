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
 * Provides cluster topology information for consensus algorithms.
 */
public interface TopologyInfo {
    /**
     * This node's identifier.
     */
    NodeId self();

    /**
     * Configured cluster size.
     */
    int clusterSize();

    /**
     * The quorum size (majority) for the cluster.
     * Default: clusterSize / 2 + 1
     */
    default int quorumSize() {
        return clusterSize() / 2 + 1;
    }

    /**
     * The f+1 size where f is the maximum number of failures.
     * Default: clusterSize - quorumSize + 1
     */
    default int fPlusOne() {
        return clusterSize() - quorumSize() + 1;
    }

    /**
     * Create a simple topology info for a known cluster size.
     */
    static TopologyInfo topologyInfo(NodeId self, int clusterSize) {
        record simpleTopology(NodeId self, int clusterSize) implements TopologyInfo {}
        return new simpleTopology(self, clusterSize);
    }
}
