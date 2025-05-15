package org.pragmatica.cluster.consensus;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.lang.Cause;

public sealed interface ConsensusErrors extends Cause {
    record NodeInactive(String message) implements ConsensusErrors {}

    record CommandBatchIsEmpty(String message) implements ConsensusErrors {}

    static NodeInactive nodeInactive(NodeId nodeId) {
        return new NodeInactive("Node [" + nodeId.id() + "] is inactive");
    }

    static CommandBatchIsEmpty commandBatchIsEmpty() {
        return new CommandBatchIsEmpty("Submitted command batch is empty");
    }
}
