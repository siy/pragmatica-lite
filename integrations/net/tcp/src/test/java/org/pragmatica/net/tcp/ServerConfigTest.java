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
import static org.pragmatica.net.tcp.ServerConfig.serverConfig;

class ServerConfigTest {

    @Test
    void serverConfig_creates_with_defaults() {
        var config = serverConfig("test-server", 8080);

        assertThat(config.name()).isEqualTo("test-server");
        assertThat(config.port()).isEqualTo(8080);
        assertThat(config.tls().isEmpty()).isTrue();
        assertThat(config.clientTls().isEmpty()).isTrue();
        assertThat(config.socketOptions()).isEqualTo(SocketOptions.defaults());
    }

    @Test
    void serverConfig_creates_with_tls() {
        var tls = TlsConfig.selfSignedServer();
        var config = serverConfig("secure-server", 8443, tls);

        assertThat(config.name()).isEqualTo("secure-server");
        assertThat(config.port()).isEqualTo(8443);
        config.tls()
            .onEmpty(() -> assertThat(true).isFalse())
            .onPresent(t -> assertThat(t).isEqualTo(tls));
    }

    @Test
    void withTls_adds_server_tls_configuration() {
        var config = serverConfig("server", 8080)
            .withTls(TlsConfig.selfSignedServer());

        config.tls()
            .onEmpty(() -> assertThat(true).isFalse())
            .onPresent(t -> assertThat(t).isInstanceOf(TlsConfig.Server.class));
    }

    @Test
    void withClientTls_adds_client_tls_configuration() {
        var config = serverConfig("server", 8080)
            .withClientTls(TlsConfig.client());

        config.clientTls()
            .onEmpty(() -> assertThat(true).isFalse())
            .onPresent(t -> assertThat(t).isInstanceOf(TlsConfig.Client.class));
    }

    @Test
    void withSocketOptions_sets_socket_options() {
        var socketOptions = SocketOptions.socketOptions()
            .withSoBacklog(256)
            .withSoKeepalive(false);

        var config = serverConfig("server", 8080)
            .withSocketOptions(socketOptions);

        assertThat(config.socketOptions().soBacklog()).isEqualTo(256);
        assertThat(config.socketOptions().soKeepalive()).isFalse();
    }

    @Test
    void chained_configuration_works() {
        var config = serverConfig("full-config", 9000)
            .withTls(TlsConfig.selfSignedServer())
            .withClientTls(TlsConfig.insecureClient())
            .withSocketOptions(SocketOptions.socketOptions().withSoBacklog(512));

        assertThat(config.name()).isEqualTo("full-config");
        assertThat(config.port()).isEqualTo(9000);
        assertThat(config.tls().isPresent()).isTrue();
        assertThat(config.clientTls().isPresent()).isTrue();
        assertThat(config.socketOptions().soBacklog()).isEqualTo(512);
    }

    @Test
    void mutual_tls_can_be_used_for_both_server_and_client() {
        var mtls = TlsConfig.selfSignedMutual();
        var config = serverConfig("mtls-server", 9000)
            .withTls(mtls)
            .withClientTls(mtls);

        config.tls()
            .onEmpty(() -> assertThat(true).isFalse())
            .onPresent(t -> assertThat(t).isInstanceOf(TlsConfig.Mutual.class));
        config.clientTls()
            .onEmpty(() -> assertThat(true).isFalse())
            .onPresent(t -> assertThat(t).isInstanceOf(TlsConfig.Mutual.class));
    }
}
