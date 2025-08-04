package com.realworld.business.domain;

/// Raw data container for follow/unfollow operations
/// No validation - empty, null or invalid values are acceptable at this level
/// - **targetUsername**: Username of the user to follow/unfollow
/// - **currentUser**: Authentication context of the user performing the action
public record FollowOperation(
    String targetUsername,
    AuthenticationContext currentUser
) {}