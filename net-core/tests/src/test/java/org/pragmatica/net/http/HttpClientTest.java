package org.pragmatica.net.http;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test HTTP client functionality
 */
class HttpClientTest {
    
    @Test
    void shouldCreateHttpClient() {
        var client = HttpClient.create();
        
        assertThat(client).isNotNull();
    }
    
    @Test 
    void shouldCreateHttpClientWithConfig() {
        var config = HttpClientConfig.builder()
            .userAgent("test-client")
            .followRedirects(false)
            .build();
            
        var client = HttpClient.create(config);
        
        assertThat(client).isNotNull();
    }
    
    @Test
    void shouldCreateRequestBuilder() {
        var client = HttpClient.create();
        var builder = client.request();
        
        assertThat(builder).isNotNull();
    }
    
    @Test
    void shouldBuildRequest() {
        var client = HttpClient.create();
        
        var request = client.request()
            .url("https://httpbin.org/get")
            .method(HttpMethod.GET)
            .header("Accept", "application/json")
            .responseType(String.class);
            
        assertThat(request).isNotNull();
        assertThat(request.url()).isEqualTo("https://httpbin.org/get");
        assertThat(request.method()).isEqualTo(HttpMethod.GET);
        assertThat(request.headers().first("Accept")).isPresent();
        assertThat(request.responseType()).isEqualTo(String.class);
    }
    
    @Test
    void shouldBuildRequestWithTypeToken() {
        var client = HttpClient.create();
        
        var request = client.request()
            .url("https://httpbin.org/get")
            .responseType(new TypeToken<Map<String, Object>>(){});
            
        assertThat(request).isNotNull();
        assertThat(request.responseTypeToken()).isNotNull();
    }
    
    @Test
    void shouldCreateResource() {
        var client = HttpClient.create();
        var resource = client.resource("https://api.example.com");
        
        assertThat(resource).isNotNull();
        assertThat(resource.baseUrl()).isEqualTo("https://api.example.com");
    }
    
    @Test
    void shouldHandleHttpStatusCorrectly() {
        assertThat(HttpStatus.OK.isSuccess()).isTrue();
        assertThat(HttpStatus.NOT_FOUND.isClientError()).isTrue();
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.isServerError()).isTrue();
        assertThat(HttpStatus.NOT_FOUND.isError()).isTrue();
    }
    
    @Test
    void shouldCreateHttpHeaders() {
        var headers = new HttpHeaders()
            .add("Accept", "application/json")
            .add("Content-Type", "application/json")
            .set("Authorization", "Bearer token");
            
        assertThat(headers.first("Accept")).contains("application/json");
        assertThat(headers.first("Authorization")).contains("Bearer token");
        assertThat(headers.contains("Content-Type")).isTrue();
    }
    
    @Test
    void shouldStartAndStopClient() {
        var client = HttpClient.create();
        
        var startResult = client.start().await();
        assertThat(startResult.isSuccess()).isTrue();
        
        var stopResult = client.stop().await();
        assertThat(stopResult.isSuccess()).isTrue();
    }
}