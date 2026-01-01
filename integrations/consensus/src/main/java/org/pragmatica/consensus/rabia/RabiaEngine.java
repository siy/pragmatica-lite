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

package org.pragmatica.consensus.rabia;

import org.pragmatica.consensus.Command;
import org.pragmatica.consensus.ConsensusErrors;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.StateMachine;
import org.pragmatica.consensus.net.ClusterNetwork;
import org.pragmatica.consensus.rabia.RabiaEngineIO.SubmitCommands;
import org.pragmatica.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Asynchronous.NewBatch;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Synchronous.*;
import org.pragmatica.consensus.topology.QuorumStateNotification;
import org.pragmatica.consensus.topology.TopologyManager;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.SharedScheduler;
import org.pragmatica.messaging.MessageReceiver;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.consensus.rabia.Batch.batch;
import static org.pragmatica.consensus.rabia.Batch.emptyBatch;
import static org.pragmatica.consensus.rabia.RabiaPersistence.SavedState.savedState;
import static org.pragmatica.consensus.rabia.RabiaProtocolMessage.Asynchronous.SyncRequest;

/**
 * Implementation of the Rabia consensus protocol.
 * <p>
 * Rabia is a crash-fault-tolerant (CFT) consensus algorithm that provides:
 * <ul>
 *   <li>No persistent event log required</li>
 *   <li>Batch-based command processing</li>
 *   <li>Automatic state synchronization</li>
 *   <li>Deterministic decision-making with coin-flip fallback</li>
 * </ul>
 *
 * @param <C> Command type
 */
public class RabiaEngine<C extends Command> {
    private static final Logger log = LoggerFactory.getLogger(RabiaEngine.class);
    private static final double SCALE = 0.5d;

    private final NodeId self;
    private final TopologyManager topologyManager;
    private final ClusterNetwork network;
    private final StateMachine<C> stateMachine;
    private final ProtocolConfig config;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<CorrelationId, Batch<C>> pendingBatches = new ConcurrentHashMap<>();
    private final Map<NodeId, SavedState<C>> syncResponses = new ConcurrentHashMap<>();
    private final RabiaPersistence<C> persistence = RabiaPersistence.inMemory();
    @SuppressWarnings("rawtypes")
    private final Map<CorrelationId, Promise> correlationMap = new ConcurrentHashMap<>();

    //--------------------------------- Node State Start
    private final Map<Phase, PhaseData<C>> phases = new ConcurrentHashMap<>();
    private final AtomicReference<Phase> currentPhase = new AtomicReference<>(Phase.ZERO);
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean isInPhase = new AtomicBoolean(false);
    private final AtomicReference<Promise<Unit>> startPromise = new AtomicReference<>(Promise.promise());
    private final AtomicReference<Phase> lastCommittedPhase = new AtomicReference<>(Phase.ZERO);

    //--------------------------------- Node State End
    /**
     * Creates a new Rabia consensus engine.
     *
     * @param topologyManager The topology manager for node communication
     * @param network         The network implementation
     * @param stateMachine    The state machine to apply commands to
     * @param config          Configuration for the consensus engine
     */
    public RabiaEngine(TopologyManager topologyManager,
                       ClusterNetwork network,
                       StateMachine<C> stateMachine,
                       ProtocolConfig config) {
        this.self = topologyManager.self()
                                   .id();
        this.topologyManager = topologyManager;
        this.network = network;
        this.stateMachine = stateMachine;
        this.config = config;
        // Setup periodic tasks
        SharedScheduler.scheduleAtFixedRate(this::cleanupOldPhases, config.cleanupInterval());
        SharedScheduler.schedule(this::synchronize, config.syncRetryInterval());
    }

    @MessageReceiver
    public void quorumState(QuorumStateNotification quorumStateNotification) {
        log.trace("Node {} received quorum state {}", self, quorumStateNotification);
        switch (quorumStateNotification) {
            case ESTABLISHED -> clusterConnected();
            case DISAPPEARED -> clusterDisconnected();
        }
    }

    private void clusterConnected() {
        log.info("Node {}: quorum connected. Starting synchronization attempts", self);
        SharedScheduler.schedule(this::synchronize,
                                 config.syncRetryInterval()
                                       .randomize(SCALE));
    }

    private void clusterDisconnected() {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        persistence.save(stateMachine,
                         lastCommittedPhase.get(),
                         pendingBatches.values())
                   .onSuccessRun(() -> log.info("Node {} disconnected. State persisted", self))
                   .onFailure(cause -> log.error("Node {} failed to persist state: {}", self, cause));
        phases.clear();
        currentPhase.set(Phase.ZERO);
        isInPhase.set(false);
        stateMachine.reset();
        startPromise.set(Promise.promise());
        pendingBatches.clear();
    }

