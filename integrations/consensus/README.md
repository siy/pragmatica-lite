# Consensus Module

Rabia CFT (Crash Fault Tolerant) consensus algorithm for replicated state machines.

## Features

- Crash-fault-tolerant consensus without persistent event log
- Batch-based command processing for efficiency
- Automatic state synchronization on node recovery
- Deterministic decision-making with coin-flip fallback
- Pluggable network and persistence layers

## Usage

### Basic Setup

```java
import org.pragmatica.consensus.*;
import org.pragmatica.consensus.rabia.*;

// 1. Implement your state machine
class CounterStateMachine implements StateMachine<CounterCommand> {
    private int value = 0;

    @Override
    public <R> R process(CounterCommand command) {
        return switch (command) {
            case Increment() -> { value++; yield (R) Integer.valueOf(value); }
            case Decrement() -> { value--; yield (R) Integer.valueOf(value); }
            case Get() -> (R) Integer.valueOf(value);
        };
    }

    @Override
    public Result<byte[]> makeSnapshot() { /* serialize value */ }

    @Override
    public Result<Unit> restoreSnapshot(byte[] snapshot) { /* deserialize */ }

    @Override
    public void reset() { value = 0; }

    @Override
    public void configure(MessageRouter.MutableRouter router) {}
}

// 2. Create topology configuration
var nodeId = NodeId.nodeId("node-1");
var coreNodes = List.of(
    new NodeInfo(NodeId.nodeId("node-1"), NodeAddress.nodeAddress("localhost", 5580).expect("valid")),
    new NodeInfo(NodeId.nodeId("node-2"), NodeAddress.nodeAddress("localhost", 5581).expect("valid")),
    new NodeInfo(NodeId.nodeId("node-3"), NodeAddress.nodeAddress("localhost", 5582).expect("valid"))
);
var topologyConfig = new TopologyConfig(
    nodeId,
    coreNodes.size(),           // Fixed cluster size for quorum calculations
    timeSpan(1).seconds(),      // Reconciliation interval
    timeSpan(10).seconds(),     // Ping interval
    coreNodes
);

// 3. Create message router and components
var router = MessageRouter.mutable();
var topologyManager = TcpTopologyManager.tcpTopologyManager(topologyConfig, router);
var network = new NettyClusterNetwork(topologyManager, router);

// 4. Create and configure the engine
var engine = new RabiaEngine<>(topologyManager, network, stateMachine, ProtocolConfig.defaultConfig());
engine.configure(router);

// 5. Start and use
engine.start().await();
engine.apply(List.of(new Increment())).await(); // Returns List<Integer> with results
```

### Configuration

```java
// Production configuration
var config = ProtocolConfig.defaultConfig();

// Test configuration with faster intervals
var testConfig = ProtocolConfig.testConfig();

// Custom configuration
var custom = new ProtocolConfig(
    timeSpan(30).seconds(),  // cleanup interval
    timeSpan(2).seconds(),   // sync retry interval
    50                       // phases to keep
);
```

## Architecture

### Components

- **RabiaEngine** - Main consensus engine coordinating the protocol
- **StateMachine** - User-provided replicated state machine
- **ClusterNetwork** - Abstraction for node-to-node communication
- **TopologyManager** - Cluster topology and quorum calculations
- **TopologyConfig** - Configuration including fixed cluster size
- **RabiaPersistence** - State persistence for recovery

### Protocol Flow

1. **Propose** - Node proposes a batch of commands
2. **Round 1 Vote** - Nodes vote on whether to accept the proposal
3. **Round 2 Vote** - Based on round 1 majority, nodes refine their vote (may be skipped via fast path)
4. **Decision** - With f+1 votes, nodes commit or use coin flip

#### Super-Majority Fast Path

When `n - f` nodes vote the same value in Round 1, the protocol skips Round 2 and decides immediately. This optimization reduces latency in the common case where nodes agree quickly.

- **Threshold**: `n - f` nodes (super-majority)
- **Correctness**: At least one non-faulty node voted, so any future quorum would reach the same decision

### Quorum Requirements

- **Cluster Size = N**: Tolerates up to f = (N-1)/2 failures
- **Quorum Size**: N/2 + 1 (majority)
- **f+1 Size**: N - quorum + 1
- **Super-Majority**: N - f (fast path threshold)

| Nodes | Quorum | f+1 | Super-Majority | Max Failures |
|-------|--------|-----|----------------|--------------|
| 3     | 2      | 2   | 2              | 1            |
| 5     | 3      | 3   | 4              | 2            |
| 7     | 4      | 4   | 5              | 3            |

## Message Types

### Synchronous (Consensus Rounds)
- `Propose` - Initial batch proposal
- `VoteRound1` - First round vote (V0/V1)
- `VoteRound2` - Second round vote (V0/V1/VQUESTION)
- `Decision` - Final decision broadcast
- `SyncResponse` - State synchronization response

### Asynchronous
- `SyncRequest` - Request state from other nodes
- `NewBatch` - Distribute new command batch
