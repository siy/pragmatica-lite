package org.pragmatica.db.postgres;

import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.FieldNames;
import org.pragmatica.lang.type.FieldValues;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

public interface TestRecordTemplate extends RecordTemplate<TestRecord> {
    TestRecordTemplate INSTANCE = new TestRecordTemplate() {};

    static TestRecordBuilder builder() {
        return id -> value -> new TestRecord(id, value);
    }

    interface TestRecordBuilder {
        Value id(int id);

        interface Value {
            TestRecord value(String value);
        }
    }

    @Override
    default Result<TestRecord> load(KeyToValue mapping) {
        return Result.all(mapping.get("id", new TypeToken<Integer>() {}),
                          mapping.get("value", new TypeToken<String>() {}))
                     .map(TestRecord::new);
    }

    @Override
    default Stream<Tuple2<String, TypeToken<?>>> fieldDescriptors() {
        return FIELDS.stream();
    }

    @Override
    default Stream<Tuple3<String, TypeToken<?>, ?>> valueDescriptors(TestRecord record) {
        return VALUE_EXTRACTORS.stream()
                               .map(tuple -> tuple.map((name, type, fn) -> tuple(name, type, fn.apply(record))));
    }

    @Override
    default FieldNames fieldNames() {
        return () -> FORMATTED_NAMES;
    }

    @Override
    default FieldValues fieldValues(TestRecord record) {
        record fieldValues(List<?> values) implements FieldValues {
            @Override
            public int formatParameters(StringBuilder builder, int startIndex) {
                var paramString = IntStream.range(startIndex, startIndex + values.size())
                    .mapToObj(i -> STR."$\{i}")
                    .collect(Collectors.joining(", "));
                builder.append(paramString);

                return startIndex + values.size();
            }
        }

        var values = VALUE_EXTRACTORS.stream()
                                     .map(tuple -> tuple.map((_, _, fn) -> fn.apply(record)))
                                     .collect(Collectors.toList());

        return new fieldValues(values);
    }

    List<Tuple2<String, TypeToken<?>>> FIELDS = List.of(
        tuple("id", new TypeToken<Integer>() {}),
        tuple("value", new TypeToken<String>() {})
    );

    List<Tuple3<String, TypeToken<?>, Functions.Fn1<?, TestRecord>>> VALUE_EXTRACTORS = List.of(
        tuple("id", new TypeToken<Integer>() {}, TestRecord::id),
        tuple("value", new TypeToken<String>() {}, TestRecord::value)
    );

    String FORMATTED_NAMES = FIELDS.stream().map(Tuple2::first).reduce((a, b) -> STR."\{a}, \{b}").orElse("");
}
