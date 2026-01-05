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

import java.net.InetSocketAddress;

/// Network node address (host and port).
public interface NodeAddress {
    String host();

    int port();

    default String asString() {
        return host() + ":" + port();
    }

    static NodeAddress nodeAddress(String host, int port) {
        record nodeAddress(String host, int port) implements NodeAddress {}
        return new nodeAddress(host, port);
    }

    static NodeAddress nodeAddress(InetSocketAddress socketAddress) {
        // Use getHostAddress() to get consistent IP representation (e.g., "127.0.0.1" not "localhost")
        return nodeAddress(socketAddress.getAddress()
                                        .getHostAddress(),
                           socketAddress.getPort());
    }
}
