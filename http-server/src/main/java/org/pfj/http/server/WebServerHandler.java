package org.pfj.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.pfj.http.server.config.Configuration;
import org.pfj.http.server.error.WebError;
import org.pfj.http.server.routing.RoutingTable;

import static org.pfj.http.server.util.Utils.normalize;

class WebServerHandler extends SimpleChannelInboundHandler<Object> {
    private final Configuration configuration;
    private final RoutingTable routingTable;

    WebServerHandler(Configuration configuration, RoutingTable routingTable) {
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

        var context = RequestContext.from(ctx, request, configuration);

        routingTable.findRoute(request.method(), normalize(request.uri()))
                    .onEmpty(() -> context.sendFailure(WebError.NOT_FOUND))
                    .onPresent(route -> context.setRoute(route).invokeAndRespond());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}
