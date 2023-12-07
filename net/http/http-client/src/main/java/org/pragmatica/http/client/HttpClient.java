package org.pragmatica.http.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.pragmatica.dns.DomainAddress;
import org.pragmatica.dns.DomainName;
import org.pragmatica.dns.DomainNameResolver;
import org.pragmatica.http.HttpError;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.net.InetPort;
import org.pragmatica.net.transport.api.TransportConfiguration;
import org.pragmatica.uri.IRI;

import static org.pragmatica.dns.DomainName.*;
import static org.pragmatica.http.HttpError.httpError;
import static org.pragmatica.http.protocol.HttpStatus.NOT_IMPLEMENTED;
import static org.pragmatica.lang.Unit.unit;
import static org.pragmatica.lang.Unit.unitResult;

//TODO: add customisation for name resolver
@SuppressWarnings("unused")
public interface HttpClient {
    static HttpClient create(HttpClientConfiguration configuration) {
        var bootstrap = TransportConfiguration
            .transportConfiguration()
            .clientBootstrap()
            .option(ChannelOption.SO_RCVBUF, configuration.receiveBufferSize())
            .option(ChannelOption.SO_SNDBUF, configuration.sendBufferSize())
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    var pipeline = ch.pipeline();

                    pipeline.addLast(new HttpClientCodec());

                    // TODO: consider adding support for compression
                    // pipeline.addLast(new HttpContentDecompressor());
                    pipeline.addLast(new HttpObjectAggregator(configuration.maxContentLen()));
                }
            });

        return new HttpClientImpl(configuration, bootstrap);
    }

    <T> Promise<HttpClientResponse> call(HttpClientRequest<T> request);
}

record HttpClientImpl(HttpClientConfiguration configuration, Bootstrap bootstrap) implements HttpClient {
    private static final HttpError ONLY_HTTP_IS_SUPPORTED = httpError(NOT_IMPLEMENTED, "Only HTTP is supported");
    private static final HttpError ONLY_ABSOLUTE_IRI_IS_SUPPORTED = httpError(NOT_IMPLEMENTED, "Only HTTP is supported");
    private static final DomainName LOCALHOST = domainName("localhost");

    @Override
    public <T> Promise<HttpClientResponse> call(HttpClientRequest<T> request) {
        if (!request.iri().isAbsolute()) {
            return Promise.failed(ONLY_ABSOLUTE_IRI_IS_SUPPORTED);
        }

        if (!request.iri().isHttp()) {
            return Promise.failed(ONLY_HTTP_IS_SUPPORTED);
        }

        var domain = request.iri()
                            .domain()
                            .or(LOCALHOST); // actually newer happens, iri.isAbsolute() filters out IRIs with missing domain

        return DomainNameResolver.defaultResolver()
                                 .resolve(domain)
                                 .flatMap(address -> setupRequest(request, address));
    }

    private <T> Promise<HttpClientResponse> setupRequest(HttpClientRequest<T> request, DomainAddress address) {
        return connect(address, calculatePort(request.iri()))
            .flatMap(channel -> sendRequest(channel, request));
    }

    Promise<Channel> connect(DomainAddress address, int port) {
        return Promise.promise(promise -> bootstrap()
            .connect(address.ip(), port)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    promise.success(future.channel());
                } else {
                    promise.failure(Causes.fromThrowable(future.cause()));
                }
            }));
    }

    private int calculatePort(IRI iri) {
        return iri.port()
                  .map(InetPort::port)
                  .map(Integer::valueOf)
                  .or(() -> iri.isSecure() ? 443 : 80);
    }

    private static Result<SslContext> newUntrustedContext() {
        return Result.lift(Causes::fromThrowable,
                           () -> SslContextBuilder.forClient()
                                                  .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                                  .build());
    }

    private <T> Promise<HttpClientResponse> sendRequest(Channel channel, HttpClientRequest<T> request) {
        return Promise.resolved(configureConnection(channel, request.iri().isSecure()))
                      .flatMap(_ -> Promise.promise(promise -> {
                          channel.pipeline()
                                 .addLast(new ContextHandler<>(promise, request));
                          channel.write(serializeRequest(request));
                      }));
    }

    private Result<Unit> configureConnection(Channel channel, boolean secure) {
        if (!secure) {
            return unitResult();
        }

        if (configuration().sslContext().isPresent()) {
            configuration().sslContext()
                           .onPresent(sslContext -> addSslHandler(channel, sslContext));
            return unitResult();
        }

        return newUntrustedContext()
            .onSuccess(sslContext -> addSslHandler(channel, sslContext))
            .map(Unit::unit);
    }

    private static ChannelPipeline addSslHandler(Channel channel, SslContext sslContext) {
        return channel.pipeline()
                      .addFirst(sslContext.newHandler(channel.alloc()));
    }

    private <T> ByteBuf serializeRequest(HttpClientRequest<T> request) {
        throw new UnsupportedOperationException();
    }
}

class ContextHandler<T> extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final Promise<HttpClientResponse> promise;
    private final HttpClientRequest<T> request;

    public ContextHandler(Promise<HttpClientResponse> promise, HttpClientRequest<T> request) {
        this.promise = promise;
        this.request = request;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        throw new UnsupportedOperationException();
    }
}