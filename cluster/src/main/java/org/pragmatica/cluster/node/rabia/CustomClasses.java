package org.pragmatica.cluster.node.rabia;

import org.pragmatica.cluster.consensus.rabia.*;
import org.pragmatica.cluster.net.NetworkMessage;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.kvstore.KVCommand;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static org.pragmatica.utility.HierarchyScanner.concreteSubtypes;

public interface CustomClasses {

    static void configure(Consumer<Class<?>> consumer) {
        concreteSubtypes(RabiaProtocolMessage.class)
                        .forEach(consumer);
        concreteSubtypes(NetworkMessage.class)
                        .forEach(consumer);
        concreteSubtypes(KVCommand.class)
                        .forEach(consumer);

        consumer.accept(HashMap.class);
        consumer.accept(RabiaPersistence.SavedState.empty().getClass());
        consumer.accept(NodeId.class);
        consumer.accept(BatchId.class);
        consumer.accept(CorrelationId.class);
        consumer.accept(Phase.class);
        consumer.accept(Batch.class);
        consumer.accept(StateValue.class);
        consumer.accept(byte[].class);
        consumer.accept(List.of().getClass());
        consumer.accept(List.of(1).getClass());
        consumer.accept(List.of(1, 2, 3).getClass());
    }
}