    public <R> Promise<List<R>> apply(List<C> commands) {
        var pendingAnswer = Promise.<List<R>>promise();
        return submitCommands(commands,
                              batch -> correlationMap.put(batch.correlationId(),
                                                          pendingAnswer))
                             .async()
                             .flatMap(_ -> pendingAnswer);
    }

    @MessageReceiver
    public void handleSubmit(SubmitCommands<C> submitCommands) {
        submitCommands(submitCommands.commands(),
                       _ -> {});
    }

    private Result<Batch<C>> submitCommands(List<C> commands, Consumer<Batch<C>> onBatchPrepared) {
        if (commands.isEmpty()) {
            return ConsensusErrors.commandBatchIsEmpty()
                                  .result();
        }
        if (!active.get()) {
            return ConsensusErrors.nodeInactive(self)
                                  .result();
        }
        var batch = batch(commands);
        log.trace("Node {}: client submitted {} command(s). Prepared batch: {}", self, commands.size(), batch);
        pendingBatches.put(batch.correlationId(), batch);
        onBatchPrepared.accept(batch);
        network.broadcast(new NewBatch<>(self, batch));
        if (!isInPhase.get()) {
            executor.execute(this::startPhase);
        }
        return Result.success(batch);
    }

    public Promise<Unit> start() {
        return startPromise.get();
    }

    public Promise<Unit> stop() {
        return Promise.promise(promise -> {
            clusterDisconnected();
            executor.shutdown();
            promise.succeed(Unit.unit());
        });
    }

    @MessageReceiver
    public void processPropose(Propose<C> propose) {
        executor.execute(() -> handlePropose(propose));
    }

    @MessageReceiver
    public void processVoteRound1(VoteRound1 voteRound1) {
        executor.execute(() -> handleVoteRound1(voteRound1));
    }

    @MessageReceiver
    public void processVoteRound2(VoteRound2 voteRound2) {
        executor.execute(() -> handleVoteRound2(voteRound2));
    }

    @MessageReceiver
    public void processDecision(Decision<C> decision) {
        executor.execute(() -> handleDecision(decision));
    }

    @MessageReceiver
    public void processSyncResponse(SyncResponse<C> syncResponse) {
        executor.execute(() -> handleSyncResponse(syncResponse));
    }

    @SuppressWarnings("unchecked")
    @MessageReceiver
    public void handleNewBatch(NewBatch< ? > newBatch) {
        pendingBatches.put(newBatch.batch()
                                   .correlationId(),
                           (Batch<C>) newBatch.batch());
    }

    /**
     * Starts a new phase with pending commands.
     */
    private void startPhase() {
        // Use compareAndSet to atomically check and set - prevents race condition
        if (!isInPhase.compareAndSet(false, true)) {
            return;
        }
        if (pendingBatches.isEmpty()) {
            isInPhase.set(false);
            // Reset since we won't actually start
            return;
        }
        var phase = currentPhase.get();
        var batch = pendingBatches.values()
                                  .stream()
                                  .sorted()
                                  .findFirst()
                                  .orElse(emptyBatch());
        log.trace("Node {} starting phase {} with batch {}", self, phase, batch.id());
        var phaseData = getOrCreatePhaseData(phase);
        phaseData.proposals.put(self, batch);
        network.broadcast(new Propose<>(self, phase, batch));
    }

    /**
     * Synchronizes with other nodes to catch up if needed.
     */
    private void synchronize() {
        if (active.get()) {
            return;
        }
        // Clear stale responses
        syncResponses.clear();
        var request = new SyncRequest(self);
        log.trace("Node {}: requesting phase synchronization {}", self, request);
        network.broadcast(request);
        SharedScheduler.schedule(this::synchronize,
                                 config.syncRetryInterval()
                                       .randomize(SCALE));
    }

    /**
     * Handles a synchronization response from another node.
     */
    private void handleSyncResponse(SyncResponse<C> response) {
        if (active.get()) {
            log.trace("Node {} ignoring synchronization response {}. Node is active", self, response);
            return;
        }
        syncResponses.put(response.sender(), response.state());
        if (syncResponses.size() < topologyManager.quorumSize()) {
            log.trace("Node {} received {} responses {}, not enough to proceed (quorum size = {})",
                      self,
                      syncResponses.size(),
                      syncResponses.keySet(),
                      topologyManager.quorumSize());
            return;
        }
        log.trace("Node {} received {} responses, collected: {}", self, syncResponses.size(), syncResponses);
        // Use the latest known state among received responses
        var candidate = syncResponses.values()
                                     .stream()
                                     .sorted(Comparator.comparing(SavedState::lastCommittedPhase))
                                     .toList()
                                     .getLast();
        log.trace("Node {} uses {} as synchronization candidate out of {}", self, candidate, syncResponses.size());
        restoreState(candidate);
    }

