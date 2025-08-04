package com.realworld.business.domain;

/// Raw data container for article identification
/// No validation - empty, null or invalid values are acceptable at this level
/// - **slug**: Article slug (URL-friendly identifier)
public record ArticleIdentifier(
    String slug
) {}