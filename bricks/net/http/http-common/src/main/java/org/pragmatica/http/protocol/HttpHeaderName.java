package org.pragmatica.http.protocol;

@SuppressWarnings("unused")
public interface HttpHeaderName {
    String asString();

    static HttpHeaderName httpHeaderName(String name) {
        record headerName(String asString) implements HttpHeaderName {}

        return new headerName(name);
    }

    static HttpHeaderName fromRaw(String name) {
        return CommonHeaders.lookup(name)
                            .or(() -> HttpHeaderName.httpHeaderName(name));
    }
}
