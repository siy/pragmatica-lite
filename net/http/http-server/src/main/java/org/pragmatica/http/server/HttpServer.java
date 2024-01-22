package org.pragmatica.http.server;

import io.netty.channel.ChannelOption;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.impl.HttpServerInitializer;
import org.pragmatica.http.server.routing.RequestRouter;
import org.pragmatica.http.server.routing.RouteSource;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.net.transport.api.TransportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static org.pragmatica.lang.Result.unitResult;
import static org.pragmatica.net.transport.api.TransportConfiguration.transportConfiguration;

//TODO: injection of routes via SPI (metrics, health, etc.)
//TODO: structured error responses. See https://www.rfc-editor.org/rfc/rfc7807 for more details
//TODO: Support for HTTP/2
public class HttpServer {
    static {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);

        if (System.getProperty("io.netty.leakDetection.level") == null &&
            System.getProperty("io.netty.leakDetectionLevel") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    private final RequestRouter requestRouter;
    private final HttpServerConfig configuration;

    private HttpServer(HttpServerConfig configuration, RequestRouter requestRouter) {
        this.configuration = configuration;
        this.requestRouter = requestRouter;
    }

    @FunctionalInterface
    public interface Builder {
        HttpServer serve(RouteSource... routeSources);

        default Result<Unit> serveNow(RouteSource... routeSources) {
            return serve(routeSources).start().await();
        }
    }

    public static Builder withConfig(HttpServerConfig configuration) {
        return (RouteSource... routeSources) -> new HttpServer(configuration, RequestRouter.with(routeSources));
    }

    public interface HttpServerRunner {
        Result<Unit> serveNow(RouteSource... routeSources);
    }

    public static Result<Unit> with(HttpServerConfig configuration, RouteSource... routeSources) {
        return withConfig(configuration).serve(routeSources).startNow();
    }

    public Result<Unit> startNow() {
        return start().await();
    }

    public Promise<Unit> start() {
        var transportConfiguration = transportConfiguration();
        var promise = Promise.<Unit>promise()
                             .onResultRun(() -> gracefulShutdown(transportConfiguration))
                             .onFailure(cause -> log.error("Failed to start server: {}", cause));

        try {
            var bindAddress = configuration.bindAddress()
                                           .map(address -> new InetSocketAddress(address, configuration.port()))
                                           .or(() -> new InetSocketAddress(configuration.port()));

            log.info("Starting server on {} using {} transport", bindAddress, transportConfiguration.name());

            requestRouter.print();

            transportConfiguration
                .serverBootstrap()
                .childOption(ChannelOption.SO_RCVBUF, configuration.receiveBufferSize())
                .childOption(ChannelOption.SO_SNDBUF, configuration.sendBufferSize())
                .childHandler(new HttpServerInitializer(configuration, requestRouter))
                .bind(bindAddress)
                .sync()
                .channel()
                .closeFuture()
                .addListener(future -> decode(promise, future));

            log.info("Server started on port {}", configuration.port());
        } catch (InterruptedException e) {
            //In rare cases when .sync() will be interrupted, fail with error
            log.error("Failed to start server", e);
            promise.resolve(HttpError.httpError(HttpStatus.SERVICE_UNAVAILABLE, e).result());
        }

        return promise;
    }

    @SuppressWarnings("resource")
    private void gracefulShutdown(TransportConfiguration reactorConfig) {
        reactorConfig.bossGroup().shutdownGracefully();
        reactorConfig.workerGroup().shutdownGracefully();
    }

    private void decode(Promise<Unit> promise, Future<? super Void> future) {
        promise.resolve(future.isSuccess()
                        ? unitResult()
                        : Causes.fromThrowable(future.cause()).result());
    }
}
