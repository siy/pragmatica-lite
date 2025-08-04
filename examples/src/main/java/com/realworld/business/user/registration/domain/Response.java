package com.realworld.business.user.registration.domain;

/// Response data for successful user registration
/// Contains user profile information and authentication token
/// - **user**: User profile data without sensitive information
/// - **token**: JWT authentication token for API access
public record Response(Profile user, AuthenticationToken token) {
    public static Response response(Profile user, AuthenticationToken token) {
        return new Response(user, token);
    }
}