package org.pragmatica.http.protocol;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Tuple.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Tuple.tuple;

@SuppressWarnings("unused")
public interface HttpHeaders {
    Option<String> header(HttpHeaderName name);

    List<String> all(HttpHeaderName name);

    HttpHeaders add(HttpHeaderName name, List<String> values);

    default Stream<String> stream(HttpHeaderName name) {
        return all(name).stream();
    }

    Stream<Tuple2<HttpHeaderName, String>> expanded();

    default HttpHeaders add(HttpHeaderName name, String... values) {
        return add(name, List.of(values));
    }

    HttpHeaders remove(HttpHeaderName name);

    default HttpHeaders addIfMissing(HttpHeaderName name, String... values) {
        return addIfMissing(name, List.of(values));
    }

    default HttpHeaders addIfMissing(HttpHeaderName name, List<String> values) {
        return header(name).isEmpty() ? add(name, values) : this;
    }

    static HttpHeaders httpHeaders() {
        record httpHeaders(Map<HttpHeaderName, List<String>> headers) implements HttpHeaders {
            private Option<List<String>> get(HttpHeaderName name) {
                return option(headers.get(name));
            }

            @Override
            public Stream<Tuple2<HttpHeaderName, String>> expanded() {
                return headers().entrySet()
                                .stream()
                                .flatMap(entry -> entry.getValue()
                                                       .stream()
                                                       .map(value -> tuple(entry.getKey(), value)));
            }

            @Override
            public Option<String> header(HttpHeaderName name) {
                return Option.from(all(name)
                                       .stream()
                                       .findFirst());
            }

            @Override
            public List<String> all(HttpHeaderName name) {
                return get(name).or(List::of);
            }

            @Override
            public HttpHeaders add(HttpHeaderName name, List<String> values) {
                headers().computeIfAbsent(name, k -> new ArrayList<>())
                         .addAll(values);
                return this;
            }

            @Override
            public HttpHeaders remove(HttpHeaderName name) {
                headers().remove(name);
                return this;
            }
        }
        return new httpHeaders(new HashMap<>());
    }
}
