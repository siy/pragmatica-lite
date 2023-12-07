package org.pragmatica.http.protocol;

import org.pragmatica.lang.Option;

import java.util.List;

@SuppressWarnings("unused")
public interface HttpHeaders {
    Option<String> header(HttpHeaderName name);
    Option<List<String>> allValues(HttpHeaderName name);

    HttpHeaders add(HttpHeaderName name, List<String> values);

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
}
