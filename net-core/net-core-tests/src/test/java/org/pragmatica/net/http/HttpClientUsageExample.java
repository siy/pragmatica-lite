package org.pragmatica.net.http;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive example demonstrating HTTP client usage patterns
 */
class HttpClientUsageExample {
    
    record User(String name, String email, int id) {}
    
    @Test
    void demonstrateHttpClientUsage() {
        // 1. Create HTTP client with custom configuration
        var config = HttpClientConfig.builder()
            .userAgent("my-awesome-app/1.0")
            .followRedirects(true)
            .defaultHeader("Accept", "application/json")
            .build();
            
        var client = HttpClient.create(config);
        
        // 2. Request Builder Pattern - Flexible for complex requests
        var complexRequest = client.request()
            .url("https://jsonplaceholder.typicode.com/users")
            .method(HttpMethod.GET)
            .header("Authorization", "Bearer your-token")
            .header("X-Custom-Header", "custom-value")
            .expectedType(new TypeToken<List<User>>(){});
            
        // 3. Resource-based DSL - Great for REST APIs
        var usersApi = client.resource("https://jsonplaceholder.typicode.com")
            .path("users")
            .header("Authorization", "Bearer your-token");
            
        // 4. CRUD operations with type safety
        // GET /users
        var getAllUsers = usersApi.get(new TypeToken<List<User>>(){});
        
        // POST /users
        var newUser = new User("John Doe", "john@example.com", 0);
        var createUser = usersApi.post(newUser, User.class);
        
        // GET /users/1
        var getUser = usersApi.path("1").get(User.class);
        
        // PUT /users/1  
        var updatedUser = new User("John Smith", "john.smith@example.com", 1);
        var updateUser = usersApi.path("1").put(updatedUser, User.class);
        
        // DELETE /users/1
        var deleteUser = usersApi.path("1").delete(String.class);
        
        // 5. Query parameters and path building
        var searchApi = client.resource("https://jsonplaceholder.typicode.com")
            .path("posts")
            .queryParam("userId", "1")
            .queryParam("title", "searchTerm");
            
        // 6. Working with different response types
        // String response
        var textResponse = client.request()
            .url("https://httpbin.org/html")
            .expectedType(String.class);
            
        // JSON object response
        var jsonResponse = client.request()
            .url("https://httpbin.org/json")
            .expectedType(new TypeToken<Map<String, Object>>(){});
            
        // Binary response
        var binaryResponse = client.request()
            .url("https://httpbin.org/bytes/1024")
            .expectedType(byte[].class);
        
        // 7. Error handling with Result pattern
        /*
        var result = client.request()
            .url("https://httpbin.org/status/404")
            .get(String.class)
            .map(HttpResponse::result) // Convert to Result<String>
            .await();
            
        if (result.isFailure()) {
            var error = result.cause();
            System.out.println("Request failed: " + error.message());
        }
        */
        
        // 8. Async processing with Promise chaining
        /*
        client.request()
            .url("https://jsonplaceholder.typicode.com/users/1")
            .get(User.class)
            .map(HttpResponse::body)
            .onSuccess(user -> System.out.println("Got user: " + user.name()))
            .onFailure(cause -> System.out.println("Failed: " + cause.message()));
        */
        
        System.out.println("HTTP client usage examples created successfully!");
    }
}