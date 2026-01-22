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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

/// Socket-level options for server configuration.
///
/// @param soBacklog   maximum queue length for incoming connection requests (SO_BACKLOG)
/// @param soKeepalive whether to enable TCP keepalive probes (SO_KEEPALIVE)
/// @param tcpNoDelay  whether to disable Nagle's algorithm for low-latency (TCP_NODELAY)
public record SocketOptions(int soBacklog, boolean soKeepalive, boolean tcpNoDelay) {
    private static final Cause INVALID_BACKLOG = Causes.cause("soBacklog must be positive");
    private static final SocketOptions DEFAULT = new SocketOptions(128, true, true);

    /// Create socket options with validation.
    public static Result<SocketOptions> socketOptions(int soBacklog, boolean soKeepalive, boolean tcpNoDelay) {
        if (soBacklog <= 0) {
            return INVALID_BACKLOG.result();
        }
        return Result.success(new SocketOptions(soBacklog, soKeepalive, tcpNoDelay));
    }

    /// Get default socket options.
    public static SocketOptions socketOptions() {
        return defaults();
    }

    /// Get default socket options (backlog=128, keepalive=true, nodelay=true).
    public static SocketOptions defaults() {
        return DEFAULT;
    }

    /// Create new options with different backlog value.
    public Result<SocketOptions> withSoBacklog(int soBacklog) {
        return socketOptions(soBacklog, soKeepalive, tcpNoDelay);
    }

    /// Create new options with different keepalive setting.
    public SocketOptions withSoKeepalive(boolean soKeepalive) {
        return new SocketOptions(soBacklog, soKeepalive, tcpNoDelay);
    }

    /// Create new options with different TCP nodelay setting.
    public SocketOptions withTcpNoDelay(boolean tcpNoDelay) {
        return new SocketOptions(soBacklog, soKeepalive, tcpNoDelay);
    }
}
