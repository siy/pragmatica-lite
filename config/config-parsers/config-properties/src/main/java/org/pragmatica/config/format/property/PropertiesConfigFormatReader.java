package org.pragmatica.config.format.property;


import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ConfigFormatReader;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.List;
import java.util.stream.Collectors;

import static org.pragmatica.lang.Tuple.tuple;

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
                             .map(parts -> tuple(parts[0].strip(), parts[1].strip()))
                             .collect(Collectors.toMap(Tuple2::first, Tuple2::last));

        return Result.success(() -> valueMap);
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of("properties");
    }
}
