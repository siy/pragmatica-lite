package org.pragmatica.http.server;

import org.junit.jupiter.api.Test;
import org.pragmatica.net.tcp.TlsConfig;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TlsConfigTest {

    @Test
    void selfSigned_creates_self_signed_config() {
        var config = TlsConfig.selfSigned();

        assertThat(config).isInstanceOf(TlsConfig.SelfSigned.class);
    }

    @Test
    void fromFiles_creates_file_based_config() {
        var cert = Path.of("/path/to/cert.pem");
        var key = Path.of("/path/to/key.pem");

        var config = TlsConfig.fromFiles(cert, key);

        assertThat(config).isInstanceOf(TlsConfig.FromFiles.class);
        var fromFiles = (TlsConfig.FromFiles) config;
        assertThat(fromFiles.certificatePath()).isEqualTo(cert);
        assertThat(fromFiles.privateKeyPath()).isEqualTo(key);
        assertThat(fromFiles.keyPassword().isPresent()).isFalse();
    }

    @Test
    void fromFiles_with_password_creates_protected_config() {
        var cert = Path.of("/path/to/cert.pem");
        var key = Path.of("/path/to/key.pem");

        var config = TlsConfig.fromFiles(cert, key, "secret");

        assertThat(config).isInstanceOf(TlsConfig.FromFiles.class);
        var fromFiles = (TlsConfig.FromFiles) config;
        assertThat(fromFiles.keyPassword().isPresent()).isTrue();
        fromFiles.keyPassword().onPresent(pwd -> assertThat(pwd).isEqualTo("secret"));
    }
}
