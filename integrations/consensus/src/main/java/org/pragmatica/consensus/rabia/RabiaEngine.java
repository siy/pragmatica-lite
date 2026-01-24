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
import org.pragmatica.consensus.ConsensusError;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.consensus.StateMachine;
import org.pragmatica.consensus.net.ClusterNetwork;
import org.pragmatica.consensus.rabia.RabiaEngineIO.SubmitCommands;
import org.pragmatica.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Asynchronous.NewBatch;
import org.pragmatica.consensus.rabia.RabiaProtocolMessage.Synchronous.*;
import org.pragmatica.consensus.topology.QuorumStateNotification;
import org.pragmatica.consensus.topology.TopologyManager;
import org.pragmatica.lang.Option;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.consensus.rabia.Batch.batch;
import static org.pragmatica.consensus.rabia.Batch.emptyBatch;
import static org.pragmatica.consensus.rabia.RabiaPersistence.SavedState.savedState;
import static org.pragmatica.consensus.rabia.RabiaProtocolMessage.Asynchronous.SyncRequest;

/// Implementation of the Rabia consensus protocol.
///
/// Rabia is a crash-fault-tolerant (CFT) consensus algorithm that provides:
///
///   - No persistent event log required
///   - Batch-based command processing
///   - Automatic state synchronization
///   - Deterministic decision-making with coin-flip fallback
///
/// @param <C> Command type
public class RabiaEngine<C extends Command> {
    private static final Logger log = LoggerFactory.getLogger(RabiaEngine.class);
    private static final double SCALE = 0.5d;

    private final NodeId self;
    private final TopologyManager topologyManager;
    private final ClusterNetwork network;
    private final StateMachine<C> stateMachine;
    private final ProtocolConfig config;
    private final ConsensusMetrics metrics;

    // Single-thread executor with DiscardPolicy to silently drop tasks after shutdown
    private final ExecutorService executor = new ThreadPoolExecutor(1,
                                                                    1,
                                                                    0L,
                                                                    TimeUnit.MILLISECONDS,
                                                                    new LinkedBlockingQueue<>(),
                                                                    new ThreadPoolExecutor.DiscardPolicy());
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

    // Per Rabia spec: after a decision, the next phase inherits this value for round 1 vote
    private final AtomicReference<Option<StateValue>> lockedValue = new AtomicReference<>(Option.none());
    private final Option<ScheduledFuture<?>> cleanupTask;
    private final Option<ScheduledFuture<?>> syncTask;

    //--------------------------------- Node State End
    /// Creates a new Rabia consensus engine without metrics.
    ///
    /// @param topologyManager The topology manager for node communication
    /// @param network         The network implementation
    /// @param stateMachine    The state machine to apply commands to
    /// @param config          Configuration for the consensus engine
    public RabiaEngine(TopologyManager topologyManager,
                       ClusterNetwork network,
                       StateMachine<C> stateMachine,
                       ProtocolConfig config) {
        this(topologyManager, network, stateMachine, config, ConsensusMetrics.noop());
    }

    /// Creates a new Rabia consensus engine with metrics.
    ///
    /// @param topologyManager The topology manager for node communication
    /// @param network         The network implementation
    /// @param stateMachine    The state machine to apply commands to
    /// @param config          Configuration for the consensus engine
    /// @param metrics         Metrics collector for observability
    public RabiaEngine(TopologyManager topologyManager,
                       ClusterNetwork network,
                       StateMachine<C> stateMachine,
                       ProtocolConfig config,
                       ConsensusMetrics metrics) {
        this.self = topologyManager.self()
                                   .id();
        this.topologyManager = topologyManager;
        this.network = network;
        this.stateMachine = stateMachine;
        this.config = config;
        this.metrics = Option.option(metrics)
                             .or(ConsensusMetrics.noop());
        this.cleanupTask = Option.some(SharedScheduler.scheduleAtFixedRate(this::cleanupOldPhases,
                                                                           config.cleanupInterval()));
        this.syncTask = Option.some(SharedScheduler.schedule(this::synchronize, config.syncRetryInterval()));
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
        executor.execute(this::doClusterDisconnected);
    }

    private void doClusterDisconnected() {
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
        lockedValue.set(Option.none());
        stateMachine.reset();
        startPromise.set(Promise.promise());
        pendingBatches.clear();
        correlationMap.forEach((_, promise) -> promise.fail(ConsensusError.nodeInactive(self)));
        correlationMap.clear();
    }

