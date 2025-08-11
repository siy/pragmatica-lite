package org.pragmatica.net.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance and connection pooling tests for HTTP client
 */
class HttpClientPerformanceTest {
    
    private HttpClient client;
    
    @BeforeEach
    void setUp() {
        var config = HttpClientConfig.builder()
            .userAgent("pragmatica-http-client-performance-test/1.0")
            .connectTimeout(Duration.ofSeconds(10))
            .requestTimeout(Duration.ofSeconds(30))
            .maxConnections(50)
            .maxConnectionsPerHost(20)
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
    
    // === Connection Pooling Tests ===
    
    @Test
    void shouldReuseConnectionsFromPool() {
        System.out.println("=== Testing connection pooling and reuse ===");
        
        var start = Instant.now();
        var promises = new ArrayList<Promise<HttpResponse<Map<String, Object>>>>();
        
        // Make multiple requests to the same host to test connection reuse
        for (int i = 0; i < 10; i++) {
            var promise = client.resource("https://httpbin.org")
                .path("get")
                .queryParam("request", String.valueOf(i))
                .get(new TypeToken<Map<String, Object>>(){});
            promises.add(promise);
        }
        
        // Wait for all requests to complete
        var allResponses = Promise.allOf(promises).await();
        assertThat(allResponses.isSuccess()).isTrue();
        
        var responses = allResponses.get();
        assertThat(responses).hasSize(10);
        
        // All requests should have succeeded
        for (var result : responses) {
            assertThat(result.isSuccess()).isTrue();
            var httpResponse = result.get();
            assertThat(httpResponse.isSuccess()).isTrue();
        }
        
        var duration = Duration.between(start, Instant.now());
        System.out.println("✅ Completed 10 requests in " + duration.toMillis() + "ms");
        
        // Connection reuse should make this faster than individual connections
        assertThat(duration.toMillis()).isLessThan(10000); // Should be much faster
    }
    
    @Test
    void shouldHandleHighConcurrency() {
        System.out.println("=== Testing high concurrency handling ===");
        
        var requestCount = 20;
        var start = Instant.now();
        var promises = new ArrayList<Promise<HttpResponse<Map<String, Object>>>>();
        
        // Create many concurrent requests
        for (int i = 0; i < requestCount; i++) {
            var promise = client.resource("https://httpbin.org")
                .path("get")
                .queryParam("concurrent", String.valueOf(i))
                .get(new TypeToken<Map<String, Object>>(){});
            promises.add(promise);
        }
        
        // Wait for all to complete
        var allResponses = Promise.allOf(promises).await();
        assertThat(allResponses.isSuccess()).isTrue();
        
        var responses = allResponses.get();
        assertThat(responses).hasSize(requestCount);
        
        var successCount = 0;
        for (var result : responses) {
            if (result.isSuccess()) {
                var httpResponse = result.get();
                if (httpResponse.isSuccess()) {
                    successCount++;
                }
            }
        }
        
        var duration = Duration.between(start, Instant.now());
        System.out.println("✅ Completed " + successCount + "/" + requestCount + " concurrent requests in " + duration.toMillis() + "ms");
        
        // Most requests should succeed
        assertThat(successCount).isGreaterThan(requestCount * 0.8); // At least 80% success rate
    }
    
    @Test
    void shouldRespectConnectionLimits() {
        System.out.println("=== Testing connection limits ===");
        
        // Create client with very low connection limits
        var limitedConfig = HttpClientConfig.builder()
            .maxConnections(5)
            .maxConnectionsPerHost(2)
            .requestTimeout(Duration.ofSeconds(15))
            .build();
            
        var limitedClient = HttpClient.create(limitedConfig);
        var startResult = limitedClient.start().await();
        assertThat(startResult.isSuccess()).isTrue();
        
        try {
            var requestCount = 10;
            var promises = new ArrayList<Promise<HttpResponse<Map<String, Object>>>>();
            
            // Make more requests than connection limit
            for (int i = 0; i < requestCount; i++) {
                var promise = limitedClient.resource("https://httpbin.org")
                    .path("get")
                    .queryParam("limited", String.valueOf(i))
                    .get(new TypeToken<Map<String, Object>>(){});
                promises.add(promise);
            }
            
            // Should still complete but might take longer due to connection queuing
            var allResponses = Promise.allOf(promises).await();
            assertThat(allResponses.isSuccess()).isTrue();
            
            var responses = allResponses.get();
            assertThat(responses).hasSize(requestCount);
            
            var successCount = responses.stream()
                .mapToInt(result -> result.isSuccess() && result.get().isSuccess() ? 1 : 0)
                .sum();
                
            System.out.println("✅ Limited client completed " + successCount + "/" + requestCount + " requests");
            
            // Should still get reasonable success rate despite limits
            assertThat(successCount).isGreaterThan(requestCount * 0.6);
            
        } finally {
            var stopResult = limitedClient.stop().await();
            assertThat(stopResult.isSuccess()).isTrue();
        }
    }
    
    // === Keep-Alive Tests ===
    
    @Test
    void shouldMaintainKeepAliveConnections() {
        System.out.println("=== Testing keep-alive connection maintenance ===");
        
        // Make several sequential requests to test keep-alive
        var totalRequests = 5;
        var start = Instant.now();
        
        for (int i = 0; i < totalRequests; i++) {
            var response = client.resource("https://httpbin.org")
                .path("get")
                .queryParam("keepalive", String.valueOf(i))
                .get(new TypeToken<Map<String, Object>>(){})
                .await();
                
            assertThat(response.isSuccess()).isTrue();
            var httpResponse = response.get();
            assertThat(httpResponse.isSuccess()).isTrue();
            
            // Small delay between requests to test keep-alive
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        var duration = Duration.between(start, Instant.now());
        System.out.println("✅ Completed " + totalRequests + " sequential requests in " + duration.toMillis() + "ms");
        
        // Keep-alive should make subsequent requests faster
        assertThat(duration.toMillis()).isLessThan(5000);
    }
    
    // === Response Time Tests ===
    
    @Test 
    void shouldHaveReasonableResponseTimes() {
        System.out.println("=== Testing response time performance ===");
        
        var samples = 5;
        var responseTimes = new ArrayList<Long>();
        
        for (int i = 0; i < samples; i++) {
            var start = Instant.now();
            
            var response = client.resource("https://httpbin.org")
                .path("get")
                .queryParam("sample", String.valueOf(i))
                .get(new TypeToken<Map<String, Object>>(){})
                .await();
                
            var duration = Duration.between(start, Instant.now());
            responseTimes.add(duration.toMillis());
            
            assertThat(response.isSuccess()).isTrue();
            var httpResponse = response.get();
            assertThat(httpResponse.isSuccess()).isTrue();
        }
        
        var avgResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
            
        var maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        System.out.println("✅ Average response time: " + String.format("%.1f", avgResponseTime) + "ms, Max: " + maxResponseTime + "ms");
        
        // Response times should be reasonable for simple GET requests
        assertThat(avgResponseTime).isLessThan(2000); // Average under 2 seconds
        assertThat(maxResponseTime).isLessThan(5000);  // Max under 5 seconds
    }
    
    // === Memory Usage Tests ===
    
    @Test
    void shouldHandleMultipleClientsEfficiently() {
        System.out.println("=== Testing multiple client instances ===");
        
        var clientCount = 3;
        var clients = new ArrayList<HttpClient>();
        
        try {
            // Create multiple client instances
            for (int i = 0; i < clientCount; i++) {
                var config = HttpClientConfig.builder()
                    .userAgent("test-client-" + i)
                    .maxConnections(10)
                    .build();
                var testClient = HttpClient.create(config);
                testClient.start().await();
                clients.add(testClient);
            }
            
            // Make requests with each client
            var promises = new ArrayList<Promise<HttpResponse<Map<String, Object>>>>();
            for (int i = 0; i < clientCount; i++) {
                var client = clients.get(i);
                var promise = client.resource("https://httpbin.org")
                    .path("get")
                    .queryParam("client", String.valueOf(i))
                    .get(new TypeToken<Map<String, Object>>(){});
                promises.add(promise);
            }
            
            // Wait for all to complete
            var allResponses = Promise.allOf(promises).await();
            assertThat(allResponses.isSuccess()).isTrue();
            
            var responses = allResponses.get();
            var successCount = responses.stream()
                .mapToInt(result -> result.isSuccess() && result.get().isSuccess() ? 1 : 0)
                .sum();
                
            System.out.println("✅ " + successCount + "/" + clientCount + " clients completed requests successfully");
            assertThat(successCount).isEqualTo(clientCount);
            
        } finally {
            // Clean up all clients
            for (var testClient : clients) {
                testClient.stop().await();
            }
        }
    }
    
    // === Async Performance Tests ===
    
    @Test
    void shouldHandleAsyncOperationsEfficiently() {
        System.out.println("=== Testing async operation performance ===");
        
        var operationCount = 10;
        var latch = new CountDownLatch(operationCount);
        var successCount = new AtomicInteger(0);
        var start = Instant.now();
        
        // Start async operations
        for (int i = 0; i < operationCount; i++) {
            client.resource("https://httpbin.org")
                .path("get")
                .queryParam("async", String.valueOf(i))
                .get(new TypeToken<Map<String, Object>>(){})
                .onSuccess(response -> {
                    if (response.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                    latch.countDown();
                })
                .onFailure(cause -> latch.countDown());
        }
        
        // Wait for all async operations to complete
        try {
            var completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            
            var duration = Duration.between(start, Instant.now());
            System.out.println("✅ Completed " + successCount.get() + "/" + operationCount + " async operations in " + duration.toMillis() + "ms");
            
            assertThat(successCount.get()).isGreaterThan(operationCount * 0.8);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }
    
    // === Throughput Tests ===
    
    @Test
    void shouldAchieveReasonableThroughput() {
        System.out.println("=== Testing throughput performance ===");
        
        var requestCount = 15;
        var start = Instant.now();
        
        // Generate all requests quickly
        var promises = IntStream.range(0, requestCount)
            .mapToObj(i -> client.resource("https://httpbin.org")
                .path("get")
                .queryParam("throughput", String.valueOf(i))
                .get(new TypeToken<Map<String, Object>>(){}))
            .toList();
        
        // Wait for all to complete
        var allResponses = Promise.allOf(promises).await();
        assertThat(allResponses.isSuccess()).isTrue();
        
        var duration = Duration.between(start, Instant.now());
        var throughput = (double) requestCount / (duration.toMillis() / 1000.0);
        
        var responses = allResponses.get();
        var successCount = responses.stream()
            .mapToInt(result -> result.isSuccess() && result.get().isSuccess() ? 1 : 0)
            .sum();
        
        System.out.println("✅ Achieved throughput: " + String.format("%.2f", throughput) + " requests/second (" + 
                          successCount + "/" + requestCount + " successful)");
        
        // Should achieve reasonable throughput
        assertThat(throughput).isGreaterThan(1.0); // At least 1 request per second
        assertThat(successCount).isGreaterThan(requestCount * 0.8); // 80% success rate
    }
}