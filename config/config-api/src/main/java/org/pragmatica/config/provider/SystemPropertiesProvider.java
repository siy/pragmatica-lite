package org.pragmatica.config.provider;

import org.pragmatica.config.api.DataProvider;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.stream.Collectors;

import static org.pragmatica.lang.Tuple.tuple;

public class SystemPropertiesProvider implements DataProvider {
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
