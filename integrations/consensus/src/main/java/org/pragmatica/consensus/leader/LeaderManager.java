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

package org.pragmatica.consensus.leader;

import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.topology.QuorumStateNotification;
import org.pragmatica.consensus.topology.TopologyChangeNotification.NodeAdded;
import org.pragmatica.consensus.topology.TopologyChangeNotification.NodeDown;
import org.pragmatica.consensus.topology.TopologyChangeNotification.NodeRemoved;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.SharedScheduler;
import org.pragmatica.messaging.MessageReceiver;
import org.pragmatica.messaging.MessageRouter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.consensus.leader.LeaderNotification.leaderChange;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/// Leader manager is responsible for choosing which node is the leader.
/// Although consensus is leaderless, it is often necessary to have a single
/// source of truth for the cluster management. The leader node can be used for this purpose.
///
/// Two modes of operation:
/// 1. **Local election** (backward compatible): Leader is computed locally on view change
///    and notifications are sent immediately via synchronous `router.route()`.
/// 2. **Consensus-based election**: Leader proposal is submitted through consensus.
///    Notifications are sent asynchronously via `router.routeAsync()` only after commit.
///
/// TODO: Consider consolidating TopologyManager and LeaderManager into a single ClusterViewManager.
///
/// **Current issue:** LeaderManager duplicates topology tracking (currentTopology field) because
/// it needs the full topology to determine if this node should submit a leader proposal. This
/// duplication exists to prevent race conditions where multiple nodes think they're the candidate.
///
/// **Arguments FOR consolidation:**
/// - Eliminates state duplication and synchronization issues between components
/// - Topology change and leader re-election could happen atomically
/// - Simpler message flow - no need to route NodeAdded/NodeRemoved/NodeDown events
/// - Single "cluster view" component owns the complete picture: who's in the cluster and who's leading
///
/// **Arguments AGAINST consolidation:**
/// - Separation of concerns: topology detection (network-level) vs leader election (application policy)
/// - Election algorithm flexibility: could swap strategies (first-in-topology, consensus-based, Raft)
///   without touching topology code
/// - Testability: easier to test each concern in isolation
/// - Reusability: some use cases might want topology tracking without leader election
///
/// **Possible design if consolidated:**
/// ```
/// interface ClusterView {
///     List<NodeId> topology();
///     Option<NodeId> leader();
///     boolean isLeader();
/// }
///
/// ClusterViewManager.clusterViewManager(
///     self,
///     router,
///     LeaderElectionStrategy.consensusBased(proposalHandler)  // or .localFirstNode()
/// )
/// ```
/// This keeps the algorithm swappable while eliminating state synchronization issues.
public interface LeaderManager {
    /// Get the current leader node ID.
    /// @return the current leader, or empty if no leader is elected
    Option<NodeId> leader();

    /// Check if this node is the current leader.
    boolean isLeader();

    /// Called when leader election is committed through consensus.
    /// Updates local state and sends async notification.
    ///
    /// @param leader the committed leader node ID
    /// @param viewSequence the view sequence number for consistency checking
    void onLeaderCommitted(NodeId leader, long viewSequence);

    /// Trigger leader election manually. Call this after consensus is ready.
    /// In consensus mode, submits a proposal for the tracked candidate.
    /// In local mode, this is a no-op (election happens automatically on quorum).
    void triggerElection();

    /// Handler for submitting leader proposals through consensus.
    /// When provided, leader election goes through consensus instead of local computation.
    @FunctionalInterface
    interface LeaderProposalHandler {
        /// Submit a leader proposal through consensus.
        ///
        /// @param candidate the proposed leader node ID
        /// @param viewSequence monotonic view sequence number
        /// @return Promise that completes when proposal is submitted (not necessarily committed)
        Promise<Unit> propose(NodeId candidate, long viewSequence);
    }

    /// Create a leader manager with local election (backward compatible).
    /// Leader is computed locally on view change and notifications are sent immediately.
    static LeaderManager leaderManager(NodeId self, MessageRouter router) {
        return leaderManager(self, router, Option.none(), List.of());
    }

