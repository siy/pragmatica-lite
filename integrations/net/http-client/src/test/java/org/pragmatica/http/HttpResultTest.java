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

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpResultTest {

    @Test
    void isSuccess_returnsTrue_for2xxStatusCodes() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);

        assertThat(new HttpResult<>(200, headers, "OK").isSuccess()).isTrue();
        assertThat(new HttpResult<>(201, headers, "Created").isSuccess()).isTrue();
        assertThat(new HttpResult<>(204, headers, null).isSuccess()).isTrue();
        assertThat(new HttpResult<>(299, headers, "Custom").isSuccess()).isTrue();
    }

    @Test
    void isSuccess_returnsFalse_forNon2xxStatusCodes() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);

        assertThat(new HttpResult<>(199, headers, "Info").isSuccess()).isFalse();
        assertThat(new HttpResult<>(301, headers, "Redirect").isSuccess()).isFalse();
        assertThat(new HttpResult<>(400, headers, "Bad Request").isSuccess()).isFalse();
        assertThat(new HttpResult<>(500, headers, "Error").isSuccess()).isFalse();
    }

    @Test
    void isClientError_returnsTrue_for4xxStatusCodes() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);

        assertThat(new HttpResult<>(400, headers, "Bad").isClientError()).isTrue();
        assertThat(new HttpResult<>(404, headers, "Not Found").isClientError()).isTrue();
        assertThat(new HttpResult<>(422, headers, "Unprocessable").isClientError()).isTrue();
    }

    @Test
    void isServerError_returnsTrue_for5xxStatusCodes() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);

        assertThat(new HttpResult<>(500, headers, "Error").isServerError()).isTrue();
        assertThat(new HttpResult<>(502, headers, "Bad Gateway").isServerError()).isTrue();
        assertThat(new HttpResult<>(503, headers, "Unavailable").isServerError()).isTrue();
    }

    @Test
    void header_returnsValue_whenPresent() {
        var headers = HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (_, _) -> true);
        var result = new HttpResult<>(200, headers, "body");

        assertThat(result.header("Content-Type").isPresent()).isTrue();
        assertThat(result.header("Content-Type").or("")).isEqualTo("application/json");
    }

    @Test
    void header_returnsEmpty_whenAbsent() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);
        var result = new HttpResult<>(200, headers, "body");

        assertThat(result.header("Content-Type").isEmpty()).isTrue();
    }

    @Test
    void toResult_returnsSuccess_for2xxStatus() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);
        var result = new HttpResult<>(200, headers, "body");

        result.toResult()
            .onFailure(_ -> org.junit.jupiter.api.Assertions.fail("Expected success"))
            .onSuccess(body -> assertThat(body).isEqualTo("body"));
    }

    @Test
    void toResult_returnsFailure_forErrorStatus() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);
        var result = new HttpResult<>(404, headers, "Not Found");

        result.toResult()
            .onSuccess(_ -> org.junit.jupiter.api.Assertions.fail("Expected failure"))
            .onFailure(cause -> {
                assertThat(cause).isInstanceOf(HttpError.RequestFailed.class);
                assertThat(cause.message()).contains("404");
            });
    }

    @Test
    void statusMessage_returnsReadableMessage() {
        var headers = HttpHeaders.of(Map.of(), (_, _) -> true);

        assertThat(new HttpResult<>(200, headers, null).statusMessage()).isEqualTo("OK");
        assertThat(new HttpResult<>(404, headers, null).statusMessage()).isEqualTo("Not Found");
        assertThat(new HttpResult<>(500, headers, null).statusMessage()).isEqualTo("Internal Server Error");
        assertThat(new HttpResult<>(999, headers, null).statusMessage()).isEqualTo("HTTP 999");
    }
}
