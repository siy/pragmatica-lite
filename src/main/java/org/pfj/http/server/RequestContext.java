package org.pfj.http.server;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIteratorPool;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pfj.lang.Result;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.pfj.http.server.ContentType.TEXT_PLAIN;
import static org.pfj.http.server.Utils.*;

public class RequestContext {
    private static final Logger log = LogManager.getLogger();

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final String SERVER_NAME = "PFJ Netty Server";
    private static final int PATH_PARAM_LIMIT = 1024;

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;
    private Route<?> route;

    private RequestContext(ChannelHandlerContext ctx, FullHttpRequest request) {
        this.ctx = ctx;
        this.request = request;
    }

    public static RequestContext from(ChannelHandlerContext ctx, FullHttpRequest request) {
        return new RequestContext(ctx, request);
    }

    public void setRoute(Route<?> route) {
        this.route = route;
    }

    //-------------------------------------------------------------------------------------------

    public RequestContext sendFailure(CompoundCause error) {
        return sendResponse(error.status(), TEXT_PLAIN, asByteBuf(error.message()));
    }

    public RequestContext sendSuccess(ContentType contentType, Object entity) {
        return sendResponse(HttpResponseStatus.OK, contentType, serializeResponse(contentType, entity));
    }

    public RequestContext sendSuccess(ContentType contentType, ByteBuf entity) {
        return sendResponse(HttpResponseStatus.OK, contentType, entity);
    }

    public RequestContext sendResponse(HttpResponseStatus status, ContentType contentType, ByteBuf entity) {
        var keepAlive = HttpUtil.isKeepAlive(request);
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, entity);

        response.headers()
            .set(HttpHeaderNames.SERVER, SERVER_NAME)
            .set(HttpHeaderNames.DATE, ZonedDateTime.now().format(DATETIME_FORMATTER))
            .set(HttpHeaderNames.CONTENT_TYPE, contentType.text())
            .set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(entity.maxCapacity()));

        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response, ctx.voidPromise());
        }
        return this;
    }

    private ByteBuf serializeResponse(ContentType contentType, Object success) {
        return switch (contentType) {
            case TEXT_PLAIN -> asByteBuf(success.toString());
            case APPLICATION_JSON -> asByteBuf(JsonStream.serialize(success));
        };
    }

    //-------------------------------------------------------------------------------------------

    public ByteBuf body() {
        return request.content();
    }

    public String bodyAsString() {
        return body().toString(StandardCharsets.UTF_8);
    }

    public <T> Result<T> fromJson(TypeLiteral<T> literal) {
        return deserialize(request.content(), literal);
    }

    private Supplier<List<String>> pathParamsSupplier = lazy(() -> pathParamsSupplier = value(initPathParams()));
    private Supplier<Map<String, List<String>>> queryStringParamsSupplier = lazy(() -> queryStringParamsSupplier = value(initQueryStringParams()));

    public List<String> pathParams() {
        return pathParamsSupplier.get();
    }

    public Map<String, List<String>> queryParams() {
        return queryStringParamsSupplier.get();
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

    private static <T> Result<T> deserialize(ByteBuf entity, TypeLiteral<T> literal) {
        var iter = JsonIteratorPool.borrowJsonIterator();

        try {
            iter.reset(entity.array(), entity.arrayOffset(), entity.readableBytes());

            T val = iter.read(literal);

            if (CodegenAccess.head(iter) != entity.readableBytes()) {
                log.warn("Request body contains garbage after JSON body");
                return WebError.UNPROCESSABLE_ENTITY.result();
            }

            return Result.success(val);
        } catch (Exception e) {
            log.warn("Error while parsing JSON", e);
            return WebError.UNPROCESSABLE_ENTITY.result();
        } finally {
            JsonIteratorPool.returnJsonIterator(iter);
        }
    }
}
