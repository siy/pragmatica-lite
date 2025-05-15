package org.pragmatica.net.serialization.binary.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import org.pragmatica.serialization.binary.ClassRegistrator;
import org.pragmatica.net.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface KryoSerializer extends Serializer {
    static KryoSerializer kryoSerializer(ClassRegistrator registrator) {
        record kryoSerializer(Pool<Kryo> pool) implements KryoSerializer {
            private static final Logger log = LoggerFactory.getLogger(KryoSerializer.class);

            @Override
            public <T> void write(ByteBuf byteBuf, T object) {
                var kryo = pool.obtain();

                try (var byteBufOutputStream = new ByteBufOutputStream(byteBuf);
                     var output = new Output(byteBufOutputStream)) {
                    kryo.writeClassAndObject(output, object);
                } catch (Exception e) {
                    log.error("Error serializing object", e);
                    throw new RuntimeException(e);
                } finally {
                    pool.free(kryo);
                }
            }
        }

        return new kryoSerializer(KryoPoolFactory.kryoPool(registrator));
    }
}
