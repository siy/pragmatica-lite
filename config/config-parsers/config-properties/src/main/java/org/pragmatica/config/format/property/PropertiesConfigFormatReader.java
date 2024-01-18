package org.pragmatica.config.format.property;


import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ConfigFormatReader;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;

import java.util.List;
import java.util.stream.Collectors;

//TODO: tests
@SuppressWarnings("unused")
@AutoService(ConfigFormatReader.class)
public class PropertiesConfigFormatReader implements ConfigFormatReader {
    @Override
    public Result<StringMap> read(String source) {
        var valueMap = source.lines()
                             .map(String::strip)
                             .filter(line -> !line.isBlank())
                             .filter(line -> !line.startsWith("#"))
                             .map(line -> line.split("=", 2))
                             .filter(parts -> parts.length == 2)
                             .collect(Collectors.toMap(parts -> parts[0].strip(), parts -> parts[1].strip()));

        return Result.success(() -> valueMap);
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of("properties");
    }
}
