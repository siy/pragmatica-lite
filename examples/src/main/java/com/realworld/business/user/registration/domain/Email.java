package com.realworld.business.user.registration.domain;

import com.realworld.business.user.registration.domain.Error.ParsingError.EmailError;
import org.pragmatica.lang.Result;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.pragmatica.lang.Verify.Is;


/// Representation of the valid email
///
public record Email(String email) {
    public static Result<Email> parse(String email) {
        return Result.success(email)
                     .filter(EmailError::mustBeProvided, Is::notNull)
                     .filter(EmailError::cannotBeBlank, Is::notBlank)
                     .filter(EmailError::formatIsInvalid, EMAIL_FORMAT)
                     .map(Email::new);
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Predicate<String> EMAIL_FORMAT = email -> Is.matches(email, EMAIL_PATTERN);
}
