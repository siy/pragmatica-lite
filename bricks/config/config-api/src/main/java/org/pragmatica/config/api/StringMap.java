package org.pragmatica.config.api;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * Functional wrapper for map of strings.
 */
public interface StringMap {
    Map<String, String> rawMap();

    default Result<String> get(String key) {
        return Option.option(rawMap().get(key))
                     .toResult(DataConversionError.keyNotFound("Key", key));
    }

    default StringMap merge(StringMap other) {
        return merge(other.rawMap());
    }

    default StringMap merge(Map<String, String> other) {
        var merged = new HashMap<>(rawMap());
        merged.putAll(other);

        return () -> merged;
    }
}
