# Rabia Consensus Algorithm Implementation

Rabia consensus algorithm is a CFT (crash-fault-tolerant) distributed consensus algorithm. In a sense it is similar to RAFT, Paxos, or Zab (Zookeeper Atomic Broadcast), although it is much simpler and does not rely on the persistent event log. This makes Rabia much more convenient to implement and use. [Rabia](https://dl.acm.org/doi/10.1145/3477132.3483582) is a relatively new algorithm, published in 2021.

The implementation is highly modular and can be used with any state machine. For internal communication implementation used Message Router: variation of the Message Bus pattern with targeted message delivery instead of broadcast.

In many real use cases it is necessary to have a single authority for the whole cluster to perform specific tasks. For example, automation of cluster-wide topology changes. Since Rabia is leaderless, the "Leader" is determined by deterministic selection of one node. The `LeaderManager` class performs this task and generates relevant messages when leader is elected or changed.

## Architecture

The Pragmatica Lite Cluster module provides a robust implementation of the Rabia consensus algorithm with the following key components:

### Core Components

1. **Consensus Engine**
   - Central component that orchestrates the consensus process
   - Manages command batching, tracking, and coordination among nodes
   - Implements the core Rabia protocol for message processing and decision making
   - Provides a Promise-based API for command submission.

2. **State Machine**
   - Pluggable component that processes commands after consensus is reached
   - Implements application-specific state machine logic controlled by command sequences
   - Returns deterministic results for the same commands across all nodes
   - Capable of serializing and deserializing state (used for cluster/node bootstrapping and recovery)

3. **Persistence Layer**
   - The current implementation uses in-memory storage, but implementation is pluggable
   - Used for cluster/node bootstrapping and recovery.
   - Stores critical information including:
     - Current application state
     - Last completed consensus phase
     - Pending command batches

4. **Message Router**
   - Enables targeted message delivery between nodes
   - Decouples different components and simplifies complex interactions
   
5. **Leader Manager**
   - Deterministically selects a leader node for special operations
   - Detects leader changes and notifies cluster components
   - Ensures consistent leadership view across the cluster

### Key Interactions

1. **Command Processing Flow**
   - Client submits commands to the Consensus Engine
   - Commands are batched with a correlation ID for tracking
   - Consensus Engine broadcasts the batch to all nodes
   - Nodes vote on the batch according to the Rabia protocol
   - Upon reaching consensus, the batch is committed to the State Machine
   - Results of command sequence application are returned to the client

2. **Failure Handling**
   - Network layer tracks node connection/disconnection
   - Once node detects disappearance of the quorum, it stops all operations and persists its state
   - Once node detects appearance of the quorum, it resynchronizes its state and resumes operations

3. **State Synchronization**
New or rejoining nodes request state snapshot from other nodes and when majority of responses are received, node selects most recent one, restores local state from it, and joins the consensus message exchange.
The same algorithm is used for bootstrapping. Nodes can share saved state even if they are dormant (i.e. not yet active for any reason).

The architecture emphasizes resilience, correctness, and performance while maintaining a clean separation of concerns between the consensus protocol, application logic, and network communication.

## Diagrams

Below provided sequence diagrams for the most important parts of the algorithm.

### Sequence Diagrams

#### Rabia Protocol Message Processing 

<details>
@startuml protocolEngine


participant Client
participant ConsensusEngine
participant StateMachine
participant Persistence

Client->>ConsensusEngine: apply(commands) → Promise<List<R>>
ConsensusEngine->>ConsensusEngine: Create Batch with CorrelationId
ConsensusEngine->>ConsensusEngine: Track batch & promise
ConsensusEngine->>ConsensusEngine: Broadcast NewBatch (async)
ConsensusEngine->>ConsensusEngine: Start consensus phase
ConsensusEngine->>ConsensusEngine: Collect votes, reach decision
alt Decision = Commit
    ConsensusEngine->>StateMachine: process(commands)
    StateMachine-->>ConsensusEngine: List<R> results
    ConsensusEngine->>Persistence: save(state, lastPhase, pendingBatches)
end
ConsensusEngine-->>Client: Promise completes with List<R>

@enduml
</details>

![](protocolEngine.svg)

#### State Persistence During Disconnection/Reconnection

<details>

@startuml statePersistence

participant ConsensusEngine
participant Persistence
participant Network

ConsensusEngine->>Persistence: save(state, lastPhase, pendingBatches)
ConsensusEngine<<--Network: Disconnect (no messages)
ConsensusEngine->>Persistence: load()
Persistence-->>ConsensusEngine: SavedState
ConsensusEngine->>ConsensusEngine: Restore state, resume operation

@enduml

</details>

![](statePersistence.svg)

#### Poem (generated by Code Rabbit)

```
  .-.
 (o o)   A cluster of nodes, now smarter and keen,
 ( = )   With batches and promises, errors well seen.
 /   \   Persistence for safety, new tests for the fight,
(__ __)  Consensus grows stronger, from morning to night.
  🥕      Debug logs are hopping, all systems in sync,
          The rabbits are cheering, “We’re better, we think!”
```
