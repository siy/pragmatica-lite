package org.pragmatica.net.serialization.binary.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import org.pragmatica.serialization.binary.ClassRegistrator;

public sealed interface KryoPoolFactory {

    static Pool<Kryo> kryoPool(ClassRegistrator registrator) {
        return new Pool<>(true, false, Runtime.getRuntime().availableProcessors() * 2) {
            @Override
            protected Kryo create() {
                var kryo = new Kryo();

                registrator.registerClasses(kryo::register);

                return kryo;
            }
        };
    }

    @SuppressWarnings("unused")
    record unused() implements KryoPoolFactory {}
}
