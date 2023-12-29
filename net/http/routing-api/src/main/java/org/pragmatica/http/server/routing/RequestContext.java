package org.pragmatica.http.server.routing;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import org.pragmatica.http.codec.TypeToken;
import org.pragmatica.lang.Result;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public interface RequestContext {
    Route<?> route();

    String requestId();

    ByteBuf body();

    String bodyAsString();

    <T> Result<T> fromJson(TypeToken<T> literal);

    List<String> pathParams();

    Map<String, List<String>> queryParams();

    Map<String, String> requestHeaders();

    HttpHeaders responseHeaders();
}
