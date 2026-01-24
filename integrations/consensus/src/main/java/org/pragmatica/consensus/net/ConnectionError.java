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

package org.pragmatica.consensus.net;

import org.pragmatica.lang.Cause;

public sealed interface ConnectionError extends Cause {
    record ConnectionRefused(String address) implements ConnectionError {
        @Override
        public String message() {
            return "Connection refused: " + address;
        }
    }

    record HelloTimeout(String address) implements ConnectionError {
        @Override
        public String message() {
            return "Hello handshake timeout: " + address;
        }
    }

    record NetworkError(String address, String details) implements ConnectionError {
        @Override
        public String message() {
            return "Network error connecting to " + address + ": " + details;
        }
    }

    static ConnectionRefused connectionRefused(String address) {
        return new ConnectionRefused(address);
    }

    static HelloTimeout helloTimeout(String address) {
        return new HelloTimeout(address);
    }

    static NetworkError networkError(String address, String details) {
        return new NetworkError(address, details);
    }
}
