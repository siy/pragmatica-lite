/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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
 *
 */

package org.pragmatica.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HttpErrorTest {

    @Test
    void connectionFailed_containsMessage() {
        var error = HttpError.ConnectionFailed.of("Connection refused");

        assertThat(error.message()).contains("Connection refused");
        assertThat(error.cause().isEmpty()).isTrue();
    }

    @Test
    void connectionFailed_containsCause() {
        var cause = new IOException("Network error");
        var error = HttpError.ConnectionFailed.of("Failed", cause);

        assertThat(error.message()).contains("Failed");
        assertThat(error.cause().isPresent()).isTrue();
    }

    @Test
    void timeout_withoutDuration_displaysMessage() {
        var error = HttpError.Timeout.of("Request timed out");

        assertThat(error.message()).isEqualTo("Timeout: Request timed out");
    }

    @Test
    void timeout_withDuration_displaysMillis() {
        var error = HttpError.Timeout.of("Connect", Duration.ofSeconds(5));

        assertThat(error.message()).contains("5000ms");
        assertThat(error.message()).contains("Connect");
    }

    @Test
    void requestFailed_displaysStatusAndReason() {
        var error = new HttpError.RequestFailed(404, "Not Found");

        assertThat(error.message()).isEqualTo("HTTP 404: Not Found");
    }

    @Test
    void invalidResponse_containsDetails() {
        var error = HttpError.InvalidResponse.of("Malformed JSON");

        assertThat(error.message()).contains("Malformed JSON");
    }

    @Test
    void failure_wrapsException() {
        var cause = new RuntimeException("Unexpected");
        var error = HttpError.Failure.of(cause);

        assertThat(error.message()).contains("Unexpected");
        assertThat(error.cause()).isSameAs(cause);
    }

    @Test
    void fromException_mapsTimeoutException() {
        var ex = new HttpTimeoutException("Timed out");
        var error = HttpError.fromException(ex);

        assertInstanceOf(HttpError.Timeout.class, error);
    }

    @Test
    void fromException_mapsConnectException() {
        var ex = new ConnectException("Connection refused");
        var error = HttpError.fromException(ex);

        assertInstanceOf(HttpError.ConnectionFailed.class, error);
    }

    @Test
    void fromException_mapsUnknownHostException() {
        var ex = new UnknownHostException("example.invalid");
        var error = HttpError.fromException(ex);

        assertInstanceOf(HttpError.ConnectionFailed.class, error);
        assertThat(error.message()).contains("Unknown host");
    }

    @Test
    void fromException_mapsInterruptedException() {
        var ex = new InterruptedException();
        var error = HttpError.fromException(ex);

        assertInstanceOf(HttpError.Timeout.class, error);
        assertThat(error.message()).contains("interrupted");
    }

    @Test
    void fromException_mapsUnknownToFailure() {
        var ex = new IllegalStateException("Unknown error");
        var error = HttpError.fromException(ex);

        assertInstanceOf(HttpError.Failure.class, error);
    }
}
