package org.pragmatica.http.protocol;

@SuppressWarnings("unused")
public interface HttpHeaderName {
    String asString();

    static HttpHeaderName of(String name) {
        record headerName(String asString) implements HttpHeaderName {}

        return new headerName(name);
    }
}
