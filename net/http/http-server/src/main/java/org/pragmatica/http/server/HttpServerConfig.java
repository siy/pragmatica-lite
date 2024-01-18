package org.pragmatica.http.server;

import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.ssl.SslContext;
import org.pragmatica.http.codec.CustomCodec;
import org.pragmatica.http.codec.JsonCodec;
import org.pragmatica.lang.Option;

import java.net.InetAddress;

import static org.pragmatica.lang.Option.none;

//TODO: support for HTTP/2, compression, WebSocket, SSE, etc., etc.
public record HttpServerConfig(int port, Option<InetAddress> bindAddress, int sendBufferSize,
                               int receiveBufferSize, int maxContentLen, boolean nativeTransport,
                               Option<CustomCodec> customCodec,
                               Option<JsonCodec> jsonCodec,
                               Option<SslContext> sslContext, Option<CorsConfig> corsConfig) implements HttpServerConfigTemplate.With {
    public static final int DEFAULT_PORT = 8000;
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 32768; // 32KB
    public static final int DEFAULT_SEND_BUFFER_SIZE = 65536; // 64KB
    public static final int DEFAULT_MAX_CONTENT_LEN = 262144; // 256KB
    public static final boolean DEFAULT_NATIVE_TRANSPORT = true;

    public static HttpServerConfig defaultConfiguration() {
        return new HttpServerConfig(DEFAULT_PORT, none(), DEFAULT_SEND_BUFFER_SIZE, DEFAULT_RECEIVE_BUFFER_SIZE,
                                    DEFAULT_MAX_CONTENT_LEN, DEFAULT_NATIVE_TRANSPORT,
                                    none(), none(), none(), none());
    }
}