    private void restoreState(SavedState<C> state) {
        syncResponses.clear();
        if (state.snapshot().length == 0) {
            activate();
            return;
        }
        stateMachine.restoreSnapshot(state.snapshot())
                    .onSuccess(_ -> {
                                   currentPhase.set(state.lastCommittedPhase());
                                   lastCommittedPhase.set(state.lastCommittedPhase());
                                   state.pendingBatches()
                                        .forEach(batch -> pendingBatches.put(batch.correlationId(),
                                                                             batch));
                                   persistence.save(stateMachine,
                                                    currentPhase.get(),
                                                    pendingBatches.values());
                                   log.info("Node {} restored state from persistence. Current phase {}",
                                            self,
                                            currentPhase.get());
                               })
                    .onSuccessRun(this::activate)
                    .onFailure(cause -> log.error("Node {} failed to restore state: {}", self, cause));
    }

    /**
     * Activate node and adjust phase, if necessary.
     */
    private void activate() {
        active.set(true);
        startPromise.get()
                    .succeed(Unit.unit());
        syncResponses.clear();
        log.info("Node {} activated in phase {}", self, currentPhase.get());
        executor.execute(this::startPhase);
    }

    /**
     * Handles a synchronization request from another node.
     */
    @MessageReceiver
    public void handleSyncRequest(SyncRequest request) {
        if (active.get()) {
            stateMachine.makeSnapshot()
                        .map(snapshot -> new SyncResponse<>(self,
                                                            savedState(snapshot,
                                                                       lastCommittedPhase.get(),
                                                                       pendingBatches.values())))
                        .onSuccess(response -> network.send(request.sender(),
                                                            response))
                        .onFailure(cause -> log.error("Node {} failed to create snapshot: {}", self, cause));
        } else {
            log.trace("Node {} is inactive, trying to share saved (or empty) state for request: {}", self, request);
            var response = new SyncResponse<>(self,
                                              persistence.load()
                                                         .or(SavedState.empty()));
            network.send(request.sender(), response);
        }
    }

    /**
     * Cleans up old phase data to prevent memory leaks.
     */
    private void cleanupOldPhases() {
        if (!active.get()) {
            return;
        }
        var current = currentPhase.get();
        phases.keySet()
              .removeIf(phase -> isExpiredPhase(phase, current));
    }

    private boolean isExpiredPhase(Phase phase, Phase current) {
        return phase.compareTo(current) < 0 && current.value() - phase.value() > config.removeOlderThanPhases();
    }

    /**
     * Handles a Propose message from another node.
     */
    private void handlePropose(Propose<C> propose) {
        if (!active.get()) {
            log.warn("Node {} ignores proposal {}. Node is dormant", self, propose);
            return;
        }
        log.trace("Node {} received proposal from {} for phase {}", self, propose.sender(), propose.phase());
        var currentPhaseValue = currentPhase.get();
        // Ignore proposals for past phases
        if (propose.phase()
                   .compareTo(currentPhaseValue) < 0) {
            log.trace("Node {} ignoring proposal for past phase {}", self, propose.phase());
            return;
        }
        // Potentially add check for proposals too far in the future
        var phaseData = getOrCreatePhaseData(propose.phase());
        // Ensure the phase state is correct. If we receive a proposal for the current
        // phase value but aren't "in" it, enter it now.
        // This assumes the currentPhaseValue is correctly managed by sync/moveToNextPhase
        if (propose.phase()
                   .equals(currentPhaseValue) && !isInPhase.get()) {
            log.trace("Node {} entering phase {} triggered by proposal from {}", self, propose.phase(), propose.sender());
            isInPhase.set(true);
        }
        phaseData.proposals.putIfAbsent(propose.sender(), propose.value());
        if (isInPhase.get() && currentPhase.get()
                                           .equals(propose.phase()) && !phaseData.round1Votes.containsKey(self)) {
            var vote = phaseData.evaluateInitialVote(self, propose);
            log.trace("Node {} broadcasting R1 vote {} for phase {} based on received proposal from {}",
                      self,
                      vote,
                      propose.phase(),
                      propose.sender());
            network.broadcast(vote);
            phaseData.round1Votes.put(self, vote.stateValue());
        } else {
            log.trace("Node {} conditions not met to vote R1 on proposal from {} for phase {}. Active: {}, InPhase: {}, CurrentPhase: {}, HasVotedR1: {}",
                      self,
                      propose.sender(),
                      propose.phase(),
                      active.get(),
                      isInPhase.get(),
                      currentPhase.get(),
                      phaseData.round1Votes.containsKey(self));
        }
    }

