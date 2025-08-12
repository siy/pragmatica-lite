package org.pragmatica.net.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import org.pragmatica.lang.io.TimeSpan;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using real external APIs to validate HTTP client functionality
 * These tests require internet connectivity and may be affected by external service availability
 */
class HttpClientRealAPITest {
    
    private HttpClient client;
    
    record JsonPlaceholderPost(int id, int userId, String title, String body) {}
    record JsonPlaceholderUser(int id, String name, String username, String email, 
                              JsonPlaceholderAddress address, String phone, String website) {}
    record JsonPlaceholderAddress(String street, String suite, String city, String zipcode) {}
    record HttpBinResponse(Map<String, Object> headers, String origin, String url, Map<String, Object> args) {}
    
    @BeforeEach
    void setUp() {
        var config = HttpClientConfig.builder()
            .userAgent("pragmatica-http-client-integration-test/1.0")
            .connectTimeout(TimeSpan.timeSpan(10).seconds())
            .requestTimeout(TimeSpan.timeSpan(30).seconds())
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
    
    // === JSON API Tests ===
    
    @Test
    void shouldGetJsonFromJsonPlaceholder() {
        System.out.println("=== Testing GET request with JSON response ===");
        
        var response = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts").path("1")
            .get(JsonPlaceholderPost.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(200);
        assertThat(httpResponse.result().isSuccess()).isTrue();
        assertThat(httpResponse.result().orElseThrow().id()).isEqualTo(1);
        assertThat(httpResponse.result().orElseThrow().title()).isNotEmpty();
        
        System.out.println("✅ Successfully fetched post: " + httpResponse.result().orElseThrow().title());
    }
    
    @Test
    void shouldGetJsonArrayWithTypeToken() {
        System.out.println("=== Testing GET request with JSON array response ===");
        
        var response = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts")
            .queryParam("userId", "1")
            .get(new TypeToken<List<JsonPlaceholderPost>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.result().orElseThrow()).isNotEmpty();
        assertThat(httpResponse.result().orElseThrow().get(0).userId()).isEqualTo(1);
        
        System.out.println("✅ Successfully fetched " + httpResponse.result().orElseThrow().size() + " posts");
    }
    
    @Test
    void shouldPostJsonToJsonPlaceholder() {
        System.out.println("=== Testing POST request with JSON body ===");
        
        var newPost = new JsonPlaceholderPost(0, 1, "Test Post", "This is a test post body");
        
        var response = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts")
            .json()
            .post(newPost, JsonPlaceholderPost.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(201);
        assertThat(httpResponse.result().isSuccess()).isTrue();
        assertThat(httpResponse.result().orElseThrow().title()).isEqualTo("Test Post");
        
        System.out.println("✅ Successfully posted data, created ID: " + httpResponse.result().orElseThrow().id());
    }
    
    @Test
    void shouldPutJsonToJsonPlaceholder() {
        System.out.println("=== Testing PUT request with JSON body ===");
        
        var updatedPost = new JsonPlaceholderPost(1, 1, "Updated Post", "This is an updated post body");
        
        var response = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts").path("1")
            .json()
            .put(updatedPost, JsonPlaceholderPost.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.result().isSuccess()).isTrue();
        assertThat(httpResponse.result().orElseThrow().title()).isEqualTo("Updated Post");
        
        System.out.println("✅ Successfully updated post: " + httpResponse.result().orElseThrow().title());
    }
    
    @Test
    void shouldDeleteFromJsonPlaceholder() {
        System.out.println("=== Testing DELETE request ===");
        
        var response = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts").path("1")
            .delete(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(200);
        
        System.out.println("✅ Successfully deleted post");
    }
    
    // === Plain Text Tests ===
    
    @Test
    void shouldGetPlainTextFromHttpBin() {
        System.out.println("=== Testing plain text response ===");
        
        var response = client.resource("https://httpbin.org")
            .path("robots.txt")
            .plainText()
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.result().orElseThrow()).contains("User-agent");
        
        System.out.println("✅ Successfully fetched robots.txt");
    }
    
    // === Header Tests ===
    
    @Test
    void shouldSendAndReceiveHeaders() {
        System.out.println("=== Testing custom headers ===");
        
        var response = client.resource("https://httpbin.org")
            .path("headers")
            .header("X-Custom-Header", "test-value")
            .header("Authorization", "Bearer fake-token")
            .get(new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        var responseBody = httpResponse.result().orElseThrow();
        
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("X-Custom-Header");
        assertThat(headers).containsKey("Authorization");
        
        System.out.println("✅ Successfully sent and received custom headers");
    }
    
    // === Error Handling Tests ===
    
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
        
        // Test Result conversion
        var result = httpResponse.result();
        assertThat(result.isFailure()).isTrue();
        
        System.out.println("✅ Successfully handled 404 error");
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
        
        System.out.println("✅ Successfully handled 500 error");
    }
    
    // === Request Builder API Tests ===
    
    @Test
    void shouldUseRequestBuilderAPI() {
        System.out.println("=== Testing Request Builder API ===");
        
        var response = client.request()
            .url("https://httpbin.org/get")
            .method(HttpMethod.GET)
            .header("Accept", "application/json")
            .header("X-Test-Request-Builder", "true")
            .responseType(new TypeToken<Map<String, Object>>(){})
            .send()
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        var responseBody = httpResponse.result().orElseThrow();
        
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("X-Test-Request-Builder");
        
        System.out.println("✅ Successfully used Request Builder API");
    }
    
    // === Redirect Tests ===
    
    @Test
    void shouldFollowRedirects() {
        System.out.println("=== Testing redirect following ===");
        
        var response = client.resource("https://httpbin.org")
            .path("redirect").path("3")
            .get(new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(200);
        
        System.out.println("✅ Successfully followed redirects");
    }
    
    // === Template API Tests ===
    
    @Test
    void shouldUseTemplateAPI() {
        System.out.println("=== Testing Template API ===");
        
        var userId = 1;
        var response = client.get(
            "https://jsonplaceholder.typicode.com/users/{}", 
            new Object[]{userId}, 
            JsonPlaceholderUser.class
        ).await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.result().isSuccess()).isTrue();
        assertThat(httpResponse.result().orElseThrow().id()).isEqualTo(userId);
        
        System.out.println("✅ Successfully used Template API for user: " + httpResponse.result().orElseThrow().name());
    }
    
    @Test
    void shouldPostWithTemplateAPI() {
        System.out.println("=== Testing Template API POST ===");
        
        var newPost = Map.of(
            "title", "Template API Test",
            "body", "Testing template API POST",
            "userId", 1
        );
        
        var response = client.post(
            "https://jsonplaceholder.typicode.com/posts",
            new Object[]{},
            newPost,
            new TypeToken<Map<String, Object>>(){}
        ).await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.status().code()).isEqualTo(201);
        
        System.out.println("✅ Successfully used Template API POST");
    }
    
    // === Async Processing Tests ===
    
    @Test
    void shouldHandleMultipleAsyncRequests() {
        System.out.println("=== Testing multiple async requests ===");
        
        var post1Promise = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts").path("1")
            .get(JsonPlaceholderPost.class);
            
        var post2Promise = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts").path("2")
            .get(JsonPlaceholderPost.class);
            
        var post3Promise = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts").path("3")
            .get(JsonPlaceholderPost.class);
        
        // Wait for all to complete
        var allPosts = org.pragmatica.lang.Promise.all(post1Promise, post2Promise, post3Promise)
            .map((p1, p2, p3) -> List.of(p1.result().orElseThrow(), p2.result().orElseThrow(), p3.result().orElseThrow()))
            .await();
            
        assertThat(allPosts.isSuccess()).isTrue();
        var posts = allPosts.get();
        
        assertThat(posts).hasSize(3);
        assertThat(posts.get(0).id()).isEqualTo(1);
        assertThat(posts.get(1).id()).isEqualTo(2);
        assertThat(posts.get(2).id()).isEqualTo(3);
        
        System.out.println("✅ Successfully handled multiple async requests");
    }
}