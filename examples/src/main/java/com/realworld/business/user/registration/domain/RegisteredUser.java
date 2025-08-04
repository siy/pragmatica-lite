package com.realworld.business.user.registration.domain;

import java.time.Instant;

/// User entity representing a registered user in the system
/// This record contains all user information stored in the data store
/// - **id**: Unique user identifier
/// - **email**: User's email address (unique)
/// - **name**: User's name (unique)
/// - **passwordHash**: Hashed password for security
/// - **bio**: User biography (optional)
/// - **image**: URL to user's avatar image (optional)
/// - **createdAt**: Timestamp when user was created
/// - **updatedAt**: Timestamp when user was last updated
public record RegisteredUser(
        UserId id,
        Email email,
        Name username,
        ProfileDetails details,
        HashedPassword password,
        Instant createdAt,
        Instant updatedAt
) {
    public static RegisteredUser registeredUser(UserId id, Email email, Name username, ProfileDetails details, HashedPassword password, Instant createdAt, Instant updatedAt) {
        return new RegisteredUser(id, email, username, details, password, createdAt, updatedAt);
    }
}