    /**
     * Handles a round 1 vote from another node.
     */
    private void handleVoteRound1(VoteRound1 vote) {
        if (!active.get()) {
            log.warn("Node {} ignores vote1 {}. Node is dormant", self, vote);
            return;
        }
        log.trace("Node {} received round 1 vote from {} for phase {} with value {}",
                  self,
                  vote.sender(),
                  vote.phase(),
                  vote.stateValue());
        var phaseData = getOrCreatePhaseData(vote.phase());
        phaseData.round1Votes.put(vote.sender(), vote.stateValue());
        // If we're active and in this phase, check if we can proceed to round 2
        if (isInPhase.get() && currentPhase.get()
                                           .equals(vote.phase()) && !phaseData.round2Votes.containsKey(self)) {
            if (!phaseData.hasRound1MajorityVotes(topologyManager.quorumSize())) {
                return;
            }
            var round2Vote = phaseData.evaluateRound2Vote(topologyManager.quorumSize());
            log.trace("Node {} votes in round 2 {}", self, round2Vote);
            network.broadcast(new VoteRound2(self, vote.phase(), round2Vote));
            phaseData.round2Votes.put(self, round2Vote);
        }
    }

    /**
     * Handles a round 2 vote from another node.
     */
    private void handleVoteRound2(VoteRound2 vote) {
        if (!active.get()) {
            log.warn("Node {} ignores vote2 {}. Node is dormant", self, vote);
            return;
        }
        log.trace("Node {} received round 2 vote from {} for phase {} with value {}",
                  self,
                  vote.sender(),
                  vote.phase(),
                  vote.stateValue());
        var phaseData = getOrCreatePhaseData(vote.phase());
        phaseData.round2Votes.put(vote.sender(), vote.stateValue());
        // If we're active and in this phase, check if we can make a decision
        if (isInPhase.get() && currentPhase.get()
                                           .equals(vote.phase()) && !phaseData.hasDecided.get()) {
            if (phaseData.hasRound2MajorityVotes(topologyManager.quorumSize())) {
                var decision = phaseData.processRound2Completion(self, topologyManager.fPlusOne());
                network.broadcast(decision);
                processDecision(decision);
            }
        }
    }

    private void commitDecision(PhaseData<C> phaseData, Decision<C> decision) {
        // Broadcast the decision
        if (phaseData.hasDecided.compareAndSet(false, true)) {
            // Apply commands to state machine ONLY if it was a V1 decision with a non-empty batch
            if (decision.stateValue() == StateValue.V1 && !decision.value()
                                                                   .commands()
                                                                   .isEmpty()) {
                commitChanges(phaseData, decision);
            }
            moveToNextPhase(phaseData.phase);
        }
    }

    @SuppressWarnings("unchecked")
    private void commitChanges(PhaseData<C> phaseData, Decision<C> decision) {
        log.trace("Node {} applies decision {}", self, decision);
        var results = stateMachine.process(decision.value()
                                                   .commands());
        lastCommittedPhase.set(phaseData.phase);
        pendingBatches.remove(decision.value()
                                      .correlationId());
        correlationMap.computeIfPresent(decision.value()
                                                .correlationId(),
                                        (_, promise) -> {
                                            promise.succeed(results);
                                            return null;
                                        });
    }

    /**
     * Handles a decision message from another node.
     */
    private void handleDecision(Decision<C> decision) {
        if (!active.get()) {
            log.warn("Node {} ignores decision {}. Node is dormant", self, decision);
            return;
        }
        log.trace("Node {} received decision {}", self, decision);
        commitDecision(getOrCreatePhaseData(decision.phase()), decision);
    }

    /**
     * Moves to the next phase after a decision.
     */
    private void moveToNextPhase(Phase currentPhase) {
        var nextPhase = currentPhase.successor();
        this.currentPhase.set(nextPhase);
        isInPhase.set(false);
        log.trace("Node {} moving to phase {}", self, nextPhase);
        // If we have more commands to process, start a new phase
        if (!pendingBatches.isEmpty()) {
            executor.execute(this::startPhase);
        }
    }

