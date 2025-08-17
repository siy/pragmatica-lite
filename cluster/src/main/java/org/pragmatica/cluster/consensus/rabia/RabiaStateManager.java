package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.cluster.state.StateMachine;
import org.pragmatica.cluster.consensus.rabia.RabiaPersistence.SavedState;
import org.pragmatica.cluster.topology.TopologyManager;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RabiaStateManager<C extends Command> {
    private static final Logger log = LoggerFactory.getLogger(RabiaStateManager.class);
    
    private final NodeId self;
    private final StateMachine<C> stateMachine;
    private final TopologyManager topologyManager;
    private final RabiaPersistence<C> persistence = RabiaPersistence.inMemory();
    
    private final AtomicReference<Phase> currentPhase = new AtomicReference<>(Phase.ZERO);
    private final AtomicReference<Phase> lastCommittedPhase = new AtomicReference<>(Phase.ZERO);
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean isInPhase = new AtomicBoolean(false);
    private final AtomicReference<Promise<Unit>> startPromise = new AtomicReference<>(Promise.promise());
    
    private final Map<NodeId, SavedState<C>> syncResponses = new ConcurrentHashMap<>();
    
    public RabiaStateManager(NodeId self, StateMachine<C> stateMachine, TopologyManager topologyManager) {
        this.self = self;
        this.stateMachine = stateMachine;
        this.topologyManager = topologyManager;
    }
    
    public Phase getCurrentPhase() {
        return currentPhase.get();
    }
    
    public Phase getLastCommittedPhase() {
        return lastCommittedPhase.get();
    }
    
    public boolean isActive() {
        return active.get();
    }
    
    public boolean isInPhase() {
        return isInPhase.get();
    }
    
    public void setInPhase(boolean inPhase) {
        isInPhase.set(inPhase);
    }
    
    public Promise<Unit> getStartPromise() {
        return startPromise.get();
    }
    
    public void moveToNextPhase(Phase currentPhase) {
        var nextPhase = currentPhase.successor();
        this.currentPhase.set(nextPhase);
        isInPhase.set(false);
        log.trace("Node {} moving to phase {}", self, nextPhase);
    }
    
    public void activate() {
        active.set(true);
        startPromise.get().succeed(Unit.unit());
        syncResponses.clear();
        log.info("Node {} activated in phase {}", self, currentPhase.get());
    }
    
    public void deactivate(BoundedLRUMap<CorrelationId, Batch<C>> pendingBatches) {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        
        persistence.save(stateMachine, lastCommittedPhase.get(), pendingBatches.values())
                   .onSuccessRun(() -> log.info("Node {} disconnected. State persisted", self))
                   .onFailure(cause -> log.error("Node {} failed to persist state: {}", self, cause));
        
        reset();
    }
    
    public void reset() {
        currentPhase.set(Phase.ZERO);
        isInPhase.set(false);
        stateMachine.reset();
        startPromise.set(Promise.promise());
        syncResponses.clear();
    }
    
    public void addSyncResponse(NodeId nodeId, SavedState<C> state) {
        syncResponses.put(nodeId, state);
    }
    
    public boolean hasSufficientSyncResponses() {
        return syncResponses.size() >= topologyManager.quorumSize();
    }
    
    public SavedState<C> selectBestSyncResponse() {
        return syncResponses.values()
                           .stream()
                           .sorted(Comparator.comparing(SavedState::lastCommittedPhase))
                           .toList()
                           .getLast();
    }
    
    public void clearSyncResponses() {
        syncResponses.clear();
    }
    
    public Promise<Unit> restoreState(SavedState<C> state, BoundedLRUMap<CorrelationId, Batch<C>> pendingBatches) {
        if (state.snapshot().length == 0) {
            return Promise.unitPromise();
        }
        
        return Promise.resolved(stateMachine.restoreSnapshot(state.snapshot()))
                        .map(_ -> {
                              currentPhase.set(state.lastCommittedPhase());
                              lastCommittedPhase.set(state.lastCommittedPhase());
                              state.pendingBatches().forEach(batch -> 
                                  pendingBatches.put(batch.correlationId(), batch));
                              persistence.save(stateMachine, currentPhase.get(), pendingBatches.values());
                              
                              log.info("Node {} restored state from persistence. Current phase {}", 
                                      self, currentPhase.get());
                              return Unit.unit();
                          });
    }
    
    @SuppressWarnings("unchecked")
    public <R> List<R> commitChanges(Batch<C> batch, Phase phase) {
        log.trace("Node {} applying batch {}", self, batch);
        var results = stateMachine.process(batch.commands());
        lastCommittedPhase.set(phase);
        return (List<R>) results;
    }
    
    @SuppressWarnings("unchecked")
    public SavedState<C> createSyncResponse() {
        if (active.get()) {
            return stateMachine.makeSnapshot().fold(
                cause -> {
                    log.error("Node {} failed to create snapshot: {}", self, cause);
                    return persistence.load().or((SavedState<C>) SavedState.empty());
                },
                snapshot -> SavedState.<C>savedState(snapshot, lastCommittedPhase.get(), List.of())
            );
        } else {
            return persistence.load().or((SavedState<C>) SavedState.empty());
        }
    }
}