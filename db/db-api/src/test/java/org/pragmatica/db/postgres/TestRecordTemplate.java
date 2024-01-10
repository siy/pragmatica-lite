package org.pragmatica.db.postgres;

import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

public interface TestRecordTemplate extends RecordTemplate<TestRecord> {
    TestRecordTemplate INSTANCE = new TestRecordTemplate() {};

    static TestRecordBuilder builder() {
        return id -> value -> () -> new TestRecord(id, value);
    }

    interface TestRecordBuilder {
        Value id(int id);

        interface Value {
            Build value(String value);
        }

        interface Build {
            TestRecord build();
        }
    }

    @Override
    default Result<TestRecord> load(KeyToValue mapping) {
        return Result.all(mapping.get("id", new TypeToken<Integer>() {}),
                          mapping.get("value", new TypeToken<String>() {}))
                     .map(TestRecord::new);
    }

    @Override
    default Stream<Tuple2<String, TypeToken<?>>> fields() {
        return FIELDS.stream();
    }

    @Override
    default Stream<Tuple3<String, TypeToken<?>, ?>> values(TestRecord record) {
        return VALUE_EXTRACTORS.stream()
                               .map(tuple -> tuple.map((name, type, fn) -> tuple(name, type, fn.apply(record))));
    }

    List<Tuple2<String, TypeToken<?>>> FIELDS = List.of(
        tuple("id", new TypeToken<Integer>() {}),
        tuple("value", new TypeToken<String>() {})
    );

    List<Tuple3<String, TypeToken<?>, Functions.Fn1<?, TestRecord>>> VALUE_EXTRACTORS = List.of(
        tuple("id", new TypeToken<Integer>() {}, TestRecord::id),
        tuple("value", new TypeToken<String>() {}, TestRecord::value)
    );
}
