package com.realworld.business.domain;

/// Raw data container for article listing query parameters
/// No validation - empty, null or invalid values are acceptable at this level
/// - **tag**: Filter articles by tag
/// - **author**: Filter articles by author name
/// - **favorited**: Filter articles favorited by this name
/// - **pagination**: Pagination parameters (limit and offset)
public record ArticleListQuery(
    String tag,
    String author,
    String favorited,
    Pagination pagination
) {}