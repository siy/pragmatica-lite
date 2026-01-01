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

import org.pragmatica.lang.Option;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Consistent hash ring for distributing data across nodes.
 * Uses virtual nodes for even distribution and MurmurHash3-like hashing.
 * <p>
 * The ring maps keys to partitions (0-1023), and partitions to nodes.
 * Each physical node has multiple virtual nodes spread across the ring
 * for better load distribution.
 *
 * @param <N> Node identifier type
 */
public final class ConsistentHashRing<N extends Comparable<N>> {
    private static final int VIRTUAL_NODES_PER_PHYSICAL = 150;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final NavigableMap<Integer, N> ring = new TreeMap<>();
    private final Map<N, List<Integer>> nodeToVirtualNodes = new HashMap<>();
    private final int virtualNodesPerPhysical;

    private ConsistentHashRing(int virtualNodesPerPhysical) {
        this.virtualNodesPerPhysical = virtualNodesPerPhysical;
    }

    /**
     * Create a new empty consistent hash ring with default virtual node count.
     */
    public static <N extends Comparable<N>> ConsistentHashRing<N> consistentHashRing() {
        return new ConsistentHashRing<>(VIRTUAL_NODES_PER_PHYSICAL);
    }

    /**
     * Create a new empty consistent hash ring with specified virtual node count.
     */
    public static <N extends Comparable<N>> ConsistentHashRing<N> consistentHashRing(int virtualNodesPerPhysical) {
        return new ConsistentHashRing<>(virtualNodesPerPhysical);
    }

    /**
     * Add a node to the ring.
     */
    public void addNode(N node) {
        lock.writeLock()
            .lock();
        try{
            if (nodeToVirtualNodes.containsKey(node)) {
                return;
            }
            List<Integer> virtualNodes = new ArrayList<>(virtualNodesPerPhysical);
            for (int i = 0; i < virtualNodesPerPhysical; i++ ) {
                int hash = hash(node.toString() + "#" + i);
                ring.put(hash, node);
                virtualNodes.add(hash);
            }
            nodeToVirtualNodes.put(node, virtualNodes);
        } finally{
            lock.writeLock()
                .unlock();
        }
    }

    /**
     * Remove a node from the ring.
     */
    public void removeNode(N node) {
        lock.writeLock()
            .lock();
        try{
            List<Integer> virtualNodes = nodeToVirtualNodes.remove(node);
            if (virtualNodes != null) {
                virtualNodes.forEach(ring::remove);
            }
        } finally{
            lock.writeLock()
                .unlock();
        }
    }

    /**
     * Get the partition for a given key.
     * Note: This method does not require locking as it only uses the hash function.
     */
    public Partition partitionFor(byte[] key) {
        int hash = hash(key);
        // Use bitwise AND to ensure non-negative result (Math.abs fails for Integer.MIN_VALUE)
        return Partition.partitionUnsafe((hash & 0x7FFFFFFF) % Partition.MAX_PARTITIONS);
    }

    /**
     * Get the partition for a given string key.
     */
    public Partition partitionFor(String key) {
        return partitionFor(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the primary node for a given key.
     * Returns empty if no nodes are in the ring.
     */
    public Option<N> primaryFor(byte[] key) {
        lock.readLock()
            .lock();
        try{
            if (ring.isEmpty()) {
                return Option.none();
            }
            int hash = hash(key);
            return Option.some(getNodeForHash(hash));
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Get the primary node for a given string key.
     */
    public Option<N> primaryFor(String key) {
        return primaryFor(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the primary and replica nodes for a given key.
     * Returns up to replicaCount nodes, starting with primary.
     */
    public List<N> nodesFor(byte[] key, int replicaCount) {
        lock.readLock()
            .lock();
        try{
            if (ring.isEmpty()) {
                return List.of();
            }
            if (replicaCount <= 0) {
                return List.of();
            }
            int hash = hash(key);
            Set<N> seen = new LinkedHashSet<>();
            // Start from the hash position and walk clockwise
            Integer current = ring.ceilingKey(hash);
            if (current == null) {
                current = ring.firstKey();
            }
            while (seen.size() < replicaCount && seen.size() < nodeToVirtualNodes.size()) {
                N node = ring.get(current);
                seen.add(node);
                current = ring.higherKey(current);
                if (current == null) {
                    current = ring.firstKey();
                }
            }
            return new ArrayList<>(seen);
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Get the primary and replica nodes for a given string key.
     */
    public List<N> nodesFor(String key, int replicaCount) {
        return nodesFor(key.getBytes(StandardCharsets.UTF_8), replicaCount);
    }

    /**
     * Get all nodes currently in the ring.
     */
    public Set<N> nodes() {
        lock.readLock()
            .lock();
        try{
            return new HashSet<>(nodeToVirtualNodes.keySet());
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Get the number of nodes in the ring.
     */
    public int nodeCount() {
        lock.readLock()
            .lock();
        try{
            return nodeToVirtualNodes.size();
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Check if the ring is empty.
     */
    public boolean isEmpty() {
        lock.readLock()
            .lock();
        try{
            return ring.isEmpty();
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    private N getNodeForHash(int hash) {
        Integer key = ring.ceilingKey(hash);
        if (key == null) {
            key = ring.firstKey();
        }
        return ring.get(key);
    }

    /**
     * MurmurHash3-like hash function.
     * Provides good distribution for consistent hashing.
     */
    private static int hash(byte[] data) {
        int h = 0x811c9dc5;
        for (byte b : data) {
            h ^= b;
            h *= 0x01000193;
        }
        // Final mix
        h ^= h>>> 16;
        h *= 0x85ebca6b;
        h ^= h>>> 13;
        h *= 0xc2b2ae35;
        h ^= h>>> 16;
        return h;
    }

    private static int hash(String data) {
        return hash(data.getBytes(StandardCharsets.UTF_8));
    }
}
