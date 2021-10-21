package org.pfj.http.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.pfj.http.server.CompoundCause.fromThrowable;
import static org.pfj.http.server.ContentType.TEXT_PLAIN;
import static org.pfj.http.server.Utils.*;
import static org.pfj.lang.Promise.failure;
import static org.pfj.lang.Promise.promise;
import static org.pfj.lang.Result.success;

public class RequestContext {
    private static final Logger log = LogManager.getLogger(RequestContext.class);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final String SERVER_NAME = "PFJ Netty Server";
    private static final int PATH_PARAM_LIMIT = 1024;

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;
    private final WebServer server;
    private final HttpHeaders responseHeaders = new CombinedHttpHeaders(true);

    private Supplier<List<String>> pathParamsSupplier = lazy(() -> pathParamsSupplier = value(initPathParams()));
    private Supplier<Map<String, List<String>>> queryStringParamsSupplier = lazy(() -> queryStringParamsSupplier = value(initQueryStringParams()));
    private Supplier<Map<String, String>> headersSupplier = lazy(() -> headersSupplier = value(initHeaders()));
    private Route<?> route;

    private RequestContext(ChannelHandlerContext ctx, FullHttpRequest request, WebServer server) {
        this.ctx = ctx;
        this.request = request;
        this.server = server;
    }

    public static RequestContext from(ChannelHandlerContext ctx, FullHttpRequest request, WebServer server) {
        return new RequestContext(ctx, request, server);
    }

    public RequestContext setRoute(Route<?> route) {
        this.route = route;
        return this;
    }

    public Route<?> route() {
        return route;
    }

    public ByteBuf body() {
        return request.content();
    }

    public String bodyAsString() {
        return body().toString(StandardCharsets.UTF_8);
    }

    public <T> Result<T> fromJson(TypeReference<T> literal) {
        return deserialize(request.content(), literal);
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

    public RequestContext sendFailure(CompoundCause error) {
        return sendResponse(error.status(), TEXT_PLAIN, wrap(error.message()));
    }

    public void invokeAndRespond() {
        safeCall()
            .flatMap(value -> promise(serializeResponse(value)))
            .onResult(result -> result.fold(
                failure -> sendFailure(server.causeMapper().apply(failure)),
                success -> sendSuccess(route().contentType(), success)
            ));
    }

    private RequestContext sendSuccess(ContentType contentType, Object entity) {
        if (entity instanceof Redirect redirect) {
            return sendRedirect(redirect);
        } else if (entity instanceof ByteBuf byteBuf) {
            return sendResponse(HttpResponseStatus.OK, contentType, byteBuf);
        } else {
            throw new IllegalStateException("Invalid payload type " + entity.getClass().getSimpleName());
        }
    }

    private RequestContext sendRedirect(Redirect redirect) {
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, redirect.status(), Unpooled.EMPTY_BUFFER);

        response.headers()
            .set(HttpHeaderNames.LOCATION, URLEncoder.encode(redirect.url(), StandardCharsets.ISO_8859_1));

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

        return this;
    }

    private RequestContext sendResponse(HttpResponseStatus status, ContentType contentType, ByteBuf entity) {
        var keepAlive = HttpUtil.isKeepAlive(request);
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, entity);

        response.headers()
            .add(responseHeaders)
            .set(HttpHeaderNames.SERVER, SERVER_NAME)
            .set(HttpHeaderNames.DATE, now().format(DATETIME_FORMATTER))
            .set(HttpHeaderNames.CONTENT_TYPE, contentType.text())
            .set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(entity.maxCapacity()));

        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response, ctx.voidPromise());
        }
        return this;
    }

    private Result<Object> serializeResponse(Object value) {
        if (value instanceof Redirect redirect) {
            return success(redirect);
        }

        return switch (route.contentType()) {
            case TEXT_PLAIN -> success(wrap(value));
            case APPLICATION_JSON -> serializeJson(value);
        };
    }

    private Result<Object> serializeJson(Object success) {
        try {
            return success(wrappedBuffer(server.objectMapper().writeValueAsBytes(success)));
        } catch (JsonProcessingException e) {
            return fromThrowable(WebError.INTERNAL_SERVER_ERROR, e).result();
        }
    }

    private List<String> initPathParams() {
        var elements = normalize(request.uri())
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
            return route().handler().handle(this);
        } catch (Throwable t) {
            return failure(fromThrowable(WebError.INTERNAL_SERVER_ERROR, t));
        }
    }

    private <T> Result<T> deserialize(ByteBuf entity, TypeReference<T> literal) {
        try {
            return success(server.objectMapper().readValue(
                entity.array(),
                entity.arrayOffset(),
                entity.readableBytes(),
                literal));
        } catch (IOException e) {
            return fromThrowable(WebError.UNPROCESSABLE_ENTITY, e).result();
        }
    }

    private static ZonedDateTime now() {
        return ZonedDateTime.now(Clock.systemUTC());
    }

    private static ByteBuf wrap(Object value) {
        return wrappedBuffer(value.toString().getBytes(StandardCharsets.UTF_8));
    }
}
