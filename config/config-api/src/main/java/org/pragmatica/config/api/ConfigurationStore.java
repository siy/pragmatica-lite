package org.pragmatica.config.api;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.TypeToken;

import java.util.Map;

import static org.pragmatica.config.api.DataConversionError.keyNotFound;
import static org.pragmatica.lang.Option.option;

//TODO: finish implementation
public final class ConfigurationStore implements KeyToValue {
    private final Map<String, String> sourceData;
    private final Map<TypeToken<?>, Fn1<Result<?>, String>> mapping;
//    private final Map<TypeToken<?>, RecordDescriptor<?>> mapping;

    private ConfigurationStore(Map<String, String> sourceData,
                               Map<TypeToken<?>, Fn1<Result<?>, String>> mapping) {
        this.sourceData = sourceData;
        this.mapping = mapping;
    }

    public void mergeData(Map<String, String> data) {
        sourceData.putAll(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Result<T> get(String key, TypeToken<T> typeToken) {
        var source = option(sourceData.get(key))
            .toResult(keyNotFound("Key", "key"));
        var tokenResult = option(mapping.get(typeToken))
            .toResult(keyNotFound("Type token", typeToken));

        return (Result<T>) Result.all(source, tokenResult)
                                 .map((value, mapping) -> mapping.apply(value));
    }
}
