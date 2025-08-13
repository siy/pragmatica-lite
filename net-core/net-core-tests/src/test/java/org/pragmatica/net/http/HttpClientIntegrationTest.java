package org.pragmatica.net.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test demonstrating HTTP client API usage patterns
 */
class HttpClientIntegrationTest {
    
    private HttpClient client;
    
    @BeforeEach
    void setUp() {
        var config = HttpClientConfig.builder()
            .userAgent("pragmatica-http-client-test/1.0")
            .followRedirects(true)
            .build();
            
        client = HttpClient.create(config);
    }
    
    @Test
    void demonstrateRequestBuilderAPI() {
        // This demonstrates the fluent request builder API
        var request = client.request()
            .url("https://httpbin.org/get")
            .method(HttpMethod.GET)
            .header("Accept", "application/json")
            .header("X-Test-Header", "test-value")
            .expectedType(String.class);
            
        // Verify request was built correctly
        assertThat(request.url()).isEqualTo("https://httpbin.org/get");
        assertThat(request.method()).isEqualTo(HttpMethod.GET);
        assertThat(request.headers().first("Accept")).contains("application/json");
        assertThat(request.headers().first("X-Test-Header")).contains("test-value");
        assertThat(request.expectedType().getRawType()).isEqualTo(String.class);
    }
    
    @Test
    void demonstrateResourceBasedAPI() {
        // This demonstrates the resource-based DSL API
        var apiResource = client.resource("https://httpbin.org")
            .path("json")
            .queryParam("test", "value")
            .header("Accept", "application/json");
            
        // Verify resource was built correctly
        assertThat(apiResource.baseUrl()).isEqualTo("https://httpbin.org");
        assertThat(apiResource.path()).isEqualTo("json");
        assertThat(apiResource.headers().first("Accept")).contains("application/json");
    }
    
    @Test
    void demonstrateTypeTokenSupport() {
        // This demonstrates TypeToken support for generic types
        var request = client.request()
            .url("https://httpbin.org/json")
            .expectedType(new TypeToken<Map<String, Object>>(){});
            
        assertThat(request.expectedType()).isNotNull();
    }
    
    @Test
    void demonstrateConvenienceMethods() {
        // This demonstrates the convenience methods
        var resource = client.resource("https://httpbin.org");
        
        // These methods return Promises that would normally be executed
        var getRequest = resource.path("get");
        var postRequest = resource.path("post");
        var putRequest = resource.path("put");
        var deleteRequest = resource.path("delete");
        
        assertThat(getRequest).isNotNull();
        assertThat(postRequest).isNotNull();
        assertThat(putRequest).isNotNull();
        assertThat(deleteRequest).isNotNull();
    }
    
    @Test
    void demonstrateStartStopLifecycle() {
        var testClient = HttpClient.create();
        
        // Start the client
        var startResult = testClient.start().await();
        assertThat(startResult.isSuccess()).isTrue();
        
        // Stop the client
        var stopResult = testClient.stop().await();
        assertThat(stopResult.isSuccess()).isTrue();
    }
}