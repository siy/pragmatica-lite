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
 * Unified TLS configuration supporting server-side, client-side, and mutual TLS.
 * <p>
 * Configuration is built from two orthogonal concepts:
 * <ul>
 *   <li><b>Identity</b> - "Who am I?" (certificate + private key for presenting to peers)</li>
 *   <li><b>Trust</b> - "Who do I trust?" (CA certificates for verifying peers)</li>
 * </ul>
 * <p>
 * Three modes are supported:
 * <ul>
 *   <li>{@link Server} - Accept TLS connections, optionally require client certificates</li>
 *   <li>{@link Client} - Connect to TLS servers, optionally present client certificate</li>
 *   <li>{@link Mutual} - Both sides authenticate (mTLS for service-to-service)</li>
 * </ul>
 *
 * @see TlsContextFactory
 */
public sealed interface TlsConfig {
    // ===== Identity Configuration =====
    /**
     * Identity configuration - certificate and private key for presenting to peers.
     */
    sealed interface Identity {
        /**
         * Self-signed certificate generated at startup.
         * Suitable for development and testing only.
         */
        record SelfSigned() implements Identity {}

        /**
         * Certificate and private key loaded from PEM files.
         * Suitable for production use.
         */
        record FromFiles(Path certificatePath,
                         Path privateKeyPath,
                         Option<String> keyPassword) implements Identity {}
    }

    // ===== Trust Configuration =====
    /**
     * Trust configuration - CA certificates for verifying peers.
     */
    sealed interface Trust {
        /**
         * Use system default CA certificates.
         * Suitable for clients connecting to public servers.
         */
        record SystemDefault() implements Trust {}

        /**
         * Trust certificates signed by specific CA.
         * Suitable for self-signed servers or internal CAs.
         */
        record FromCaFile(Path caCertificatePath) implements Trust {}

        /**
         * Trust all certificates without verification.
         * <b>WARNING: FOR DEVELOPMENT ONLY!</b> Disables certificate verification.
         */
        record InsecureTrustAll() implements Trust {}
    }

    // ===== TLS Modes =====
    /**
     * Server-only TLS configuration.
     * Accepts TLS connections, optionally requires client certificates.
     *
     * @param identity   server's certificate and private key
     * @param clientAuth optional trust config for client certificate verification (mTLS server-side)
     */
    record Server(Identity identity, Option<Trust> clientAuth) implements TlsConfig {}

    /**
     * Client-only TLS configuration.
     * Connects to TLS servers, optionally presents client certificate.
     *
     * @param trust    how to verify server certificates
     * @param identity optional client certificate for mTLS
     */
    record Client(Trust trust, Option<Identity> identity) implements TlsConfig {}

    /**
     * Mutual TLS configuration.
     * Both sides present and verify certificates. Suitable for service-to-service communication.
     *
     * @param identity this node's certificate and private key
     * @param trust    CA certificate for verifying peer certificates
     */
    record Mutual(Identity identity, Trust trust) implements TlsConfig {}

    // ===== Server Factory Methods =====
    /**
     * Create self-signed server TLS configuration.
     * Generates certificate at startup. For development only.
     */
    static TlsConfig selfSignedServer() {
        return new Server(new Identity.SelfSigned(), Option.empty());
    }

    /**
     * Create server TLS configuration from PEM files.
     *
     * @param certificatePath path to server certificate PEM file
     * @param privateKeyPath  path to server private key PEM file
     */
    static TlsConfig server(Path certificatePath, Path privateKeyPath) {
        return new Server(new Identity.FromFiles(certificatePath, privateKeyPath, Option.empty()),
                          Option.empty());
    }

    /**
     * Create server TLS configuration with password-protected private key.
     *
     * @param certificatePath path to server certificate PEM file
     * @param privateKeyPath  path to server private key PEM file
     * @param keyPassword     password for the private key
     */
    static TlsConfig server(Path certificatePath, Path privateKeyPath, String keyPassword) {
        return new Server(new Identity.FromFiles(certificatePath, privateKeyPath, Option.some(keyPassword)),
                          Option.empty());
    }

