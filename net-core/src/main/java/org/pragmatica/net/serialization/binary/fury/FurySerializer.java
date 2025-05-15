package org.pragmatica.net.serialization.binary.fury;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.apache.fury.ThreadSafeFury;
import org.pragmatica.serialization.binary.ClassRegistrator;
import org.pragmatica.net.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FurySerializer extends Serializer {
    static FurySerializer furySerializer(ClassRegistrator registrator) {
        record furySerializer(ThreadSafeFury fury) implements FurySerializer {
            private static final Logger log = LoggerFactory.getLogger(FurySerializer.class);

            @Override
            public <T> void write(ByteBuf byteBuf, T object) {
                try (var outputStream = new ByteBufOutputStream(byteBuf)) {
                    fury().serialize(outputStream, object);
                } catch (Exception e) {
                    log.error("Error serializing object", e);
                    throw new RuntimeException(e);
                }
            }
        }

        return new furySerializer(FuryFactory.fury(registrator));
    }
}
