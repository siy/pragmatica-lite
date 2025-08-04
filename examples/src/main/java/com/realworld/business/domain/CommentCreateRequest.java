package com.realworld.business.domain;

/// Raw data container for comment creation request
/// No validation - empty, null or invalid values are acceptable at this level
/// - **body**: Comment content
public record CommentCreateRequest(
    String body
) {}