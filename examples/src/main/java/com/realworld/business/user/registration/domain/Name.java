package com.realworld.business.user.registration.domain;

import com.realworld.business.user.registration.domain.Error.ParsingError.NameError;
import org.pragmatica.lang.Result;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.pragmatica.lang.Verify.Is;

/// Representation of the valid name
///
public record Name(String name) {
    public static Result<Name> parse(String name) {
        return Result.success(name)
                     .filter(NameError::mustBeProvided, Is::notNull)
                     .filter(NameError::cannotBeBlank, Is::notBlank)
                     .filter(NameError::formatIsInvalid, NAME_FORMAT)
                     .map(Name::new);
    }

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    private static final Predicate<String> NAME_FORMAT = username -> Is.matches(username, NAME_PATTERN);
}
