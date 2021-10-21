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
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pfj.lang.*;

import static org.pfj.http.server.Utils.normalize;

public class WebServer {
    static {
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
    }

    private static final Logger log = LogManager.getLogger(WebServer.class);
    private static final int DEFAULT_PORT = 8000;

    private final EndpointTable endpointTable;
    private final int port;
    private CauseMapper causeMapper = CauseMapper::defaultConverter;

    private WebServer(int port, EndpointTable endpointTable) {
        this.port = port;
        this.endpointTable = endpointTable;
    }

    public static WebServer create(int port, RouteSource... routes) {
        return new WebServer(port, EndpointTable.with(routes));
    }

    public static WebServer create(RouteSource... routes) {
        return create(DEFAULT_PORT, routes);
    }

    public WebServer withCauseMapper(CauseMapper causeMapper) {
        this.causeMapper = causeMapper;
        return this;
    }

    public Promise<Void> start() throws InterruptedException {
        log.info("Starting WebServer...");

        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerChannel> serverChannelClass;

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            serverChannelClass = EpollServerSocketChannel.class;
            log.info("Using epoll native transport");
        } else if (KQueue.isAvailable()) {
            bossGroup = new KQueueEventLoopGroup(1);
            workerGroup = new KQueueEventLoopGroup();
            serverChannelClass = KQueueServerSocketChannel.class;
            log.info("Using kqueue native transport");
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            serverChannelClass = NioServerSocketChannel.class;
            log.info("Using NIO transport");
        }

        endpointTable.print();

        var promise = Promise.<Void>promise();

        try {
            new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(serverChannelClass)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new WebServerInitializer())
                .bind(port)
                .sync()
                .channel()
                .closeFuture()
                .addListener(future -> decode(promise, future));
        } finally {
            promise.onResult(__ -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });
        }

        return promise;
    }

    private void decode(Promise<Void> promise, Future<? super Void> future) {
        promise.resolve(future.isSuccess()
            ? Result.success(null)
            : Causes.fromThrowable(future.cause()).result());
    }

    private class WebServerInitializer extends ChannelInitializer<SocketChannel> {
        public static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

        @Override
        public void initChannel(SocketChannel ch) {
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

            var context = RequestContext.from(ctx, request, causeMapper);

            endpointTable.findRoute(request.method(), normalize(request.uri()))
                .whenEmpty(() -> context.sendFailure(WebError.NOT_FOUND))
                .whenPresent(route -> context.setRoute(route).invokeAndRespond());
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
