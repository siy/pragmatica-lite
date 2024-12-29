package org.pragmatica.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.codec.CustomCodec;
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

import static org.pragmatica.http.protocol.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.pragmatica.http.util.Utils.lazy;
import static org.pragmatica.http.util.Utils.value;
import static org.pragmatica.lang.Option.some;
import static org.pragmatica.lang.Result.success;

//TODO: support structured error responses. See https://www.rfc-editor.org/rfc/rfc7807 for more details
//TODO: support cookies
@SuppressWarnings("unused")
public class RequestContextImpl implements RequestContext {
    private static final int PATH_PARAM_LIMIT = 1024;
    private static final Result<DataContainer<?>> MISSING_CUSTOM_CODEC_ERROR = INTERNAL_SERVER_ERROR.with(
                                                                                                        "Custom codec is missing in server configuration")
                                                                                                    .result();

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;
    private final ContextConfig configuration;
    private final HttpHeaders responseHeaders = DefaultHttpHeadersFactory.headersFactory().withCombiningHeaders(true).newHeaders();
    private final Route<?> route;
    private final String requestId;

    private Supplier<List<String>> pathParamsSupplier = lazy(() -> pathParamsSupplier = value(initPathParams()));
    private Supplier<Map<String, List<String>>> queryStringParamsSupplier = lazy(() -> queryStringParamsSupplier = value(initQueryStringParams()));
    private Supplier<Map<String, String>> headersSupplier = lazy(() -> headersSupplier = value(initHeaders()));
    private boolean keepAlive = false;

    private RequestContextImpl(ChannelHandlerContext ctx, FullHttpRequest request, Route<?> route, ContextConfig configuration) {
        this.ctx = ctx;
        this.request = request;
        this.route = route;
        this.configuration = configuration;
        this.requestId = ULID.randomULID().encoded();
    }

    public static void handle(ChannelHandlerContext ctx, FullHttpRequest request, Route<?> route, ContextConfig configuration) {
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

    private void sendResponse(DataContainer<?> dataContainer) {
        HttpServerHandler.sendResponse(ctx, dataContainer, keepAlive, some(requestId));
    }

    private Result<DataContainer<?>> serializeResponse(Object value) {
        return switch (value) {
            case DataContainer<?> dataContainer -> success(dataContainer).map(ctr -> ctr.withContentType(route.contentType()));
            case Redirect redirect -> success(redirect).map(DataContainer.RedirectData::from)
                                                       .map(ctr -> ctr.withContentType(route.contentType()));
            case String string -> success(string).map(DataContainer.StringData::from)
                                                 .map(ctr -> ctr.withContentType(route.contentType()));
            case byte[] bytes -> success(bytes).map(DataContainer.BinaryData::from)
                                               .map(ctr -> ctr.withContentType(route.contentType()));
            case HttpError httpError -> success(httpError).map(DataContainer.StringData::from)
                                                          .map(ctr -> ctr.withContentType(CommonContentTypes.TEXT_PLAIN));

            default -> switch (route.contentType().category()) {
                case PLAIN_TEXT -> success(value).map(Object::toString)
                                                 .map(DataContainer.StringData::from)
                                                 .map(ctr -> ctr.withContentType(route.contentType()));
                case JSON -> configuration.jsonCodec()
                                          .serialize(value)
                                          .map(DataContainer.ByteBufData::from)
                                          .map(ctr -> ctr.withContentType(route.contentType()));
                case CUSTOM -> configuration.customCodec()
                                            .serialize(value, route.contentType())
                                            .map(DataContainer.ByteBufData::from)
                                            .map(ctr -> ctr.withContentType(route.contentType()));
                case BINARY -> INTERNAL_SERVER_ERROR.with("Content type is binary, but the response is not a byte array")
                                                    .result();

            };
        };
    }

    private Result<DataContainer.ByteBufData> serializeCustom(Object value, CustomCodec codec) {
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
            return INTERNAL_SERVER_ERROR.with(t).promise();
        } finally {
            MDC.remove("requestId");
        }
    }
}
