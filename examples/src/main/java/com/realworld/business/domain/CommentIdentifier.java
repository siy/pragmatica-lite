package com.realworld.business.domain;

/// Raw data container for comment identification
/// No validation - empty, null or invalid values are acceptable at this level
/// - **articleSlug**: Slug of the article containing the comment
/// - **commentId**: ID of the comment
public record CommentIdentifier(
    String articleSlug,
    String commentId
) {}