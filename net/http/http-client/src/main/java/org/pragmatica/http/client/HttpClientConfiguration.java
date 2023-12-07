package org.pragmatica.http.client;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.ssl.SslContext;
import org.pragmatica.http.codec.JsonCodec;
import org.pragmatica.codec.json.JsonCodecFactory;
import org.pragmatica.lang.Option;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

//TODO: add customisation for name resolver
public interface HttpClientConfiguration {
    int DEFAULT_PORT = 8000;
    int DEFAULT_RECEIVE_BUFFER_SIZE = 32768; // 32KB
    int DEFAULT_SEND_BUFFER_SIZE = 1048576; // 1MB
    int DEFAULT_MAX_CONTENT_LEN = 2097152; // 2MB
    boolean DEFAULT_NATIVE_TRANSPORT = true;

    static HttpClientConfiguration allDefaults() {
        record configuration(int port, int sendBufferSize,
                             int receiveBufferSize, int maxContentLen, boolean nativeTransport,
                             JsonCodec jsonCodec, Option<SslContext> sslContext, Option<CorsConfig> corsConfig) implements HttpClientConfiguration {
            @Override
            public HttpClientConfiguration withPort(int port) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpClientConfiguration withSendBufferSize(int sendBufferSize) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpClientConfiguration withReceiveBufferSize(int receiveBufferSize) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpClientConfiguration withMaxContentLen(int maxContentLen) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpClientConfiguration withNativeTransport(boolean nativeTransport) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, corsConfig);
            }

            @Override
            public HttpClientConfiguration withSerializer(JsonCodec serializer) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         serializer, sslContext, corsConfig);
            }

            @Override
            public HttpClientConfiguration withSslContext(SslContext sslContext) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, some(sslContext), corsConfig);
            }

            @Override
            public HttpClientConfiguration withCorsConfig(CorsConfig corsConfig) {
                return new configuration(port, sendBufferSize, receiveBufferSize, maxContentLen, nativeTransport,
                                         jsonCodec, sslContext, some(corsConfig));
            }
        }

        return new configuration(DEFAULT_PORT, DEFAULT_SEND_BUFFER_SIZE, DEFAULT_RECEIVE_BUFFER_SIZE,
                                 DEFAULT_MAX_CONTENT_LEN, DEFAULT_NATIVE_TRANSPORT,
                                 JsonCodecFactory.defaultFactory().withDefaultConfiguration(), none(), none());
    }

    int port();

    int sendBufferSize();

    int receiveBufferSize();

    int maxContentLen();

    boolean nativeTransport();

    JsonCodec jsonCodec();

    Option<SslContext> sslContext();

    Option<CorsConfig> corsConfig();

    HttpClientConfiguration withPort(int port);

    HttpClientConfiguration withSendBufferSize(int sendBufferSize);

    HttpClientConfiguration withReceiveBufferSize(int receiveBufferSize);

    HttpClientConfiguration withMaxContentLen(int maxContentLen);

    HttpClientConfiguration withNativeTransport(boolean nativeTransport);

    HttpClientConfiguration withSerializer(JsonCodec jsonCodec);

    HttpClientConfiguration withSslContext(SslContext sslContext);

    HttpClientConfiguration withCorsConfig(CorsConfig corsConfig);
}
