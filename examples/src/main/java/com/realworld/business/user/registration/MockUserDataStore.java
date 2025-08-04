package com.realworld.business.user.registration;

import com.realworld.business.user.registration.domain.RegisteredUser;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/// Mock in-memory data store for users
/// This provides thread-safe storage and retrieval operations for User entities
/// Uses Promise-based API to simulate asynchronous database operations
public class MockUserDataStore {
    
    private final ConcurrentMap<String, RegisteredUser> usersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RegisteredUser> usersByEmail = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RegisteredUser> usersByUsername = new ConcurrentHashMap<>();
    
    /// Check if an email address is already registered
    /// - **email**: Email address to check
    /// - **Returns**: Promise resolving to true if email exists, false otherwise
    public Promise<Boolean> isEmailTaken(String email) {
        return Promise.success(usersByEmail.containsKey(email.toLowerCase()));
    }
    
    /// Check if a name is already registered
    /// - **name**: Username to check
    /// - **Returns**: Promise resolving to true if name exists, false otherwise
    public Promise<Boolean> isUsernameTaken(String username) {
        return Promise.success(usersByUsername.containsKey(username.toLowerCase()));
    }
    
    /// Store a new user in the data store
    /// - **user**: User entity to store
    /// - **Returns**: Promise resolving to the stored user or storage error
    public Promise<RegisteredUser> saveUser(RegisteredUser user) {
        return Promise.promise(() -> {
            // Check for concurrent registration attempts
            if (usersByEmail.containsKey(user.profile().email().email().toLowerCase())) {
                return Result.failure(new StorageException("Email already exists: " + user.profile().email().email()));
            }
            
            if (usersByUsername.containsKey(user.profile().username().name().toLowerCase())) {
                return Result.failure(new StorageException("Username already exists: " + user.profile().username().name()));
            }
            
            // Store user in all indexes
            usersById.put(user.profile().id().id(), user);
            usersByEmail.put(user.profile().email().email().toLowerCase(), user);
            usersByUsername.put(user.profile().username().name().toLowerCase(), user);
            
            return Result.success(user);
        });
    }
    
    /// Find a user by their unique ID
    /// - **id**: User ID to search for
    /// - **Returns**: Promise resolving to user if found, empty if not found
    public Promise<Option<RegisteredUser>> findById(String id) {
        return Promise.success(Option.option(usersById.get(id)));
    }
    
    /// Find a user by their email address
    /// - **email**: Email address to search for  
    /// - **Returns**: Promise resolving to user if found, empty if not found
    public Promise<Option<RegisteredUser>> findByEmail(String email) {
        return Promise.success(Option.option(usersByEmail.get(email.toLowerCase())));
    }
    
    /// Find a user by their name
    /// - **name**: Username to search for
    /// - **Returns**: Promise resolving to user if found, empty if not found  
    public Promise<Option<RegisteredUser>> findByUsername(String username) {
        return Promise.success(Option.option(usersByUsername.get(username.toLowerCase())));
    }
    
    /// Update an existing user in the data store
    /// - **user**: Updated user entity
    /// - **Returns**: Promise resolving to updated user or update error
    public Promise<RegisteredUser> updateUser(RegisteredUser user) {
        return Promise.promise(() -> {
            RegisteredUser existingUser = usersById.get(user.profile().id().id());
            if (existingUser == null) {
                return Result.failure(new StorageException("User not found: " + user.profile().id().id()));
            }
            
            // Update all indexes
            usersById.put(user.profile().id().id(), user);
            usersByEmail.put(user.profile().email().email().toLowerCase(), user);
            usersByUsername.put(user.profile().username().name().toLowerCase(), user);
            
            return Result.success(user);
        });
    }
    
    /// Clear all data from the store (useful for testing)
    /// - **Returns**: Promise that completes when store is cleared
    public Promise<Void> clear() {
        return Promise.promise(() -> {
            usersById.clear();
            usersByEmail.clear();
            usersByUsername.clear();
            return Result.success(null);
        });
    }
    
    /// Get total number of users in the store
    /// - **Returns**: Promise resolving to user count
    public Promise<Integer> getUserCount() {
        return Promise.success(usersById.size());
    }
    
    /// Storage exception for data store operations
    public static class StorageException extends Exception implements org.pragmatica.lang.Cause {
        public StorageException(String message) {
            super(message);
        }
        
        @Override
        public String message() {
            return getMessage();
        }
    }
}