package com.realworld.business.user.registration.domain;

public record HashedPassword(String salt, String hash) {
    public static HashedPassword hashedPassword(String salt, String hash) {
        return new HashedPassword(salt, hash);
    }
}
