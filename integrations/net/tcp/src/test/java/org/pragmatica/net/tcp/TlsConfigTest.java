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
    void selfSigned_creates_correct_type() {
        var config = TlsConfig.selfSigned();

        assertThat(config).isInstanceOf(TlsConfig.SelfSigned.class);
    }

    @Test
    void fromFiles_creates_config_without_password() {
        var certPath = Path.of("/path/to/cert.pem");
        var keyPath = Path.of("/path/to/key.pem");

        var config = TlsConfig.fromFiles(certPath, keyPath);

        assertThat(config).isInstanceOf(TlsConfig.FromFiles.class);
        var fromFiles = (TlsConfig.FromFiles) config;
        assertThat(fromFiles.certificatePath()).isEqualTo(certPath);
        assertThat(fromFiles.privateKeyPath()).isEqualTo(keyPath);
        assertThat(fromFiles.keyPassword().isEmpty()).isTrue();
    }

    @Test
    void fromFiles_creates_config_with_password() {
        var certPath = Path.of("/path/to/cert.pem");
        var keyPath = Path.of("/path/to/key.pem");
        var password = "secret123";

        var config = TlsConfig.fromFiles(certPath, keyPath, password);

        assertThat(config).isInstanceOf(TlsConfig.FromFiles.class);
        var fromFiles = (TlsConfig.FromFiles) config;
        assertThat(fromFiles.certificatePath()).isEqualTo(certPath);
        assertThat(fromFiles.privateKeyPath()).isEqualTo(keyPath);
        fromFiles.keyPassword()
            .onEmpty(() -> assertThat(true).isFalse())
            .onPresent(pwd -> assertThat(pwd).isEqualTo(password));
    }

    @Test
    void sealed_interface_covers_all_types() {
        TlsConfig selfSigned = TlsConfig.selfSigned();
        TlsConfig fromFiles = TlsConfig.fromFiles(Path.of("c"), Path.of("k"));

        // Pattern matching works
        var result = switch (selfSigned) {
            case TlsConfig.SelfSigned() -> "self-signed";
            case TlsConfig.FromFiles(var c, var k, var p) -> "from-files";
        };
        assertThat(result).isEqualTo("self-signed");

        result = switch (fromFiles) {
            case TlsConfig.SelfSigned() -> "self-signed";
            case TlsConfig.FromFiles(var c, var k, var p) -> "from-files";
        };
        assertThat(result).isEqualTo("from-files");
    }
}
