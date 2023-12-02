package org.pragmatica.http.server;

import io.netty.channel.ChannelOption;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.pragmatica.http.error.WebError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.config.WebServerConfiguration;
import org.pragmatica.http.server.impl.WebServerInitializer;
import org.pragmatica.http.server.routing.RequestRouter;
import org.pragmatica.http.server.routing.RouteSource;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.net.transport.api.ReactorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static org.pragmatica.lang.Unit.unitResult;
import static org.pragmatica.net.transport.api.ReactorConfiguration.reactorConfiguration;

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
    private final WebServerConfiguration configuration;

    private WebServer(WebServerConfiguration configuration, RequestRouter requestRouter) {
        this.configuration = configuration;
        this.requestRouter = requestRouter;
    }

    @FunctionalInterface
    public interface Builder {
        WebServer serve(RouteSource... routeSources);
    }

    public static Builder with(WebServerConfiguration configuration) {
        return (RouteSource... routeSources) -> new WebServer(configuration, RequestRouter.with(routeSources));
    }

    public Promise<Unit> start() {
        var reactorConfig = reactorConfiguration();
        var promise = Promise.<Unit>promise()
                             .onResultDo(() -> gracefulShutdown(reactorConfig));

        try {
            var bindAddress = configuration.bindAddress()
                                           .map(address -> new InetSocketAddress(address, configuration.port()))
                                           .or(() -> new InetSocketAddress(configuration.port()));

            log.info("Starting WebServer on {} using {} transport", bindAddress, reactorConfig.name());

            requestRouter.print();

            reactorConfig
                .serverBootstrap()
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
    private void gracefulShutdown(ReactorConfiguration reactorConfig) {
        reactorConfig.bossGroup().shutdownGracefully();
        reactorConfig.workerGroup().shutdownGracefully();
    }

    private void decode(Promise<Unit> promise, Future<? super Void> future) {
        promise.resolve(future.isSuccess()
                        ? unitResult()
                        : Causes.fromThrowable(future.cause()).result());
    }
}
