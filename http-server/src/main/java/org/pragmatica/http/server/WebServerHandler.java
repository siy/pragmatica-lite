package org.pragmatica.http.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.pragmatica.http.protocol.HttpMethod;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.config.Configuration;
import org.pragmatica.http.server.config.serialization.ContentType;
import org.pragmatica.http.server.error.WebError;
import org.pragmatica.http.server.impl.DataContainer;
import org.pragmatica.http.server.routing.RequestRouter;
import org.pragmatica.lang.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static org.pragmatica.http.util.Utils.normalize;

class WebServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(WebServerHandler.class);
    private static final String SERVER_NAME = "Pragmatica Web Server";
    private static final Supplier<String> dateTimeNow = () -> ZonedDateTime.now(Clock.systemUTC())
                                                                   .format(DateTimeFormatter.RFC_1123_DATE_TIME);

    private final Configuration configuration;
    private final RequestRouter routingTable;

    WebServerHandler(Configuration configuration, RequestRouter routingTable) {
        this.configuration = configuration;
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
                    .toResult(() -> WebError.from(HttpStatus.NOT_FOUND, path).result())
                    .onFailure(cause -> sendErrorResponse(ctx, cause))
                    .onSuccess(route -> RequestContext.handle(ctx, request, route, configuration));
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, Result.Cause cause) {
        //TODO: finish
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    public static DataContainer decodeError(Result.Cause cause) {
        return cause instanceof WebError webError
               ? DataContainer.StringData.from(webError)
               : DataContainer.StringData.from(HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    public static void sendResponse(ChannelHandlerContext ctx, DataContainer dataContainer, ContentType contentType, boolean keepAlive) {
        var content = dataContainer.responseBody();
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, dataContainer.status().toInternal(), content);

        response.headers()
                .set(HttpHeaderNames.SERVER, SERVER_NAME)
                .set(HttpHeaderNames.DATE, dateTimeNow.get())
                .set(HttpHeaderNames.CONTENT_TYPE, contentType.text())
                .set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(content.readableBytes()));
        dataContainer.responseHeaders()
                     .forEach(header -> response.headers().set(header.first().headerName(), header.last()));

        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response, ctx.voidPromise());
        }
    }
//        writer.addHeader(HttpHeaderName.SERVER, SERVER_NAME);
//        writer.addHeader(HttpHeaderName.DATE, now.get());
//
//        writer.sendResponseHeaders(dataContainer.status(), dataContainer.responseLength())
//              .onFailure(cause -> log.error(STR. "Failed to send response headers \{ cause }" ))
//              .flatMap(_ -> dataContainer.writeResponseBody(writer))
//              .onFailure(cause -> log.error(STR. "Failed to send response \{ cause }" ))
//              .onFailureDo(writer::close);
//    private RequestContext sendResponse(HttpResponseStatus status, ContentType contentType, ByteBuf entity) {
//        var keepAlive = HttpUtil.isKeepAlive(request);
//        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, entity);
//
//        response.headers()
//                .add(responseHeaders)
//                .set(HttpHeaderNames.SERVER, SERVER_NAME)
//                .set(HttpHeaderNames.DATE, now().format(DATETIME_FORMATTER))
//                .set(HttpHeaderNames.CONTENT_TYPE, contentType.text())
//                .set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(entity.maxCapacity()));
//
//        return this;
//    }

}
