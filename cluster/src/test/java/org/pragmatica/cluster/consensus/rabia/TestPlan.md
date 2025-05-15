# Rabia Consensus Algorithm – Test Plan

## Objective

Validate safety (linearizability) and liveness of a Rabia implementation under nominal load, fault scenarios, and boundary conditions. Focus on edge‑case behaviors including quorum loss/restore and Byzantine actions. Logging and log truncation are absent in Rabia; tests target state convergence through direct state digests and application‑level snapshots.

---

## Test Suite 1 – Nominal Operationv (done)

| Case | Description                       | Setup                                  | Steps                                 | Assertions                                                                        |
|------|-----------------------------------|----------------------------------------|---------------------------------------|-----------------------------------------------------------------------------------|
| 1.1  | Single‑client sequential requests | 5 replicas, stable links               | Issue 10 000 commands from one client | Identical state digests; latency ≤ 2×mean RTT                                     |
| 1.2  | High concurrency                  | 5 replicas, 100 clients, random delays | 10 000 total commands                 | Global order deterministic; no duplicates; throughput scales until CPU saturation |

## Test Suite 2 – Message Semantics (done)

| Case | Fault                                                         | Assertion                                               |
|------|---------------------------------------------------------------|---------------------------------------------------------|
| 2.1  | Duplicate `PROPOSE` broadcasts                                | Replicas ignore/coalesce duplicates; continued progress |
| 2.2  | Out‑of‑order delivery (`COMMIT` before `PREPARE`) at one node | Buffering preserves safety                              |
| 2.3  | 10 % random message loss for 30 s                             | Commits proceed; latency increase only                  |
| 2.4  | Stale view numbers                                            | Messages discarded; no regression                       |

## Test Suite 3 – Crash‑Stop Failures (done)

| Case | Event                                | Expected                                                                       |
|------|--------------------------------------|--------------------------------------------------------------------------------|
| 3.1  | Proposer crashes after broadcast     | Round completes; client reply delivered                                        |
| 3.2  | Replica crashes pre‑`COMMIT`         | Round completes if quorum present; recovering node resyncs sequence and digest |
| 3.3  | Crash of *f* replicas simultaneously | Cluster maintains safety and liveness                                          |

## Test Suite 4 – Byzantine Behaviors (done)

| Case | Fault                                | Expected                                                          |
|------|--------------------------------------|-------------------------------------------------------------------|
| 4.1  | Equivocation (conflicting `COMMIT`s) | Signature aggregation fails; traitor excluded; progress continues |
| 4.2  | Malformed threshold‑sig share        | Verification fails; round aborted, next view started              |
| 4.3  | Silent replica (receive‑only)        | Liveness maintained with quorum                                   |

## Test Suite 5 - Cluster Reconfiguration (done)

| Case | Fault                                                | Expected                                               |
|------|------------------------------------------------------|--------------------------------------------------------|
| 5.1  | Add node to active cluster                           | New node syncs and participates; quorum size increases |
| 5.2  | Remove node from active cluster                      | Remaining nodes continue; quorum size decreases        |
| 5.3  | Replace primary (two nodes removed, two nodes added) | Cluster state preserved; consensus maintained          |
| 5.4  | Majority replacement (f+1 nodes replaced)            | Full state transfer; operation continues               |

## Test Suite 6 – Quorum Loss & Recovery 

| Case | Partition Pattern               | Duration | Expected                                                                 |
|------|---------------------------------|----------|--------------------------------------------------------------------------|
| 6.1  | Majority isolated from minority | 60 s     | Majority continues; minority stalls; on heal minority syncs state digest |
| 6.2  | Every fragment < quorum         | 60 s     | System halts; first reformed quorum resumes without forks                |
| 6.3  | Flapping partitions every 5 s   | 120 s    | No divergence; progress only with quorum availability                    |

## Test Suite 7 – View/Epoch Management

| Case | Trigger                    | Expected                                             |
|------|----------------------------|------------------------------------------------------|
| 7.1  | Seed collision             | Deterministic tie‑break gives single sequence number |
| 7.2  | View change at 1 000 cmd/s | No command loss; latency spike < 3×mean RTT          |

## Test Suite 8 – State Persistence & Reconciliation

| Case | Scenario                         | Expected                                                      |
|------|----------------------------------|---------------------------------------------------------------|
| 8.1  | Atomic state write torn          | Replica discards corrupted state; pulls clean state from peer |
| 8.2  | Cold start of new node           | Bulk state sync; joins within bounded time                    |
| 8.3  | Sequential restarts of all nodes | Quorum always preserved; safety intact                        |

## Test Suite 9 – Stress & Chaos

| Case | Injection                                | Duration | Expected                                                                 |
|------|------------------------------------------|----------|--------------------------------------------------------------------------|
| 9.1  | 1 000 ms jitter on random links          | 5 min    | Safety intact; sub‑linear throughput degradation                         |
| 9.2  | Chaos‑monkey (kill/pause/partition/drop) | 60 min   | Safety never violated; liveness whenever quorum available ≥ 50 % of time |

## Test Suite 10 – Parameter Boundaries

| Case | Parameter                       | Expected                                      |
|------|---------------------------------|-----------------------------------------------|
| 10.1 | Max command size                | Commit succeeds; oversize rejected gracefully |
| 10.2 | Sequence number wrap at 2⁶³ – 1 | Controlled rollover/fail‑fast per spec        |
| 10.3 | Threshold sig count = *f* + 1   | Aggregate succeeds; deficient count fails     |

---

## Instrumentation & Tooling

* **State digest**: cryptographic hash after each commit; compared across replicas.
* **Fault‑injection hooks**: pause, kill, partition, message filter.
* **Metrics**: commit latency, throughput, quorum availability timeline, digest divergence alerts.

### Coverage Goal

Safety under any single‑fault case; liveness whenever ≥ ⌈N/2⌉ honest, connected replicas.
