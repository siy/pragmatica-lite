package org.pragmatica.config.api.spi;

import org.pragmatica.config.api.AppConfig;
import org.pragmatica.config.api.Store;
import org.pragmatica.config.api.Strategy;

import java.util.ServiceLoader;

/**
 * If you really need to change strategy, you can do this by implementing this interface and providing it via ServiceLoader API.
 * If more than one implementation is found, first one is used. Only if none provided at all, default strategy is used.
 */
public interface AppConfigProvider {
    AppConfig loadAppConfig();

    static AppConfig load() {
        return ServiceLoader.load(AppConfigProvider.class)
                .findFirst()
                .orElseGet(() -> AppConfigProvider::defaultApplicationConfig)
                .loadAppConfig();
    }
    static AppConfig defaultApplicationConfig() {
        return applicationConfigWithStrategy(Strategy.defaultStrategy());
    }

    static AppConfig applicationConfigWithStrategy(Strategy strategy) {
        var store = Store.configStore();

        strategy.configurationSources()
                .forEach(source -> source.load(store));

        return () -> store;
    }
}
