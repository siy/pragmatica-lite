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

import org.pragmatica.lang.Cause;

import java.nio.file.Path;

/// Error types for TLS operations.
public sealed interface TlsError extends Cause {
    /// Failed to load certificate from file.
    record CertificateLoadFailed(Path path, Throwable cause) implements TlsError {
        @Override
        public String message() {
            return "Failed to load certificate from " + path + ": " + cause.getMessage();
        }
    }

    /// Failed to load private key from file.
    record PrivateKeyLoadFailed(Path path, Throwable cause) implements TlsError {
        @Override
        public String message() {
            return "Failed to load private key from " + path + ": " + cause.getMessage();
        }
    }

    /// Failed to load CA certificate (trust store) from file.
    record TrustStoreLoadFailed(Path path, Throwable cause) implements TlsError {
        @Override
        public String message() {
            return "Failed to load CA certificate from " + path + ": " + cause.getMessage();
        }
    }

    /// Failed to generate self-signed certificate.
    record SelfSignedGenerationFailed(Throwable cause) implements TlsError {
        @Override
        public String message() {
            return "Failed to generate self-signed certificate: " + cause.getMessage();
        }
    }

    /// Failed to build SSL context.
    record ContextBuildFailed(Throwable cause) implements TlsError {
        @Override
        public String message() {
            return "Failed to build SSL context: " + cause.getMessage();
        }
    }

    /// Invalid TLS mode for the requested operation.
    record WrongMode(String details) implements TlsError {
        @Override
        public String message() {
            return "Invalid TLS mode: " + details;
        }
    }

    /// Create a WrongMode error with the given details.
    static TlsError wrongMode(String details) {
        return new WrongMode(details);
    }
}
