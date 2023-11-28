package org.pragmatica.http.server;

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
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.config.Configuration;
import org.pragmatica.http.server.error.WebError;
import org.pragmatica.http.server.routing.RequestRouter;
import org.pragmatica.http.server.routing.RouteSource;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static org.pragmatica.lang.Unit.unitResult;

//TODO: Support for HTTP/2
public class WebServer {
    static {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        if (System.getProperty("io.netty.leakDetection.level") == null &&
            System.getProperty("io.netty.leakDetectionLevel") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(WebServer.class);
    private final RequestRouter requestRouter;
    private final Configuration configuration;

    private WebServer(Configuration configuration, RequestRouter requestRouter) {
        this.configuration = configuration;
        this.requestRouter = requestRouter;
    }

    @FunctionalInterface
    public interface Builder {
        WebServer serve(RouteSource... routeSources);
    }

    public static Builder with(Configuration configuration) {
        return (RouteSource... routeSources) -> new WebServer(configuration, RequestRouter.with(routeSources));
    }

    public Promise<Unit> start() {
        var reactorConfig = configureReactor();

        var promise = Promise.<Unit>promise().onResultDo(() -> gracefulShutdown(reactorConfig));

        try {
            var bindAddress = configuration.bindAddress()
                                           .map(address -> new InetSocketAddress(address, configuration.port()))
                                           .or(() -> new InetSocketAddress(configuration.port()));

            log.info("Starting WebServer on {}", bindAddress);

            requestRouter.print();

            configureBootstrap(reactorConfig)
                .childOption(ChannelOption.SO_RCVBUF, configuration.receiveBufferSize())
                .childOption(ChannelOption.SO_SNDBUF, configuration.sendBufferSize())
                .childHandler(new WebServerInitializer(configuration, requestRouter))
                .bind(bindAddress)
                .sync()
                .channel()
                .closeFuture()
                .addListener(future -> decode(promise, future));
            log.info("WebServer started on port {}", configuration.port());
        } catch (InterruptedException e) {
            //In rare cases when .sync() will be interrupted, fail with error
            log.error("Failed to start WebServer", e);
            promise.resolve(WebError.fromThrowable(HttpStatus.SERVICE_UNAVAILABLE, e).result());
        }

        return promise;
    }

    @SuppressWarnings("resource")
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
        if (Epoll.isAvailable() && configuration.nativeTransport()) {
            log.info("Using epoll native transport");
            return ReactorConfig.epoll();
        } else if (KQueue.isAvailable() && configuration.nativeTransport()) {
            log.info("Using kqueue native transport");
            return ReactorConfig.kqueue();
        } else {
            log.info("Using NIO transport");
            return ReactorConfig.nio();
        }
    }

    private void decode(Promise<Unit> promise, Future<? super Void> future) {
        promise.resolve(future.isSuccess()
                        ? unitResult()
                        : Causes.fromThrowable(future.cause()).result());
    }

    record ReactorConfig(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Class<? extends ServerChannel> serverChannelClass) {
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
