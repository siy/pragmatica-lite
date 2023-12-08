package org.pragmatica.http.server;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.ssl.SslContext;
import org.pragmatica.http.codec.CustomCodec;
import org.pragmatica.http.codec.JsonCodec;
import org.pragmatica.codec.json.JsonCodecFactory;
import org.pragmatica.lang.Option;

import java.net.InetAddress;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

//TODO: support for HTTP/2, compression,
@SuppressWarnings("unused")
public interface HttpServerConfiguration {
    int DEFAULT_PORT = 8000;
    int DEFAULT_RECEIVE_BUFFER_SIZE = 32768; // 32KB
    int DEFAULT_SEND_BUFFER_SIZE = 1048576; // 1MB
    int DEFAULT_MAX_CONTENT_LEN = 2097152; // 2MB
    boolean DEFAULT_NATIVE_TRANSPORT = true;

    static HttpServerConfiguration allDefaults() {
        record configuration(int port, Option<InetAddress> bindAddress, int sendBufferSize,
                             int receiveBufferSize, int maxContentLen, boolean nativeTransport,
                             Option<CustomCodec> customCodec,
                             JsonCodec jsonCodec,
                             Option<SslContext> sslContext, Option<CorsConfig> corsConfig) implements HttpServerConfiguration {
            @Override
            public HttpServerConfiguration withPort(int port) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withBindAddress(InetAddress host) {
                return new configuration(port, some(host), sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withSendBufferSize(int sendBufferSize) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withReceiveBufferSize(int receiveBufferSize) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withMaxContentLen(int maxContentLen) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withNativeTransport(boolean nativeTransport) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withJsonCodec(JsonCodec jsonCodec) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withCustomCodec(CustomCodec customCodec) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         some(customCodec), configuration.this.jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpServerConfiguration withSslContext(SslContext sslContext) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, some(sslContext), corsConfig);
            }

            @Override
            public HttpServerConfiguration withCorsConfig(CorsConfig corsConfig) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         customCodec, jsonCodec, sslContext, some(corsConfig));
            }
        }

        return new configuration(DEFAULT_PORT, none(), DEFAULT_SEND_BUFFER_SIZE, DEFAULT_RECEIVE_BUFFER_SIZE,
                                 DEFAULT_MAX_CONTENT_LEN, DEFAULT_NATIVE_TRANSPORT,
                                 none(), JsonCodecFactory.defaultFactory().withDefaultConfiguration(), none(), none());
    }

    int port();

    Option<InetAddress> bindAddress();

    int sendBufferSize();

    int receiveBufferSize();

    int maxContentLen();

    boolean nativeTransport();

    JsonCodec jsonCodec();

    Option<CustomCodec> customCodec();

    Option<SslContext> sslContext();

    Option<CorsConfig> corsConfig();

    HttpServerConfiguration withPort(int port);

    HttpServerConfiguration withBindAddress(InetAddress host);

    HttpServerConfiguration withSendBufferSize(int sendBufferSize);

    HttpServerConfiguration withReceiveBufferSize(int receiveBufferSize);

    HttpServerConfiguration withMaxContentLen(int maxContentLen);

    HttpServerConfiguration withNativeTransport(boolean nativeTransport);

    HttpServerConfiguration withJsonCodec(JsonCodec jsonCodec);

    HttpServerConfiguration withCustomCodec(CustomCodec customCodec);

    HttpServerConfiguration withSslContext(SslContext sslContext);

    HttpServerConfiguration withCorsConfig(CorsConfig corsConfig);
}
