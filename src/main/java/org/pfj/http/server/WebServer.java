package org.pfj.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.pfj.lang.Cause;

public class WebServer {
    private static final int DEFAULT_PORT = 8000;

    private final EndpointTable endpointTable;
    private final int port;

    private WebServer(int port, EndpointTable endpointTable) {
        this.port = port;
        this.endpointTable = endpointTable;
    }

    public static WebServer create(int port, RouteSource ... routes) {
        return new WebServer(port, EndpointTable.with(routes));
    }

    public static WebServer create(RouteSource ... routes) {
        return new WebServer(DEFAULT_PORT, EndpointTable.with(routes));
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerChannel> serverChannelClass;

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            serverChannelClass = EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            bossGroup = new KQueueEventLoopGroup(1);
            workerGroup = new KQueueEventLoopGroup();
            serverChannelClass = KQueueServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            serverChannelClass = NioServerSocketChannel.class;
        }

        try {
            new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(serverChannelClass)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new WebServerInitializer())
                .bind(port)
                .sync()
                .channel()
                .closeFuture()
                .sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class WebServerInitializer extends ChannelInitializer<SocketChannel> {
        public static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                .addLast("decoder", new HttpRequestDecoder())
                .addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                .addLast("encoder", new HttpResponseEncoder())
                .addLast("handler", new WebServerHandler());
        }
    }

    private class WebServerHandler extends SimpleChannelInboundHandler<Object> {
        /**
         * Handles a new message.
         *
         * @param ctx The channel context.
         * @param msg The HTTP request message.
         */
        @Override
        public void channelRead0(final ChannelHandlerContext ctx, final Object msg) {
            if (!(msg instanceof final FullHttpRequest request)) {
                return;
            }

            if (HttpUtil.is100ContinueExpected(request)) {
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            var context = RequestContext.from(ctx, request);

            try {
                endpointTable
                    .findRoute(request.method(), request.uri())
                    .map(route -> route.handler()
                        .handle(context)
                        .onResult(result -> result.fold(
                            failure -> context.sendFailure(convertError(failure)),
                            success -> context.sendSuccess(route.contentType(), success)
                        ))
                    )
                    .whenEmpty(() -> context.sendFailure(WebError.NOT_FOUND));
            } catch (RuntimeException ex) {
                context.sendFailure(CompoundCause.fromThrowable(WebError.INTERNAL_SERVER_ERROR, ex));
            }
        }

        private CompoundCause convertError(Cause failure) {
            if (failure instanceof CompoundCause compoundCause) {
                return compoundCause;
            }

            return CompoundCause.from(WebError.INTERNAL_SERVER_ERROR.status(), failure);
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            ctx.close();
        }

        @Override
        public void channelReadComplete(final ChannelHandlerContext ctx) {
            ctx.flush();
        }
    }
}
