package org.pragmatica.config.api;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.function.Consumer;

public interface ConfigProvider {
    /**
     * Load the configuration. Note that the returned configuration is a plain instance and would not change over the time. If application expects
     * dynamic configuration changes, it should pass a change listener. It is called once, when configuration changes. The application then should
     * reload the configuration (and provide change listener again to get notified on next change).
     * <p>
     * It should be noted, that not all configuration providers support dynamic configuration, so it is not guaranteed that change listener will be
     * ever invoked.
     * <p>
     * It is assumed
     *
     * @param key            The key to help locate the configuration.
     * @param configRecord      Configuration object type
     * @param changeListener listener to be called when configuration changes
     *
     * @return loaded configuration object instance.
     */
    <T extends Record> Result<T> configuration(SubsystemKey key, Class<T> configRecord, Consumer<SubsystemKey> changeListener);

    default <T extends Record> Result<T> staticConfiguration(SubsystemKey key, Class<T> configRecord) {
        return configuration(key, configRecord, _ -> {});
    }
}
