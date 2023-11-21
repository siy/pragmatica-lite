package org.pragmatica.http.server;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.pragmatica.http.server.config.Configuration;
import org.pragmatica.http.server.config.serialization.ContentType;
import org.pragmatica.http.server.error.CompoundCause;
import org.pragmatica.http.server.error.WebError;
import org.pragmatica.http.server.routing.Redirect;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.http.server.util.Either;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

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
import static org.pragmatica.http.server.config.serialization.ContentType.TEXT_PLAIN;
import static org.pragmatica.http.server.error.CompoundCause.fromThrowable;
import static org.pragmatica.http.server.util.Utils.*;
import static org.pragmatica.lang.Promise.failed;
import static org.pragmatica.lang.Result.success;

public class RequestContext {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final String SERVER_NAME = "PFJ Netty Server";
    private static final int PATH_PARAM_LIMIT = 1024;

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;
    private final Configuration configuration;
    private final HttpHeaders responseHeaders = new CombinedHttpHeaders(true);

    private Supplier<List<String>> pathParamsSupplier = lazy(() -> pathParamsSupplier = value(initPathParams()));
    private Supplier<Map<String, List<String>>> queryStringParamsSupplier = lazy(() -> queryStringParamsSupplier = value(initQueryStringParams()));
    private Supplier<Map<String, String>> headersSupplier = lazy(() -> headersSupplier = value(initHeaders()));
    private Route<?> route;

    private RequestContext(ChannelHandlerContext ctx, FullHttpRequest request, Configuration configuration) {
        this.ctx = ctx;
        this.request = request;
        this.configuration = configuration;
    }

    public static RequestContext from(ChannelHandlerContext ctx, FullHttpRequest request, Configuration configuration) {
        return new RequestContext(ctx, request, configuration);
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
        return configuration.serializer().deserialize(request.content(), literal);
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
            .onResult(result -> result
                .flatMap(this::serializeResponse)
                .fold(
                    failure -> sendFailure(configuration.causeMapper().apply(failure)),
                    success -> {
                        //TODO: replace with switch pattern matching once it will be not a preview feature
                        if (success instanceof Either.Left<Redirect, ByteBuf> redirect) {
                            return sendRedirect(redirect.value());
                        } else if (success instanceof Either.Right<Redirect, ByteBuf> buffer) {
                            return sendSuccess(route().contentType(), buffer.value());
                        } else {
                            throw new UnsupportedOperationException("Can't happen");
                        }
                    }
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

    private Result<Either<Redirect, ByteBuf>> serializeResponse(Object value) {
        if (value instanceof Redirect redirect) {
            return success(Either.left(redirect));
        }

        return switch (route.contentType()) {
            case TEXT_PLAIN -> success(wrap(value)).map(Either::right);
            case APPLICATION_JSON -> configuration.serializer().serialize(value).map(Either::right);
        };
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
            return failed(fromThrowable(WebError.INTERNAL_SERVER_ERROR, t));
        }
    }

    private static ZonedDateTime now() {
        return ZonedDateTime.now(Clock.systemUTC());
    }

    private static ByteBuf wrap(Object value) {
        return wrappedBuffer(value.toString().getBytes(StandardCharsets.UTF_8));
    }
}
