package org.pragmatica.net.serialization.binary.fury;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.io.FuryInputStream;
import org.pragmatica.serialization.binary.ClassRegistrator;
import org.pragmatica.net.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FuryDeserializer extends Deserializer {
    static FuryDeserializer furyDeserializer(ClassRegistrator registrator) {
        record furyDeserializer(ThreadSafeFury fury) implements FuryDeserializer {
            private static final Logger log = LoggerFactory.getLogger(FuryDeserializer.class);

            @SuppressWarnings("unchecked")
            @Override
            public <T> T read(ByteBuf byteBuf) {
                try (var stream = new FuryInputStream(new ByteBufInputStream(byteBuf))) {
                    return (T) fury().deserialize(stream);
                } catch (Exception e) {
                    log.error("Error deserializing object", e);
                    throw new RuntimeException(e);
                }
            }
        }

        return new furyDeserializer(FuryFactory.fury(registrator));
    }
}
