package com.realworld.business.domain;

/// Raw data container for authentication context (extracted from JWT token)
/// No validation - empty, null or invalid values are acceptable at this level
/// - **userId**: Authenticated user's ID
/// - **name**: Authenticated user's name
/// - **email**: Authenticated user's email
public record AuthenticationContext(
    String userId,
    String username,
    String email
) {}