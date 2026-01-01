package org.pragmatica.http.server;

import org.junit.jupiter.api.Test;
import org.pragmatica.http.Headers;
import org.pragmatica.http.HttpMethod;
import org.pragmatica.http.QueryParams;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextTest {

    // Test implementation of RequestContext interface
    record TestRequestContext(
        String requestId,
        HttpMethod method,
        String path,
        Headers headers,
        QueryParams queryParams,
        byte[] body
    ) implements RequestContext {}

    @Test
    void header_returns_value_case_insensitively() {
        var headers = Headers.fromSingleValueMap(Map.of("content-type", "application/json", "x-custom", "value"));
        var ctx = new TestRequestContext("req_1", HttpMethod.GET, "/path", headers, QueryParams.empty(), new byte[0]);

        assertThat(ctx.headers().get("Content-Type").isPresent()).isTrue();
        ctx.headers().get("Content-Type").onPresent(v -> assertThat(v).isEqualTo("application/json"));

        assertThat(ctx.headers().get("X-Custom").isPresent()).isTrue();
        ctx.headers().get("X-Custom").onPresent(v -> assertThat(v).isEqualTo("value"));
    }

    @Test
    void header_returns_empty_for_missing_header() {
        var ctx = new TestRequestContext("req_1", HttpMethod.GET, "/path", Headers.empty(), QueryParams.empty(), new byte[0]);

        assertThat(ctx.headers().get("X-Missing").isPresent()).isFalse();
    }

    @Test
    void queryParam_returns_values() {
        var params = QueryParams.queryParams(Map.of(
            "foo", List.of("bar"),
            "baz", List.of("qux")
        ));
        var ctx = new TestRequestContext("req_1", HttpMethod.GET, "/path", Headers.empty(), params, new byte[0]);

        assertThat(ctx.queryParams().get("foo").isPresent()).isTrue();
        ctx.queryParams().get("foo").onPresent(v -> assertThat(v).isEqualTo("bar"));

        assertThat(ctx.queryParams().get("baz").isPresent()).isTrue();
        ctx.queryParams().get("baz").onPresent(v -> assertThat(v).isEqualTo("qux"));
    }

    @Test
    void queryParam_returns_empty_for_missing_param() {
        var params = QueryParams.queryParams(Map.of("foo", List.of("bar")));
        var ctx = new TestRequestContext("req_1", HttpMethod.GET, "/path", Headers.empty(), params, new byte[0]);

        assertThat(ctx.queryParams().get("missing").isPresent()).isFalse();
    }

    @Test
    void queryParams_returns_all_params() {
        var params = QueryParams.queryParams(Map.of(
            "a", List.of("1"),
            "b", List.of("2"),
            "c", List.of("3")
        ));
        var ctx = new TestRequestContext("req_1", HttpMethod.GET, "/path", Headers.empty(), params, new byte[0]);

        var map = ctx.queryParams().asMap();
        assertThat(map).containsEntry("a", List.of("1"));
        assertThat(map).containsEntry("b", List.of("2"));
        assertThat(map).containsEntry("c", List.of("3"));
    }

    @Test
    void queryParams_returns_empty_map_for_empty_query() {
        var ctx = new TestRequestContext("req_1", HttpMethod.GET, "/path", Headers.empty(), QueryParams.empty(), new byte[0]);

        assertThat(ctx.queryParams().asMap()).isEmpty();
    }

    @Test
    void bodyAsString_returns_utf8_content() {
        var body = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        var ctx = new TestRequestContext("req_1", HttpMethod.POST, "/path", Headers.empty(), QueryParams.empty(), body);

        assertThat(ctx.bodyAsString()).isEqualTo("Hello, World!");
    }

    @Test
    void bodyAsString_returns_empty_for_empty_body() {
        var ctx = new TestRequestContext("req_1", HttpMethod.POST, "/path", Headers.empty(), QueryParams.empty(), new byte[0]);

        assertThat(ctx.bodyAsString()).isEmpty();
    }

    @Test
    void hasBody_returns_true_when_body_present() {
        var body = "content".getBytes(StandardCharsets.UTF_8);
        var ctx = new TestRequestContext("req_1", HttpMethod.POST, "/path", Headers.empty(), QueryParams.empty(), body);

        assertThat(ctx.hasBody()).isTrue();
    }

    @Test
    void hasBody_returns_false_when_body_empty() {
        var ctx = new TestRequestContext("req_1", HttpMethod.POST, "/path", Headers.empty(), QueryParams.empty(), new byte[0]);

        assertThat(ctx.hasBody()).isFalse();
    }

    @Test
    void contentType_returns_content_type_header() {
        var headers = Headers.fromSingleValueMap(Map.of("content-type", "application/json"));
        var ctx = new TestRequestContext("req_1", HttpMethod.POST, "/path", headers, QueryParams.empty(), new byte[0]);

        assertThat(ctx.headers().get("content-type").isPresent()).isTrue();
        ctx.headers().get("content-type").onPresent(v -> assertThat(v).isEqualTo("application/json"));
    }

    @Test
    void requestId_is_accessible() {
        var ctx = new TestRequestContext("req_123", HttpMethod.GET, "/path", Headers.empty(), QueryParams.empty(), new byte[0]);

        assertThat(ctx.requestId()).isEqualTo("req_123");
    }

    @Test
    void method_is_accessible() {
        var ctx = new TestRequestContext("req_1", HttpMethod.POST, "/path", Headers.empty(), QueryParams.empty(), new byte[0]);

        assertThat(ctx.method()).isEqualTo(HttpMethod.POST);
    }

    @Test
    void path_is_accessible() {
        var ctx = new TestRequestContext("req_1", HttpMethod.GET, "/api/users", Headers.empty(), QueryParams.empty(), new byte[0]);

        assertThat(ctx.path()).isEqualTo("/api/users");
    }
}
