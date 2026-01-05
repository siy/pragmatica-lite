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
import org.pragmatica.messaging.MessageReceiver;
import org.pragmatica.messaging.MessageRouter;
import org.pragmatica.lang.Option;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.pragmatica.consensus.leader.LeaderNotification.leaderChange;
import static org.pragmatica.lang.Option.option;

/// Leader manager is responsible for choosing which node is the leader.
/// Although consensus is leaderless, it is often necessary to have a single
/// source of truth for the cluster management. The leader node can be used for this purpose.
public interface LeaderManager {
    /// Get the current leader node ID.
    /// @return the current leader, or empty if no leader is elected
    Option<NodeId> leader();

    /// Check if this node is the current leader.
    boolean isLeader();

    static LeaderManager leaderManager(NodeId self, MessageRouter router) {
        record leaderManager(NodeId self,
                             MessageRouter router,
                             AtomicBoolean active,
                             AtomicReference<NodeId> currentLeader) implements LeaderManager {
            @Override
            public Option<NodeId> leader() {
                return option(currentLeader.get());
            }

            @Override
            public boolean isLeader() {
                return self.equals(currentLeader.get());
            }

            @Override
            public void nodeAdded(NodeAdded nodeAdded) {
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
                tryElect(nodeRemoved.topology()
                                    .getFirst());
            }

            @Override
            public void nodeDown(NodeDown nodeDown) {
                currentLeader()
                             .set(null);
                stop();
            }

            private void tryElect(NodeId candidate) {
                var oldLeader = currentLeader()
                                             .get();
                // Potential leader change. Implicitly handles initial election.
                if (currentLeader()
                                 .compareAndSet(oldLeader, candidate)) {
                    if (!candidate.equals(oldLeader)) {
                        notifyLeaderChange();
                    }
                }
            }

            private void notifyLeaderChange() {
                if (active()
                          .get()) {
                    var nodeId = currentLeader()
                                              .get();
                    router()
                          .route(leaderChange(option(nodeId),
                                              self.equals(nodeId)));
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
                notifyLeaderChange();
            }

            private void stop() {
                // Set inactive first to prevent new elections during shutdown
                active.set(false);
                currentLeader()
                             .set(null);
                // Send notification directly since active is now false
                router()
                      .route(leaderChange(option(null),
                                          false));
            }
        }
        return new leaderManager(self, router, new AtomicBoolean(false), new AtomicReference<>());
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
