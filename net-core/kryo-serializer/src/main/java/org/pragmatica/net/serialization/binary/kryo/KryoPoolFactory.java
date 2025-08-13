package org.pragmatica.net.serialization.binary.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import org.pragmatica.net.serialization.binary.ClassRegistrator;

import java.util.stream.Stream;

public sealed interface KryoPoolFactory {

    static Pool<Kryo> kryoPool(ClassRegistrator... registrators) {
        return new Pool<>(true, false, Runtime.getRuntime().availableProcessors() * 2) {
            @Override
            protected Kryo create() {
                var kryo = new Kryo();

                Stream.of(registrators)
                      .forEach(registrator -> registrator.registerClasses(kryo::register));

                return kryo;
            }
        };
    }

    @SuppressWarnings("unused")
    record unused() implements KryoPoolFactory {}
}
