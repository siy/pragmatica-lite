package org.pragmatica.config.api;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.TypeToken;

import java.util.Map;

public final class ConfigurationStore implements KeyToValue {
    private final Converter converter;
    private StringMap sourceData = Map::of;

    private ConfigurationStore(Converter converter) {
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
    public <T> Result<T> get(String key, TypeToken<T> typeToken) {
        if (typeToken.rawType() == Option.class) {
            if (sourceData.get(key).isFailure()) {
                return (Result<T>) Result.success(Option.none());
            } else {
                return (Result<T>) typeToken.subType(0)
                                            .toResult(DataConversionError.cantRetrieveSubTypeFrom(typeToken))
                                            .flatMap(subType -> get(key, subType))
                                            .map(Option::option);
            }
        }

        return sourceData.get(key)
                         .flatMap(value -> converter.convert(typeToken, value));
    }
}
