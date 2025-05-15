package org.pragmatica.net.serialization.binary.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.util.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.pragmatica.serialization.binary.ClassRegistrator;
import org.pragmatica.net.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface KryoDeserializer extends Deserializer {
    static KryoDeserializer kryoDeserializer(ClassRegistrator registrator) {
        record kryoDeserializer(Pool<Kryo> pool) implements KryoDeserializer {
            private static final Logger log = LoggerFactory.getLogger(KryoDeserializer.class);

            @SuppressWarnings("unchecked")
            @Override
            public <T> T read(ByteBuf byteBuf) {
                var kryo = pool.obtain();

                try (var byteBufInputStream = new ByteBufInputStream(byteBuf);
                     var input = new Input(byteBufInputStream)) {
                    return (T) kryo.readClassAndObject(input);
                } catch (Exception e) {
                    log.error("Error deserializing object", e);
                    throw new RuntimeException(e);
                } finally {
                    pool().free(kryo);
                }
            }
        }

        return new kryoDeserializer(KryoPoolFactory.kryoPool(registrator));
    }
}
