package org.pragmatica.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.pragmatica.codec.json.JsonCodecFactory;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.codec.CustomCodec;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.HttpServerConfiguration;
import org.pragmatica.http.server.routing.Redirect;
import org.pragmatica.http.server.routing.RequestContext;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.http.util.Utils;
import org.pragmatica.id.ulid.ULID;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;
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
public class RequestContextImpl implements RequestContext {
    private static final int PATH_PARAM_LIMIT = 1024;
    private static final Result<DataContainer> MISSING_CUSTOM_CODEC_ERROR = HttpError.httpError(HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                                "Custom codec is missing in server configuration")
                                                                                     .result();

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;
    private final HttpServerConfiguration configuration;
    private final HttpHeaders responseHeaders = new CombinedHttpHeaders(true);
    private final Route<?> route;
    private final String requestId;

    private Supplier<List<String>> pathParamsSupplier = lazy(() -> pathParamsSupplier = value(initPathParams()));
    private Supplier<Map<String, List<String>>> queryStringParamsSupplier = lazy(() -> queryStringParamsSupplier = value(initQueryStringParams()));
    private Supplier<Map<String, String>> headersSupplier = lazy(() -> headersSupplier = value(initHeaders()));
    private boolean keepAlive = false;

    private RequestContextImpl(ChannelHandlerContext ctx, FullHttpRequest request, Route<?> route, HttpServerConfiguration configuration) {
        this.ctx = ctx;
        this.request = request;
        this.route = route;
        this.configuration = configuration;
        this.requestId = ULID.randomULID().encoded();
    }

    public static void handle(ChannelHandlerContext ctx, FullHttpRequest request, Route<?> route, HttpServerConfiguration configuration) {
        new RequestContextImpl(ctx, request, route, configuration).invokeAndRespond();
    }

    @Override
    public Route<?> route() {
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
        return body().toString(StandardCharsets.UTF_8);
    }

    @Override
    public <T> Result<T> fromJson(TypeToken<T> literal) {
        return configuration.jsonCodec()
                            .or(JsonCodecFactory.defaultFactory()::withDefaultConfiguration)
                            .deserialize(request.content(), literal);
    }

    @Override
    public List<String> pathParams() {
        return pathParamsSupplier.get();
    }

    @Override
    public Map<String, List<String>> queryParams() {
        return queryStringParamsSupplier.get();
    }

    @Override
    public Map<String, String> requestHeaders() {
        return headersSupplier.get();
    }

    @Override
    public HttpHeaders responseHeaders() {
        return responseHeaders;
    }

    private void invokeAndRespond() {
        safeCall().onResult(this::sendResponse);
    }

    //TODO: automatic adjustment of the response code
    private void sendResponse(Result<?> result) {
        result
            .flatMap(this::serializeResponse)
            .onSuccessRun(this::setKeepAlive)        // Set keepAlive only for successful responses
            .recover(HttpServerHandler::decodeError)
            .onSuccess(this::sendResponse);
    }

    private void setKeepAlive() {
        keepAlive = HttpUtil.isKeepAlive(request);
    }

    private void sendResponse(DataContainer dataContainer) {
        HttpServerHandler.sendResponse(ctx, dataContainer, route.contentType(), keepAlive, some(requestId));
    }

    private Result<DataContainer> serializeResponse(Object value) {
        return switch (value) {
            case DataContainer dataContainer -> success(dataContainer);
            case Redirect redirect -> success(redirect).map(DataContainer.RedirectData::from);
            case HttpError httpError -> success(httpError).map(DataContainer.StringData::from);
            case String string -> success(string).map(DataContainer.StringData::from);
            case byte[] bytes -> success(bytes).map(DataContainer.BinaryData::from);

            default -> switch (route.contentType().category()) {
                case PLAIN_TEXT -> success(value).map(Object::toString)
                                                 .map(DataContainer.StringData::from);
                case JSON -> configuration.jsonCodec()
                                          .or(JsonCodecFactory.defaultFactory()::withDefaultConfiguration)
                                          .serialize(value)
                                          .map(DataContainer.ByteBufData::from);
                case CUSTOM -> configuration.customCodec()
                                            .map(codec -> serializeCustom(value, codec))
                                            .or(MISSING_CUSTOM_CODEC_ERROR);

            };
        };
    }

    private Result<DataContainer> serializeCustom(Object value, CustomCodec codec) {
        return codec.serialize(value, route.contentType())
                    .map(DataContainer.ByteBufData::from);
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
            return failed(HttpError.httpError(HttpStatus.INTERNAL_SERVER_ERROR, t));
        } finally {
            MDC.remove("requestId");
        }
    }
}