    /**
     * Gets or creates phase data for a specific phase.
     */
    private PhaseData<C> getOrCreatePhaseData(Phase phase) {
        return phases.computeIfAbsent(phase, PhaseData::new);
    }

    /**
     * Data structure to hold all state related to a specific phase.
     */
    private static class PhaseData<C extends Command> {
        final Phase phase;
        final Map<NodeId, Batch<C>> proposals = new ConcurrentHashMap<>();
        final Map<NodeId, StateValue> round1Votes = new ConcurrentHashMap<>();
        final Map<NodeId, StateValue> round2Votes = new ConcurrentHashMap<>();
        final AtomicBoolean hasDecided = new AtomicBoolean(false);

        PhaseData(Phase phase) {
            this.phase = phase;
        }

        /**
         * Checks if we have a majority of votes in round 1.
         */
        boolean hasRound1MajorityVotes(int quorumSize) {
            return round1Votes.size() >= quorumSize;
        }

        /**
         * Checks if we have a majority of votes in round 2.
         */
        boolean hasRound2MajorityVotes(int quorumSize) {
            return round2Votes.size() >= quorumSize;
        }

        /**
         * Finds the agreed proposal when a V1 decision is made.
         */
        public Batch<C> findAgreedProposal(NodeId self) {
            // If all proposals are the same, return that one
            long distinctProposals = proposals.values()
                                              .stream()
                                              .map(Batch::correlationId)
                                              .distinct()
                                              .count();
            if (distinctProposals == 1) {
                return proposals.values()
                                .iterator()
                                .next();
            }
            // Otherwise, just use our own proposal or an empty one
            return proposals.getOrDefault(self, emptyBatch());
        }

        public VoteRound1 evaluateInitialVote(NodeId self, Propose<C> propose) {
            var existingProposal = proposals.values()
                                            .stream()
                                            .filter(Batch::isNotEmpty)
                                            .map(Batch::correlationId)
                                            .filter(correlationId -> correlationId.equals(propose.value()
                                                                                                 .correlationId()))
                                            .distinct()
                                            .toList();
            if (existingProposal.size() > 1) {
                return new VoteRound1(self, propose.phase(), StateValue.V0);
            }
            proposals.put(self, propose.value());
            return new VoteRound1(self, propose.phase(), StateValue.V1);
        }

        public StateValue evaluateRound2Vote(int quorumSize) {
            // If a majority voted for the same value in round 1, vote that value
            for (var value : List.of(StateValue.V0, StateValue.V1)) {
                if (countRound1VotesForValue(value) >= quorumSize) {
                    return value;
                }
            }
            // Otherwise, vote VQUESTION
            return StateValue.VQUESTION;
        }

        public int countRound1VotesForValue(StateValue value) {
            return ( int) round1Votes.values()
                                    .stream()
                                    .filter(v -> v == value)
                                    .count();
        }

        public int countRound2VotesForValue(StateValue value) {
            return ( int) round2Votes.values()
                                    .stream()
                                    .filter(v -> v == value)
                                    .count();
        }

        public Decision<C> processRound2Completion(NodeId self, int fPlusOneSize) {
            // 1. Check for f+1 votes for V1
            if (countRound2VotesForValue(StateValue.V1) >= fPlusOneSize) {
                return new Decision<>(self, phase, StateValue.V1, findAgreedProposal(self));
            }
            // 2. Check for f+1 votes for V0
            if (countRound2VotesForValue(StateValue.V0) >= fPlusOneSize) {
                return new Decision<>(self, phase, StateValue.V0, emptyBatch());
            }
            // 3. Fallback: If none of the above conditions are met, use the coin flip.
            //    This covers the ambiguous cases (mixed votes, including VQUESTION,
            //    that don't meet thresholds) and the "all VQUESTION" case implicitly.
            var decision = coinFlip(self);
            // If coin flip is V1, we still need to try and find *an* agreed proposal.
            // The definition of findAgreedProposal might need review for this case,
            // but typically it would look for *any* proposal associated with V1 votes
            // or a default empty batch if none is found.
            var batch = decision == StateValue.V1
                        ? findAgreedProposal(self)
                        : Batch.<C>emptyBatch();
            return new Decision<>(self, phase, decision, batch);
        }

        /**
         * Gets a coin flip value for a phase, creating one if needed.
         */
        private StateValue coinFlip(NodeId self) {
            long seed = phase.value() ^ self.id()
                                            .hashCode();
            return ( Math.abs(seed) % 2 == 0)
                   ? StateValue.V0
                   : StateValue.V1;
        }
    }
}
