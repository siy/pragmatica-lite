package org.pfj.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pfj.http.server.config.Configuration;
import org.pfj.http.server.error.WebError;
import org.pfj.http.server.routing.RouteSource;
import org.pfj.http.server.routing.RoutingTable;
import org.pfj.lang.Causes;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;
import org.pfj.lang.Tuple;
import org.pfj.lang.Tuple.Tuple3;

import java.util.ArrayList;
import java.util.List;

import static org.pfj.lang.Tuple.tuple;

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

        routingTable.print();

        var reactorConfig = configureReactor();

        var promise = Promise.<Void>promise().onResultDo(() -> gracefulShutdown(reactorConfig));

        try {
            configureBootstrap(reactorConfig)
                .childOption(ChannelOption.SO_SNDBUF, configuration.sendBufferSize())
                .childOption(ChannelOption.SO_RCVBUF, configuration.receiveBufferSize())
                .handler(new LoggingHandler(configuration.logLevel()))
                .childHandler(new WebServerInitializer(configuration, routingTable))
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

    private void gracefulShutdown(ReactorConfig reactorConfig) {
        reactorConfig.bossGroup().shutdownGracefully();
        reactorConfig.workerGroup().shutdownGracefully();
    }

    private ServerBootstrap configureBootstrap(ReactorConfig reactorConfig) {
        return new ServerBootstrap()
            .group(reactorConfig.bossGroup(), reactorConfig.workerGroup())
            .channel(reactorConfig.serverChannelClass());
    }

    private ReactorConfig configureReactor() {
        if (Epoll.isAvailable() && configuration.enableNative()) {
            log.info("Using epoll native transport");
            return ReactorConfig.epoll();
        } else if (KQueue.isAvailable()&& configuration.enableNative()) {
            log.info("Using kqueue native transport");
            return ReactorConfig.kqueue();
        } else {
            log.info("Using NIO transport");
            return ReactorConfig.nio();
        }
    }

    private void decode(Promise<Void> promise, Future<? super Void> future) {
        promise.resolve(future.isSuccess()
            ? Result.success(null)
            : Causes.fromThrowable(future.cause()).result());
    }

    static record ReactorConfig(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Class<? extends ServerChannel> serverChannelClass) {
        static ReactorConfig epoll() {
            return new ReactorConfig(new EpollEventLoopGroup(1), new EpollEventLoopGroup(), EpollServerSocketChannel.class);
        }

        static ReactorConfig kqueue() {
            return new ReactorConfig(new KQueueEventLoopGroup(1), new KQueueEventLoopGroup(), KQueueServerSocketChannel.class);
        }

        public static ReactorConfig nio() {
            return new ReactorConfig(new NioEventLoopGroup(1), new NioEventLoopGroup(), NioServerSocketChannel.class);
        }
    }
}
