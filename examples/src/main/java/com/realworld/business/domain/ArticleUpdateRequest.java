package com.realworld.business.domain;

import java.util.List;

/// Raw data container for article update request
/// No validation - empty, null or invalid values are acceptable at this level
/// All fields are optional (nullable) for partial updates
/// - **title**: New article title
/// - **description**: New short description
/// - **body**: New article content (markdown)
/// - **tagList**: New list of tags (replaces existing tags)
public record ArticleUpdateRequest(
    String title,
    String description,
    String body,
    List<String> tagList
) {}