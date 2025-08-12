package org.pragmatica.net.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import org.pragmatica.lang.io.TimeSpan;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for error handling and edge cases in HTTP client
 */
class HttpClientErrorHandlingTest {
    
    private HttpClient client;
    
    @BeforeEach
    void setUp() {
        var config = HttpClientConfig.builder()
            .userAgent("pragmatica-http-client-error-test/1.0")
            .connectTimeout(TimeSpan.timeSpan(5).seconds())
            .requestTimeout(TimeSpan.timeSpan(10).seconds())
            .followRedirects(true)
            .build();
            
        client = HttpClient.create(config);
        var startResult = client.start().await();
        assertThat(startResult.isSuccess()).isTrue();
    }
    
    @AfterEach
    void tearDown() {
        if (client != null) {
            var stopResult = client.stop().await();
            assertThat(stopResult.isSuccess()).isTrue();
        }
    }
    
    // === HTTP Error Status Tests ===
    
    @Test
    void shouldHandle400BadRequest() {
        System.out.println("=== Testing 400 Bad Request handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("400")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isError()).isTrue();
        assertThat(httpResponse.isClientError()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(400);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Test Result conversion
        var result = httpResponse.result();
        assertThat(result.isFailure()).isTrue();
        
        System.out.println("✅ Successfully handled 400 Bad Request");
    }
    
    @Test
    void shouldHandle401Unauthorized() {
        System.out.println("=== Testing 401 Unauthorized handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("401")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isError()).isTrue();
        assertThat(httpResponse.isClientError()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(401);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        System.out.println("✅ Successfully handled 401 Unauthorized");
    }
    
    @Test
    void shouldHandle403Forbidden() {
        System.out.println("=== Testing 403 Forbidden handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("403")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isError()).isTrue();
        assertThat(httpResponse.isClientError()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(403);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.FORBIDDEN);
        
        System.out.println("✅ Successfully handled 403 Forbidden");
    }
    
    @Test
    void shouldHandle404NotFound() {
        System.out.println("=== Testing 404 Not Found handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("404")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isError()).isTrue();
        assertThat(httpResponse.isClientError()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(404);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.NOT_FOUND);
        
        System.out.println("✅ Successfully handled 404 Not Found");
    }
    
    @Test
    void shouldHandle500InternalServerError() {
        System.out.println("=== Testing 500 Internal Server Error handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("500")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isError()).isTrue();
        assertThat(httpResponse.isServerError()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(500);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        System.out.println("✅ Successfully handled 500 Internal Server Error");
    }
    
    @Test
    void shouldHandle502BadGateway() {
        System.out.println("=== Testing 502 Bad Gateway handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("502")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isError()).isTrue();
        assertThat(httpResponse.isServerError()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(502);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
        
        System.out.println("✅ Successfully handled 502 Bad Gateway");
    }
    
    @Test
    void shouldHandle503ServiceUnavailable() {
        System.out.println("=== Testing 503 Service Unavailable handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("503")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isError()).isTrue();
        assertThat(httpResponse.isServerError()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(503);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        System.out.println("✅ Successfully handled 503 Service Unavailable");
    }
    
    // === Connection Error Tests ===
    
    @Test
    void shouldHandleConnectionRefused() {
        System.out.println("=== Testing connection refused handling ===");
        
        var response = client.resource("http://localhost:99999")
            .path("test")
            .get(String.class)
            .await();
            
        assertThat(response.isFailure()).isTrue();
        var cause = response.cause();
        
        // Should get a connection-related error
        assertThat(cause).isNotNull();
        System.out.println("✅ Successfully handled connection refused: " + cause.getClass().getSimpleName());
    }
    
    @Test
    void shouldHandleInvalidHostname() {
        System.out.println("=== Testing invalid hostname handling ===");
        
        var response = client.resource("https://this-hostname-definitely-does-not-exist-12345.com")
            .path("test")
            .get(String.class)
            .await();
            
        assertThat(response.isFailure()).isTrue();
        var cause = response.cause();
        
        // Should get a DNS resolution error
        assertThat(cause).isNotNull();
        System.out.println("✅ Successfully handled invalid hostname: " + cause.getClass().getSimpleName());
    }
    
    // === Timeout Tests ===
    
    @Test
    void shouldHandleTimeout() {
        System.out.println("=== Testing timeout handling ===");
        
        // Use httpbin's delay endpoint with a delay longer than our timeout
        var shortTimeoutClient = HttpClient.create(
            HttpClientConfig.builder()
                .requestTimeout(TimeSpan.timeSpan(1).seconds())
                .build()
        );
        
        var startResult = shortTimeoutClient.start().await();
        assertThat(startResult.isSuccess()).isTrue();
        
        try {
            var response = shortTimeoutClient.resource("https://httpbin.org")
                .path("delay").path("3") // 3 second delay
                .get(String.class)
                .await();
                
            assertThat(response.isFailure()).isTrue();
            var cause = response.cause();
            
            // Should get a timeout-related error
            assertThat(cause).isNotNull();
            System.out.println("✅ Successfully handled timeout: " + cause.getClass().getSimpleName());
            
        } finally {
            var stopResult = shortTimeoutClient.stop().await();
            assertThat(stopResult.isSuccess()).isTrue();
        }
    }
    
    // === Malformed URL Tests ===
    
    @Test
    void shouldHandleMalformedURL() {
        System.out.println("=== Testing malformed URL handling ===");
        
        try {
            var request = client.request()
                .url("not-a-valid-url")
                .get(String.class);
                
            var response = request.send().await();
            
            // Should either fail during request building or during execution
            assertThat(response.isFailure()).isTrue();
            System.out.println("✅ Successfully handled malformed URL");
            
        } catch (Exception e) {
            // Or it might throw an exception during request building
            assertThat(e).isNotNull();
            System.out.println("✅ Successfully caught malformed URL exception: " + e.getClass().getSimpleName());
        }
    }
    
    // === Large Response Tests ===
    
    @Test 
    void shouldHandleLargeResponse() {
        System.out.println("=== Testing large response handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("bytes").path("10240") // 10KB response
            .get(byte[].class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.body().isSuccess()).isTrue();
        assertThat(httpResponse.body().orElseThrow().length).isEqualTo(10240);
        
        System.out.println("✅ Successfully handled large response");
    }
    
    // === Special Character Tests ===
    
    @Test
    void shouldHandleSpecialCharactersInURL() {
        System.out.println("=== Testing special characters in URL ===");
        
        var response = client.resource("https://httpbin.org")
            .path("get")
            .queryParam("message", "Hello World! @#$%^&*()")
            .queryParam("unicode", "世界 🌍")
            .get(new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        var responseBody = httpResponse.body().orElseThrow();
        @SuppressWarnings("unchecked")
        var args = (Map<String, Object>) responseBody.get("args");
        assertThat(args).containsKey("message");
        assertThat(args).containsKey("unicode");
        
        System.out.println("✅ Successfully handled special characters in URL");
    }
    
    // === Empty Response Tests ===
    
    @Test
    void shouldHandleEmptyResponse() {
        System.out.println("=== Testing empty response handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("status").path("204") // No Content
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(204);
        assertThat(httpResponse.status()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Body might be null or empty for 204 No Content
        System.out.println("✅ Successfully handled empty response (204 No Content)");
    }
    
    // === Redirect Edge Cases ===
    
    @Test
    void shouldHandleTooManyRedirects() {
        System.out.println("=== Testing too many redirects handling ===");
        
        // httpbin.org's redirect endpoint with a high number
        var response = client.resource("https://httpbin.org")
            .path("redirect").path("10") // 10 redirects should be within limits
            .get(new TypeToken<Map<String, Object>>(){})
            .await();
            
        // This should either succeed (if within redirect limits) or fail gracefully
        if (response.isSuccess()) {
            var httpResponse = response.get();
            assertThat(httpResponse.isSuccess()).isTrue();
            System.out.println("✅ Successfully handled redirects");
        } else {
            // If it fails, it should be a proper error
            assertThat(response.cause()).isNotNull();
            System.out.println("✅ Successfully handled too many redirects error");
        }
    }
    
    // === Client State Tests ===
    
    @Test
    void shouldFailRequestsWhenClientNotStarted() {
        System.out.println("=== Testing requests on non-started client ===");
        
        var unstartedClient = HttpClient.create();
        
        var response = unstartedClient.resource("https://httpbin.org")
            .path("get")
            .get(String.class)
            .await();
            
        assertThat(response.isFailure()).isTrue();
        var cause = response.cause();
        
        assertThat(cause).isNotNull();
        System.out.println("✅ Successfully handled request on non-started client: " + cause.getClass().getSimpleName());
    }
    
    // === Concurrent Error Tests ===
    
    @Test
    void shouldHandleConcurrentErrors() {
        System.out.println("=== Testing concurrent error handling ===");
        
        var promise1 = client.resource("https://httpbin.org").path("status").path("404").get(String.class);
        var promise2 = client.resource("https://httpbin.org").path("status").path("500").get(String.class);
        var promise3 = client.resource("https://httpbin.org").path("status").path("503").get(String.class);
        
        // Wait for all to complete
        var allResponses = org.pragmatica.lang.Promise.all(promise1, promise2, promise3)
            .id()
            .await();
            
        assertThat(allResponses.isSuccess()).isTrue();
        var responses = allResponses.get();
        
        // All should be HTTP errors but the Promise should succeed
        assertThat(responses.t1().isError()).isTrue();
        assertThat(responses.t2().isError()).isTrue();
        assertThat(responses.t3().isError()).isTrue();
        
        assertThat(responses.t1().status().code()).isEqualTo(404);
        assertThat(responses.t2().status().code()).isEqualTo(500);
        assertThat(responses.t3().status().code()).isEqualTo(503);
        
        System.out.println("✅ Successfully handled concurrent errors");
    }
}