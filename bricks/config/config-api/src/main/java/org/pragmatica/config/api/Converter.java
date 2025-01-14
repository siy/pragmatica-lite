package org.pragmatica.config.api;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.Map;
import java.util.stream.Collectors;

import static org.pragmatica.config.api.DataConversionError.keyNotFound;

public interface Converter {
    @SuppressWarnings("unchecked")
    static Converter converter() {
        Map<TypeToken<?>, Fn1<Result<?>, String>> map =
            ParameterType.knownParameterTypes()
                         .stream()
                         .collect(Collectors.toMap(ParameterType::token, pt -> pt));

        return () -> map;
    }

    @SuppressWarnings("unchecked")
    default <T> Result<T> convert(TypeToken<T> typeToken, String value) {
        return Option.option(typeMapping().get(typeToken))
                     .toResult(keyNotFound("Type token", typeToken))
                     .flatMap(fn -> fn.apply(value))
                     .map(converted -> (T) converted);
    }

    Map<TypeToken<?>, Fn1<Result<?>, String>> typeMapping();
}
