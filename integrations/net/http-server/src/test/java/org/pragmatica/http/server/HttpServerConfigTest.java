package org.pragmatica.http.server;

import org.junit.jupiter.api.Test;
import org.pragmatica.net.tcp.TlsConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.http.server.HttpServerConfig.httpServerConfig;

class HttpServerConfigTest {

    @Test
    void httpServerConfig_creates_http_config_with_defaults() {
        var config = httpServerConfig("test", 8080);

        assertThat(config.port()).isEqualTo(8080);
        assertThat(config.name()).isEqualTo("test");
        assertThat(config.tls().isPresent()).isFalse();
        assertThat(config.maxContentLength()).isEqualTo(65536);
    }

    @Test
    void withMaxContentLength_customizes_max_content() {
        var config = httpServerConfig("test", 9000)
            .withMaxContentLength(2048);

        assertThat(config.port()).isEqualTo(9000);
        assertThat(config.maxContentLength()).isEqualTo(2048);
    }

    @Test
    void withTls_creates_tls_config() {
        var tls = TlsConfig.selfSigned();
        var config = httpServerConfig("test", 8443)
            .withTls(tls);

        assertThat(config.port()).isEqualTo(8443);
        assertThat(config.tls().isPresent()).isTrue();
    }

    @Test
    void withTls_and_maxContentLength_combines_options() {
        var tls = TlsConfig.selfSigned();
        var config = httpServerConfig("test", 443)
            .withTls(tls)
            .withMaxContentLength(4096);

        assertThat(config.port()).isEqualTo(443);
        assertThat(config.maxContentLength()).isEqualTo(4096);
        assertThat(config.tls().isPresent()).isTrue();
    }

    @Test
    void withChunkedWrite_enables_chunked_transfer() {
        var config = httpServerConfig("test", 8080)
            .withChunkedWrite();

        assertThat(config.chunkedWriteEnabled()).isTrue();
    }

    @Test
    void webSocketEndpoints_is_empty_by_default() {
        var config = httpServerConfig("test", 8080);

        assertThat(config.webSocketEndpoints()).isEmpty();
    }
}
