package org.pragmatica.http.server.impl;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.CommonHeaders;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.http.server.routing.RequestRouter;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result.Cause;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static org.pragmatica.http.util.Utils.normalize;

class HttpServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final String SERVER_NAME = "Pragmatica Web Server";
    private static final Supplier<String> dateTimeNow = () -> ZonedDateTime.now(Clock.systemUTC())
                                                                           .format(DateTimeFormatter.RFC_1123_DATE_TIME);

    private final RequestRouter routingTable;
    private final ContextConfig contextConfig;

    HttpServerHandler(HttpServerConfig configuration, RequestRouter routingTable) {
        this.contextConfig = ContextConfig.fromHttpServerConfig(configuration);
        this.routingTable = routingTable;
    }

    /**
     * Handles a new message.
     *
     * @param ctx The channel context.
     * @param msg The HTTP request message.
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof FullHttpRequest request)) {
            return;
        }

        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }

        var path = normalize(request.uri());

        routingTable.findRoute(HttpMethod.from(request.method()), path)
                    .toResult(() -> HttpError.httpError(HttpStatus.NOT_FOUND, path).result())
                    .onFailure(cause -> sendErrorResponse(ctx, cause))
                    .onSuccess(route -> RequestContextImpl.handle(ctx, request, route, contextConfig));
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, Cause cause) {
        sendResponse(ctx, decodeError(cause), false, Option.none());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    public static DataContainer.StringData decodeError(Cause cause) {
        return (cause instanceof HttpError httpError
                ? DataContainer.StringData.from(httpError)
                : DataContainer.StringData.from(HttpStatus.INTERNAL_SERVER_ERROR, cause))
            .withHeader(CommonHeaders.CONTENT_TYPE, CommonContentTypes.TEXT_PLAIN.headerText());
    }

    public static void sendResponse(ChannelHandlerContext ctx,
                                    DataContainer<?> dataContainer,
                                    boolean keepAlive,
                                    Option<String> requestId) {
        var content = dataContainer.responseBody();
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, dataContainer.status().toInternal(), content);

        response.headers()
                .set(CommonHeaders.SERVER.asString(), SERVER_NAME)
                .set(CommonHeaders.DATE.asString(), dateTimeNow.get())
                .set(CommonHeaders.CONTENT_LENGTH.asString(), Long.toString(content.readableBytes()));
        dataContainer.responseHeaders()
                     .forEach(header -> response.headers().set(header.first().asString(), header.last()));
        requestId.onPresent(id -> response.headers().set(CommonHeaders.REQUEST_ID.asString(), id));

        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response, ctx.voidPromise());
        }
    }
}
