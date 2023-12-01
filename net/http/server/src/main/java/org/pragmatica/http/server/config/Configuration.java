package org.pragmatica.http.server.config;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.ssl.SslContext;
import org.pragmatica.codec.json.JsonCodec;
import org.pragmatica.codec.json.JsonCodecFactory;
import org.pragmatica.lang.Option;

import java.net.InetAddress;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

//TODO: support for HTTP/2, compression,
@SuppressWarnings("unused")
public interface Configuration {
    int DEFAULT_PORT = 8000;
    int DEFAULT_RECEIVE_BUFFER_SIZE = 32768; // 32KB
    int DEFAULT_SEND_BUFFER_SIZE = 1048576; // 1MB
    int DEFAULT_MAX_CONTENT_LEN = 2097152; // 2MB
    boolean DEFAULT_NATIVE_TRANSPORT = true;

    static Configuration allDefaults() {
        record configuration(int port, Option<InetAddress> bindAddress, int sendBufferSize,
                             int receiveBufferSize, int maxContentLen, boolean nativeTransport,
                             JsonCodec<?> jsonCodec, Option<SslContext> sslContext, Option<CorsConfig> corsConfig) implements Configuration {
            @Override
            public Configuration withPort(int port) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public Configuration withBindAddress(InetAddress host) {
                return new configuration(port, some(host), sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public Configuration withSendBufferSize(int sendBufferSize) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public Configuration withReceiveBufferSize(int receiveBufferSize) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public Configuration withMaxContentLen(int maxContentLen) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public Configuration withNativeTransport(boolean nativeTransport) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public Configuration withSerializer(JsonCodec serializer) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         serializer, sslContext, corsConfig);
            }

            @Override
            public Configuration withSslContext(SslContext sslContext) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, some(sslContext), corsConfig);
            }

            @Override
            public Configuration withCorsConfig(CorsConfig corsConfig) {
                return new configuration(port, bindAddress, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, some(corsConfig));
            }
        }

        return new configuration(DEFAULT_PORT, none(), DEFAULT_SEND_BUFFER_SIZE, DEFAULT_RECEIVE_BUFFER_SIZE,
                                 DEFAULT_MAX_CONTENT_LEN, DEFAULT_NATIVE_TRANSPORT,
                                 JsonCodecFactory.defaultFactory().withDefaultConfiguration(), none(), none());
    }

    int port();

    Option<InetAddress> bindAddress();

    int sendBufferSize();

    int receiveBufferSize();

    int maxContentLen();

    boolean nativeTransport();

    JsonCodec<?> jsonCodec();

    Option<SslContext> sslContext();

    Option<CorsConfig> corsConfig();

    Configuration withPort(int port);

    Configuration withBindAddress(InetAddress host);

    Configuration withSendBufferSize(int sendBufferSize);

    Configuration withReceiveBufferSize(int receiveBufferSize);

    Configuration withMaxContentLen(int maxContentLen);

    Configuration withNativeTransport(boolean nativeTransport);

    Configuration withSerializer(JsonCodec jsonCodec);

    Configuration withSslContext(SslContext sslContext);

    Configuration withCorsConfig(CorsConfig corsConfig);
}
