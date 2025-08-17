package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.ConsensusErrors;
import org.pragmatica.cluster.consensus.rabia.RabiaEngineIO.SubmitCommands;
import org.pragmatica.cluster.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Asynchronous.NewBatch;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Synchronous.*;
import org.pragmatica.cluster.net.ClusterNetwork;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.cluster.state.StateMachine;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.SharedScheduler;
import org.pragmatica.message.MessageReceiver;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.Comparator;

import static org.pragmatica.cluster.consensus.rabia.Batch.batch;
import static org.pragmatica.cluster.consensus.rabia.Batch.emptyBatch;
import static org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Asynchronous.SyncRequest;

/// Implementation of the Rabia consensus protocol.
public class RabiaEngine<C extends Command> {
    private static final Logger log = LoggerFactory.getLogger(RabiaEngine.class);
    private static final double SCALE = 0.5d;
    private static final int MAX_PENDING_BATCHES = 10000;
    private static final int MAX_CORRELATION_MAP = 10000;

    private final NodeId self;
    private final TopologyManager topologyManager;
    private final ProtocolConfig config;
    private final OptimizedConsensusExecutor consensusExecutor = new OptimizedConsensusExecutor();
    
    // Use bounded collections to prevent memory leaks
    private final BoundedLRUMap<CorrelationId, Batch<C>> pendingBatches = new BoundedLRUMap<>(MAX_PENDING_BATCHES);
    @SuppressWarnings("rawtypes")
    private final BoundedLRUMap<CorrelationId, Promise> correlationMap = new BoundedLRUMap<>(MAX_CORRELATION_MAP);
    
    // Optimize batch selection with priority queue
    private final PriorityBlockingQueue<Batch<C>> batchQueue = new PriorityBlockingQueue<>(100, Comparator.naturalOrder());

    // Extracted components
    private final RabiaConsensusManager<C> consensusManager = new RabiaConsensusManager<>();
    private final RabiaNetworkManager<C> networkManager;
    private final RabiaStateManager<C> stateManager;
    
    // Performance monitoring
    private final RabiaPerformanceMetrics performanceMetrics = new RabiaPerformanceMetrics();

    /// Creates a new Rabia consensus engine.
    ///
    /// @param topologyManager The address book for node communication
    /// @param network         The network implementation
    /// @param stateMachine    The state machine to apply commands to
    /// @param config          Configuration for the consensus engine
    public RabiaEngine(TopologyManager topologyManager,
                       ClusterNetwork network,
                       StateMachine<C> stateMachine,
                       ProtocolConfig config) {
        this.self = topologyManager.self().id();
        this.topologyManager = topologyManager;
        this.config = config;
        this.networkManager = new RabiaNetworkManager<>(self, network);
        this.stateManager = new RabiaStateManager<>(self, stateMachine, topologyManager);

        // Setup periodic tasks
        SharedScheduler.scheduleAtFixedRate(this::cleanupOldPhases, config.cleanupInterval());
        SharedScheduler.schedule(this::synchronize, config.syncRetryInterval());
        
        // Setup performance monitoring
        SharedScheduler.scheduleAtFixedRate(
            performanceMetrics::logPerformanceSummary, 
            Duration.ofSeconds(30) // Log performance every 30 seconds
        );
    }

    public void configure(MessageRouter.MutableRouter router) {
        // Subscribe to quorum events
        router.addRoute(QuorumStateNotification.class, this::quorumState);

        // Synchronous messages
        router.addRoute(Propose.class, this::processPropose);
        router.addRoute(VoteRound1.class, this::processVoteRound1);
        router.addRoute(VoteRound2.class, this::processVoteRound2);
        router.addRoute(Decision.class, this::processDecision);
        router.addRoute(SyncResponse.class, this::processSyncResponse);

        // Asynchronous messages
        router.addRoute(SyncRequest.class, this::handleSyncRequest);
        router.addRoute(NewBatch.class, this::handleNewBatch);

        // Local command submission requests
        router.addRoute(SubmitCommands.class, this::handleSubmit);
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

        SharedScheduler.schedule(this::synchronize, config.syncRetryInterval().randomize(SCALE));
    }

    private void clusterDisconnected() {
        stateManager.deactivate(pendingBatches);
        consensusManager.clear();
        pendingBatches.clear();
    }

    public <R> Promise<List<R>> apply(List<C> commands) {
        var pendingAnswer = Promise.<List<R>>promise();

        return submitCommands(commands, batch -> correlationMap.put(batch.correlationId(), pendingAnswer))
                .async()
                .flatMap(_ -> pendingAnswer);
    }

    @MessageReceiver
    public void handleSubmit(SubmitCommands<C> submitCommands) {
        submitCommands(submitCommands.commands(), _ -> {});
    }

