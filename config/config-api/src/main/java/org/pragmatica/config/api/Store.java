package org.pragmatica.config.api;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.TypeToken;

import java.util.Map;

import static org.pragmatica.config.api.Converter.converter;

public final class Store implements KeyToValue {
    private final Converter converter;
    private StringMap sourceData = Map::of;

    private Store(Converter converter) {
        this.converter = converter;
    }

    public static Store configStore() {
        return new Store(converter());
    }

    public void append(StringMap data) {
        sourceData = sourceData.merge(data);
    }

    @Override
    public <T> Result<T> get(String prefix, String key, TypeToken<T> typeToken) {
        var effectiveKey = prependPrefix(prefix, key);

        if (typeToken.rawType() == Option.class) {
            return getOption(effectiveKey, typeToken);
        }

        return sourceData.get(effectiveKey)
                         .flatMap(value -> converter.convert(typeToken, value));
    }

    @SuppressWarnings("unchecked")
    private <T> Result<T> getOption(String effectiveKey, TypeToken<T> typeToken) {
        if (sourceData.get(effectiveKey).isFailure()) {
            return (Result<T>) Result.success(Option.none());
        }

        return (Result<T>) typeToken.subType(0)
                                    .toResult(DataConversionError.cantRetrieveSubTypeFrom(typeToken))
                                    .flatMap(subType -> get("", effectiveKey, subType))
                                    .map(Option::option);
    }
}
