/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.http.server;

import org.pragmatica.http.websocket.WebSocketEndpoint;
import org.pragmatica.lang.Option;
import org.pragmatica.net.tcp.SocketOptions;
import org.pragmatica.net.tcp.TlsConfig;

import java.util.ArrayList;
import java.util.List;

/// Configuration for HTTP server.
///
/// @param name                 server name for logging
/// @param port                 port to bind to
/// @param tls                  optional TLS configuration
/// @param maxContentLength     maximum content length for requests
/// @param webSocketEndpoints   WebSocket endpoints
/// @param chunkedWriteEnabled  whether to enable chunked transfer encoding
/// @param socketOptions        socket-level options
public record HttpServerConfig(String name,
                               int port,
                               Option<TlsConfig> tls,
                               int maxContentLength,
                               List<WebSocketEndpoint> webSocketEndpoints,
                               boolean chunkedWriteEnabled,
                               SocketOptions socketOptions) {
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 65536;

    public static HttpServerConfig httpServerConfig(String name, int port) {
        return new HttpServerConfig(name,
                                    port,
                                    Option.empty(),
                                    DEFAULT_MAX_CONTENT_LENGTH,
                                    List.of(),
                                    false,
                                    SocketOptions.defaults());
    }

    public HttpServerConfig withTls(TlsConfig tls) {
        return new HttpServerConfig(name,
                                    port,
                                    Option.some(tls),
                                    maxContentLength,
                                    webSocketEndpoints,
                                    chunkedWriteEnabled,
                                    socketOptions);
    }

    public HttpServerConfig withMaxContentLength(int maxContentLength) {
        return new HttpServerConfig(name,
                                    port,
                                    tls,
                                    maxContentLength,
                                    webSocketEndpoints,
                                    chunkedWriteEnabled,
                                    socketOptions);
    }

    public HttpServerConfig withWebSocket(WebSocketEndpoint endpoint) {
        var endpoints = new ArrayList<>(webSocketEndpoints);
        endpoints.add(endpoint);
        return new HttpServerConfig(name,
                                    port,
                                    tls,
                                    maxContentLength,
                                    List.copyOf(endpoints),
                                    chunkedWriteEnabled,
                                    socketOptions);
    }

    public HttpServerConfig withChunkedWrite() {
        return new HttpServerConfig(name, port, tls, maxContentLength, webSocketEndpoints, true, socketOptions);
    }

    public HttpServerConfig withSocketOptions(SocketOptions socketOptions) {
        return new HttpServerConfig(name,
                                    port,
                                    tls,
                                    maxContentLength,
                                    webSocketEndpoints,
                                    chunkedWriteEnabled,
                                    socketOptions);
    }
}
