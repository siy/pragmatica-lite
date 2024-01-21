package org.pragmatica.http.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

import java.nio.charset.StandardCharsets;

import static org.pragmatica.dns.DomainName.domainName;
import static org.pragmatica.http.HttpError.httpError;
import static org.pragmatica.http.protocol.HttpStatus.NOT_IMPLEMENTED;
import static org.pragmatica.lang.Promise.resolved;
import static org.pragmatica.lang.Result.*;

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
    private static final HttpError MISSING_CUSTOM_CODEC_ERROR = HttpError.httpError(NOT_IMPLEMENTED, "Custom codec is missing in configuration");
    private static final HttpError NON_BINARY_VALUE_ERROR = HttpError.httpError(NOT_IMPLEMENTED, "Content type is binary, but value is not a byte array or ByteBuf");
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
                                 .flatMap(address -> setupRequest(request, address, domain));
    }

    private <T> Promise<HttpClientResponse> setupRequest(HttpClientRequest<T> request, DomainAddress address, DomainName domain) {
        return connect(address, calculatePort(request.iri()))
            .flatMap(channel -> sendRequest(channel, request, domain));
    }

    Promise<Channel> connect(DomainAddress address, int port) {
        return Promise.promise(promise -> bootstrap()
            .connect(address.ip(), port)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    promise.succeed(future.channel());
                } else {
                    promise.fail(Causes.fromThrowable(future.cause()));
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

    private <T> Promise<HttpClientResponse> sendRequest(Channel channel, HttpClientRequest<T> request, DomainName domain) {
        var serializedRequest = configureConnection(channel, request.iri().isSecure())
            .flatMap(_ -> serializeRequest(request, domain));

        return resolved(serializedRequest)
            .flatMap(requestContent -> Promise.promise(promise -> {
                channel.pipeline()
                       .addLast(new ContextHandler<>(promise, request, configuration));
                channel.write(requestContent);
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
            .mapToUnit();
    }

    private static void addSslHandler(Channel channel, SslContext sslContext) {
        channel.pipeline()
               .addFirst(sslContext.newHandler(channel.alloc()));
    }

    private <T> Result<HttpRequest> serializeRequest(HttpClientRequest<T> request, DomainName domain) {
        var uri = request.parameters()
                         .map(params -> request.iri().withQuery(params))
                         .or(request::iri)
                         .pathAndQuery()
                         .toString();

        return request.body()
                      .map(value -> serializeBody(request, value))
                      .or(success(Unpooled.EMPTY_BUFFER))
                      .map(body -> new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, request.method().into(), uri, body))
                      .map(content -> appendHeaders(content, request, domain));
    }

    private static <T> HttpRequest appendHeaders(HttpRequest content, HttpClientRequest<T> request,
                                                 DomainName domain) {
        var requestHeaders = content.headers();

        requestHeaders
            .set(HttpHeaderNames.HOST, domain.name())
            .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE); //TODO: add support for keep-alive
        request.headers()
               .expanded()
               .forEach(pair -> pair.map((name, value) -> requestHeaders.add(name.asString(), value)));

        return content;
    }

    private <T> Result<ByteBuf> serializeBody(HttpClientRequest<T> request, T value) {
        return switch (request.contentType().category()) {
            case PLAIN_TEXT -> success(Unpooled.wrappedBuffer(value.toString().getBytes(StandardCharsets.UTF_8)));
            case JSON -> configuration().jsonCodec().serialize(value);
            case BINARY -> ensureByteArray(value);
            case CUSTOM -> configuration().customCodec()
                                          .map(codec -> codec.serialize(value, request.contentType()))
                                          .or(() -> failure(MISSING_CUSTOM_CODEC_ERROR));
        };
    }

    private static <T> Result<ByteBuf> ensureByteArray(T value) {
        return switch (value) {
            case byte[] bytes -> Result.success(Unpooled.wrappedBuffer(bytes));
            case ByteBuf byteBuf -> Result.success(byteBuf);
            default -> NON_BINARY_VALUE_ERROR.result();
        };
    }
}

class ContextHandler<T> extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final Promise<HttpClientResponse> promise;
    private final HttpClientRequest<T> request;
    private final HttpClientConfiguration configuration;

    public ContextHandler(Promise<HttpClientResponse> promise, HttpClientRequest<T> request, HttpClientConfiguration configuration) {
        this.promise = promise;
        this.request = request;
        this.configuration = configuration;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        var response = HttpClientResponse.httpClientResponse(msg, configuration);
        promise.succeed(response);
        ctx.close();    //TODO: add support for keep-alive
    }
}