package com.realworld.business.domain;

/// Common pagination parameters used across multiple endpoints
/// - **limit**: Maximum number of items to return (nullable, empty, or invalid values acceptable)
/// - **offset**: Number of items to skip (nullable, empty, or invalid values acceptable)
public record Pagination(
    Integer limit,
    Integer offset
) {}