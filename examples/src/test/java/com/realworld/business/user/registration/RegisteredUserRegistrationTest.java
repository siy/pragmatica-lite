package com.realworld.business.user.registration;

import com.realworld.business.user.registration.domain.Error;
import com.realworld.business.user.registration.domain.Request;
import com.realworld.business.user.registration.domain.Response;
import com.realworld.business.user.registration.domain.Profile;
import com.realworld.business.user.registration.hashpassword.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Result;

import static org.junit.jupiter.api.Assertions.*;

/// Comprehensive tests for User Registration endpoint
/// Tests the complete registration flow from validation to response generation  
class RegisteredUserRegistrationTest {
    
    private MockUserDataStore userDataStore;
    private PasswordHasher passwordHasher;
    private JwtTokenGenerator jwtTokenGenerator;
    private UserRegistration userRegistration;
    
    @BeforeEach
    void setUp() {
        userDataStore = new MockUserDataStore();
        passwordHasher = new PasswordHasher();
        jwtTokenGenerator = new JwtTokenGenerator();
        userRegistration = UserRegistration.userRegistration(userDataStore, passwordHasher, jwtTokenGenerator);
    }
    
    @Test
    void shouldSuccessfullyRegisterValidUser() {
        // Given
        var request = new Request(
            "john.doe@example.com",
            "johndoe",
            "securepassword123"
        );
        
        // When
        Result<Response> result = userRegistration.perform(request).await();
        
        // Then
        assertTrue(result.isSuccess());
        
        Response response = result.unwrap();
        assertNotNull(response.user());
        assertNotNull(response.token());
        
        // Verify user profile
        Profile profile = response.user();
        assertNotNull(profile.id());
        assertEquals("john.doe@example.com", profile.email());
        assertEquals("johndoe", profile.username());
        assertNull(profile.bio());
        assertNull(profile.image());
        
        // Verify token is valid
        Result<String> tokenValidation = jwtTokenGenerator.validateToken(response.token());
        assertTrue(tokenValidation.isSuccess());
        assertEquals(profile.id(), tokenValidation.unwrap());
        
        // Verify user is stored
        Result<Boolean> emailExists = userDataStore.isEmailTaken("john.doe@example.com").await();
        assertTrue(emailExists.isSuccess() && emailExists.unwrap());
        
        Result<Boolean> usernameExists = userDataStore.isUsernameTaken("johndoe").await();
        assertTrue(usernameExists.isSuccess() && usernameExists.unwrap());
    }
    
    @Test
    void shouldRejectInvalidEmail() {
        // Given
        var request = new Request(
            "invalid-email",
            "johndoe", 
            "securepassword123"
        );
        
        // When
        Result<Response> result = userRegistration.perform(request).await();
        
        // Then
        assertTrue(result.isFailure());
        switch (result) {
            case Result.Success<Response> success -> fail("Expected failure but got success");
            case Result.Failure<Response> failure ->
                assertTrue(failure.cause() instanceof Error.ParsingError.EmailError.FormatIsInvalid);
        }
    }
    
    @Test
    void shouldRejectInvalidUsername() {
        // Given
        var request = new Request(
            "john.doe@example.com",
            "ab", // Too short
            "securepassword123"
        );
        
        // When
        Result<Response> result = userRegistration.perform(request).await();
        
        // Then
        assertTrue(result.isFailure());
        switch (result) {
            case Result.Success<Response> success -> fail("Expected failure but got success");
            case Result.Failure<Response> failure ->
                assertTrue(failure.cause() instanceof Error.ParsingError.NameError.FormatIsInvalid);
        }
    }
    
    @Test
    void shouldRejectWeakPassword() {
        // Given
        var request = new Request(
            "john.doe@example.com",
            "johndoe",
            "weak" // Too short
        );
        
        // When
        Result<Response> result = userRegistration.perform(request).await();
        
        // Then
        assertTrue(result.isFailure());
        switch (result) {
            case Result.Success<Response> success -> fail("Expected failure but got success");
            case Result.Failure<Response> failure ->
                assertTrue(failure.cause() instanceof Error.ParsingError.PasswordError.FormatIsInvalid);
        }
    }
    
