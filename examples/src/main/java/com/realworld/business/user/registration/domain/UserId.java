package com.realworld.business.user.registration.domain;

public record UserId(String id) {
    public static UserId userId(String id) {
        return new UserId(id);
    }
}
