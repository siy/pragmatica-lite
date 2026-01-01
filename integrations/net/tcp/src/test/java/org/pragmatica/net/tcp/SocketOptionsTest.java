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

class SocketOptionsTest {

    @Test
    void defaults_has_standard_values() {
        var options = SocketOptions.defaults();

        assertThat(options.soBacklog()).isEqualTo(128);
        assertThat(options.soKeepalive()).isTrue();
        assertThat(options.tcpNoDelay()).isTrue();
    }

    @Test
    void factory_creates_with_default_values() {
        var options = SocketOptions.socketOptions();

        assertThat(options.soBacklog()).isEqualTo(128);
        assertThat(options.soKeepalive()).isTrue();
        assertThat(options.tcpNoDelay()).isTrue();
    }

    @Test
    void withSoBacklog_returns_new_instance() {
        var original = SocketOptions.socketOptions();
        var modified = original.withSoBacklog(256);

        assertThat(modified.soBacklog()).isEqualTo(256);
        assertThat(modified.soKeepalive()).isTrue();
        assertThat(modified.tcpNoDelay()).isTrue();
        assertThat(original.soBacklog()).isEqualTo(128);
    }

    @Test
    void withSoKeepalive_returns_new_instance() {
        var original = SocketOptions.socketOptions();
        var modified = original.withSoKeepalive(false);

        assertThat(modified.soKeepalive()).isFalse();
        assertThat(modified.soBacklog()).isEqualTo(128);
        assertThat(modified.tcpNoDelay()).isTrue();
        assertThat(original.soKeepalive()).isTrue();
    }

    @Test
    void withTcpNoDelay_returns_new_instance() {
        var original = SocketOptions.socketOptions();
        var modified = original.withTcpNoDelay(false);

        assertThat(modified.tcpNoDelay()).isFalse();
        assertThat(modified.soBacklog()).isEqualTo(128);
        assertThat(modified.soKeepalive()).isTrue();
        assertThat(original.tcpNoDelay()).isTrue();
    }

    @Test
    void chained_modifications_work() {
        var options = SocketOptions.socketOptions()
            .withSoBacklog(512)
            .withSoKeepalive(false)
            .withTcpNoDelay(false);

        assertThat(options.soBacklog()).isEqualTo(512);
        assertThat(options.soKeepalive()).isFalse();
        assertThat(options.tcpNoDelay()).isFalse();
    }
}
