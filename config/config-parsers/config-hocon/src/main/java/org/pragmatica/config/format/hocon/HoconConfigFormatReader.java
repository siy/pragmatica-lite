package org.pragmatica.config.format.hocon;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ConfigFormatReader;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.List;

/**
 */
@SuppressWarnings("unused")
@AutoService(ConfigFormatReader.class)
public class HoconConfigFormatReader implements ConfigFormatReader {
    @Override
    public List<String> supportedExtensions() {
        return List.of("conf", "hocon");
    }

    @Override
    public Result<StringMap> read(Option<String> content) {
        return null;
    }
}
