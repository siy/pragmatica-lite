package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.net.ClusterNetwork;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Asynchronous.NewBatch;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Synchronous.*;
import org.pragmatica.cluster.consensus.rabia.RabiaPersistence.SavedState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.Asynchronous.SyncRequest;
import static org.pragmatica.cluster.consensus.rabia.RabiaPersistence.SavedState.savedState;

public class RabiaNetworkManager<C extends Command> {
    private static final Logger log = LoggerFactory.getLogger(RabiaNetworkManager.class);
    
    private final NodeId self;
    private final ClusterNetwork network;
    
    public RabiaNetworkManager(NodeId self, ClusterNetwork network) {
        this.self = self;
        this.network = network;
    }
    
    public void broadcastNewBatch(Batch<C> batch) {
        log.trace("Node {} broadcasting new batch: {}", self, batch.id());
        network.broadcast(new NewBatch<>(self, batch));
    }
    
    public void broadcastProposal(Phase phase, Batch<C> batch) {
        log.trace("Node {} broadcasting proposal for phase {} with batch {}", self, phase, batch.id());
        network.broadcast(new Propose<>(self, phase, batch));
    }
    
    public void broadcastVoteRound1(VoteRound1 vote) {
        log.trace("Node {} broadcasting R1 vote {} for phase {}", self, vote.stateValue(), vote.phase());
        network.broadcast(vote);
    }
    
    public void broadcastVoteRound2(VoteRound2 vote) {
        log.trace("Node {} broadcasting R2 vote {} for phase {}", self, vote.stateValue(), vote.phase());
        network.broadcast(vote);
    }
    
    public void broadcastDecision(Decision<C> decision) {
        log.trace("Node {} broadcasting decision {} for phase {}", self, decision.stateValue(), decision.phase());
        network.broadcast(decision);
    }
    
    public void requestSynchronization() {
        var request = new SyncRequest(self);
        log.trace("Node {} requesting phase synchronization", self);
        network.broadcast(request);
    }
    
    public void sendSyncResponse(NodeId target, SavedState<C> state) {
        var response = new SyncResponse<>(self, state);
        log.trace("Node {} sending sync response to {}", self, target);
        network.send(target, response);
    }
}