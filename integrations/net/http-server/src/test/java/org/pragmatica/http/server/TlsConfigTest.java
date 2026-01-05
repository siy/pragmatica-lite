package org.pragmatica.http.server;

import org.junit.jupiter.api.Test;
import org.pragmatica.net.tcp.TlsConfig;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TlsConfigTest {

    @Test
    void selfSignedServer_creates_server_config() {
        var config = TlsConfig.selfSignedServer();

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
        var server = (TlsConfig.Server) config;
        assertThat(server.identity()).isInstanceOf(TlsConfig.Identity.SelfSigned.class);
    }

    @Test
    void server_creates_file_based_config() {
        var cert = Path.of("/path/to/cert.pem");
        var key = Path.of("/path/to/key.pem");

        var config = TlsConfig.server(cert, key);

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
        var server = (TlsConfig.Server) config;
        assertThat(server.identity()).isInstanceOf(TlsConfig.Identity.FromFiles.class);
        var fromFiles = (TlsConfig.Identity.FromFiles) server.identity();
        assertThat(fromFiles.certificatePath()).isEqualTo(cert);
        assertThat(fromFiles.privateKeyPath()).isEqualTo(key);
        assertThat(fromFiles.keyPassword().isPresent()).isFalse();
    }

    @Test
    void server_with_password_creates_protected_config() {
        var cert = Path.of("/path/to/cert.pem");
        var key = Path.of("/path/to/key.pem");

        var config = TlsConfig.server(cert, key, "secret");

        assertThat(config).isInstanceOf(TlsConfig.Server.class);
        var server = (TlsConfig.Server) config;
        assertThat(server.identity()).isInstanceOf(TlsConfig.Identity.FromFiles.class);
        var fromFiles = (TlsConfig.Identity.FromFiles) server.identity();
        assertThat(fromFiles.keyPassword().isPresent()).isTrue();
        fromFiles.keyPassword().onPresent(pwd -> assertThat(pwd).isEqualTo("secret"));
    }
}
