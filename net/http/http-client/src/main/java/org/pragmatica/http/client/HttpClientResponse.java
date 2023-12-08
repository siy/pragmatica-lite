package org.pragmatica.http.client;

import io.netty.handler.codec.http.FullHttpResponse;

public interface HttpClientResponse {

    static HttpClientResponse httpClientResponse(FullHttpResponse response, HttpClientConfiguration configuration) {
        record httpClientResponse(FullHttpResponse response, HttpClientConfiguration configuration) implements HttpClientResponse {
        }

        return new httpClientResponse(response, configuration);
    }
}
