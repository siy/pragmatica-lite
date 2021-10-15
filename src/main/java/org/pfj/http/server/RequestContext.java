package org.pfj.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

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

    public RequestContext sendErrorStatus(WebError error) {
        return sendErrorStatus(error, error.message());
    }

    public RequestContext sendErrorStatus(WebError error, String text) {
        return sendErrorStatus(error.status(), text);
    }

    public RequestContext sendErrorStatus(HttpResponseStatus status) {
        return sendErrorStatus(status, status.reasonPhrase());
    }

    public RequestContext sendErrorStatus(HttpResponseStatus status, String text) {
        return sendResponse(status, TEXT_PLAIN, asByteBuf(text));
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

    //-------------------------------------------------------------------------------------------

    public ByteBuf body() {
        return request.content();
    }

    public String bodyAsString() {
        return request.content().toString(StandardCharsets.UTF_8);
    }
}