    public boolean isActive() {
        return active.get();
    }

    public <R> Promise<List<R>> apply(List<C> commands) {
        var pendingAnswer = Promise.<List<R>>promise();
        return submitCommands(commands,
                              batch -> correlationMap.put(batch.correlationId(),
                                                          pendingAnswer)).async()
                             .flatMap(_ -> pendingAnswer);
    }

    @MessageReceiver
    public void handleSubmit(SubmitCommands<C> submitCommands) {
        submitCommands(submitCommands.commands(),
                       _ -> {});
    }

    private Result<Batch<C>> submitCommands(List<C> commands, Consumer<Batch<C>> onBatchPrepared) {
        return validateSubmission(commands).map(_ -> prepareBatch(commands))
                                 .onSuccess(batch -> executor.execute(() -> registerBatch(batch, onBatchPrepared)))
                                 .onSuccess(batch -> executor.execute(() -> broadcastBatch(batch)));
    }

    private Result<List<C>> validateSubmission(List<C> commands) {
        if (commands.isEmpty()) {
            return ConsensusError.commandBatchIsEmpty()
                                 .result();
        }
        if (!active.get()) {
            return ConsensusError.nodeInactive(self)
                                 .result();
        }
        return Result.success(commands);
    }

    private Batch<C> prepareBatch(List<C> commands) {
        var batch = batch(commands);
        log.trace("Node {}: client submitted {} command(s). Prepared batch: {}", self, commands.size(), batch);
        return batch;
    }

    private void registerBatch(Batch<C> batch, Consumer<Batch<C>> onBatchPrepared) {
        pendingBatches.put(batch.correlationId(), batch);
        metrics.updatePendingBatches(self, pendingBatches.size());
        onBatchPrepared.accept(batch);
        triggerPhaseIfNeeded();
    }

    private void broadcastBatch(Batch<C> batch) {
        network.broadcast(new NewBatch<>(self, batch));
    }

    private void triggerPhaseIfNeeded() {
        if (!isInPhase.get()) {
            executor.execute(this::startPhase);
        }
    }

    public Promise<Unit> start() {
        return startPromise.get();
    }

    public Promise<Unit> stop() {
        return Promise.promise(this::performStop);
    }

    private void performStop(Promise<Unit> promise) {
        cleanupTask.onPresent(task -> task.cancel(false));
        syncTask.onPresent(task -> task.cancel(false));
        clusterDisconnected();
        executor.shutdown();
        promise.succeed(Unit.unit());
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
    public void handleNewBatch(NewBatch<?> newBatch) {
        executor.execute(() -> {
                             pendingBatches.put(newBatch.batch()
                                                        .correlationId(),
                                                (Batch<C>) newBatch.batch());
                             if (isInPhase.get() && active.get()) {
                                 // Already in phase - broadcast our proposal for this batch if not already proposed
        broadcastOwnProposalIfNeeded();
                             } else {
                                 triggerPhaseIfNeeded();
                             }
                         });
    }

    /// Broadcasts own proposal for pending batch if not already proposed in current phase.
    private void broadcastOwnProposalIfNeeded() {
        var phase = currentPhase.get();
        var phaseData = getOrCreatePhaseData(phase);
        if (phaseData.hasProposal(self)) {
            return;
        }
        pendingBatches.values()
                      .stream()
                      .sorted()
                      .findFirst()
                      .ifPresent(batch -> broadcastOwnProposal(phase, phaseData, batch));
    }

    /// Starts a new phase with pending commands.
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
        phaseData.registerProposal(self, batch);
        network.broadcast(new Propose<>(self, phase, batch));
        broadcastLockedValueIfPresent(phase, phaseData);
    }

    private void broadcastLockedValueIfPresent(Phase phase, PhaseData<C> phaseData) {
        lockedValue.getAndSet(Option.none())
                   .onPresent(locked -> broadcastLockedVote(phase, phaseData, locked));
    }

    private void broadcastLockedVote(Phase phase, PhaseData<C> phaseData, StateValue locked) {
        var vote = new VoteRound1(self, phase, locked);
        log.trace("Node {} immediately voting locked value {} for phase {}", self, locked, phase);
        network.broadcast(vote);
        phaseData.registerRound1Vote(self, locked);
    }

    /// Synchronizes with other nodes to catch up if needed.
    private void synchronize() {
        executor.execute(this::doSynchronize);
    }

