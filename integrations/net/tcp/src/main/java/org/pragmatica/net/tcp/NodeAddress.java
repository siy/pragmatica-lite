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
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.utils.Causes;

import java.net.InetSocketAddress;

/// Network node address (host and port).
public record NodeAddress(String host, int port) {
    private static final Cause BLANK_HOST = Causes.cause("Host must not be blank");
    private static final Cause INVALID_PORT = Causes.cause("Port must be between 1 and 65535");

    public String asString() {
        return host + ":" + port;
    }

    public static Result<NodeAddress> nodeAddress(String host, int port) {
        return Result.all(Verify.ensure(host, Verify.Is::notBlank, BLANK_HOST),
                          Verify.ensure(port, Verify.Is::between, 1, 65535, INVALID_PORT))
                     .map(NodeAddress::new);
    }

    public static Result<NodeAddress> nodeAddress(InetSocketAddress socketAddress) {
        if (socketAddress == null) {
            return BLANK_HOST.result();
        }
        // Prefer getHostAddress() for consistent IP representation; fall back to getHostString() for unresolved
        var address = socketAddress.getAddress();
        var host = address != null
                   ? address.getHostAddress()
                   : socketAddress.getHostString();
        return nodeAddress(host, socketAddress.getPort());
    }
}
