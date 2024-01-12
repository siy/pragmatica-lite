package org.pragmatica.db.postgres;

import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.FieldNames;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;

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
    default Result<TestRecord> load(String prefix, KeyToValue mapping) {
        return Result.all(mapping.get(prefix, "id", new TypeToken<Integer>() {}),
                          mapping.get(prefix, "value", new TypeToken<String>() {}))
                     .map(TestRecord::new);
    }

    @Override
    default FieldNames fieldNames() {
        return () -> FORMATTED_NAMES;
    }

    @Override
    default List<Tuple3<String, TypeToken<?>, Functions.Fn1<?, TestRecord>>> extractors() {
        return VALUE_EXTRACTORS;
    }

    List<Tuple3<String, TypeToken<?>, Functions.Fn1<?, TestRecord>>> VALUE_EXTRACTORS = List.of(
        tuple("id", new TypeToken<Integer>() {}, TestRecord::id),
        tuple("value", new TypeToken<String>() {}, TestRecord::value)
    );

    String FORMATTED_NAMES = RecordTemplate.buildFormattedNames(VALUE_EXTRACTORS);
}
