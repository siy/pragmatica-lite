package org.pragmatica.config.api;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.TypeToken;

import java.util.Map;

public final class ConfigStore implements KeyToValue {
    private final Converter converter;
    private StringMap sourceData = Map::of;

    private ConfigStore(Converter converter) {
        this.converter = converter;
    }

    public void append(Map<String, String> data) {
        sourceData = sourceData.merge(data);
    }

    public void append(StringMap data) {
        sourceData = sourceData.merge(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Result<T> get(String prefix, String key, TypeToken<T> typeToken) {
        var effectiveKey = prependPrefix(prefix, key);

        if (typeToken.rawType() == Option.class) {
            if (sourceData.get(effectiveKey).isFailure()) {
                return (Result<T>) Result.success(Option.none());
            } else {
                return (Result<T>) typeToken.subType(0)
                                            .toResult(DataConversionError.cantRetrieveSubTypeFrom(typeToken))
                                            .flatMap(subType -> get(prefix, key, subType))
                                            .map(Option::option);
            }
        }

        return sourceData.get(effectiveKey)
                         .flatMap(value -> converter.convert(typeToken, value));
    }
}
