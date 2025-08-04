package com.realworld.business.user.registration.domain;

/// Raw data container for user registration request
/// No validation - empty, null or invalid values are acceptable at this level
/// - **email**: User's email address
/// - **name**: Desired name
/// - **password**: Plain text password
public record Request(
    String email,
    String username,
    String password
) {}