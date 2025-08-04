package com.realworld.business.domain;

/// Raw data container for profile identification
/// No validation - empty, null or invalid values are acceptable at this level
/// - **name**: Username of the profile to retrieve
public record ProfileIdentifier(
    String username
) {}