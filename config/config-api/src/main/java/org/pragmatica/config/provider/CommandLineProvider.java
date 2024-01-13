package org.pragmatica.config.provider;

import org.pragmatica.config.api.ConfigDataProvider;
import org.pragmatica.config.api.ConfigError;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

public interface CommandLineProvider extends ConfigDataProvider {
    String[] arguments();

    @Override
    default Result<StringMap> read() {
        return Result.allOf(processCommandLine())
                     .map(CommandLineProvider::tupleListAsMap)
                     .map(map -> () -> map);
    }

    private static Map<String, String> tupleListAsMap(List<Tuple2<String, String>> list) {
        return list.stream()
                   .collect(Collectors.toMap(Tuple2::first, Tuple2::last));
    }

    private Stream<Result<Tuple2<String, String>>> processCommandLine() {
        return Stream.of(arguments())
                     .filter(arg -> arg.startsWith("--"))
                     .map(arg -> arg.split("="))
                     .map(CommandLineProvider::buildTuple);
    }

    private static Result<Tuple2<String, String>> buildTuple(String[] argument) {
        if (argument.length == 2) {
            return Result.success(tuple(argument[0].substring(2).trim(), argument[1].trim()));
        }
        return Result.failure(ConfigError.invalidParameter(argument));
    }
}