    private Result<Batch<C>> submitCommands(List<C> commands, Consumer<Batch<C>> onBatchPrepared) {
        if (commands.isEmpty()) {
            return ConsensusErrors.commandBatchIsEmpty().result();
        }

        if (!stateManager.isActive()) {
            return ConsensusErrors.nodeInactive(self).result();
        }

        var batch = batch(commands);

        log.trace("Node {}: client submitted {} command(s). Prepared batch: {}", self, commands.size(), batch);

        pendingBatches.put(batch.correlationId(), batch);
        batchQueue.offer(batch); // Add to priority queue for efficient selection
        onBatchPrepared.accept(batch);
        
        // Record performance metrics
        performanceMetrics.recordBatchSubmitted(batch.correlationId());
        performanceMetrics.updateMemoryUsage(pendingBatches.size(), 0); // Active phase count would need to be tracked
        
        networkManager.broadcastNewBatch(batch);

        if (!stateManager.isInPhase()) {
            consensusExecutor.executeCritical(this::startPhase);
        }

        return Result.success(batch);
    }

    public Promise<Unit> start() {
        return stateManager.getStartPromise();
    }

    public Promise<Unit> stop() {
        return Promise.promise(promise -> {
            clusterDisconnected();
            consensusExecutor.shutdown();
            promise.succeed(Unit.unit());
        });
    }

    @MessageReceiver
    public void processPropose(Propose<C> propose) {
        consensusExecutor.executeFastPath(() -> handlePropose(propose));
    }

    @MessageReceiver
    public void processVoteRound1(VoteRound1 voteRound1) {
        consensusExecutor.executeCritical(() -> handleVoteRound1(voteRound1));
    }

    @MessageReceiver
    public void processVoteRound2(VoteRound2 voteRound2) {
        consensusExecutor.executeCritical(() -> handleVoteRound2(voteRound2));
    }

    @MessageReceiver
    public void processDecision(Decision<C> decision) {
        consensusExecutor.executeCritical(() -> handleDecision(decision));
    }

    @MessageReceiver
    public void processSyncResponse(SyncResponse<C> syncResponse) {
        consensusExecutor.executeDeferred(() -> handleSyncResponse(syncResponse));
    }

    @SuppressWarnings("unchecked")
    @MessageReceiver
    public void handleNewBatch(NewBatch<?> newBatch) {
        var batch = (Batch<C>) newBatch.batch();
        pendingBatches.put(batch.correlationId(), batch);
        batchQueue.offer(batch);
    }

    /// Starts a new phase with pending commands.
    private void startPhase() {
        if (stateManager.isInPhase() || pendingBatches.isEmpty()) {
            return;
        }

        var phase = stateManager.getCurrentPhase();
        // Optimize: use priority queue instead of sorting all batches
        var batch = batchQueue.poll();
        if (batch == null) {
            batch = emptyBatch();
        }

        log.trace("Node {} starting phase {} with batch {}", self, phase, batch.id());

        var phaseData = consensusManager.getOrCreatePhaseData(phase);
        phaseData.proposals.put(self, batch);

        stateManager.setInPhase(true);
        
        // Record phase start for metrics
        performanceMetrics.recordPhaseStarted(phase);

        networkManager.broadcastProposal(phase, batch);
    }

    /// Synchronizes with other nodes to catch up if needed.
    private void synchronize() {
        if (stateManager.isActive()) {
            return;
        }

        stateManager.clearSyncResponses();
        networkManager.requestSynchronization();

        SharedScheduler.schedule(this::synchronize, config.syncRetryInterval().randomize(SCALE));
    }

    /// Handles a synchronization response from another node.
    private void handleSyncResponse(SyncResponse<C> response) {
        if (stateManager.isActive()) {
            log.trace("Node {} ignoring synchronization response {}. Node is active", self, response);
            return;
        }

        stateManager.addSyncResponse(response.sender(), response.state());

        if (!stateManager.hasSufficientSyncResponses()) {
            log.trace("Node {} received responses, not enough to proceed (quorum size = {})",
                      self, topologyManager.quorumSize());
            return;
        }

        log.trace("Node {} received sufficient responses", self);

        var candidate = stateManager.selectBestSyncResponse();
        log.trace("Node {} uses {} as synchronization candidate", self, candidate);
        restoreState(candidate);
    }

    private void restoreState(SavedState<C> state) {
        stateManager.clearSyncResponses();

        if (state.snapshot().length == 0) {
            activate();
            return;
        }
        
        stateManager.restoreState(state, pendingBatches)
                    .onSuccessRun(this::activate)
                    .onFailure(cause -> log.error("Node {} failed to restore state: {}", self, cause));
    }

    /// Activate node and adjust phase, if necessary.
    private void activate() {
        stateManager.activate();
        consensusExecutor.executeCritical(this::startPhase);
    }

