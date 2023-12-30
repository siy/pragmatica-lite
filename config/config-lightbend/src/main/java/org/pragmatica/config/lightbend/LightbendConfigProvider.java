package org.pragmatica.config.lightbend;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ConfigProvider;
import org.pragmatica.config.api.SubsystemKey;
import org.pragmatica.lang.type.TypeToken;

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
    public <T> T configuration(SubsystemKey key, TypeToken<T> typeToken, Consumer<SubsystemKey> changeListener) {
        return null;
    }
}
