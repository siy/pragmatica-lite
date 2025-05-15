package org.pragmatica.serialization.binary;

import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.Batch;
import org.pragmatica.cluster.consensus.rabia.Phase;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.node.rabia.CustomClasses;
import org.pragmatica.cluster.state.kvstore.KVCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.net.serialization.binary.fury.FuryDeserializer.furyDeserializer;
import static org.pragmatica.net.serialization.binary.fury.FurySerializer.furySerializer;

class RabiaFurySerializerTest {
    @Test
    void testRoundTripForProtocolMessage() {
        var serializer = furySerializer(CustomClasses::configure);
        var deserializer = furyDeserializer(CustomClasses::configure);

        var commands = List.of(new KVCommand.Put<>("k1", "v1"),
                               new KVCommand.Get<>("k2"),
                               new KVCommand.Remove<>("k3"));
        var message = new RabiaProtocolMessage.Synchronous.Propose<>(NodeId.randomNodeId(),
                                                                     new Phase(123L),
                                                                     Batch.batch(commands));

        var serialized = serializer.encode(message);
        var deserialized = deserializer.decode(serialized);

        assertEquals(message, deserialized);
    }

}