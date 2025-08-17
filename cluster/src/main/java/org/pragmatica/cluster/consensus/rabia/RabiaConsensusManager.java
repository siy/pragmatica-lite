package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Synchronous.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.pragmatica.cluster.consensus.rabia.Batch.emptyBatch;

public class RabiaConsensusManager<C extends Command> {
    private static final Logger log = LoggerFactory.getLogger(RabiaConsensusManager.class);
    
    private final Map<Phase, PhaseData<C>> phases = new ConcurrentHashMap<>();
    
    public PhaseData<C> getOrCreatePhaseData(Phase phase) {
        return phases.computeIfAbsent(phase, PhaseData::new);
    }
    
    public void cleanupOldPhases(Phase currentPhase, int removeOlderThanPhases) {
        phases.keySet().removeIf(phase -> isExpiredPhase(phase, currentPhase, removeOlderThanPhases));
    }
    
    private boolean isExpiredPhase(Phase phase, Phase current, int removeOlderThanPhases) {
        return phase.compareTo(current) < 0
                && current.value() - phase.value() > removeOlderThanPhases;
    }
    
    public VoteRound1 processProposal(NodeId self, Propose<C> propose, Phase currentPhase, boolean isInPhase) {
        if (propose.phase().compareTo(currentPhase) < 0) {
            log.trace("Node {} ignoring proposal for past phase {}", self, propose.phase());
            return null;
        }
        
        var phaseData = getOrCreatePhaseData(propose.phase());
        phaseData.proposals.putIfAbsent(propose.sender(), propose.value());
        
        if (isInPhase 
                && currentPhase.equals(propose.phase())
                && !phaseData.round1Votes.containsKey(self)) {
            
            var vote = phaseData.evaluateInitialVote(self, propose);
            phaseData.round1Votes.put(self, vote.stateValue());
            return vote;
        }
        
        return null;
    }
    
    public VoteRound2 processRound1Vote(NodeId self, VoteRound1 vote, Phase currentPhase, boolean isInPhase, int quorumSize) {
        var phaseData = getOrCreatePhaseData(vote.phase());
        phaseData.round1Votes.put(vote.sender(), vote.stateValue());
        
        if (isInPhase
                && currentPhase.equals(vote.phase())
                && !phaseData.round2Votes.containsKey(self)
                && phaseData.hasRound1MajorityVotes(quorumSize)) {
            
            var round2Vote = phaseData.evaluateRound2Vote(quorumSize);
            phaseData.round2Votes.put(self, round2Vote);
            return new VoteRound2(self, vote.phase(), round2Vote);
        }
        
        return null;
    }
    
    public Decision<C> processRound2Vote(NodeId self, VoteRound2 vote, Phase currentPhase, boolean isInPhase, int quorumSize, int fPlusOne) {
        var phaseData = getOrCreatePhaseData(vote.phase());
        phaseData.round2Votes.put(vote.sender(), vote.stateValue());
        
        if (isInPhase
                && currentPhase.equals(vote.phase())
                && !phaseData.hasDecided.get()
                && phaseData.hasRound2MajorityVotes(quorumSize)) {
            
            return phaseData.processRound2Completion(self, fPlusOne);
        }
        
        return null;
    }
    
    public boolean shouldCommitDecision(Phase phase) {
        var phaseData = phases.get(phase);
        return phaseData != null && phaseData.hasDecided.compareAndSet(false, true);
    }
    
    public void clear() {
        phases.clear();
    }
    
    public static class PhaseData<C extends Command> {
        final Phase phase;
        final Map<NodeId, Batch<C>> proposals = new ConcurrentHashMap<>();
        final Map<NodeId, StateValue> round1Votes = new ConcurrentHashMap<>();
        final Map<NodeId, StateValue> round2Votes = new ConcurrentHashMap<>();
        final AtomicBoolean hasDecided = new AtomicBoolean(false);

        PhaseData(Phase phase) {
            this.phase = phase;
        }

        boolean hasRound1MajorityVotes(int quorumSize) {
            return round1Votes.size() >= quorumSize;
        }

        boolean hasRound2MajorityVotes(int quorumSize) {
            return round2Votes.size() >= quorumSize;
        }

        public Batch<C> findAgreedProposal(NodeId self) {
            long distinctProposals = proposals.values()
                                              .stream()
                                              .map(Batch::correlationId)
                                              .distinct()
                                              .count();
            if (distinctProposals == 1) {
                return proposals.values().iterator().next();
            }

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
            for (var value : List.of(StateValue.V0, StateValue.V1)) {
                if (countRound1VotesForValue(value) >= quorumSize) {
                    return value;
                }
            }
            return StateValue.VQUESTION;
        }

        public int countRound1VotesForValue(StateValue value) {
            return (int) round1Votes.values()
                                    .stream()
                                    .filter(v -> v == value)
                                    .count();
        }

        public int countRound2VotesForValue(StateValue value) {
            return (int) round2Votes.values()
                                    .stream()
                                    .filter(v -> v == value)
                                    .count();
        }

        public Decision<C> processRound2Completion(NodeId self, int fPlusOneSize) {
            if (countRound2VotesForValue(StateValue.V1) >= fPlusOneSize) {
                return new Decision<>(self, phase, StateValue.V1, findAgreedProposal(self));
            }

            if (countRound2VotesForValue(StateValue.V0) >= fPlusOneSize) {
                return new Decision<>(self, phase, StateValue.V0, emptyBatch());
            }

            var decision = coinFlip(self);
            var batch = decision == StateValue.V1 ? findAgreedProposal(self) : Batch.<C>emptyBatch();
            return new Decision<>(self, phase, decision, batch);
        }

        private StateValue coinFlip(NodeId self) {
            long seed = phase.value() ^ self.id().hashCode();
            return (Math.abs(seed) % 2 == 0) ? StateValue.V0 : StateValue.V1;
        }
    }
}