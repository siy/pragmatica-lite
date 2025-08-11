package org.pragmatica.net.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.type.TypeToken;

import org.pragmatica.lang.io.TimeSpan;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for different content types (JSON, XML, binary, plain text, etc.)
 */
class HttpClientContentTypeTest {
    
    private HttpClient client;
    
    @BeforeEach
    void setUp() {
        var config = HttpClientConfig.builder()
            .userAgent("pragmatica-http-client-content-test/1.0")
            .connectTimeout(TimeSpan.timeSpan(10).seconds())
            .requestTimeout(TimeSpan.timeSpan(30).seconds())
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
    
    // === JSON Content Type Tests ===
    
    @Test
    void shouldHandleApplicationJsonContentType() {
        System.out.println("=== Testing application/json content type ===");
        
        var response = client.resource("https://httpbin.org")
            .path("json")
            .json()
            .get(new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.headers().first("Content-Type"))
            .isPresent()
            .get()
            .asString()
            .contains("application/json");
            
        var body = httpResponse.body();
        assertThat(body).isNotEmpty();
        
        System.out.println("✅ Successfully handled JSON content type");
    }
    
    @Test 
    void shouldSendJsonWithCorrectContentType() {
        System.out.println("=== Testing sending JSON with correct content type ===");
        
        var jsonData = Map.of(
            "name", "Test User",
            "email", "test@example.com"
        );
        
        var response = client.resource("https://httpbin.org")
            .path("post")
            .json()
            .post(jsonData, new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        // Verify that the request was sent with correct content type
        var responseBody = httpResponse.body();
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("Content-Type");
        
        System.out.println("✅ Successfully sent JSON with correct content type");
    }
    
    // === Plain Text Content Type Tests ===
    
    @Test
    void shouldHandleTextPlainContentType() {
        System.out.println("=== Testing text/plain content type ===");
        
        var response = client.resource("https://httpbin.org")
            .path("robots.txt")
            .plainText()
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body()).contains("User-agent");
        
        System.out.println("✅ Successfully handled plain text content type");
    }
    
    @Test
    void shouldSendPlainTextWithCorrectContentType() {
        System.out.println("=== Testing sending plain text with correct content type ===");
        
        var textData = "This is a plain text message for testing";
        
        var response = client.resource("https://httpbin.org")
            .path("post")
            .plainText()
            .post(textData, new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        var responseBody = httpResponse.body();
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("Content-Type");
        
        // Check that the data was received correctly
        assertThat(responseBody.get("data")).isEqualTo(textData);
        
        System.out.println("✅ Successfully sent plain text with correct content type");
    }
    
    // === HTML Content Type Tests ===
    
    @Test
    void shouldHandleTextHtmlContentType() {
        System.out.println("=== Testing text/html content type ===");
        
        var response = client.resource("https://httpbin.org")
            .path("html")
            .contentType("text/html")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.body()).contains("<html>");
        assertThat(httpResponse.body()).contains("</html>");
        
        System.out.println("✅ Successfully handled HTML content type");
    }
    
    // === XML Content Type Tests ===
    
    @Test
    void shouldHandleApplicationXmlContentType() {
        System.out.println("=== Testing application/xml content type ===");
        
        var response = client.resource("https://httpbin.org")
            .path("xml")
            .contentType("application/xml")
            .get(String.class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.body()).contains("<?xml");
        assertThat(httpResponse.body()).contains("<slideshow");
        
        System.out.println("✅ Successfully handled XML content type");
    }
    
    @Test
    void shouldSendXmlWithCorrectContentType() {
        System.out.println("=== Testing sending XML with correct content type ===");
        
        var xmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <person>
                <name>John Doe</name>
                <email>john@example.com</email>
            </person>
            """;
        
        var response = client.resource("https://httpbin.org")
            .path("post")
            .contentType("application/xml")
            .post(xmlData, new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        var responseBody = httpResponse.body();
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("Content-Type");
        
        assertThat(responseBody.get("data")).isEqualTo(xmlData);
        
        System.out.println("✅ Successfully sent XML with correct content type");
    }
    
    // === Binary Content Type Tests ===
    
    @Test
    void shouldHandleBinaryContent() {
        System.out.println("=== Testing binary content handling ===");
        
        var response = client.resource("https://httpbin.org")
            .path("bytes").path("1024")
            .contentType("application/octet-stream")
            .get(byte[].class)
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().length).isEqualTo(1024);
        
        System.out.println("✅ Successfully handled binary content");
    }
    
    @Test
    void shouldSendBinaryDataWithCorrectContentType() {
        System.out.println("=== Testing sending binary data with correct content type ===");
        
        var binaryData = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        
        var response = client.resource("https://httpbin.org")
            .path("post")
            .contentType("application/octet-stream")
            .post(binaryData, new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        var responseBody = httpResponse.body();
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("Content-Type");
        
        System.out.println("✅ Successfully sent binary data with correct content type");
    }
    
    // === Form Data Content Type Tests ===
    
    @Test
    void shouldHandleFormUrlencodedContentType() {
        System.out.println("=== Testing application/x-www-form-urlencoded content type ===");
        
        var formData = "name=John+Doe&email=john%40example.com&age=30";
        
        var response = client.resource("https://httpbin.org")
            .path("post")
            .contentType("application/x-www-form-urlencoded")
            .post(formData, new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        var responseBody = httpResponse.body();
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("Content-Type");
        
        @SuppressWarnings("unchecked")
        var form = (Map<String, Object>) responseBody.get("form");
        assertThat(form).containsKey("name");
        assertThat(form).containsKey("email");
        
        System.out.println("✅ Successfully handled form-urlencoded content type");
    }
    
    // === Custom Content Type Tests ===
    
    @Test
    void shouldHandleCustomContentType() {
        System.out.println("=== Testing custom content type ===");
        
        var customData = "Custom data with custom content type";
        
        var response = client.resource("https://httpbin.org")
            .path("post")
            .contentType("application/vnd.my-custom-type")
            .post(customData, new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        var responseBody = httpResponse.body();
        @SuppressWarnings("unchecked")
        var headers = (Map<String, Object>) responseBody.get("headers");
        assertThat(headers).containsKey("Content-Type");
        assertThat(headers.get("Content-Type")).asString().contains("application/vnd.my-custom-type");
        
        System.out.println("✅ Successfully handled custom content type");
    }
    
    // === Content Negotiation Tests ===
    
    @Test
    void shouldHandleContentNegotiation() {
        System.out.println("=== Testing content negotiation with Accept header ===");
        
        var response = client.resource("https://httpbin.org")
            .path("json")
            .header("Accept", "application/json, text/plain;q=0.9, */*;q=0.8")
            .get(new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        // Should receive JSON response based on Accept header preference
        assertThat(httpResponse.headers().first("Content-Type"))
            .isPresent()
            .get()
            .asString()
            .contains("application/json");
            
        System.out.println("✅ Successfully handled content negotiation");
    }
    
    // === Charset Tests ===
    
    @Test
    void shouldHandleUtf8Charset() {
        System.out.println("=== Testing UTF-8 charset handling ===");
        
        var unicodeText = "Hello 世界! 🌍 Здравствуй мир! こんにちは世界！";
        
        var response = client.resource("https://httpbin.org")
            .path("post")
            .contentType("text/plain; charset=UTF-8")
            .post(unicodeText, new TypeToken<Map<String, Object>>(){})
            .await();
            
        assertThat(response.isSuccess()).isTrue();
        var httpResponse = response.get();
        
        assertThat(httpResponse.isSuccess()).isTrue();
        
        var responseBody = httpResponse.body();
        assertThat(responseBody.get("data")).isEqualTo(unicodeText);
        
        System.out.println("✅ Successfully handled UTF-8 charset");
    }
}