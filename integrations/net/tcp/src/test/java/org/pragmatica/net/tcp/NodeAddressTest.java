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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.net.tcp.NodeAddress.nodeAddress;

class NodeAddressTest {

    @Test
    void creates_address_with_host_and_port() {
        var address = nodeAddress("localhost", 8080);

        assertThat(address.host()).isEqualTo("localhost");
        assertThat(address.port()).isEqualTo(8080);
    }

    @Test
    void formats_as_host_colon_port() {
        var address = nodeAddress("example.com", 443);

        assertThat(address.asString()).isEqualTo("example.com:443");
    }

    @Test
    void handles_ip_addresses() {
        var address = nodeAddress("192.168.1.1", 9000);

        assertThat(address.host()).isEqualTo("192.168.1.1");
        assertThat(address.port()).isEqualTo(9000);
        assertThat(address.asString()).isEqualTo("192.168.1.1:9000");
    }

    @Test
    void handles_ipv6_addresses() {
        var address = nodeAddress("::1", 8080);

        assertThat(address.host()).isEqualTo("::1");
        assertThat(address.asString()).isEqualTo("::1:8080");
    }
}
