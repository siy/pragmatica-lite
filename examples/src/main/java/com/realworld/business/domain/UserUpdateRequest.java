package com.realworld.business.domain;

/// Raw data container for user profile update request
/// No validation - empty, null or invalid values are acceptable at this level
/// All fields are optional (nullable) for partial updates
/// - **email**: New email address
/// - **name**: New name
/// - **password**: New password (plain text)
/// - **bio**: User biography
/// - **image**: URL to user's avatar image
public record UserUpdateRequest(
    String email,
    String username,
    String password,
    String bio,
    String image
) {}