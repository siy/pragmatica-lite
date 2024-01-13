package org.pragmatica.config.provider;

import org.pragmatica.config.api.ConfigDataProvider;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.stream.Collectors;

import static org.pragmatica.lang.Tuple.tuple;

/**
 * Retrieves configuration from environment variables.
 * <p>
 * All environment variables are converted to lower case and then underscores are replaced with dots to form keys compatible with the configuration
 * API.
 */
public interface EnvironmentConfigDataProvider extends ConfigDataProvider {
    EnvironmentConfigDataProvider INSTANCE = new EnvironmentConfigDataProvider() {};

    @Override
    default Result<StringMap> read() {
        var map = System.getenv().entrySet().stream()
                        .map(entry -> tuple(entry.getKey().toLowerCase().replace('_', '.'),
                                            entry.getValue()))
                        .collect(Collectors.toMap(Tuple2::first, Tuple2::last));

        return Result.success(() -> map);
    }
}
