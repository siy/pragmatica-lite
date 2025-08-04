package com.realworld.business.user.registration;

import org.pragmatica.lang.Result;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/// JWT token generator for user authentication
/// Creates JWT tokens with user ID claims for authentication
/// Uses HMAC-SHA256 for token signing
public class JwtTokenGenerator {
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SECRET_KEY = "realworld-secret-key-change-in-production";
    private static final long TOKEN_VALIDITY_HOURS = 24;
    
    /// Generate a JWT token for a user
    /// - **userId**: Unique user identifier to include in token
    /// - **Returns**: JWT token string or generation error
    public Result<String> generateToken(String userId) {
        return Result.lift(() -> {
            long expirationTime = Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS).getEpochSecond();
            
            String header = """
                {
                  "alg": "HS256",
                  "typ": "JWT"
                }
                """;
            
            String payload = """
                {
                  "sub": "%s",
                  "exp": %d,
                  "iat": %d
                }
                """.formatted(userId, expirationTime, Instant.now().getEpochSecond());
            
            String encodedHeader = base64UrlEncode(header);
            String encodedPayload = base64UrlEncode(payload);
            String message = encodedHeader + "." + encodedPayload;
            
            String signature = hmacSha256(message, SECRET_KEY);
            String encodedSignature = base64UrlEncode(signature);
            
            return message + "." + encodedSignature;
        });
    }
    
    /// Validate and extract user ID from JWT token
    /// - **token**: JWT token to validate
    /// - **Returns**: User ID if token is valid, or validation error
    public Result<String> validateToken(String token) {
        return Result.lift(() -> {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            
            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];
            
            String message = header + "." + payload;
            String expectedSignature = base64UrlEncode(hmacSha256(message, SECRET_KEY));
            
            if (!constantTimeEquals(signature, expectedSignature)) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }
            
            String decodedPayload = base64UrlDecode(payload);
            
            // Simple JSON parsing for user ID - in production use proper JSON library
            String userIdPrefix = "\"sub\": \"";
            int userIdStart = decodedPayload.indexOf(userIdPrefix);
            if (userIdStart == -1) {
                throw new IllegalArgumentException("User ID not found in token");
            }
            
            userIdStart += userIdPrefix.length();
            int userIdEnd = decodedPayload.indexOf("\"", userIdStart);
            if (userIdEnd == -1) {
                throw new IllegalArgumentException("Invalid user ID format in token");
            }
            
            return decodedPayload.substring(userIdStart, userIdEnd);
        });
    }
    
    private String base64UrlEncode(String input) {
        return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    
    private String base64UrlDecode(String input) {
        return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
    }
    
    private String hmacSha256(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return new String(hash, StandardCharsets.UTF_8);
    }
    
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}