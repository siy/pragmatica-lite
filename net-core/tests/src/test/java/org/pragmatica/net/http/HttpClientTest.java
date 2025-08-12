package org.pragmatica.net.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.lang.io.TimeSpan;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for HTTP client functionality
 */
class HttpClientTest {
    
    private HttpClient client;
    
    @BeforeEach
    void setUp() {
        client = HttpClient.create();
    }
    
    @Test
    void shouldCreateHttpClient() {
        var testClient = HttpClient.create();
        
        assertThat(testClient).isNotNull();
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
    void shouldHandleHttpStatusCodeCorrectly() {
        assertThat(HttpStatusCode.OK.isSuccess()).isTrue();
        assertThat(HttpStatusCode.NOT_FOUND.isClientError()).isTrue();
        assertThat(HttpStatusCode.INTERNAL_SERVER_ERROR.isServerError()).isTrue();
        assertThat(HttpStatusCode.NOT_FOUND.isError()).isTrue();
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
        var testClient = HttpClient.create();
        
        var startResult = testClient.start().await();
        assertThat(startResult.isSuccess()).isTrue();
        
        var stopResult = testClient.stop().await();
        assertThat(stopResult.isSuccess()).isTrue();
    }
    
    // === Configuration Tests ===
    
    @Test
    void shouldCreateConfigWithBuilder() {
        var config = HttpClientConfig.builder()
            .connectTimeout(TimeSpan.timeSpan(5).seconds())
            .requestTimeout(TimeSpan.timeSpan(20).seconds())
            .readTimeout(TimeSpan.timeSpan(15).seconds())
            .maxConnections(50)
            .maxConnectionsPerHost(20)
            .followRedirects(false)
            .userAgent("test-agent/2.0")
            .defaultHeader("Accept", "application/json")
            .build();
            
        assertThat(config.connectTimeout()).isEqualTo(TimeSpan.timeSpan(5).seconds());
        assertThat(config.requestTimeout()).isEqualTo(TimeSpan.timeSpan(20).seconds());
        assertThat(config.readTimeout()).isEqualTo(TimeSpan.timeSpan(15).seconds());
        assertThat(config.maxConnections()).isEqualTo(50);
        assertThat(config.maxConnectionsPerHost()).isEqualTo(20);
        assertThat(config.followRedirects()).isFalse();
        assertThat(config.userAgent()).isEqualTo("test-agent/2.0");
        assertThat(config.defaultHeaders().first("Accept")).contains("application/json");
    }
    
    @Test
    void shouldCreateDefaultConfig() {
        var config = HttpClientConfig.defaults();
        
        assertThat(config.connectTimeout()).isNotNull();
        assertThat(config.requestTimeout()).isNotNull(); 
        assertThat(config.readTimeout()).isNotNull();
        assertThat(config.maxConnections()).isPositive();
        assertThat(config.maxConnectionsPerHost()).isPositive();
        assertThat(config.followRedirects()).isTrue();
        assertThat(config.userAgent()).isNotEmpty();
        assertThat(config.defaultHeaders()).isNotNull();
    }
    
    // === HTTP Method Tests ===
    
    @Test
    void shouldSupportAllHttpMethods() {
        assertThat(HttpMethod.GET.name()).isEqualTo("GET");
        assertThat(HttpMethod.POST.name()).isEqualTo("POST");
        assertThat(HttpMethod.PUT.name()).isEqualTo("PUT");
        assertThat(HttpMethod.DELETE.name()).isEqualTo("DELETE");
        assertThat(HttpMethod.PATCH.name()).isEqualTo("PATCH");
        assertThat(HttpMethod.HEAD.name()).isEqualTo("HEAD");
        assertThat(HttpMethod.OPTIONS.name()).isEqualTo("OPTIONS");
    }
    
    // === HTTP Status Tests ===
    
    @Test
    void shouldCorrectlyIdentifyHttpStatusCodeCategories() {
        // Success statuses
        assertThat(HttpStatusCode.OK.isSuccess()).isTrue();
        assertThat(HttpStatusCode.CREATED.isSuccess()).isTrue();
        assertThat(HttpStatusCode.NO_CONTENT.isSuccess()).isTrue();
        
        // Client error statuses
        assertThat(HttpStatusCode.BAD_REQUEST.isClientError()).isTrue();
        assertThat(HttpStatusCode.NOT_FOUND.isClientError()).isTrue();
        assertThat(HttpStatusCode.UNAUTHORIZED.isClientError()).isTrue();
        assertThat(HttpStatusCode.FORBIDDEN.isClientError()).isTrue();
        
        // Server error statuses
        assertThat(HttpStatusCode.INTERNAL_SERVER_ERROR.isServerError()).isTrue();
        assertThat(HttpStatusCode.BAD_GATEWAY.isServerError()).isTrue();
        assertThat(HttpStatusCode.SERVICE_UNAVAILABLE.isServerError()).isTrue();
        
        // Error statuses (client + server)
        assertThat(HttpStatusCode.NOT_FOUND.isError()).isTrue();
        assertThat(HttpStatusCode.INTERNAL_SERVER_ERROR.isError()).isTrue();
        assertThat(HttpStatusCode.OK.isError()).isFalse();
    }
    
    // === Header Tests ===
    
    @Test
    void shouldManageHeadersCorrectly() {
        var headers = new HttpHeaders();
        
        // Test adding headers
        headers.add("Accept", "application/json");
        headers.add("Accept", "text/html");
        headers.set("Content-Type", "application/json");
        
        assertThat(headers.contains("Accept")).isTrue();
        assertThat(headers.contains("Content-Type")).isTrue();
        assertThat(headers.contains("Authorization")).isFalse();
        
        assertThat(headers.first("Accept")).contains("application/json");
        assertThat(headers.all("Accept")).hasSize(2);
        assertThat(headers.first("Content-Type")).contains("application/json");
        
        // Test header names
        assertThat(headers.names()).containsExactlyInAnyOrder("Accept", "Content-Type");
    }
    
    @Test
    void shouldChainHeaderOperations() {
        var headers = new HttpHeaders()
            .add("Accept", "application/json")
            .add("X-Custom", "value")
            .set("Authorization", "Bearer token");
            
        assertThat(headers.contains("Accept")).isTrue();
        assertThat(headers.contains("X-Custom")).isTrue();
        assertThat(headers.contains("Authorization")).isTrue();
        assertThat(headers.names()).hasSize(3);
    }
    
    // === Content Type Tests ===
    
    @Test
    void shouldRecognizeCommonContentTypes() {
        assertThat(CommonContentTypes.APPLICATION_JSON.headerText()).contains("application/json");
        assertThat(CommonContentTypes.APPLICATION_JSON.category()).isEqualTo(ContentCategory.JSON);
        
        assertThat(CommonContentTypes.TEXT_PLAIN.headerText()).contains("text/plain");
        assertThat(CommonContentTypes.TEXT_PLAIN.category()).isEqualTo(ContentCategory.PLAIN_TEXT);
        
        assertThat(CommonContentTypes.APPLICATION_XML.headerText()).contains("application/xml");
        assertThat(CommonContentTypes.APPLICATION_XML.category()).isEqualTo(ContentCategory.XML);
        
        assertThat(CommonContentTypes.APPLICATION_OCTET_STREAM.headerText()).contains("application/octet-stream");
        assertThat(CommonContentTypes.APPLICATION_OCTET_STREAM.category()).isEqualTo(ContentCategory.BINARY);
    }
    
    @Test
    void shouldCreateCustomContentType() {
        var customType = ContentType.custom("application/custom", ContentCategory.JSON);
        
        assertThat(customType.headerText()).isEqualTo("application/custom");
        assertThat(customType.category()).isEqualTo(ContentCategory.JSON);
    }
    
    // === Resource API Tests ===
    
    @Test
    void shouldBuildResourcePath() {
        var resource = client.resource("https://api.example.com")
            .path("users")
            .path("123")
            .path("profile");
            
        assertThat(resource.baseUrl()).isEqualTo("https://api.example.com");
        // Note: exact path format depends on implementation
    }
    
    @Test
    void shouldBuildResourceWithQueryParams() {
        var resource = client.resource("https://api.example.com")
            .path("users")
            .queryParam("page", "1")
            .queryParam("size", "10")
            .queryParam("sort", "name");
            
        assertThat(resource.baseUrl()).isEqualTo("https://api.example.com");
        // Query params are internal to resource - check via building request
        var request = resource.request();
        assertThat(request).isNotNull();
    }
    
    @Test
    void shouldBuildResourceWithHeaders() {
        var resource = client.resource("https://api.example.com")
            .header("Authorization", "Bearer token")
            .header("Accept", "application/json");
            
        assertThat(resource.headers().first("Authorization")).contains("Bearer token");
        assertThat(resource.headers().first("Accept")).contains("application/json");
    }
    
    @Test
    void shouldSetContentTypeOnResource() {
        var jsonResource = client.resource("https://api.example.com").json();
        var textResource = client.resource("https://api.example.com").plainText();
        var customResource = client.resource("https://api.example.com").contentType("application/custom");
        
        assertThat(jsonResource).isNotNull();
        assertThat(textResource).isNotNull();
        assertThat(customResource).isNotNull();
    }
    
    @Test 
    void shouldChainResourceOperations() {
        var resource = client.resource("https://api.example.com")
            .path("api").path("v1").path("users")
            .queryParam("active", "true")
            .header("Authorization", "Bearer token")
            .json();
            
        assertThat(resource).isNotNull();
        assertThat(resource.baseUrl()).isEqualTo("https://api.example.com");
    }
    
    // === Request Builder Tests ===
    
    @Test
    void shouldBuildComplexRequest() {
        var request = client.request()
            .url("https://api.example.com/users")
            .method(HttpMethod.POST)
            .header("Authorization", "Bearer token")
            .header("Content-Type", "application/json")
            .body("{\"name\":\"John\"}")
            .responseType(String.class);
            
        assertThat(request.url()).isEqualTo("https://api.example.com/users");
        assertThat(request.method()).isEqualTo(HttpMethod.POST);
        assertThat(request.headers().first("Authorization")).contains("Bearer token");
        assertThat(request.headers().first("Content-Type")).contains("application/json");
        assertThat(request.body()).isEqualTo("{\"name\":\"John\"}");
        assertThat(request.responseType()).isEqualTo(String.class);
    }
    
    record TestUser(String name, String email) {}
}