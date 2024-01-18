package org.pragmatica.config.api;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.RecordTemplate;

public interface AppConfig {
    default <T extends Record> Result<T> load(String prefix, RecordTemplate<T> template) {
        return template.load(prefix, store());
    }

    ConfigStore store();

    static AppConfig defaultApplicationConfig() {
        return applicationConfigWithStrategy(ConfigStrategy.defaultStrategy());
    }

    static AppConfig applicationConfigWithStrategy(ConfigStrategy strategy) {
        var store = ConfigStore.configStore();

        strategy.configurationSources()
                .forEach(source -> source.load(store));

        return () -> store;
    }
}
