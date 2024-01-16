package org.pragmatica.config.api;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.pragmatica.config.api.DataConversionError.keyNotFound;
import static org.pragmatica.lang.Tuple.tuple;

import org.pragmatica.lang.Tuple.Tuple2;

public interface Converter {
    static Converter converter() {
        Stream<Tuple2<TypeToken<?>, Fn1<Result<?>, String>>> builtinConverters = ParameterType.BuiltInTypes.LIST
            .stream().map(pt -> tuple(pt.token(), pt::apply));
        var customConverters = RecordTemplate.templates().flatMap(RecordTemplate::customConverters);
        var map = Stream.concat(builtinConverters, customConverters)
                        .collect(Collectors.toMap(Tuple2::first, Tuple2::last));

        return () -> new HashMap<>(map);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default <T> Converter with(TypeToken<T> typeToken, Fn1<Result<T>, String> converter) {
        var newMapping = new HashMap<>(typeMapping());
        newMapping.put(typeToken, (Fn1<Result<?>, String>) (Fn1) converter);

        return () -> newMapping;
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
