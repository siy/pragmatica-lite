package org.pragmatica.http.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.pragmatica.dns.DomainAddress;
import org.pragmatica.dns.DomainName;
import org.pragmatica.dns.DomainNameResolver;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.net.InetPort;
import org.pragmatica.net.transport.api.TransportConfiguration;
import org.pragmatica.uri.IRI;

import static org.pragmatica.dns.DomainName.*;
import static org.pragmatica.http.HttpError.httpError;
import static org.pragmatica.http.protocol.HttpStatus.NOT_IMPLEMENTED;

//TODO: add customisation for name resolver
@SuppressWarnings("unused")
public interface HttpClient {
    static HttpClient create(HttpClientConfiguration configuration) {
        var bootstrap = TransportConfiguration.transportConfiguration()
                                              .clientBootstrap().handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    var pipeline = ch.pipeline();

                    // SSL is configured on per-request basis
                    //        configuration.sslContext()
                    //                     .onPresent(sslCtx -> pipeline.addLast(sslCtx.newHandler(ch.alloc())));
                    pipeline.addLast(new HttpClientCodec());

                    // TODO: consider adding support for compression
                    pipeline.addLast(new HttpContentDecompressor());
                    pipeline.addLast(new HttpObjectAggregator(1048576));
                    // Individual handler is added on per-request basis
                    //
                    //        pipeline.addLast(new HttpSnoopClientHandler());

                    // SSL is configured on per-request basis
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

//        bootstrap().connect(address.ip(), calculatePort(request.iri()))
//                   .addListener();

        return connect(address, calculatePort(request.iri()));
    }

    private int calculatePort(IRI iri) {
        return iri.port()
                  .map(InetPort::port)
                  .map(Integer::valueOf)
                  .or(() -> iri.isSecure() ? 443 : 80);
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
}
