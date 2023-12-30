package org.pragmatica.codec.json;

import org.pragmatica.http.codec.JsonCodec;
import org.pragmatica.http.codec.JsonCodecConfiguration;
import org.pragmatica.lang.Option;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public interface JsonCodecFactory<C extends JsonCodecConfiguration> {
    <T extends JsonCodec> T forConfiguration(C config);
    <T extends JsonCodec> T withDefaultConfiguration();
    Class<C> configClass();

    static JsonCodecFactory<?> defaultFactory() {
        var map = JsonCodecFactoriesHolder.INSTANCE.factories;

        if (map.size() != 1) {
            throw new IllegalStateException(
                "Default serializer is requested, but none or more than one configured.\nMake sure that only one implementation is accessible via classpath.");
        }

        return map.values().iterator().next();
    }

    @SuppressWarnings("unchecked")
    static <T extends JsonCodecConfiguration> Option<JsonCodecFactory<T>> configuredBy(Class<T> key) {
        return Option.option((JsonCodecFactory<T>) JsonCodecFactoriesHolder.INSTANCE.factories.get(key));
    }

    enum JsonCodecFactoriesHolder {
        INSTANCE;
        private final Map<Class<? extends JsonCodecConfiguration>, JsonCodecFactory<?>> factories;

        @SuppressWarnings("unchecked")
        JsonCodecFactoriesHolder() {
            factories = ServiceLoader.load(JsonCodecFactory.class)
                                     .stream()
                                     .map(ServiceLoader.Provider::get)
                                     .collect(Collectors.toMap(JsonCodecFactory::configClass, factory -> factory));

        }
    }
}
