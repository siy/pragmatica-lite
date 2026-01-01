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
/**
 * Socket-level options for server configuration.
 *
 * @param soBacklog   maximum queue length for incoming connection requests (SO_BACKLOG)
 * @param soKeepalive whether to enable TCP keepalive probes (SO_KEEPALIVE)
 */
public record SocketOptions(int soBacklog, boolean soKeepalive) {
    public static SocketOptions socketOptions(int soBacklog, boolean soKeepalive) {
        return new SocketOptions(soBacklog, soKeepalive);
    }

    public static SocketOptions socketOptions() {
        return defaults();
    }

    public static SocketOptions defaults() {
        return new SocketOptions(128, true);
    }

    public SocketOptions withSoBacklog(int soBacklog) {
        return new SocketOptions(soBacklog, soKeepalive);
    }

    public SocketOptions withSoKeepalive(boolean soKeepalive) {
        return new SocketOptions(soBacklog, soKeepalive);
    }
}
