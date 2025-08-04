package com.realworld.business.domain;

/// Raw data container for user login request
/// No validation - empty, null or invalid values are acceptable at this level
/// - **email**: User's email address
/// - **password**: Plain text password
public record UserLoginRequest(
    String email,
    String password
) {}