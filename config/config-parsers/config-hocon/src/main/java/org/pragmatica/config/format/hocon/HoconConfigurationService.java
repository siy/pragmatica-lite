package org.pragmatica.config.format.hocon;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ConfigFormatReader;
import org.pragmatica.config.api.ParameterKey;
import org.pragmatica.config.api.ParameterType;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.List;

/**
 */
@SuppressWarnings("unused")
@AutoService(ConfigFormatReader.class)
public class HoconConfigurationService implements ConfigFormatReader {
    @Override
    public <T> Result<T> get(ParameterKey key, ParameterType<T> type) {
        return null;
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of("conf", "hocon");
    }

    @Override
    public Result<Unit> read(String content) {
        return null;
    }
}
