package org.pragmatica.config.format.yaml;

import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ConfigFormatReader;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;

import java.util.List;

@SuppressWarnings("unused")
@AutoService(ConfigFormatReader.class)
public class YamlConfigFormatReader implements ConfigFormatReader {
    @Override
    public Result<StringMap> read(String content) {
        return YamlFileReader.readString(content);
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of("yaml", "yml");
    }
}
