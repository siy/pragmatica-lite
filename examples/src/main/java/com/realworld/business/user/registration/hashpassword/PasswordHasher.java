package com.realworld.business.user.registration.hashpassword;

import com.realworld.business.user.registration.domain.HashedPassword;
import org.pragmatica.lang.Result;

import java.security.SecureRandom;
import java.util.Base64;

/// Password hashing utility using PBKDF2 with SHA-256
/// Provides secure password hashing and verification functionality
/// Uses salt-based hashing to prevent rainbow table attacks
public class PasswordHasher {
    
    private static final int SALT_LENGTH = 32;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /// Hash a password using PBKDF2 with a random salt
    /// - **password**: Plain text password to hash
    /// - **Returns**: Base64-encoded salt:hash string or hashing error
    public Result<HashedPassword> hashPassword(String password) {
        return Result.lift(() -> {
            byte[] salt = generateSalt();
            byte[] hash = pbkdf2Hash(password, salt);
            
            String encodedSalt = Base64.getEncoder().encodeToString(salt);
            String encodedHash = Base64.getEncoder().encodeToString(hash);
            
            return encodedSalt + ":" + encodedHash;
        });
    }
    
    /// Verify a password against a stored hash
    /// - **password**: Plain text password to verify
    /// - **storedHash**: Previously stored salt:hash string
    /// - **Returns**: True if password matches, false otherwise, or verification error
    public Result<Boolean> verifyPassword(String password, String storedHash) {
        return Result.lift(() -> {
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = pbkdf2Hash(password, salt);
            
            return constantTimeEquals(expectedHash, actualHash);
        });
    }
    
    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /// Constant-time comparison to prevent timing attacks
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}