package com.realworld.business.domain;

import java.util.List;

/// Raw data container for article creation request
/// No validation - empty, null or invalid values are acceptable at this level
/// - **title**: Article title
/// - **description**: Short description of the article
/// - **body**: Full article content (markdown)
/// - **tagList**: List of tags associated with the article
public record ArticleCreateRequest(
    String title,
    String description,
    String body,
    List<String> tagList
) {}