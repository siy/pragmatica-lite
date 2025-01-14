package org.pragmatica.lang.type;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface RecordTemplate<T extends Record> {
    default Result<T> load(KeyToValue mapping) {
        return load("", mapping);
    }

    Result<T> load(String prefix, KeyToValue mapping);

    FieldNames fieldNames();

    List<Tuple3<String, TypeToken<?>, Fn1<?, T>>> extractors();

    @SuppressWarnings("unused")
    default Stream<Tuple2<TypeToken<?>, Fn1<Result<?>, String>>> customConverters() {
        return Stream.empty();
    }

    default FieldValues fieldValues(T record) {
        record fieldValues(List<?> values) implements FieldValues {
            @Override
            public int formatParameters(StringBuilder builder, int startIndex) {
                var paramString = IntStream.range(startIndex, startIndex + values.size())
                                           .mapToObj(i -> "$" + i)
                                           .collect(Collectors.joining(", "));
                builder.append(paramString);

                return startIndex + values.size();
            }
        }

        var values = extractors().stream()
                                 .map(Tuple3::last)
                                 .map(fn -> fn.apply(record))
                                 .toList();

        return new fieldValues(values);
    }

    static <T> String buildFormattedNames(List<Tuple3<String, TypeToken<?>, Fn1<?, T>>> extractors) {
        return extractors.stream()
                         .map(Tuple3::first)
                         .reduce((a, b) -> a + ", " + b)
                         .orElse("");
    }
}
