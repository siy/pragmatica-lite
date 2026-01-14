package org.pragmatica.http.routing;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;

import static org.pragmatica.http.routing.Utils.lazy;
import static org.pragmatica.http.routing.Utils.value;

/**
 * Implementation of RequestContext for Netty HTTP requests.
 */
@SuppressWarnings("unused")
public final class RequestContextImpl implements RequestContext {
    private static final int PATH_PARAM_LIMIT = 1024;

    private final FullHttpRequest request;
    private final Route< ? > route;
    private final JsonCodec jsonCodec;
    private final String requestId;
    private final HttpHeaders responseHeaders = DefaultHttpHeadersFactory.headersFactory()
                                                                        .withCombiningHeaders(true)
                                                                        .newHeaders();

    private Supplier<List<String>> pathParamsSupplier = lazy(() -> pathParamsSupplier = value(initPathParams()));
    private Supplier<Map<String, List<String>>> queryParamsSupplier = lazy(() -> queryParamsSupplier = value(initQueryParams()));
    private Supplier<Map<String, String>> headersSupplier = lazy(() -> headersSupplier = value(initRequestHeaders()));

    private RequestContextImpl(FullHttpRequest request, Route< ? > route, JsonCodec jsonCodec, String requestId) {
        this.request = request;
        this.route = route;
        this.jsonCodec = jsonCodec;
        this.requestId = requestId;
    }

    public static RequestContextImpl requestContext(FullHttpRequest request,
                                                    Route< ? > route,
                                                    JsonCodec jsonCodec,
                                                    String requestId) {
        return new RequestContextImpl(request, route, jsonCodec, requestId);
    }

    @Override
    public Route< ? > route() {
        return route;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ByteBuf body() {
        return request.content();
    }

    @Override
    public String bodyAsString() {
        return body()
                   .toString(StandardCharsets.UTF_8);
    }

    @Override
    public <T> Result<T> fromJson(TypeToken<T> literal) {
        return jsonCodec.deserialize(request.content(), literal);
    }

    @Override
    public List<String> pathParams() {
        return pathParamsSupplier.get();
    }

    @Override
    public Map<String, List<String>> queryParams() {
        return queryParamsSupplier.get();
    }

    @Override
    public Map<String, String> requestHeaders() {
        return headersSupplier.get();
    }

    @Override
    public HttpHeaders responseHeaders() {
        return responseHeaders;
    }

    private List<String> initPathParams() {
        var elements = PathUtils.normalize(request.uri())
                                .substring(route.path()
                                                .length())
                                .split("/", PATH_PARAM_LIMIT);
        return List.of(elements)
                   .subList(0, elements.length - 1);
    }

    private Map<String, List<String>> initQueryParams() {
        return new QueryStringDecoder(request.uri()).parameters();
    }

    private Map<String, String> initRequestHeaders() {
        var headers = new HashMap<String, String>();
        request.headers()
               .forEach(entry -> headers.put(entry.getKey(),
                                             entry.getValue()));
        return Map.copyOf(headers);
    }
}
