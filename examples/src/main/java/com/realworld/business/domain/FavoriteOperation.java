package com.realworld.business.domain;

/// Raw data container for favorite/unfavorite operations
/// No validation - empty, null or invalid values are acceptable at this level
/// - **articleSlug**: Slug of the article to favorite/unfavorite
/// - **currentUser**: Authentication context of the user performing the action
public record FavoriteOperation(
    String articleSlug,
    AuthenticationContext currentUser
) {}