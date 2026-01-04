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
import org.pragmatica.lang.Result;

import java.io.File;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating Netty SSL contexts from TLS configuration.
 * <p>
 * Supports creating both server-side and client-side SSL contexts from the unified
 * {@link TlsConfig} hierarchy.
 *
 * @see TlsConfig
 */
public final class TlsContextFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TlsContextFactory.class);

    private TlsContextFactory() {}

    /**
     * Create server-side SSL context from TLS configuration.
     *
     * @param config TLS configuration (must be Server or Mutual mode)
     * @return SSL context or error
     */
    public static Result<SslContext> createServer(TlsConfig config) {
        return switch (config) {
            case TlsConfig.Server(var identity, var clientAuth) ->
            buildServerContext(identity, clientAuth);
            case TlsConfig.Mutual(var identity, var trust) ->
            buildServerContextWithClientAuth(identity, trust);
            case TlsConfig.Client _ ->
            TlsError.wrongMode("Cannot create server context from Client config")
                    .result();
        };
    }

    /**
     * Create client-side SSL context from TLS configuration.
     *
     * @param config TLS configuration (must be Client or Mutual mode)
     * @return SSL context or error
     */
    public static Result<SslContext> createClient(TlsConfig config) {
        return switch (config) {
            case TlsConfig.Client(var trust, var identity) ->
            buildClientContext(trust, identity);
            case TlsConfig.Mutual(var identity, var trust) ->
            buildClientContextWithIdentity(identity, trust);
            case TlsConfig.Server _ ->
            TlsError.wrongMode("Cannot create client context from Server config")
                    .result();
        };
    }

    /**
     * Create SSL context from TLS configuration.
     *
     * @param config TLS configuration
     * @return SSL context or error
     * @deprecated Use {@link #createServer(TlsConfig)} or {@link #createClient(TlsConfig)} instead
     */
    @Deprecated(forRemoval = true)
    public static Result<SslContext> create(TlsConfig config) {
        return createServer(config);
    }

    // ===== Server Context Building =====
    private static Result<SslContext> buildServerContext(TlsConfig.Identity identity,
                                                         Option<TlsConfig.Trust> clientAuth) {
        return loadIdentity(identity)
                           .flatMap(keyMaterial -> {
                                        try{
                                            var builder = SslContextBuilder.forServer(keyMaterial.certFile(),
                                                                                      keyMaterial.keyFile(),
                                                                                      keyMaterial.password());
                                            // Configure client authentication if required
        clientAuth.onPresent(trust -> {
                                                configureTrust(builder, trust);
                                                builder.clientAuth(ClientAuth.REQUIRE);
                                            });
                                            return Result.success(builder.build());
                                        } catch (Exception e) {
                                            return new TlsError.ContextBuildFailed(e).result();
                                        }
                                    });
    }

    private static Result<SslContext> buildServerContextWithClientAuth(TlsConfig.Identity identity,
                                                                       TlsConfig.Trust trust) {
        return loadIdentity(identity)
                           .flatMap(keyMaterial -> {
                                        try{
                                            var builder = SslContextBuilder.forServer(keyMaterial.certFile(),
                                                                                      keyMaterial.keyFile(),
                                                                                      keyMaterial.password());
                                            configureTrust(builder, trust);
                                            builder.clientAuth(ClientAuth.REQUIRE);
                                            return Result.success(builder.build());
                                        } catch (Exception e) {
                                            return new TlsError.ContextBuildFailed(e).result();
                                        }
                                    });
    }

    // ===== Client Context Building =====
    private static Result<SslContext> buildClientContext(TlsConfig.Trust trust,
                                                         Option<TlsConfig.Identity> identity) {
        return identity.fold(// No identity - just build client context with trust
        () -> {
            try{
                var builder = SslContextBuilder.forClient();
                configureTrust(builder, trust);
                return Result.success(builder.build());
            } catch (Exception e) {
                return new TlsError.ContextBuildFailed(e).result();
            }
        },
        // Has identity - load it and configure
        id -> loadIdentity(id)
                          .flatMap(keyMaterial -> {
                                       try{
                                           var builder = SslContextBuilder.forClient();
                                           configureTrust(builder, trust);
                                           builder.keyManager(keyMaterial.certFile(),
                                                              keyMaterial.keyFile(),
                                                              keyMaterial.password());
                                           return Result.success(builder.build());
                                       } catch (Exception e) {
                                           return new TlsError.ContextBuildFailed(e).result();
                                       }
                                   }));
    }

    private static Result<SslContext> buildClientContextWithIdentity(TlsConfig.Identity identity,
                                                                     TlsConfig.Trust trust) {
        return loadIdentity(identity)
                           .flatMap(keyMaterial -> {
                                        try{
                                            var builder = SslContextBuilder.forClient();
                                            configureTrust(builder, trust);
                                            builder.keyManager(keyMaterial.certFile(),
                                                               keyMaterial.keyFile(),
                                                               keyMaterial.password());
                                            return Result.success(builder.build());
                                        } catch (Exception e) {
                                            return new TlsError.ContextBuildFailed(e).result();
                                        }
                                    });
    }

    // ===== Trust Configuration =====
    private static void configureTrust(SslContextBuilder builder, TlsConfig.Trust trust) {
        switch (trust) {
            case TlsConfig.Trust.SystemDefault() -> {}
            case TlsConfig.Trust.FromCaFile(var caPath) -> {
                builder.trustManager(caPath.toFile());
            }
            case TlsConfig.Trust.InsecureTrustAll() -> {
                LOG.warn("Using InsecureTrustAll - FOR DEVELOPMENT ONLY! Certificate verification is disabled.");
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }
        }
    }

    // ===== Identity Loading =====
    private record KeyMaterial(File certFile, File keyFile, String password) {}

    private static Result<KeyMaterial> loadIdentity(TlsConfig.Identity identity) {
        return switch (identity) {
            case TlsConfig.Identity.SelfSigned() -> generateSelfSigned();
            case TlsConfig.Identity.FromFiles(var certPath, var keyPath, var password) ->
            loadFromFiles(certPath, keyPath, password);
        };
    }

    @SuppressWarnings("deprecation") // SelfSignedCertificate is for dev/testing only
    private static Result<KeyMaterial> generateSelfSigned() {
        try{
            var ssc = new SelfSignedCertificate();
            return Result.success(new KeyMaterial(ssc.certificate(), ssc.privateKey(), null));
        } catch (Exception e) {
            return new TlsError.SelfSignedGenerationFailed(e).result();
        }
    }

    private static Result<KeyMaterial> loadFromFiles(java.nio.file.Path certPath,
                                                     java.nio.file.Path keyPath,
                                                     Option<String> password) {
        var certFile = certPath.toFile();
        var keyFile = keyPath.toFile();
        if (!certFile.exists() || !certFile.canRead()) {
            return new TlsError.CertificateLoadFailed(certPath,
                                                      new java.io.FileNotFoundException("Certificate file not found or not readable: " + certPath)).result();
        }
        if (!keyFile.exists() || !keyFile.canRead()) {
            return new TlsError.PrivateKeyLoadFailed(keyPath,
                                                     new java.io.FileNotFoundException("Private key file not found or not readable: " + keyPath)).result();
        }
        var pwd = password.fold(() -> null, p -> p);
        return Result.success(new KeyMaterial(certFile, keyFile, pwd));
    }
}