    /// Create a leader manager with consensus-based election.
    /// When proposal handler is provided, leader election goes through consensus.
    ///
    /// @param self this node's ID
    /// @param router message router for notifications
    /// @param proposalHandler handler for submitting proposals through consensus
    static LeaderManager leaderManager(NodeId self, MessageRouter router, LeaderProposalHandler proposalHandler) {
        return leaderManager(self, router, Option.some(proposalHandler), List.of());
    }

    /// Create a leader manager with consensus-based election and expected cluster membership.
    /// The expected cluster is used for deterministic leader candidate selection to prevent
    /// flapping when nodes have different views during startup.
    ///
    /// @param self this node's ID
    /// @param router message router for notifications
    /// @param proposalHandler handler for submitting proposals through consensus
    /// @param expectedCluster expected cluster members for deterministic min calculation
    static LeaderManager leaderManager(NodeId self,
                                       MessageRouter router,
                                       LeaderProposalHandler proposalHandler,
                                       List<NodeId> expectedCluster) {
        return leaderManager(self, router, Option.some(proposalHandler), expectedCluster);
    }

    Logger LOG = LoggerFactory.getLogger(LeaderManager.class);

    /// Retry delay for leader proposals that fail (e.g., when consensus not ready yet)
    TimeSpan PROPOSAL_RETRY_DELAY = timeSpan(500).millis();

