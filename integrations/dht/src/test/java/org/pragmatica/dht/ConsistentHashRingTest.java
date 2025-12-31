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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.dht.ConsistentHashRing.consistentHashRing;

class ConsistentHashRingTest {

    @Test
    void create_empty_ring() {
        ConsistentHashRing<String> ring = consistentHashRing();

        assertThat(ring.isEmpty()).isTrue();
        assertThat(ring.nodeCount()).isZero();
        assertThat(ring.nodes()).isEmpty();
    }

    @Test
    void addNode_increases_node_count() {
        ConsistentHashRing<String> ring = consistentHashRing();

        ring.addNode("node-1");

        assertThat(ring.isEmpty()).isFalse();
        assertThat(ring.nodeCount()).isEqualTo(1);
        assertThat(ring.nodes()).containsExactly("node-1");
    }

    @Test
    void addNode_is_idempotent() {
        ConsistentHashRing<String> ring = consistentHashRing();

        ring.addNode("node-1");
        ring.addNode("node-1");

        assertThat(ring.nodeCount()).isEqualTo(1);
    }

    @Test
    void removeNode_removes_from_ring() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");
        ring.addNode("node-2");

        ring.removeNode("node-1");

        assertThat(ring.nodeCount()).isEqualTo(1);
        assertThat(ring.nodes()).containsExactly("node-2");
    }

    @Test
    void partitionFor_returns_valid_partition() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");

        Partition partition = ring.partitionFor("test-key");

        assertThat(partition.value()).isGreaterThanOrEqualTo(0);
        assertThat(partition.value()).isLessThan(Partition.MAX_PARTITIONS);
    }

    @Test
    void partitionFor_is_deterministic() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");

        Partition partition1 = ring.partitionFor("test-key");
        Partition partition2 = ring.partitionFor("test-key");

        assertThat(partition1).isEqualTo(partition2);
    }

    @Test
    void primaryFor_returns_node_when_ring_has_nodes() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");

        var primary = ring.primaryFor("test-key");

        assertThat(primary).isPresent();
        assertThat(primary.get()).isEqualTo("node-1");
    }

    @Test
    void primaryFor_returns_empty_when_ring_empty() {
        ConsistentHashRing<String> ring = consistentHashRing();

        var primary = ring.primaryFor("test-key");

        assertThat(primary).isEmpty();
    }

    @Test
    void nodesFor_returns_requested_number_of_replicas() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        List<String> nodes = ring.nodesFor("test-key", 2);

        assertThat(nodes).hasSize(2);
    }

    @Test
    void nodesFor_returns_unique_nodes() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        List<String> nodes = ring.nodesFor("test-key", 3);

        assertThat(nodes).hasSize(3);
        assertThat(nodes).containsExactlyInAnyOrder("node-1", "node-2", "node-3");
    }

    @Test
    void nodesFor_respects_node_count_limit() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");
        ring.addNode("node-2");

        // Request more replicas than nodes
        List<String> nodes = ring.nodesFor("test-key", 5);

        assertThat(nodes).hasSize(2);
    }

    @Test
    void keys_are_distributed_across_nodes() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        Map<String, Integer> distribution = new HashMap<>();
        int keyCount = 10000;

        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            var primary = ring.primaryFor(key);
            assertThat(primary).isPresent();
            distribution.merge(primary.get(), 1, Integer::sum);
        }

        // All nodes should have keys
        assertThat(distribution).containsKeys("node-1", "node-2", "node-3");

        // Distribution should be roughly even (within 50% of ideal)
        int idealCount = keyCount / 3;
        for (int count : distribution.values()) {
            assertThat(count).isGreaterThan(idealCount / 2);
            assertThat(count).isLessThan(idealCount * 2);
        }
    }

    @Test
    void string_and_bytes_produce_same_partition() {
        ConsistentHashRing<String> ring = consistentHashRing();
        ring.addNode("node-1");

        String key = "test-key";
        Partition fromString = ring.partitionFor(key);
        Partition fromBytes = ring.partitionFor(key.getBytes(StandardCharsets.UTF_8));

        assertThat(fromString).isEqualTo(fromBytes);
    }
}
