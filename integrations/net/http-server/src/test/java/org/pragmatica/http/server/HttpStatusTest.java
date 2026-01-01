package org.pragmatica.http.server;

import org.junit.jupiter.api.Test;
import org.pragmatica.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class HttpStatusTest {

    @Test
    void ok_has_code_200() {
        assertThat(HttpStatus.OK.code()).isEqualTo(200);
        assertThat(HttpStatus.OK.reasonPhrase()).isEqualTo("OK");
    }

    @Test
    void not_found_has_code_404() {
        assertThat(HttpStatus.NOT_FOUND.code()).isEqualTo(404);
        assertThat(HttpStatus.NOT_FOUND.reasonPhrase()).isEqualTo("Not Found");
    }

    @Test
    void internal_server_error_has_code_500() {
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.code()).isEqualTo(500);
    }

    @Test
    void isSuccess_returns_true_for_2xx_codes() {
        assertThat(HttpStatus.OK.isSuccess()).isTrue();
        assertThat(HttpStatus.CREATED.isSuccess()).isTrue();
        assertThat(HttpStatus.NO_CONTENT.isSuccess()).isTrue();

        assertThat(HttpStatus.NOT_FOUND.isSuccess()).isFalse();
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.isSuccess()).isFalse();
    }

    @Test
    void isRedirect_returns_true_for_3xx_codes() {
        assertThat(HttpStatus.MOVED_PERMANENTLY.isRedirect()).isTrue();
        assertThat(HttpStatus.FOUND.isRedirect()).isTrue();

        assertThat(HttpStatus.OK.isRedirect()).isFalse();
        assertThat(HttpStatus.NOT_FOUND.isRedirect()).isFalse();
    }

    @Test
    void isClientError_returns_true_for_4xx_codes() {
        assertThat(HttpStatus.BAD_REQUEST.isClientError()).isTrue();
        assertThat(HttpStatus.NOT_FOUND.isClientError()).isTrue();
        assertThat(HttpStatus.FORBIDDEN.isClientError()).isTrue();

        assertThat(HttpStatus.OK.isClientError()).isFalse();
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.isClientError()).isFalse();
    }

    @Test
    void isServerError_returns_true_for_5xx_codes() {
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.isServerError()).isTrue();
        assertThat(HttpStatus.BAD_GATEWAY.isServerError()).isTrue();

        assertThat(HttpStatus.OK.isServerError()).isFalse();
        assertThat(HttpStatus.NOT_FOUND.isServerError()).isFalse();
    }
}
