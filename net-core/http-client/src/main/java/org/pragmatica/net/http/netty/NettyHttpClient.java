package org.pragmatica.net.http.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.net.http.HttpClient;
import org.pragmatica.net.http.HttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public final class NettyHttpClient implements HttpClient {
    private static final Logger log = LoggerFactory.getLogger(NettyHttpClient.class);
    
    private final HttpClientConfig config;
    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    public NettyHttpClient(HttpClientConfig config) {
        this.config = config;
        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = createBootstrap();
    }
    
    private Bootstrap createBootstrap() {
        return new Bootstrap()
            .group(eventLoopGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeout().millis())
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true);
    }
    
    @Override
    public <T, R> Promise<org.pragmatica.net.http.HttpResponse<R>> exchange(org.pragmatica.net.http.HttpRequest<T, R> request) {
        if (!started.get()) {
            return Promise.failure(org.pragmatica.net.http.HttpError.ConfigurationError.create("Client not started"));
        }
        
        return NettyHttpRequestExecutor.execute(bootstrap, request, config);
    }
    
    @Override
    public org.pragmatica.net.http.HttpRequestBuilder request() {
        return org.pragmatica.net.http.HttpRequestBuilder.create(this);
    }
    
    
    @Override
    public Promise<Unit> start() {
        return Promise.lift(() -> {
            if (started.compareAndSet(false, true)) {
                log.info("Starting Netty HTTP client");
                return Unit.unit();
            } else {
                throw new IllegalStateException("HTTP client already started");
            }
        });
    }
    
    @Override
    public Promise<Unit> stop() {
        return Promise.lift(() -> {
            if (started.compareAndSet(true, false)) {
                log.info("Stopping Netty HTTP client");
                eventLoopGroup.shutdownGracefully().sync();
                return Unit.unit();
            } else {
                throw new IllegalStateException("HTTP client not started");
            }
        });
    }
    
    public HttpClientConfig config() {
        return config;
    }
}