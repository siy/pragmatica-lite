package org.pragmatica.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.pragmatica.codec.json.TypeToken;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.config.Configuration;
import org.pragmatica.http.server.error.WebError;
import org.pragmatica.http.server.impl.DataContainer;
import org.pragmatica.http.server.routing.Redirect;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.http.util.Utils;
import org.pragmatica.http.util.ulid.ULID;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.pragmatica.http.util.Utils.lazy;
import static org.pragmatica.http.util.Utils.value;
import static org.pragmatica.lang.Option.some;
import static org.pragmatica.lang.Promise.failed;
import static org.pragmatica.lang.Result.success;

//TODO: support structured error responses. See https://www.rfc-editor.org/rfc/rfc7807 for more details
//TODO: support cookies
@SuppressWarnings("unused")
public class RequestContext {
    private static final int PATH_PARAM_LIMIT = 1024;

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;
    private final Configuration configuration;
    private final HttpHeaders responseHeaders = new CombinedHttpHeaders(true);
    private final Route<?> route;
    private final String requestId;

    private Supplier<List<String>> pathParamsSupplier = lazy(() -> pathParamsSupplier = value(initPathParams()));
    private Supplier<Map<String, List<String>>> queryStringParamsSupplier = lazy(() -> queryStringParamsSupplier = value(initQueryStringParams()));
    private Supplier<Map<String, String>> headersSupplier = lazy(() -> headersSupplier = value(initHeaders()));
    private boolean keepAlive = false;

    private RequestContext(ChannelHandlerContext ctx, FullHttpRequest request, Route<?> route, Configuration configuration) {
        this.ctx = ctx;
        this.request = request;
        this.route = route;
        this.configuration = configuration;
        this.requestId = ULID.randomULID().encoded();
    }

    public static void handle(ChannelHandlerContext ctx, FullHttpRequest request, Route<?> route, Configuration configuration) {
        new RequestContext(ctx, request, route, configuration).invokeAndRespond();
    }

    public Route<?> route() {
        return route;
    }

    public String requestId() {
        return requestId;
    }

    public ByteBuf body() {
        return request.content();
    }

    public String bodyAsString() {
        return body().toString(StandardCharsets.UTF_8);
    }

    public <T> Result<T> fromJson(TypeToken<T> literal) {
        return configuration.jsonCodec().deserialize(request.content(), literal);
    }

    public List<String> pathParams() {
        return pathParamsSupplier.get();
    }

    public Map<String, List<String>> queryParams() {
        return queryStringParamsSupplier.get();
    }

    public Map<String, String> requestHeaders() {
        return headersSupplier.get();
    }

    public HttpHeaders responseHeaders() {
        return responseHeaders;
    }

    private void invokeAndRespond() {
        safeCall().onResult(this::sendResponse);
    }

    private void sendResponse(Result<?> result) {
        result
            .flatMap(this::serializeResponse)
            .onSuccessDo(this::setKeepAlive)        // Set keepAlive only for successful responses
            .recover(WebServerHandler::decodeError)
            .onSuccess(this::sendResponse);
    }

    private void setKeepAlive() {
        keepAlive = HttpUtil.isKeepAlive(request);
    }

    private void sendResponse(DataContainer dataContainer) {
        WebServerHandler.sendResponse(ctx, dataContainer, route.contentType(), keepAlive, some(requestId));
    }

    private Result<DataContainer> serializeResponse(Object value) {
        return switch (value) {
            case DataContainer dataContainer -> success(dataContainer);
            case Redirect redirect -> success(redirect).map(DataContainer.RedirectData::from);
            case WebError webError -> success(webError).map(DataContainer.StringData::from);
            case String string -> success(string).map(DataContainer.StringData::from);
            case byte[] bytes -> success(bytes).map(DataContainer.BinaryData::from);

            default -> switch (route.contentType()) {
                case TEXT_PLAIN -> success(value).map(Object::toString)
                                                 .map(DataContainer.StringData::from);
                case APPLICATION_JSON -> configuration.jsonCodec().serialize(value)
                                                      .map(DataContainer.ByteBufData::from);
            };
        };
    }

    private List<String> initPathParams() {
        var elements = Utils.normalize(request.uri())
                            .substring(route.path().length())
                            .split("/", PATH_PARAM_LIMIT);

        return List.of(elements)
                   .subList(0, elements.length - 1);
    }

    private Map<String, List<String>> initQueryStringParams() {
        return new QueryStringDecoder(request.uri()).parameters();
    }

    private Map<String, String> initHeaders() {
        var headers = new HashMap<String, String>();

        request.headers().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));

        return headers;
    }

    private Promise<?> safeCall() {
        try {
            MDC.put("requestId", requestId);
            return route().handler().handle(this);
        } catch (Throwable t) {
            return failed(WebError.fromThrowable(HttpStatus.INTERNAL_SERVER_ERROR, t));
        } finally {
            MDC.remove("requestId");
        }
    }
}
