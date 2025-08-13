package org.pragmatica.net.http;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExternalAPITest {
    
    @Test
    void demonstrateBasicHTTPClientLifecycle() {
        System.out.println("=== Testing Basic HTTP Client Lifecycle ===\n");
        
        // Create HTTP client
        var config = HttpClientConfig.defaults();
        var client = HttpClient.create(config);
        
        // Start the client
        var startResult = client.start().await();
        assertThat(startResult.isSuccess()).isTrue();
        System.out.println("✅ HttpClient started successfully");
        
        // Stop the client
        var stopResult = client.stop().await();
        assertThat(stopResult.isSuccess()).isTrue();
        System.out.println("✅ HttpClient stopped successfully");
        
        System.out.println("\n🎉 Basic HTTP client lifecycle test completed!");
    }
    
    @Test
    void demonstrateResourceAPIWithExternalService() {
        System.out.println("=== Testing Resource API with JSONPlaceholder ===\n");
        
        // Create and start HTTP client
        var config = HttpClientConfig.defaults();
        var client = HttpClient.create(config);
        var startResult = client.start().await();
        assertThat(startResult.isSuccess()).isTrue();
        
        try {
            // Test Resource API with a real external service
            var resource = client.resource("https://jsonplaceholder.typicode.com")
                .path("posts")
                .path("1");
            
            System.out.println("✅ Resource API chain created successfully");
            System.out.println("📋 Resource URL: " + resource.baseUrl() + "/" + resource.path());
            
            // For now, just verify the resource was created correctly
            assertThat(resource.baseUrl()).isEqualTo("https://jsonplaceholder.typicode.com");
            assertThat(resource.path()).contains("posts");
            
        } finally {
            // Always stop the client
            var stopResult = client.stop().await();
            assertThat(stopResult.isSuccess()).isTrue();
        }
        
        System.out.println("✅ Resource API test completed!");
    }
    
    // Simple data class for testing
    record Post(int id, int userId, String title, String body) {}
}