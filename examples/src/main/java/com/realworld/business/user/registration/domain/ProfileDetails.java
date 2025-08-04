package com.realworld.business.user.registration.domain;

public record ProfileDetails(String bio, String image) {
    public static ProfileDetails profileDetails(String bio, String image) {
        return new ProfileDetails(bio, image);
    }
}
