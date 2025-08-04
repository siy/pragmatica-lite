package com.realworld.business.user.registration.uniquenesschecker.domain;

import com.realworld.business.user.registration.domain.Email;
import com.realworld.business.user.registration.domain.Name;
import com.realworld.business.user.registration.domain.Password;

public record UniqueRequest(Name name, Email email, Password password) {
    public static UniqueRequest uniqueRequest(Name name, Email email, Password password) {
        return new UniqueRequest(name, email, password);
    }
}
