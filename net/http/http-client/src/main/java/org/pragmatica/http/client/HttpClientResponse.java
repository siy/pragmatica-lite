package org.pragmatica.http.client;

import io.netty.handler.codec.http.FullHttpResponse;
import org.pragmatica.http.codec.TypeToken;
import org.pragmatica.http.protocol.HttpHeaders;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.lang.Result;

import java.nio.charset.StandardCharsets;

public interface HttpClientResponse {
    HttpHeaders headers();
    String body();

    HttpStatus status();

    <T> Result<T> body(TypeToken<T> typeToken);
    <T> Result<T> body(Class<T> type);

    static HttpClientResponse httpClientResponse(FullHttpResponse response, HttpClientConfiguration configuration) {
        record httpClientResponse(FullHttpResponse response, HttpClientConfiguration configuration) implements HttpClientResponse {
            @Override
            public HttpHeaders headers() {
                return HttpHeaders.from(response.headers());
            }

            @Override
            public String body() {
                return response().content()
                                 .toString(StandardCharsets.UTF_8);
            }

            @Override
            public HttpStatus status() {
                return HttpStatus.fromInternal(response().status());
            }

            @Override
            public <T> Result<T> body(TypeToken<T> typeToken) {
                //TODO: implement
                return null;
            }

            @Override
            public <T> Result<T> body(Class<T> type) {
                //TODO: implement
                return null;
            }
        }

        return new httpClientResponse(response, configuration);
    }
}
