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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.pragmatica.lang.Result;

/**
 * Factory for creating Netty SSL contexts from TLS configuration.
 */
public final class TlsContextFactory {
    private TlsContextFactory() {}

    /**
     * Create SSL context from TLS configuration.
     *
     * @param config TLS configuration
     * @return SSL context or error
     */
    public static Result<SslContext> create(TlsConfig config) {
        return switch (config) {
            case TlsConfig.SelfSigned() -> createSelfSigned();
            case TlsConfig.FromFiles fromFiles -> createFromFiles(fromFiles);
        };
    }

    private static Result<SslContext> createSelfSigned() {
        try {
            var ssc = new SelfSignedCertificate();
            var sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                              .build();
            return Result.success(sslContext);
        } catch (Exception e) {
            return new TlsError.SelfSignedGenerationFailed(e).result();
        }
    }

    private static Result<SslContext> createFromFiles(TlsConfig.FromFiles config) {
        try {
            var password = config.keyPassword()
                                 .fold(() -> null, pwd -> pwd);
            var builder = SslContextBuilder.forServer(
                config.certificatePath().toFile(),
                config.privateKeyPath().toFile(),
                password
            );
            return Result.success(builder.build());
        } catch (Exception e) {
            return new TlsError.CertificateLoadFailed(config.certificatePath(), e).result();
        }
    }
}
