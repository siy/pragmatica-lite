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
import org.pragmatica.net.http.HttpRequest;
import org.pragmatica.net.http.HttpRequestBuilder;
import org.pragmatica.net.http.HttpResponse;
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
    public <T> Promise<HttpResponse<T>> send(HttpRequest<T> request) {
        if (!started.get()) {
            return Promise.failure(org.pragmatica.net.http.HttpError.httpError(0, "Client not started", "HTTP client must be started before sending requests"));
        }
        
        return NettyHttpRequestExecutor.execute(bootstrap, request, config);
    }
    
    @Override
    public HttpRequestBuilder request() {
        return HttpRequestBuilder.create(this);
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