    private void doSynchronize() {
        if (active.get()) {
            return;
        }
        syncResponses.clear();
        var request = new SyncRequest(self);
        log.trace("Node {}: requesting phase synchronization {}", self, request);
        network.broadcast(request);
        SharedScheduler.schedule(this::synchronize,
                                 config.syncRetryInterval()
                                       .randomize(SCALE));
    }

    /// Handles a synchronization response from another node.
    private void handleSyncResponse(SyncResponse<C> response) {
        if (active.get()) {
            log.trace("Node {} ignoring synchronization response {}. Node is active", self, response);
            return;
        }
        syncResponses.put(response.sender(), response.state());
        if (syncResponses.size() < topologyManager.activeQuorumSize()) {
            log.trace("Node {} received {} responses {}, not enough to proceed (active quorum size = {})",
                      self,
                      syncResponses.size(),
                      syncResponses.keySet(),
                      topologyManager.activeQuorumSize());
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
                    .onSuccess(_ -> applyRestoredState(state))
                    .onSuccessRun(this::activate)
                    .onFailure(cause -> log.error("Node {} failed to restore state: {}", self, cause));
    }

    private void applyRestoredState(SavedState<C> state) {
        currentPhase.set(state.lastCommittedPhase());
        lastCommittedPhase.set(state.lastCommittedPhase());
        state.pendingBatches()
             .forEach(batch -> pendingBatches.put(batch.correlationId(),
                                                  batch));
        persistence.save(stateMachine, currentPhase.get(), pendingBatches.values());
        log.info("Node {} restored state from persistence. Current phase {}", self, currentPhase.get());
    }

    /// Activate node and adjust phase, if necessary.
    private void activate() {
        active.set(true);
        startPromise.get()
                    .succeed(Unit.unit());
        syncResponses.clear();
        metrics.recordSyncAttempt(self, true);
        log.info("Node {} activated in phase {}", self, currentPhase.get());
        executor.execute(this::startPhase);
    }

    /// Handles a synchronization request from another node.
    @MessageReceiver
    public void handleSyncRequest(SyncRequest request) {
        executor.execute(() -> doHandleSyncRequest(request));
    }

    private void doHandleSyncRequest(SyncRequest request) {
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

    /// Cleans up old phase data to prevent memory leaks.
    private void cleanupOldPhases() {
        executor.execute(this::doCleanupOldPhases);
    }

    private void doCleanupOldPhases() {
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

    /// Handles a Propose message from another node.
    private void handlePropose(Propose<C> propose) {
        if (!active.get()) {
            log.warn("Node {} ignores proposal {}. Node is dormant", self, propose);
            return;
        }
        log.trace("Node {} received proposal from {} for phase {}", self, propose.sender(), propose.phase());
        var currentPhaseValue = currentPhase.get();
        if (isPastPhase(propose.phase(), currentPhaseValue)) {
            log.trace("Node {} ignoring proposal for past phase {}", self, propose.phase());
            return;
        }
        if (isFarFuturePhase(propose.phase(), currentPhaseValue)) {
            log.warn("Node {} rejecting proposal for far-future phase {} (current: {})",
                     self,
                     propose.phase(),
                     currentPhaseValue);
            return;
        }
        var phaseData = getOrCreatePhaseData(propose.phase());
        enterPhaseIfNeeded(propose.phase(), currentPhaseValue, phaseData);
        registerProposal(propose, phaseData);
        tryBroadcastRound1Vote(propose.phase(), phaseData);
    }

    private static final long MAX_PHASE_AHEAD = 100;

    private boolean isFarFuturePhase(Phase proposalPhase, Phase current) {
        return proposalPhase.value() - current.value() > MAX_PHASE_AHEAD;
    }

    private boolean isPastPhase(Phase proposalPhase, Phase current) {
        return proposalPhase.compareTo(current) < 0;
    }

    private void enterPhaseIfNeeded(Phase proposalPhase, Phase currentPhaseValue, PhaseData<C> phaseData) {
        if (!proposalPhase.equals(currentPhaseValue) || isInPhase.get()) {
            return;
        }
        log.trace("Node {} entering phase {} triggered by external proposal", self, proposalPhase);
        isInPhase.set(true);
        pendingBatches.values()
                      .stream()
                      .sorted()
                      .findFirst()
                      .ifPresent(ourBatch -> broadcastOwnProposal(proposalPhase, phaseData, ourBatch));
        // Broadcast locked value if present (same as startPhase does)
        broadcastLockedValueIfPresent(proposalPhase, phaseData);
    }

    private void broadcastOwnProposal(Phase phase, PhaseData<C> phaseData, Batch<C> batch) {
        phaseData.registerProposal(self, batch);
        network.broadcast(new Propose<>(self, phase, batch));
    }

    private void registerProposal(Propose<C> propose, PhaseData<C> phaseData) {
        phaseData.registerProposal(propose.sender(), propose.value());
        metrics.recordProposal(propose.sender(), propose.phase());
    }

    private void tryBroadcastRound1Vote(Phase phase, PhaseData<C> phaseData) {
        var quorumSize = topologyManager.quorumSize();
        if (canVoteRound1(phase, phaseData, quorumSize)) {
            broadcastRound1Vote(phase, phaseData, quorumSize);
        } else {
            logRound1VoteConditionsNotMet(phase, phaseData, quorumSize);
        }
    }

    private boolean canVoteRound1(Phase phase, PhaseData<C> phaseData, int quorumSize) {
        return isInPhase.get() && currentPhase.get()
                                              .equals(phase) && !phaseData.hasVotedRound1(self) && phaseData.hasQuorumProposals(quorumSize);
    }

    private void broadcastRound1Vote(Phase phase, PhaseData<C> phaseData, int quorumSize) {
        var vote = phaseData.evaluateInitialVote(self, quorumSize);
        log.trace("Node {} broadcasting R1 vote {} for phase {} after collecting quorum proposals", self, vote, phase);
        network.broadcast(vote);
        phaseData.registerRound1Vote(self, vote.stateValue());
    }

    private void logRound1VoteConditionsNotMet(Phase phase, PhaseData<C> phaseData, int quorumSize) {
        log.trace("Node {} conditions not met to vote R1 for phase {}. InPhase: {}, CurrentPhase: {}, HasVotedR1: {}, ProposalCount: {}/{}",
                  self,
                  phase,
                  isInPhase.get(),
                  currentPhase.get(),
                  phaseData.hasVotedRound1(self),
                  phaseData.proposalCount(),
                  quorumSize);
    }

    /// Handles a round 1 vote from another node.
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
        registerRound1Vote(vote, phaseData);
        tryBroadcastRound2Vote(vote.phase(), phaseData);
    }

    private void registerRound1Vote(VoteRound1 vote, PhaseData<C> phaseData) {
        phaseData.registerRound1Vote(vote.sender(), vote.stateValue());
        metrics.recordVoteRound1(vote.sender(), vote.phase(), vote.stateValue());
    }

    private void tryBroadcastRound2Vote(Phase phase, PhaseData<C> phaseData) {
        var quorumSize = topologyManager.quorumSize();
        var superMajoritySize = topologyManager.superMajoritySize();
        // Check for fast path: if n-f nodes agree in Round 1, skip Round 2
        var superMajorityValue = phaseData.getSuperMajorityRound1Value(superMajoritySize);
        if (canUseFastPath(phase, phaseData, superMajorityValue)) {
            useFastPath(phase, phaseData, superMajorityValue, quorumSize);
            return;
        }
        // Normal path: proceed with Round 2 voting
        if (canVoteRound2(phase, phaseData, quorumSize)) {
            broadcastRound2Vote(phase, phaseData, quorumSize);
        }
    }

    private boolean canUseFastPath(Phase phase, PhaseData<C> phaseData, Option<StateValue> superMajorityValue) {
        return isInPhase.get() && currentPhase.get()
                                              .equals(phase) && !phaseData.isDecided() && !phaseData.hasVotedRound2(self) && superMajorityValue.isPresent();
    }

    private void useFastPath(Phase phase,
                             PhaseData<C> phaseData,
                             Option<StateValue> superMajorityValue,
                             int quorumSize) {
        superMajorityValue.onPresent(agreedValue -> {
                                         log.debug("Node {} using fast path for phase {} with value {} (super-majority agreement)",
                                                   self,
                                                   phase,
                                                   agreedValue);
                                         metrics.recordFastPath(self, phase, agreedValue);
                                         var decision = buildDecision(phaseData, agreedValue, quorumSize);
                                         network.broadcast(decision);
                                         processDecision(decision);
                                     });
    }

    private Decision<C> buildDecision(PhaseData<C> phaseData, StateValue agreedValue, int quorumSize) {
        var batch = agreedValue == StateValue.V1
                    ? phaseData.findAgreedProposal(quorumSize)
                    : Batch.<C>emptyBatch();
        return new Decision<>(self, phaseData.phase(), agreedValue, batch);
    }

    private boolean canVoteRound2(Phase phase, PhaseData<C> phaseData, int quorumSize) {
        return isInPhase.get() && currentPhase.get()
                                              .equals(phase) && !phaseData.hasVotedRound2(self) && phaseData.hasRound1MajorityVotes(quorumSize);
    }

    private void broadcastRound2Vote(Phase phase, PhaseData<C> phaseData, int quorumSize) {
        var round2Vote = phaseData.evaluateRound2Vote(quorumSize);
        log.trace("Node {} votes in round 2 {}", self, round2Vote);
        network.broadcast(new VoteRound2(self, phase, round2Vote));
        phaseData.registerRound2Vote(self, round2Vote);
    }

    /// Handles a round 2 vote from another node.
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
        registerRound2Vote(vote, phaseData);
        tryMakeDecision(vote.phase(), phaseData);
    }

    private void registerRound2Vote(VoteRound2 vote, PhaseData<C> phaseData) {
        phaseData.registerRound2Vote(vote.sender(), vote.stateValue());
        metrics.recordVoteRound2(vote.sender(), vote.phase(), vote.stateValue());
    }

    private void tryMakeDecision(Phase phase, PhaseData<C> phaseData) {
        var quorumSize = topologyManager.quorumSize();
        if (canMakeDecision(phase, phaseData, quorumSize)) {
            makeAndBroadcastDecision(phaseData, quorumSize);
        }
    }

    private boolean canMakeDecision(Phase phase, PhaseData<C> phaseData, int quorumSize) {
        return isInPhase.get() && currentPhase.get()
                                              .equals(phase) && !phaseData.isDecided() && phaseData.hasRound2MajorityVotes(quorumSize);
    }

    private void makeAndBroadcastDecision(PhaseData<C> phaseData, int quorumSize) {
        var decision = phaseData.processRound2Completion(self, topologyManager.fPlusOne(), quorumSize);
        network.broadcast(decision);
        processDecision(decision);
    }

    private void commitDecision(PhaseData<C> phaseData, Decision<C> decision) {
        if (phaseData.tryMarkDecided()) {
            metrics.recordDecision(self, phaseData.phase(), decision.stateValue(), 0L);
            // Apply commands to state machine ONLY if it was a V1 decision with a non-empty batch
            if (decision.stateValue() == StateValue.V1 && !decision.value()
                                                                   .commands()
                                                                   .isEmpty()) {
                commitChanges(phaseData, decision);
            }
            moveToNextPhase(phaseData.phase(), decision.stateValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void commitChanges(PhaseData<C> phaseData, Decision<C> decision) {
        log.trace("Node {} applies decision {}", self, decision);
        var results = stateMachine.process(decision.value()
                                                   .commands());
        lastCommittedPhase.set(phaseData.phase());
        pendingBatches.remove(decision.value()
                                      .correlationId());
        metrics.updatePendingBatches(self, pendingBatches.size());
        Option.option(correlationMap.remove(decision.value()
                                                    .correlationId()))
              .onPresent(promise -> promise.succeed(results));
    }

    /// Handles a decision message from another node.
    private void handleDecision(Decision<C> decision) {
        if (!active.get()) {
            log.warn("Node {} ignores decision {}. Node is dormant", self, decision);
            return;
        }
        log.trace("Node {} received decision {}", self, decision);
        commitDecision(getOrCreatePhaseData(decision.phase()), decision);
    }

    /// Moves to the next phase after a decision.
    /// Per Rabia spec: the decision value is carried forward as the round 1 vote for the next phase.
    private void moveToNextPhase(Phase currentPhase, StateValue decidedValue) {
        var nextPhase = currentPhase.successor();
        this.currentPhase.set(nextPhase);
        isInPhase.set(false);
        // Lock the value for the next phase's round 1 vote
        lockedValue.set(Option.some(decidedValue));
        log.trace("Node {} moving to phase {} with locked value {}", self, nextPhase, decidedValue);
        // If we have more commands to process, start a new phase
        if (!pendingBatches.isEmpty()) {
            executor.execute(this::startPhase);
        }
    }

    /// Gets or creates phase data for a specific phase.
    private PhaseData<C> getOrCreatePhaseData(Phase phase) {
        return phases.computeIfAbsent(phase, PhaseData::new);
    }
}
