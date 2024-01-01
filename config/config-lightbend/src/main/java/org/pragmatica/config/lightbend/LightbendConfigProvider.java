package org.pragmatica.config.lightbend;

import com.google.auto.service.AutoService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.pragmatica.config.api.ConfigProvider;
import org.pragmatica.config.api.SubsystemKey;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.util.function.Consumer;

/**
 * This implementation uses default configuration of Lightbend Config library.
 * <p>
 * See <a href="https://github.com/lightbend/config?tab=readme-ov-file#using-the-library">Lightbend Config</a> for details on where configuration is
 * loaded from.
 */
@SuppressWarnings("unused")
@AutoService(ConfigProvider.class)
public class LightbendConfigProvider implements ConfigProvider {
    @Override
    public <T extends Record> Result<T> configuration(SubsystemKey key, Class<T> configRecord, Consumer<SubsystemKey> changeListener) {
        return readInstance(ConfigHolder.INSTANCE.config(), key, configRecord);
    }

    private <T extends Record> Result<T> readInstance(Config config, SubsystemKey key, Class<T> configRecord) {
        return Result.lift(Causes::fromThrowable, () -> {
            var obj = config.getObject(key.configPrefix());

//            var beanConfig = config.getConfig(key.configPrefix());
//            return ConfigBeanFactory.create(beanConfig, typeToken.token());
            return null;
        });
    }

    private enum ConfigHolder {
        INSTANCE;

        private final Config config;

        ConfigHolder() {
            config = ConfigFactory.load();
        }

        public Config config() {
            return config;
        }
    }
}
