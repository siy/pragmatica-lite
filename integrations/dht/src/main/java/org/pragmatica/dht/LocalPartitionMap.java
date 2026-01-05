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

package org.pragmatica.dht;

import org.pragmatica.lang.Promise;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Local partition map implementation using consistent hashing.
/// Suitable for single-process clusters or testing.
///
/// @param <N> Node identifier type (must be Comparable for consistent ordering)
public final class LocalPartitionMap<N extends Comparable<N>> implements PartitionMap<N> {
    private final ConsistentHashRing<N> ring;

    private LocalPartitionMap(ConsistentHashRing<N> ring) {
        this.ring = ring;
    }

    public static <N extends Comparable<N>> LocalPartitionMap<N> localPartitionMap() {
        return new LocalPartitionMap<>(ConsistentHashRing.<N>consistentHashRing());
    }

    public static <N extends Comparable<N>> LocalPartitionMap<N> localPartitionMap(int virtualNodesPerPhysical) {
        return new LocalPartitionMap<>(ConsistentHashRing.<N>consistentHashRing(virtualNodesPerPhysical));
    }

    @Override
    public Promise<List<N>> nodesFor(Partition partition, int replicaCount) {
        // Use partition value as key for consistent node selection
        String partitionKey = "partition:" + partition.value();
        List<N> nodes = ring.nodesFor(partitionKey, replicaCount);
        return Promise.success(nodes);
    }

    @Override
    public Promise<Set<Partition>> partitionsFor(N node) {
        Set<Partition> partitions = new HashSet<>();
        for (int i = 0; i < Partition.MAX_PARTITIONS; i++ ) {
            Partition partition = Partition.partitionUnsafe(i);
            String partitionKey = "partition:" + i;
            ring.primaryFor(partitionKey)
                .onPresent(primary -> {
                    if (primary.equals(node)) {
                        partitions.add(partition);
                    }
                });
        }
        return Promise.success(partitions);
    }

    @Override
    public Promise<Set<Partition>> allPartitionsFor(N node, int replicaCount) {
        Set<Partition> partitions = new HashSet<>();
        for (int i = 0; i < Partition.MAX_PARTITIONS; i++ ) {
            Partition partition = Partition.partitionUnsafe(i);
            String partitionKey = "partition:" + i;
            List<N> nodes = ring.nodesFor(partitionKey, replicaCount);
            if (nodes.contains(node)) {
                partitions.add(partition);
            }
        }
        return Promise.success(partitions);
    }

    @Override
    public Promise<Void> updateTopology(Set<N> nodes) {
        synchronized (ring) {
            // Remove nodes no longer in topology
            Set<N> currentNodes = new HashSet<>(ring.nodes());
            for (N node : currentNodes) {
                if (!nodes.contains(node)) {
                    ring.removeNode(node);
                }
            }
            // Add new nodes
            for (N node : nodes) {
                ring.addNode(node);
            }
        }
        return Promise.success(null);
    }

    /// Add a node to the partition map.
    public void addNode(N node) {
        ring.addNode(node);
    }

    /// Remove a node from the partition map.
    public void removeNode(N node) {
        ring.removeNode(node);
    }

    /// Get all nodes in the map.
    public Set<N> nodes() {
        return ring.nodes();
    }
}
