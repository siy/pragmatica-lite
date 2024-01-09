package org.pragmatica.config.api;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.TypeToken;

import java.util.stream.Stream;

public interface RecordDescriptor<T extends Record> {
    Result<T> load(KeyToValue mapping);

    Stream<Tuple2<String, TypeToken<?>>> fields();

    Stream<Tuple3<String, TypeToken<?>, ?>> values(T record);
}
