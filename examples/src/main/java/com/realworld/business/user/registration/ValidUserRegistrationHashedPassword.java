package com.realworld.business.user.registration;

import com.realworld.business.user.registration.domain.Password;
import com.realworld.business.user.registration.domain.HashedPassword;
import org.pragmatica.lang.Result;

public record ValidUserRegistrationHashedPassword(
        ParsedRequest parsedRequest,
        HashedPassword password
) {
    public static Result<ValidUserRegistrationHashedPassword> parse(ParsedRequest parsedRequest, Password password) {
        return HashedPassword.parse(password).map(hashedPassword -> new ValidUserRegistrationHashedPassword(
                parsedRequest, hashedPassword));
    }
}