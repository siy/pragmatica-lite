package org.pragmatica.http.example;

import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.FieldNames;
import org.pragmatica.lang.type.FieldValues;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

public interface ShortenedUrlTemplate extends RecordTemplate<ShortenedUrl> {
    ShortenedUrlTemplate INSTANCE = new ShortenedUrlTemplate() {};

    static ShortenedUrlBuilder builder() {
        return id -> srcUrl -> created -> lastAccessed -> new ShortenedUrl(id, srcUrl, created, lastAccessed);
    }

    interface ShortenedUrlBuilder {
        SrcUrl id(String id);

        interface SrcUrl {
            Created srcUrl(String srcUrl);
        }

        interface Created {
            LastAccessed created(LocalDateTime created);
        }

        interface LastAccessed {
            ShortenedUrl lastAccessed(LocalDateTime lastAccessed);
        }
    }

    @Override
    default Result<ShortenedUrl> load(KeyToValue mapping) {
        return Result.all(mapping.get("id", new TypeToken<String>() {}),
                          mapping.get("srcUrl", new TypeToken<String>() {}),
                          mapping.get("created", new TypeToken<LocalDateTime>() {}),
                          mapping.get("lastAccessed", new TypeToken<LocalDateTime>() {}))
                     .map(ShortenedUrl::new);
    }

    @Override
    default Stream<Tuple2<String, TypeToken<?>>> fieldDescriptors() {
        return FIELDS.stream();
    }

    @Override
    default Stream<Tuple3<String, TypeToken<?>, ?>> valueDescriptors(ShortenedUrl record) {
        return VALUE_EXTRACTORS.stream()
                               .map(tuple -> tuple.map((name, type, fn) -> tuple(name, type, fn.apply(record))));
    }

    @Override
    default FieldValues fieldValues(ShortenedUrl record) {
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

    @Override
    default FieldNames fieldNames() {
        return () -> FORMATTED_NAMES;
    }

    List<Tuple2<String, TypeToken<?>>> FIELDS = List.of(
        tuple("id", new TypeToken<String>() {}),
        tuple("srcUrl", new TypeToken<String>() {}),
        tuple("created", new TypeToken<LocalDateTime>() {}),
        tuple("lastAccessed", new TypeToken<LocalDateTime>() {})
    );

    List<Tuple3<String, TypeToken<?>, Functions.Fn1<?, ShortenedUrl>>> VALUE_EXTRACTORS = List.of(
        tuple("id", new TypeToken<String>() {}, ShortenedUrl::id),
        tuple("srcUrl", new TypeToken<String>() {}, ShortenedUrl::srcUrl),
        tuple("created", new TypeToken<LocalDateTime>() {}, ShortenedUrl::created),
        tuple("lastAccessed", new TypeToken<LocalDateTime>() {}, ShortenedUrl::lastAccessed)
    );

    String FORMATTED_NAMES = FIELDS.stream().map(Tuple2::first).reduce((a, b) -> STR."\{a}, \{b}").orElse("");
}