package com.realworld.business.user.registration.domain;

public record AuthenticationToken(String token) {
    public static AuthenticationToken authenticationToken(String token) {
        return new AuthenticationToken(token);
    }
}
