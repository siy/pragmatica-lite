package org.pragmatica.config.provider;

import org.pragmatica.config.api.ConfigDataProvider;
import org.pragmatica.config.api.StringMap;

import java.util.stream.Collectors;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import static org.pragmatica.lang.Tuple.tuple;

public class SystemPropertiesProvider implements ConfigDataProvider {
    public static final SystemPropertiesProvider INSTANCE = new SystemPropertiesProvider();

    @Override
    public Result<StringMap> read() {
        var map = System.getProperties()
                        .entrySet()
                        .stream()
                        .map(entry -> tuple(entry.getKey().toString(),
                                            entry.getValue().toString()))
                        .collect(Collectors.toMap(Tuple2::first, Tuple2::last));

        return Result.success(() -> map);
    }
}
