package com.realworld.business.user.registration.domain;

import com.realworld.business.user.registration.domain.Error.ParsingError.PasswordError;
import org.pragmatica.lang.Result;

import java.util.function.Predicate;

import static org.pragmatica.lang.Verify.Is;


/// Representation of the valid password
///
public record Password(String password) {
    public static Result<Password> parse(String password) {
        return Result.success(password)
                     .filter(PasswordError::mustBeProvided, Is::notNull)
                     .filter(PasswordError::cannotBeBlank, Is::notBlank)
                     .filter(PasswordError::formatIsInvalid, PASSWORD_FORMAT)
                     .map(Password::new);
    }

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 64;
    
    private static final Predicate<String> PASSWORD_FORMAT = password -> 
        Is.lenBetween(password, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH);
}