    private static LeaderManager leaderManager(NodeId self,
                                               MessageRouter router,
                                               Option<LeaderProposalHandler> proposalHandler,
                                               List<NodeId> expectedCluster) {
        record leaderManager(NodeId self,
                             MessageRouter router,
                             Option<LeaderProposalHandler> proposalHandler,
                             List<NodeId> expectedCluster,
                             AtomicBoolean active,
                             AtomicLong viewSequence,
                             AtomicReference<Option<NodeId>> currentLeader,
                             AtomicReference<List<NodeId>> currentTopology,
                             AtomicBoolean proposalInFlight,
                             AtomicBoolean needsReactivation) implements LeaderManager {
            @Override
            public Option<NodeId> leader() {
                return currentLeader.get();
            }

            @Override
            public boolean isLeader() {
                return currentLeader.get()
                                    .filter(self::equals)
                                    .isPresent();
            }

            @Override
            public void onLeaderCommitted(NodeId leader, long committedViewSequence) {
                LOG.debug("onLeaderCommitted: self={}, leader={}, viewSequence={}, currentViewSequence={}, needsReactivation={}",
                          self,
                          leader,
                          committedViewSequence,
                          viewSequence.get(),
                          needsReactivation.get());
                // Reject stale proposals
                if (committedViewSequence < viewSequence.get()) {
                    LOG.debug("Rejecting stale leader proposal: {} < {}", committedViewSequence, viewSequence.get());
                    return;
                }
                // Clear in-flight flag - proposal has committed through consensus
                proposalInFlight.set(false);
                var oldLeader = currentLeader.get();
                var newLeader = Option.some(leader);
                // Check if we need reactivation (after quorum flap where stop() was called)
                var forceNotify = needsReactivation.compareAndSet(true, false);
                if (currentLeader.compareAndSet(oldLeader, newLeader)) {
                    viewSequence.set(committedViewSequence);
                    if (forceNotify || !newLeader.equals(oldLeader)) {
                        LOG.debug("Leader committed: {} (forceNotify={}, changed={}), notifying",
                                  newLeader,
                                  forceNotify,
                                  !newLeader.equals(oldLeader));
                        notifyLeaderChangeAsync();
                    } else {
                        LOG.debug("Leader unchanged ({}), skipping notification", newLeader);
                    }
                }
            }

            @Override
            public void triggerElection() {
                LOG.debug("triggerElection called: self={}, active={}, topology={}, expectedCluster={}",
                          self,
                          active.get(),
                          currentTopology.get(),
                          expectedCluster);
                if (!active.get()) {
                    LOG.debug("triggerElection skipped: not active");
                    return;
                }
                proposalHandler.onPresent(handler -> {
                                              var topology = currentTopology.get();
                                              if (topology.isEmpty()) {
                                                  LOG.debug("Skipping election trigger: topology is empty");
                                                  return;
                                              }
                                              // Deterministic candidate selection: use expectedCluster filtered by live topology.
                // This ensures we only consider nodes that are BOTH expected AND currently connected.
                // After a node dies, it's removed from topology but remains in expectedCluster.
                // Without filtering, dead nodes would still be candidates, blocking re-election.
                var candidatePool = expectedCluster.isEmpty()
                                    ? topology
                                    : expectedCluster.stream()
                                                     .filter(topology::contains)
                                                     .toList();
                                              // If all expected nodes are down, fall back to topology
                if (candidatePool.isEmpty()) {
                                                  candidatePool = topology;
                                              }
                                              var candidate = candidatePool.stream()
                                                                           .min(Comparator.naturalOrder())
                                                                           .orElse(self);
                                              if (!self.equals(candidate)) {
                                                  LOG.debug("Skipping election trigger: self={} is not min node {} in candidatePool {}",
                                                            self,
                                                            candidate,
                                                            candidatePool);
                                                  return;
                                              }
                                              LOG.debug("Submitting leader proposal: self={} (min node in {})",
                                                        self,
                                                        expectedCluster.isEmpty()
                                                        ? "topology"
                                                        : "expectedCluster");
                                              // DON'T clear currentLeader here - let consensus decide.
                // The leader will be updated via onLeaderCommitted().
                submitProposal(handler, candidate);
                                          });
            }

            @Override
            public void nodeAdded(NodeAdded nodeAdded) {
                currentTopology.set(nodeAdded.topology());
                tryElect(nodeAdded.topology()
                                  .getFirst());
            }

            @Override
            public void nodeRemoved(NodeRemoved nodeRemoved) {
                // Should not happen, but better be safe than sorry.
                if (nodeRemoved.topology()
                               .isEmpty()) {
                    return;
                }
                currentTopology.set(nodeRemoved.topology());
                // Check if the removed node was the leader
                var removedNode = nodeRemoved.nodeId();
                var leaderRemoved = currentLeader.get()
                                                 .filter(removedNode::equals)
                                                 .isPresent();
                if (leaderRemoved && active.get()) {
                    // Leader was removed while we still have quorum - trigger re-election
                    LOG.debug("Leader {} was removed, triggering re-election", removedNode);
                    currentLeader.set(Option.none());
                    proposalHandler.onPresent(_ -> triggerElection());
                } else {
                    tryElect(nodeRemoved.topology()
                                        .getFirst());
                }
            }

            @Override
            public void nodeDown(NodeDown nodeDown) {
                // If no nodes remain or no quorum, stop
                if (nodeDown.topology()
                            .isEmpty() || !active.get()) {
                    currentLeader.set(Option.none());
                    stop();
                    return;
                }
                // Node went down but we still have quorum - trigger re-election
                currentTopology.set(nodeDown.topology());
                // Check if current leader is the one that went down
                var downNode = nodeDown.nodeId();
                var leaderIsDown = currentLeader.get()
                                                .filter(downNode::equals)
                                                .isPresent();
                if (leaderIsDown) {
                    // Only clear leader if it's the one that went down
                    // New leader will be set via onLeaderCommitted() after consensus
                    currentLeader.set(Option.none());
                }
                // In consensus mode, trigger election submission if we're the candidate
                // tryElect is not needed - triggerElection handles everything
                proposalHandler.onPresent(_ -> triggerElection());
            }

            private void tryElect(NodeId candidate) {
                if (!active.get()) {
                    // In consensus mode, don't pre-cache leader - wait for onLeaderCommitted()
                    // Pre-caching causes "Leader unchanged" in onLeaderCommitted, skipping notification
                    if (proposalHandler.isPresent()) {
                        return;
                    }
                    // Local mode only: track the candidate locally
                    var oldLeader = currentLeader.get();
                    var newLeader = Option.some(candidate);
                    currentLeader.compareAndSet(oldLeader, newLeader);
                    return;
                }
                proposalHandler.fold(() -> {
                                         // Local election mode: immediate update and sync notification
                electLocally(candidate);
                                         return Unit.unit();
                                     },
                                     _ -> {
                                         // Consensus mode when active: DON'T update currentLeader here!
                // The leader will be set via onLeaderCommitted() after consensus.
                // If we set it here, onLeaderCommitted will see no change and skip notification.
                // Topology is already tracked via currentTopology for triggerElection().
                return Unit.unit();
                                     });
            }

            private void submitProposal(LeaderProposalHandler handler, NodeId candidate) {
                // Prevent duplicate submissions while a proposal is in flight
                if (!proposalInFlight.compareAndSet(false, true)) {
                    LOG.debug("Skipping proposal submission - another proposal is in flight");
                    return;
                }
                var nextViewSequence = viewSequence.incrementAndGet();
                handler.propose(candidate, nextViewSequence)
                       .onFailure(cause -> {
                                      // Clear in-flight flag on failure to allow retry
                proposalInFlight.set(false);
                                      LOG.debug("Leader proposal failed ({}), scheduling retry in {}ms",
                                                cause.message(),
                                                PROPOSAL_RETRY_DELAY.millis());
                                      scheduleProposalRetry(handler, candidate);
                                  });
            }

            private void scheduleProposalRetry(LeaderProposalHandler handler, NodeId candidate) {
                SharedScheduler.schedule(() -> {
                                             // Only retry if still active and no leader elected yet
                if (active.get() && currentLeader.get()
                                                 .isEmpty()) {
                                                 LOG.debug("Retrying leader proposal for {}", candidate);
                                                 submitProposal(handler, candidate);
                                             }
                                         },
                                         PROPOSAL_RETRY_DELAY);
            }

            private void electLocally(NodeId candidate) {
                var oldLeader = currentLeader.get();
                var newLeader = Option.some(candidate);
                // Potential leader change. Implicitly handles initial election.
                if (currentLeader.compareAndSet(oldLeader, newLeader)) {
                    if (!newLeader.equals(oldLeader)) {
                        notifyLeaderChange();
                    }
                }
            }

            private void notifyLeaderChange() {
                if (active.get()) {
                    var leaderOpt = currentLeader.get();
                    router.route(leaderChange(leaderOpt,
                                              leaderOpt.filter(self::equals)
                                                       .isPresent()));
                }
            }

            private void notifyLeaderChangeAsync() {
                if (active.get()) {
                    router.routeAsync(() -> {
                                          var leaderOpt = currentLeader.get();
                                          return leaderChange(leaderOpt,
                                                              leaderOpt.filter(self::equals)
                                                                       .isPresent());
                                      });
                }
            }

            @Override
            public void watchQuorumState(QuorumStateNotification quorumState) {
                switch (quorumState) {
                    case ESTABLISHED -> start();
                    case DISAPPEARED -> stop();
                }
            }

            private void start() {
                active.set(true);
                // In local mode, notify immediately if we have a candidate.
                // In consensus mode, trigger election - notification comes only from onLeaderCommitted().
                proposalHandler.fold(() -> {
                                         // Local mode: notify if we have a candidate
                currentLeader.get()
                             .onPresent(_ -> notifyLeaderChange());
                                         return Unit.unit();
                                     },
                                     _ -> {
                                         // Consensus mode: only schedule election trigger.
                // DO NOT re-notify here - it causes flapping when nodes have different partial views.
                // Notification will come from onLeaderCommitted() with forceNotify if this is re-establishment.
                LOG.debug("Quorum established, scheduling leader election trigger");
                                         SharedScheduler.schedule(this::triggerElection, PROPOSAL_RETRY_DELAY);
                                         return Unit.unit();
                                     });
            }

            private void stop() {
                // Set inactive first to prevent new elections during shutdown
                active.set(false);
                currentLeader.set(Option.none());
                // Clear in-flight flag to reset state
                proposalInFlight.set(false);
                // Mark that we need reactivation when leader is committed again
                // This ensures notification even if the same leader is re-committed
                needsReactivation.set(true);
                // Always send stop notification synchronously for immediate effect
                router.route(leaderChange(Option.none(), false));
            }
        }
        return new leaderManager(self,
                                 router,
                                 proposalHandler,
                                 List.copyOf(expectedCluster),
                                 new AtomicBoolean(false),
                                 new AtomicLong(0),
                                 new AtomicReference<>(Option.none()),
                                 new AtomicReference<>(List.of()),
                                 new AtomicBoolean(false),
                                 new AtomicBoolean(false));
    }

    @MessageReceiver
    void nodeAdded(NodeAdded nodeAdded);

    @MessageReceiver
    void nodeRemoved(NodeRemoved nodeRemoved);

    @MessageReceiver
    void nodeDown(NodeDown nodeDown);

    @MessageReceiver
    void watchQuorumState(QuorumStateNotification quorumState);
}
