package com.realworld.business.user.registration;

import com.realworld.business.user.registration.domain.Email;
import com.realworld.business.user.registration.domain.Password;
import com.realworld.business.user.registration.domain.Request;
import com.realworld.business.user.registration.domain.Name;
import org.pragmatica.lang.Result;

/// This record represents the validated input data after all validation checks have passed
/// - **email**: Validated email address (non-blank, valid format)
/// - **name**: Validated name (non-blank, meets format requirements)
/// - **password**: Validated password (meets strength requirements)
public record ParsedRequest(Email email, Name username, Password password) {
    public static Result<ParsedRequest> parse(Request request) {
        return Result.all(Email.parse(request.email()),
                          Name.parse(request.username()),
                          Password.parse(request.password()))
                     .map(ParsedRequest::new);
    }
}