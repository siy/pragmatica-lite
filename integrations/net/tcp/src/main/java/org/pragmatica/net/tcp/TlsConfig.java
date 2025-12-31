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

import org.pragmatica.lang.Option;

import java.nio.file.Path;

/**
 * TLS configuration for secure server connections.
 * <p>
 * Two modes supported:
 * <ul>
 *   <li>{@link SelfSigned} - generates self-signed certificate at startup (for development)</li>
 *   <li>{@link FromFiles} - loads certificate and private key from PEM files (for production)</li>
 * </ul>
 */
public sealed interface TlsConfig {
    /**
     * Self-signed certificate generated at startup.
     * Suitable for development and testing only.
     */
    record SelfSigned() implements TlsConfig {}

    /**
     * Certificate and private key loaded from PEM files.
     * Suitable for production use.
     */
    record FromFiles(Path certificatePath,
                     Path privateKeyPath,
                     Option<String> keyPassword) implements TlsConfig {}

    /**
     * Create self-signed TLS configuration.
     */
    static TlsConfig selfSigned() {
        return new SelfSigned();
    }

    /**
     * Create TLS configuration from certificate files.
     *
     * @param certificatePath path to PEM certificate file
     * @param privateKeyPath  path to PEM private key file
     */
    static TlsConfig fromFiles(Path certificatePath, Path privateKeyPath) {
        return new FromFiles(certificatePath, privateKeyPath, Option.empty());
    }

    /**
     * Create TLS configuration from password-protected certificate files.
     *
     * @param certificatePath path to PEM certificate file
     * @param privateKeyPath  path to PEM private key file
     * @param keyPassword     password for the private key
     */
    static TlsConfig fromFiles(Path certificatePath, Path privateKeyPath, String keyPassword) {
        return new FromFiles(certificatePath, privateKeyPath, Option.some(keyPassword));
    }
}
