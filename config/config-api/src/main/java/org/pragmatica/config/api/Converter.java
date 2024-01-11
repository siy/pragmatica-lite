package org.pragmatica.config.api;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.pragmatica.config.api.DataConversionError.keyNotFound;

public final class Converter {
    private final Map<TypeToken<?>, Fn1<Result<?>, String>> mapping;

    private Converter(Map<TypeToken<?>, Fn1<Result<?>, String>> mapping) {
        this.mapping = mapping;
    }

    public static Converter converter() {
        return new Converter(new HashMap<>(PREDEFINED_CONVERTERS));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Converter with(TypeToken<T> typeToken, Fn1<Result<T>, String> converter) {
        var newMapping = new HashMap<>(mapping);
        newMapping.put(typeToken, (Fn1<Result<?>, String>) (Fn1) converter);

        return new Converter(newMapping);
    }

    @SuppressWarnings("unchecked")
    public <T> Result<T> convert(TypeToken<T> typeToken, String value) {
        return Option.option(mapping.get(typeToken))
                     .toResult(keyNotFound("Type token", typeToken))
                     .flatMap(fn -> fn.apply(value))
                     .map(converted -> (T) converted);
    }

    private static final Map<TypeToken<?>, Fn1<Result<?>, String>> PREDEFINED_CONVERTERS;

    static {
        PREDEFINED_CONVERTERS = ParameterType.BuiltInTypes.LIST
            .stream()
            .collect(Collectors.toMap(ParameterType::token, pt -> pt::apply));
    }
}
