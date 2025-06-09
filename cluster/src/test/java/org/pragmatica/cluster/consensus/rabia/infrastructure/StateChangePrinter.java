package org.pragmatica.cluster.consensus.rabia.infrastructure;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.StateMachineNotification;
import org.pragmatica.message.MessageReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
public record StateChangePrinter(NodeId id) implements Consumer<StateMachineNotification> {
    private static final Logger logger = LoggerFactory.getLogger(StateChangePrinter.class);

    @MessageReceiver
    @Override
    public void accept(StateMachineNotification stateMachineNotification) {
        logger.trace("Node {} received state change: {}", id, stateMachineNotification);
    }
}
