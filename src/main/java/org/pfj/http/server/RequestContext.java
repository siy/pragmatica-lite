package org.pfj.http.server;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIteratorPool;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.pfj.lang.Cause;
import org.pfj.lang.Causes;
import org.pfj.lang.Result;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.pfj.http.server.ContentType.TEXT_PLAIN;
import static org.pfj.http.server.Utils.asByteBuf;

public class RequestContext {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final String SERVER_NAME = "PFJ Netty Server";

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest request;

    private RequestContext(ChannelHandlerContext ctx, FullHttpRequest request) {
        this.ctx = ctx;
        this.request = request;
    }

    public static RequestContext from(ChannelHandlerContext ctx, FullHttpRequest request) {
        return new RequestContext(ctx, request);
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
        return request.content().toString(StandardCharsets.UTF_8);
    }

    public <T> Result<T> fromJson(TypeLiteral<T> literal) {
        return deserialize(request.content(), literal);
    }

    private static <T> Result<T> deserialize(ByteBuf entity, TypeLiteral<T> literal) {
        var iter = JsonIteratorPool.borrowJsonIterator();

        iter.reset(entity.array(), entity.arrayOffset(), entity.readableBytes());
        try {
            T val = iter.read(literal);

            if (CodegenAccess.head(iter) != entity.readableBytes()) {
                return WebError.UNPROCESSABLE_ENTITY.result();
            }

            return Result.success(val);
        } catch (Exception e) {
            return WebError.UNPROCESSABLE_ENTITY.result();
        } finally {
            JsonIteratorPool.returnJsonIterator(iter);
        }
    }

    private Cause decodeException(Throwable throwable) {
        Causes.fromThrowable(throwable);

        return new CompoundCause() {
            @Override
            public HttpResponseStatus status() {
                return null;
            }

            @Override
            public String message() {
                return null;
            }
        };
    }
}
