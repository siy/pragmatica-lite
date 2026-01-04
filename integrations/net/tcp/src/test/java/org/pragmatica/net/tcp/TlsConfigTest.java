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

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TlsConfigTest {

    @Test
    void selfSignedServer_creates_server_with_self_signed_identity() {
        var config = TlsConfig.selfSignedServer();

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
        var server = (TlsConfig.Server) config;
        assertThat(server.identity()).isInstanceOf(TlsConfig.Identity.SelfSigned.class);
        assertThat(server.clientAuth().isEmpty()).isTrue();
    }

    @Test
    void server_creates_config_from_pem_files() {
        var certPath = Path.of("/path/to/cert.pem");
        var keyPath = Path.of("/path/to/key.pem");

        var config = TlsConfig.server(certPath, keyPath);

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
        var server = (TlsConfig.Server) config;
        assertThat(server.identity()).isInstanceOf(TlsConfig.Identity.FromFiles.class);
        var fromFiles = (TlsConfig.Identity.FromFiles) server.identity();
        assertThat(fromFiles.certificatePath()).isEqualTo(certPath);
        assertThat(fromFiles.privateKeyPath()).isEqualTo(keyPath);
        assertThat(fromFiles.keyPassword().isEmpty()).isTrue();
    }

    @Test
    void server_creates_config_with_password() {
        var certPath = Path.of("/path/to/cert.pem");
        var keyPath = Path.of("/path/to/key.pem");
        var password = "secret123";

        var config = TlsConfig.server(certPath, keyPath, password);

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
        var server = (TlsConfig.Server) config;
        assertThat(server.identity()).isInstanceOf(TlsConfig.Identity.FromFiles.class);
        var fromFiles = (TlsConfig.Identity.FromFiles) server.identity();
        fromFiles.keyPassword()
            .onEmpty(() -> assertThat(true).isFalse())
            .onPresent(pwd -> assertThat(pwd).isEqualTo(password));
    }

    @Test
    void client_creates_client_with_system_default_trust() {
        var config = TlsConfig.client();

        assertThat(config).isInstanceOf(TlsConfig.Client.class);
        var client = (TlsConfig.Client) config;
        assertThat(client.trust()).isInstanceOf(TlsConfig.Trust.SystemDefault.class);
        assertThat(client.identity().isEmpty()).isTrue();
    }

    @Test
    void insecureClient_creates_client_with_insecure_trust() {
        var config = TlsConfig.insecureClient();

        assertThat(config).isInstanceOf(TlsConfig.Client.class);
        var client = (TlsConfig.Client) config;
        assertThat(client.trust()).isInstanceOf(TlsConfig.Trust.InsecureTrustAll.class);
    }

    @Test
    void clientWithCa_creates_client_with_custom_ca() {
        var caPath = Path.of("/path/to/ca.pem");

        var config = TlsConfig.clientWithCa(caPath);

        assertThat(config).isInstanceOf(TlsConfig.Client.class);
        var client = (TlsConfig.Client) config;
        assertThat(client.trust()).isInstanceOf(TlsConfig.Trust.FromCaFile.class);
        var fromCa = (TlsConfig.Trust.FromCaFile) client.trust();
        assertThat(fromCa.caCertificatePath()).isEqualTo(caPath);
    }

    @Test
    void mutual_creates_mtls_config() {
        var certPath = Path.of("/path/to/cert.pem");
        var keyPath = Path.of("/path/to/key.pem");
        var caPath = Path.of("/path/to/ca.pem");

        var config = TlsConfig.mutual(certPath, keyPath, caPath);

        assertThat(config).isInstanceOf(TlsConfig.Mutual.class);
        var mutual = (TlsConfig.Mutual) config;
        assertThat(mutual.identity()).isInstanceOf(TlsConfig.Identity.FromFiles.class);
        assertThat(mutual.trust()).isInstanceOf(TlsConfig.Trust.FromCaFile.class);
    }

    @Test
    void selfSignedMutual_creates_dev_mtls_config() {
        var config = TlsConfig.selfSignedMutual();

        assertThat(config).isInstanceOf(TlsConfig.Mutual.class);
        var mutual = (TlsConfig.Mutual) config;
        assertThat(mutual.identity()).isInstanceOf(TlsConfig.Identity.SelfSigned.class);
        assertThat(mutual.trust()).isInstanceOf(TlsConfig.Trust.InsecureTrustAll.class);
    }

    @Test
    void sealed_interface_covers_all_modes() {
        TlsConfig server = TlsConfig.selfSignedServer();
        TlsConfig client = TlsConfig.client();
        TlsConfig mutual = TlsConfig.selfSignedMutual();

        // Pattern matching works
        var result = switch (server) {
            case TlsConfig.Server _ -> "server";
            case TlsConfig.Client _ -> "client";
            case TlsConfig.Mutual _ -> "mutual";
        };
        assertThat(result).isEqualTo("server");

        result = switch (client) {
            case TlsConfig.Server _ -> "server";
            case TlsConfig.Client _ -> "client";
            case TlsConfig.Mutual _ -> "mutual";
        };
        assertThat(result).isEqualTo("client");

        result = switch (mutual) {
            case TlsConfig.Server _ -> "server";
            case TlsConfig.Client _ -> "client";
            case TlsConfig.Mutual _ -> "mutual";
        };
        assertThat(result).isEqualTo("mutual");
    }

    @Test
    void deprecated_selfSigned_delegates_to_selfSignedServer() {
        @SuppressWarnings("deprecation")
        var config = TlsConfig.selfSigned();

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
    }

    @Test
    void deprecated_fromFiles_delegates_to_server() {
        @SuppressWarnings("deprecation")
        var config = TlsConfig.fromFiles(Path.of("c"), Path.of("k"));

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
    }
}
