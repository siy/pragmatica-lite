package org.pragmatica.codec.json;

import io.netty.buffer.ByteBuf;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Serializer {
    Result<ByteBuf> serialize(Object success);

    <T> Result<T> deserialize(ByteBuf entity, TypeToken<T> token);

    Class<? extends SerializerConfiguration> configClass();

    static Serializer defaultSerializer() {
        var map = SerializerHolder.INSTANCE.serializers;

        if (map.size() != 1) {
            throw new IllegalStateException(
                "Default serializer is requested, but none or more than one configured.\nMake sure that only one implementation is accessible via classpath.");
        }

        return map.values().iterator().next();
    }

    static Option<Serializer> configuredBy(Class<? extends SerializerConfiguration> key) {
        return Option.option(SerializerHolder.INSTANCE.serializers.get(key));
    }

    enum SerializerHolder {
        INSTANCE;
        private final Map<Class<? extends SerializerConfiguration>, Serializer> serializers;

        SerializerHolder() {
            serializers = ServiceLoader.load(Serializer.class)
                                       .stream()
                                       .map(ServiceLoader.Provider::get)
                                       .collect(Collectors.toMap(Serializer::configClass, Function.identity()));

        }
    }

}
