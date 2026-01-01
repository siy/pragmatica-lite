# Distributed Hash Table (DHT) Module

Consistent hashing with configurable replication for distributed data storage.

## Features

- Consistent hash ring with virtual nodes for even distribution
- Configurable replication factor and quorum settings
- Pluggable storage engines (in-memory included)
- Message-based protocol for distributed operations
- Integration with MessageRouter for network communication

## Usage

### Consistent Hash Ring

```java
import org.pragmatica.dht.ConsistentHashRing;

// Create a ring
var ring = ConsistentHashRing.<String>consistentHashRing();

// Add nodes
ring.addNode("node-1");
ring.addNode("node-2");
ring.addNode("node-3");

// Find primary node for a key
Optional<String> primary = ring.primaryFor("user:123");

// Get nodes for replication (primary + replicas)
List<String> nodes = ring.nodesFor("user:123", 3);

// Get partition for a key
Partition partition = ring.partitionFor("user:123");
```

### DHT Node

```java
import org.pragmatica.dht.*;
import org.pragmatica.dht.storage.MemoryStorageEngine;

// Create storage and ring
var storage = MemoryStorageEngine.memoryStorageEngine();
var ring = ConsistentHashRing.<String>consistentHashRing();
ring.addNode("node-1");

// Create DHT node
var node = DHTNode.dhtNode(
    "node-1",
    storage,
    ring,
    DHTConfig.DEFAULT
);

// Local operations
node.putLocal("key".getBytes(), "value".getBytes()).await();
Option<byte[]> value = node.getLocal("key".getBytes()).await();

// Check responsibility
boolean isResponsible = node.isResponsibleFor("key".getBytes());
boolean isPrimary = node.isPrimaryFor("key".getBytes());
```

### Configuration

```java
// Default: 3 replicas, quorum of 2
DHTConfig config = DHTConfig.DEFAULT;

// Custom replication with majority quorum
DHTConfig custom = DHTConfig.withReplication(5); // 5 replicas, quorum of 3

// Full replication (all nodes store everything)
DHTConfig full = DHTConfig.FULL;

// Single node (for testing)
DHTConfig single = DHTConfig.SINGLE_NODE;
```

## Components

### ConsistentHashRing

Distributes keys across nodes using consistent hashing with virtual nodes.
- 150 virtual nodes per physical node by default
- 1024 partitions
- MurmurHash3-like hash function

### DHTConfig

Configuration for replication and quorum:
- `replicationFactor` - number of copies (0 = full replication)
- `writeQuorum` - minimum successful writes
- `readQuorum` - minimum successful reads

### StorageEngine

Interface for pluggable storage backends:
- `MemoryStorageEngine` - ConcurrentHashMap-based in-memory storage
- Custom implementations for persistent storage

### DHTMessage

Protocol messages for distributed operations:
- GetRequest/GetResponse
- PutRequest/PutResponse
- RemoveRequest/RemoveResponse
- ExistsRequest/ExistsResponse
