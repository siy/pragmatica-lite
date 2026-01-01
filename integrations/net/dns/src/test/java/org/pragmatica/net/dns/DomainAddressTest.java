/*
 *  Copyright (c) 2022-2025 Sergiy Yevtushenko.
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

package org.pragmatica.net.dns;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.net.dns.DomainAddress.domainAddress;
import static org.pragmatica.net.dns.DomainName.domainName;

class DomainAddressTest {

    @Test
    void domainAddress_creates_instance_with_all_fields() throws UnknownHostException {
        var name = domainName("example.com");
        var ip = InetAddress.getByName("93.184.216.34");
        var ttl = Duration.ofSeconds(300);

        var address = domainAddress(name, ip, ttl);

        assertThat(address.name()).isEqualTo(name);
        assertThat(address.ip()).isEqualTo(ip);
        assertThat(address.ttl()).isEqualTo(ttl);
    }

    @Test
    void domainAddress_withDomain_creates_new_instance_with_different_name() throws UnknownHostException {
        var originalName = domainName("example.com");
        var newName = domainName("alias.example.com");
        var ip = InetAddress.getByName("93.184.216.34");
        var ttl = Duration.ofSeconds(300);

        var original = domainAddress(originalName, ip, ttl);
        var withNewDomain = original.withDomain(newName);

        assertThat(withNewDomain.name()).isEqualTo(newName);
        assertThat(withNewDomain.ip()).isEqualTo(ip);
        assertThat(withNewDomain.ttl()).isEqualTo(ttl);
        assertThat(original.name()).isEqualTo(originalName); // original unchanged
    }

    @Test
    void domainAddress_supports_ipv4() throws UnknownHostException {
        var ip = InetAddress.getByName("192.168.1.1");
        var address = domainAddress(domainName("local"), ip, Duration.ofSeconds(60));

        assertThat(address.ip().getHostAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void domainAddress_supports_ipv6() throws UnknownHostException {
        var ip = InetAddress.getByName("::1");
        var address = domainAddress(domainName("localhost"), ip, Duration.ofSeconds(0));

        assertThat(address.ip().getHostAddress()).isEqualTo("0:0:0:0:0:0:0:1");
    }

    @Test
    void domainAddress_supports_zero_ttl() throws UnknownHostException {
        var address = domainAddress(
            domainName("no-cache.example.com"),
            InetAddress.getByName("1.2.3.4"),
            Duration.ZERO
        );

        assertThat(address.ttl()).isEqualTo(Duration.ZERO);
    }
}