    @Test
    void shouldRejectDuplicateEmail() {
        // Given - register first user
        var firstRequest = new Request(
            "john.doe@example.com",
            "johndoe",
            "securepassword123"
        );
        userRegistration.perform(firstRequest).await();
        
        // When - try to register with same email
        var duplicateRequest = new Request(
            "john.doe@example.com",
            "differentuser",
            "anotherpassword"
        );
        Result<Response> result = userRegistration.perform(duplicateRequest).await();
        
        // Then
        assertTrue(result.isFailure());
        switch (result) {
            case Result.Success<Response> success -> fail("Expected failure but got success");
            case Result.Failure<Response> failure ->
                assertTrue(failure.cause() instanceof Error.BusinessError.EmailError.AlreadyTaken);
        }
        
        Error.BusinessError.EmailError.AlreadyTaken error = switch (result) {
            case Result.Success<Response> success -> throw new AssertionError("Expected failure");
            case Result.Failure<Response> failure ->
                (Error.BusinessError.EmailError.AlreadyTaken) failure.cause();
        };
        assertTrue(error.message().contains("john.doe@example.com"));
    }
    
    @Test
    void shouldRejectDuplicateUsername() {
        // Given - register first user
        var firstRequest = new Request(
            "john.doe@example.com",
            "johndoe",
            "securepassword123"
        );
        userRegistration.perform(firstRequest).await();
        
        // When - try to register with same name
        var duplicateRequest = new Request(
            "different@example.com",
            "johndoe",
            "anotherpassword"
        );
        Result<Response> result = userRegistration.perform(duplicateRequest).await();
        
        // Then
        assertTrue(result.isFailure());
        switch (result) {
            case Result.Success<Response> success -> fail("Expected failure but got success");
            case Result.Failure<Response> failure ->
                assertTrue(failure.cause() instanceof Error.BusinessError.NameError.AlreadyTaken);
        }
        
        Error.BusinessError.NameError.AlreadyTaken error = switch (result) {
            case Result.Success<Response> success -> throw new AssertionError("Expected failure");
            case Result.Failure<Response> failure ->
                (Error.BusinessError.NameError.AlreadyTaken) failure.cause();
        };
        assertTrue(error.message().contains("johndoe"));
    }
    
    @Test
    void shouldHashPasswordSecurely() {
        // Given
        var request = new Request(
            "john.doe@example.com",
            "johndoe",
            "securepassword123"
        );
        
        // When
        Result<Response> result = userRegistration.perform(request).await();
        Response response = result.unwrap();
        
        // Then - retrieve stored user and verify password is hashed
        var storedUserOption = userDataStore.findByEmail("john.doe@example.com").await().unwrap();
        var storedUser = storedUserOption.unwrap();
        assertNotEquals("securepassword123", storedUser.passwordHash());
        assertTrue(storedUser.passwordHash().contains(":")); // Should contain salt:hash format
        
        // Verify password can be verified
        Result<Boolean> verification = passwordHasher.verifyPassword("securepassword123", storedUser.passwordHash());
        assertTrue(verification.isSuccess() && verification.unwrap());
        
        // Verify wrong password fails
        Result<Boolean> wrongPasswordVerification = passwordHasher.verifyPassword("wrongpassword", storedUser.passwordHash());
        assertTrue(wrongPasswordVerification.isSuccess() && !wrongPasswordVerification.unwrap());
    }
    
    @Test
    void shouldGenerateValidJwtToken() {
        // Given
        var request = new Request(
            "john.doe@example.com",
            "johndoe",
            "securepassword123"
        );
        
        // When
        Result<Response> result = userRegistration.perform(request).await();
        Response response = result.unwrap();
        
        // Then
        String token = response.token();
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Token should have 3 parts (header.payload.signature)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length);
        
        // Should be able to extract user ID from token
        Result<String> userIdFromToken = jwtTokenGenerator.validateToken(token);
        assertTrue(userIdFromToken.isSuccess());
        assertEquals(response.user().id(), userIdFromToken.unwrap());
    }
}