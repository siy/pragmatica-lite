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

package org.pragmatica.net.tcp;

import org.pragmatica.lang.Option;

/// Configuration for TCP server.
///
/// @param name          server name for logging
/// @param port          port to bind to
/// @param tls           optional TLS configuration for incoming connections (server-side TLS)
/// @param clientTls     optional TLS configuration for outgoing connections (client-side TLS)
/// @param socketOptions socket-level options
public record ServerConfig(String name,
                           int port,
                           Option<TlsConfig> tls,
                           Option<TlsConfig> clientTls,
                           SocketOptions socketOptions) {
    public static ServerConfig serverConfig(String name, int port) {
        return new ServerConfig(name, port, Option.empty(), Option.empty(), SocketOptions.defaults());
    }

    public static ServerConfig serverConfig(String name, int port, TlsConfig tls) {
        return new ServerConfig(name, port, Option.some(tls), Option.empty(), SocketOptions.defaults());
    }

    public ServerConfig withTls(TlsConfig tls) {
        return new ServerConfig(name, port, Option.some(tls), clientTls, socketOptions);
    }

    /// Configure TLS for outgoing connections (when this server connects to other servers).
    ///
    /// @param clientTls TLS configuration for client-side connections
    /// @return new config with client TLS
    public ServerConfig withClientTls(TlsConfig clientTls) {
        return new ServerConfig(name, port, tls, Option.some(clientTls), socketOptions);
    }

    public ServerConfig withSocketOptions(SocketOptions socketOptions) {
        return new ServerConfig(name, port, tls, clientTls, socketOptions);
    }
}
