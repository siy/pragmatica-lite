package org.pragmatica.cluster.consensus.rabia.infrastructure;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.StateMachineNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public record StateChangePrinter(NodeId id) implements Consumer<StateMachineNotification> {
    private static final Logger logger = LoggerFactory.getLogger(StateChangePrinter.class);

    @Override
    public void accept(StateMachineNotification stateMachineNotification) {
        logger.trace("Node {} received state change: {}", id, stateMachineNotification);
    }
}
