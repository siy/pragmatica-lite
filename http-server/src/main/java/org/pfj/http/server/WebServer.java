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
import org.pfj.http.server.error.WebError;
import org.pfj.http.server.routing.RoutingTable;
import org.pfj.http.server.routing.RouteSource;
import org.pfj.lang.Causes;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;

import java.util.ArrayList;
import java.util.List;

import static org.pfj.http.server.util.Utils.normalize;

public class WebServer {
    static {
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
    }

    private static final Logger log = LogManager.getLogger(WebServer.class);

    private final RoutingTable routingTable;
    private final Configuration configuration;

    private WebServer(Configuration configuration, RoutingTable routingTable) {
        this.configuration = configuration;
        this.routingTable = routingTable;
    }

    public static Builder with(Configuration configuration) {
        return new Builder(configuration);
    }

    public static class Builder {
        private final Configuration configuration;
        private final List<RouteSource> routes = new ArrayList<>();

        private Builder(Configuration configuration) {
            this.configuration = configuration;
        }

        public Builder and(RouteSource... routeSources) {
            routes.addAll(List.of(routeSources));
            return this;
        }

        public WebServer build() {
            return new WebServer(configuration, RoutingTable.with(routes.stream()));
        }
    }

    public Promise<Void> start() {
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

        routingTable.print();

        var promise = Promise.<Void>promise()
            .onResultDo(() -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });

        try {
            new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(serverChannelClass)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new WebServerInitializer())
                .bind(configuration.port())
                .sync()
                .channel()
                .closeFuture()
                .addListener(future -> decode(promise, future));
        } catch (InterruptedException e) {
            //In rare cases when .sync() will be interrupted, fail with error
            promise.resolve(WebError.SERVICE_UNAVAILABLE.result());
        }

        return promise;
    }

    private void decode(Promise<Void> promise, Future<? super Void> future) {
        promise.resolve(future.isSuccess()
            ? Result.success(null)
            : Causes.fromThrowable(future.cause()).result());
    }

    public Configuration config() {
        return configuration;
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
        public void channelRead0(ChannelHandlerContext ctx, Object msg) {
            if (!(msg instanceof FullHttpRequest request)) {
                return;
            }

            if (HttpUtil.is100ContinueExpected(request)) {
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            var context = RequestContext.from(ctx, request, configuration);

            routingTable.findRoute(request.method(), normalize(request.uri()))
                .whenEmpty(() -> context.sendFailure(WebError.NOT_FOUND))
                .whenPresent(route -> context.setRoute(route).invokeAndRespond());
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
}
