package org.pragmatica.net.http.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.net.http.HttpRequest;
import org.pragmatica.net.http.HttpResponse;
import org.pragmatica.net.http.HttpClientConfig;
import org.pragmatica.net.http.impl.HttpResponseImpl;
import org.pragmatica.net.http.HttpError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class NettyHttpRequestExecutor {
    private static final Logger log = LoggerFactory.getLogger(NettyHttpRequestExecutor.class);
    
    public static <T> Promise<HttpResponse<T>> execute(Bootstrap bootstrap, HttpRequest<T> request, HttpClientConfig config) {
        return Promise.promise(() -> {
            try {
                var uri = URI.create(request.url());
                var port = uri.getPort() == -1 ? (uri.getScheme().equals("https") ? 443 : 80) : uri.getPort();
                var ssl = uri.getScheme().equals("https");
                
                var future = new CompletableFuture<HttpResponse<T>>();
                
                var channelFuture = bootstrap.clone()
                    .handler(createChannelInitializer(ssl, uri, port, config, request, future))
                    .connect(uri.getHost(), port);
                
                channelFuture.addListener(createConnectionListener(request, uri, future));
                
                var response = future.get();
                return Result.success(response);
                
            } catch (Exception e) {
                log.error("Error executing HTTP request", e);
                return Result.failure(Causes.fromThrowable(e));
            }
        });
    }
    
    private static <T> ChannelInitializer<Channel> createChannelInitializer(
            boolean ssl, URI uri, int port, HttpClientConfig config, 
            HttpRequest<T> request, CompletableFuture<HttpResponse<T>> future) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                var pipeline = ch.pipeline();
                
                if (ssl) {
                    var sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                    pipeline.addLast(sslContext.newHandler(ch.alloc(), uri.getHost(), port));
                }
                
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new HttpObjectAggregator(1048576)); // 1MB max response size
                pipeline.addLast(new ReadTimeoutHandler(config.readTimeout().secondsAndNanos().first(), TimeUnit.SECONDS));
                pipeline.addLast(new HttpResponseHandler<>(request, future));
            }
        };
    }
    
    private static <T> ChannelFutureListener createConnectionListener(
            HttpRequest<T> request, URI uri, CompletableFuture<HttpResponse<T>> future) {
        return connectFuture -> {
            if (connectFuture.isSuccess()) {
                try {
                    var httpRequest = createNettyRequest(request, uri);
                    connectFuture.channel().writeAndFlush(httpRequest);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            } else {
                future.completeExceptionally(connectFuture.cause());
            }
        };
    }
    
    private static FullHttpRequest createNettyRequest(HttpRequest<?> request, URI uri) throws Exception {
        var nettyRequest = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            io.netty.handler.codec.http.HttpMethod.valueOf(request.method().name()),
            uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "")
        );
        
        // Set headers
        nettyRequest.headers().set(HttpHeaderNames.HOST, uri.getHost());
        nettyRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        nettyRequest.headers().set(HttpHeaderNames.USER_AGENT, "pragmatica-http-client/1.0");
        
        // Add custom headers
        for (var name : request.headers().names()) {
            for (var value : request.headers().all(name)) {
                nettyRequest.headers().add(name, value);
            }
        }
        
        // Handle request body
        if (request.body() != null) {
            var bodyBytes = serializeBody(request.body());
            nettyRequest.content().writeBytes(bodyBytes);
            nettyRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.length);
            
            // Set content type if not already set
            if (!nettyRequest.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
                nettyRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            }
        }
        
        return nettyRequest;
    }
    
    private static byte[] serializeBody(Object body) throws Exception {
        return switch (body) {
            case String str -> str.getBytes(StandardCharsets.UTF_8);
            case byte[] bytes -> bytes;
            case null, default -> {
                // For now, just convert to string - users can provide pre-serialized JSON
                yield body.toString().getBytes(StandardCharsets.UTF_8);
            }
        };
    }
    
    private static class HttpResponseHandler<T> extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final HttpRequest<T> request;
        private final CompletableFuture<HttpResponse<T>> future;
        
        public HttpResponseHandler(HttpRequest<T> request, CompletableFuture<HttpResponse<T>> future) {
            this.request = request;
            this.future = future;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
            try {
                var statusCode = response.status().code();
                var statusText = response.status().reasonPhrase();
                
                // Convert headers
                var headers = new org.pragmatica.net.http.HttpHeaders();
                for (var entry : response.headers()) {
                    headers.add(entry.getKey(), entry.getValue());
                }
                
                // Convert body
                var bodyBytes = new byte[response.content().readableBytes()];
                response.content().readBytes(bodyBytes);
                var body = deserializeBody(bodyBytes, request);
                
                var error = HttpError.fromCode(statusCode, statusText);
                var httpResponse = new HttpResponseImpl<>(error, headers, body);
                future.complete(httpResponse);
                
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                ctx.close();
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            future.completeExceptionally(cause);
            ctx.close();
        }
        
        @SuppressWarnings("unchecked")
        private T deserializeBody(byte[] bodyBytes, HttpRequest<T> request) throws Exception {
            if (bodyBytes.length == 0) {
                return null;
            }
            
            var bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
            
            // Handle different response types
            if (request.responseType() != null) {
                if (request.responseType() == String.class) {
                    return (T) bodyString;
                } else if (request.responseType() == byte[].class) {
                    return (T) bodyBytes;
                } else {
                    // For now, just return the string - users can deserialize manually
                    return (T) bodyString;
                }
            } else if (request.responseTypeToken() != null) {
                // For now, just return the string - users can deserialize manually  
                return (T) bodyString;
            } else {
                // Default to String
                return (T) bodyString;
            }
        }
    }
}