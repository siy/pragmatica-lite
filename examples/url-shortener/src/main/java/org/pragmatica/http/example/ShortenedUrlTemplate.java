package org.pragmatica.http.example;

import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple;
import org.pragmatica.lang.Tuple.Tuple2;
import org.pragmatica.lang.Tuple.Tuple3;
import org.pragmatica.lang.type.KeyToValue;
import org.pragmatica.lang.type.RecordTemplate;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.uri.IRI;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

public interface ShortenedUrlTemplate extends RecordTemplate<ShortenedUrl> {
    static ShortenedUrlBuilder builder() {
        return id -> srcUrl -> created -> lastAccessed -> () -> new ShortenedUrl(id, srcUrl, created, lastAccessed);
    }

    interface ShortenedUrlBuilder {
        SrcUrl url(String id);

        interface SrcUrl {
            Created srcUrl(String srcUrl);
        }

        interface Created {
            LastAccessed created(LocalDateTime created);
        }

        interface LastAccessed {
            Build lastAccessed(LocalDateTime lastAccessed);
        }

        interface Build {
            ShortenedUrl build();
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
}