    /// Handles a synchronization request from another node.
    @MessageReceiver
    public void handleSyncRequest(SyncRequest request) {
        var state = stateManager.createSyncResponse();
        networkManager.sendSyncResponse(request.sender(), state);
    }

    /// Cleans up old phase data to prevent memory leaks.
    private void cleanupOldPhases() {
        if (!stateManager.isActive()) {
            return;
        }

        consensusManager.cleanupOldPhases(stateManager.getCurrentPhase(), config.removeOlderThanPhases());
    }

    /// Handles a Propose message from another node.
    private void handlePropose(Propose<C> propose) {
        if (!stateManager.isActive()) {
            log.warn("Node {} ignores proposal {}. Node is dormant", self, propose);
            return;
        }

        log.trace("Node {} received proposal from {} for phase {}", self, propose.sender(), propose.phase());

        var currentPhaseValue = stateManager.getCurrentPhase();
        if (propose.phase().equals(currentPhaseValue) && !stateManager.isInPhase()) {
            log.trace("Node {} entering phase {} triggered by proposal from {}",
                      self, propose.phase(), propose.sender());
            stateManager.setInPhase(true);
        }

        var vote = consensusManager.processProposal(self, propose, currentPhaseValue, stateManager.isInPhase());
        if (vote != null) {
            log.trace("Node {} broadcasting R1 vote {} for phase {} based on received proposal from {}",
                      self, vote.stateValue(), propose.phase(), propose.sender());
            networkManager.broadcastVoteRound1(vote);
        }
    }

    /// Handles a round 1 vote from another node.
    private void handleVoteRound1(VoteRound1 vote) {
        if (!stateManager.isActive()) {
            log.warn("Node {} ignores vote1 {}. Node is dormant", self, vote);
            return;
        }

        log.trace("Node {} received round 1 vote from {} for phase {} with value {}",
                  self, vote.sender(), vote.phase(), vote.stateValue());

        var round2Vote = consensusManager.processRound1Vote(self, vote, stateManager.getCurrentPhase(), 
                                                           stateManager.isInPhase(), topologyManager.quorumSize());
        if (round2Vote != null) {
            log.trace("Node {} votes in round 2 {}", self, round2Vote.stateValue());
            networkManager.broadcastVoteRound2(round2Vote);
        }
    }

    /// Handles a round 2 vote from another node.
    private void handleVoteRound2(VoteRound2 vote) {
        if (!stateManager.isActive()) {
            log.warn("Node {} ignores vote2 {}. Node is dormant", self, vote);
            return;
        }

        log.trace("Node {} received round 2 vote from {} for phase {} with value {}",
                  self, vote.sender(), vote.phase(), vote.stateValue());

        var decision = consensusManager.processRound2Vote(self, vote, stateManager.getCurrentPhase(),
                                                         stateManager.isInPhase(), topologyManager.quorumSize(),
                                                         topologyManager.fPlusOne());
        if (decision != null) {
            networkManager.broadcastDecision(decision);
            processDecision(decision);
        }
    }

    private void commitDecision(Decision<C> decision) {
        if (consensusManager.shouldCommitDecision(decision.phase())) {
            if (decision.stateValue() == StateValue.V1 && !decision.value().commands().isEmpty()) {
                commitChanges(decision);
            }
            moveToNextPhase(decision.phase());
        }
    }

    @SuppressWarnings("unchecked")
    private void commitChanges(Decision<C> decision) {
        log.trace("Node {} applies decision {}", self, decision);

        var results = stateManager.commitChanges(decision.value(), decision.phase());
        var correlationId = decision.value().correlationId();
        pendingBatches.remove(correlationId);
        batchQueue.removeIf(batch -> batch.correlationId().equals(correlationId)); // Keep queue in sync
        
        // Record performance metrics
        performanceMetrics.recordBatchCompleted(correlationId, decision.value().commands().size());
        performanceMetrics.recordPhaseCompleted(decision.phase());
        
        correlationMap.computeIfPresent(correlationId, (_, promise) -> {
            promise.succeed(results);
            return null;    // Remove mapping
        });
    }

    /// Handles a decision message from another node.
    private void handleDecision(Decision<C> decision) {
        if (!stateManager.isActive()) {
            log.warn("Node {} ignores decision {}. Node is dormant", self, decision);
            return;
        }

        log.trace("Node {} received decision {}", self, decision);
        commitDecision(decision);
    }

    /// Moves to the next phase after a decision.
    private void moveToNextPhase(Phase currentPhase) {
        stateManager.moveToNextPhase(currentPhase);

        // If we have more commands to process, start a new phase
        if (!pendingBatches.isEmpty()) {
            consensusExecutor.executeCritical(this::startPhase);
        }
    }

}
