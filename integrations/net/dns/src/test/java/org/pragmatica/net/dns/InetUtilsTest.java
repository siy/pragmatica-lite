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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InetUtilsTest {

    @Test
    void forBytes_parses_valid_ipv4_address() {
        var bytes = new byte[]{(byte) 192, (byte) 168, 1, 1};

        InetUtils.forBytes(bytes)
                 .onFailureRun(Assertions::fail)
                 .onSuccess(address -> {
                     assertThat(address.getHostAddress()).isEqualTo("192.168.1.1");
                 });
    }

    @Test
    void forBytes_parses_valid_ipv6_address() {
        // ::1 in bytes (16 bytes, all zeros except last byte)
        var bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

        InetUtils.forBytes(bytes)
                 .onFailureRun(Assertions::fail)
                 .onSuccess(address -> {
                     assertThat(address.getHostAddress()).isEqualTo("0:0:0:0:0:0:0:1");
                 });
    }

    @Test
    void forBytes_fails_for_invalid_length() {
        // Neither 4 (IPv4) nor 16 (IPv6) bytes
        var bytes = new byte[]{1, 2, 3};

        InetUtils.forBytes(bytes)
                 .onSuccessRun(Assertions::fail)
                 .onFailure(cause -> {
                     assertThat(cause).isInstanceOf(ResolverError.InvalidIpAddress.class);
                 });
    }

    @Test
    void forBytes_handles_all_zeros_ipv4() {
        var bytes = new byte[]{0, 0, 0, 0};

        InetUtils.forBytes(bytes)
                 .onFailureRun(Assertions::fail)
                 .onSuccess(address -> {
                     assertThat(address.getHostAddress()).isEqualTo("0.0.0.0");
                 });
    }

    @Test
    void forBytes_handles_broadcast_address() {
        var bytes = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255};

        InetUtils.forBytes(bytes)
                 .onFailureRun(Assertions::fail)
                 .onSuccess(address -> {
                     assertThat(address.getHostAddress()).isEqualTo("255.255.255.255");
                 });
    }
}