    /**
     * Create server TLS configuration requiring client certificates (mTLS server-side).
     *
     * @param certificatePath   path to server certificate PEM file
     * @param privateKeyPath    path to server private key PEM file
     * @param caCertificatePath path to CA certificate for verifying client certificates
     */
    static TlsConfig serverWithClientAuth(Path certificatePath, Path privateKeyPath, Path caCertificatePath) {
        return new Server(new Identity.FromFiles(certificatePath, privateKeyPath, Option.empty()),
                          Option.some(new Trust.FromCaFile(caCertificatePath)));
    }

    // ===== Client Factory Methods =====
    /**
     * Create client TLS configuration using system CA certificates.
     * Suitable for connecting to public HTTPS servers.
     */
    static TlsConfig client() {
        return new Client(new Trust.SystemDefault(), Option.empty());
    }

    /**
     * Create client TLS configuration with custom CA.
     * Suitable for connecting to servers with self-signed or internal CA certificates.
     *
     * @param caCertificatePath path to CA certificate PEM file
     */
    static TlsConfig clientWithCa(Path caCertificatePath) {
        return new Client(new Trust.FromCaFile(caCertificatePath), Option.empty());
    }

    /**
     * Create insecure client TLS configuration that trusts all certificates.
     * <b>WARNING: FOR DEVELOPMENT ONLY!</b> Disables certificate verification.
     */
    static TlsConfig insecureClient() {
        return new Client(new Trust.InsecureTrustAll(), Option.empty());
    }

    /**
     * Create client TLS configuration with client certificate (mTLS client-side).
     *
     * @param trust             how to verify server certificates
     * @param certificatePath   path to client certificate PEM file
     * @param privateKeyPath    path to client private key PEM file
     */
    static TlsConfig clientWithIdentity(Trust trust, Path certificatePath, Path privateKeyPath) {
        return new Client(trust,
                          Option.some(new Identity.FromFiles(certificatePath, privateKeyPath, Option.empty())));
    }

    // ===== Mutual TLS Factory Methods =====
    /**
     * Create mutual TLS configuration from PEM files.
     * Both sides will present and verify certificates.
     *
     * @param certificatePath   path to this node's certificate PEM file
     * @param privateKeyPath    path to this node's private key PEM file
     * @param caCertificatePath path to CA certificate for verifying peer certificates
     */
    static TlsConfig mutual(Path certificatePath, Path privateKeyPath, Path caCertificatePath) {
        return new Mutual(new Identity.FromFiles(certificatePath, privateKeyPath, Option.empty()),
                          new Trust.FromCaFile(caCertificatePath));
    }

    /**
     * Create mutual TLS configuration with password-protected private key.
     *
     * @param certificatePath   path to this node's certificate PEM file
     * @param privateKeyPath    path to this node's private key PEM file
     * @param keyPassword       password for the private key
     * @param caCertificatePath path to CA certificate for verifying peer certificates
     */
    static TlsConfig mutual(Path certificatePath, Path privateKeyPath, String keyPassword, Path caCertificatePath) {
        return new Mutual(new Identity.FromFiles(certificatePath, privateKeyPath, Option.some(keyPassword)),
                          new Trust.FromCaFile(caCertificatePath));
    }

    /**
     * Create self-signed mutual TLS configuration for development.
     * Generates certificate at startup and trusts all certificates.
     * <b>WARNING: FOR DEVELOPMENT ONLY!</b>
     */
    static TlsConfig selfSignedMutual() {
        return new Mutual(new Identity.SelfSigned(), new Trust.InsecureTrustAll());
    }

    // ===== Deprecated Legacy API =====
    /**
     * @deprecated Use {@link #selfSignedServer()} instead
     */
    @Deprecated(forRemoval = true)
    static TlsConfig selfSigned() {
        return selfSignedServer();
    }

    /**
     * @deprecated Use {@link #server(Path, Path)} instead
     */
    @Deprecated(forRemoval = true)
    static TlsConfig fromFiles(Path certificatePath, Path privateKeyPath) {
        return server(certificatePath, privateKeyPath);
    }

    /**
     * @deprecated Use {@link #server(Path, Path, String)} instead
     */
    @Deprecated(forRemoval = true)
    static TlsConfig fromFiles(Path certificatePath, Path privateKeyPath, String keyPassword) {
        return server(certificatePath, privateKeyPath, keyPassword);
    }
}
