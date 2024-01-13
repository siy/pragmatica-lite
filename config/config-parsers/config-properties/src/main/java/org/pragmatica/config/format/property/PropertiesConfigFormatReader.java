package org.pragmatica.config.format.property;


import com.google.auto.service.AutoService;
import org.pragmatica.config.api.ConfigFormatReader;
import org.pragmatica.config.api.ConfigError;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Result.Cause;

import java.util.List;
import java.util.stream.Collectors;

import  org.pragmatica.lang.Tuple.Tuple2;
import static org.pragmatica.lang.Tuple.tuple;

@SuppressWarnings("unused")
@AutoService(ConfigFormatReader.class)
public class PropertiesConfigFormatReader implements ConfigFormatReader {
    private static final Cause INPUT_IS_MISSING = new ConfigError.InputIsMissing("Input is missing");

    @Override
    public Result<StringMap> read(Option<String> content) {
        return content.toResult(INPUT_IS_MISSING)
                      .flatMap(this::parse);
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of("properties");
    }

    private Result<StringMap> parse(String source) {
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
}
