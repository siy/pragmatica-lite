package com.realworld.business.user.registration.domain;

/// User profile information returned in registration response
/// - **id**: Unique user identifier
/// - **email**: User's email address
/// - **name**: User's chosen name
/// - **details**: Profile details
public record Profile(UserId id, Email email, Name username, ProfileDetails details) {
    public static Profile profile(UserId id, Email email, Name username, ProfileDetails details) {
        return new Profile(id, email, username, details);
    }
}
