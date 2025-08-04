package com.realworld.business.domain;

/// Raw data container for article feed query parameters (articles from followed users)
/// No validation - empty, null or invalid values are acceptable at this level
/// - **pagination**: Pagination parameters (limit and offset)
public record ArticleFeedQuery(
    Pagination pagination
) {}