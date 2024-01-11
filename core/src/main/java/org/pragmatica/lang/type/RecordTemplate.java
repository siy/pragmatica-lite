package org.pragmatica.lang.type;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;

import java.util.stream.Stream;

public interface RecordTemplate<T extends Record> {
    Result<T> load(KeyToValue mapping);

    Stream<Tuple2<String, TypeToken<?>>> fieldDescriptors();
    Stream<Tuple3<String, TypeToken<?>, ?>> valueDescriptors(T record);

    FieldNames fieldNames();
    FieldValues fieldValues(T record);
}
