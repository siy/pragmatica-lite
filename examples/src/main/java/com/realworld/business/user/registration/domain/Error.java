package com.realworld.business.user.registration.domain;

import com.realworld.business.user.registration.ParsedRequest;
import org.pragmatica.lang.Cause;

import static com.realworld.business.user.registration.domain.Password.MAX_PASSWORD_LENGTH;
import static com.realworld.business.user.registration.domain.Password.MIN_PASSWORD_LENGTH;

/// User registration errors
/// They are structured as sealed interface hierarchy.
///
/// TODO: extend them as more checks will be added
///
public sealed interface Error extends Cause {
    /// Input parsing errors
    /// Note that factory methods accept an argument. This enables insertion of context information
    /// where possible/necessary. The argument is present even if it is not used for consistency purposes.
    sealed interface ParsingError extends Error {
        sealed interface EmailError extends ParsingError {
            record MustBeProvided(String message) implements EmailError {}

            record CannotBeBlank(String message) implements EmailError {}

            record FormatIsInvalid(String message) implements EmailError {}

            MustBeProvided MUST_BE_PROVIDED = new MustBeProvided("Email must be provided");
            CannotBeBlank CANNOT_BE_BLANK = new CannotBeBlank("Email cannot be blank");

            static MustBeProvided mustBeProvided(String email) {
                return MUST_BE_PROVIDED;
            }

            static CannotBeBlank cannotBeBlank(String email) {
                return CANNOT_BE_BLANK;
            }

            static FormatIsInvalid formatIsInvalid(String email) {
                return new FormatIsInvalid("Email format is invalid: " + email);
            }
        }

        sealed interface NameError extends ParsingError {
            record MustBeProvided(String message) implements NameError {}

            record CannotBeBlank(String message) implements NameError {}

            record FormatIsInvalid(String message) implements NameError {}

            MustBeProvided MUST_BE_PROVIDED = new MustBeProvided("Username must be provided");

            CannotBeBlank CANNOT_BE_BLANK = new CannotBeBlank("Username cannot be blank");

            static MustBeProvided mustBeProvided(String username) {
                return MUST_BE_PROVIDED;
            }

            static CannotBeBlank cannotBeBlank(String username) {
                return CANNOT_BE_BLANK;
            }

            static FormatIsInvalid formatIsInvalid(String username) {
                return new FormatIsInvalid("Username [" + username + "] must be 3-20 characters and contain only letters, numbers, hyphens, and underscores");
            }
        }

        sealed interface PasswordError extends ParsingError {
            record MustBeProvided(String message) implements PasswordError {}

            record CannotBeBlank(String message) implements PasswordError {}

            record FormatIsInvalid(String message) implements PasswordError {}

            MustBeProvided MUST_BE_PROVIDED = new MustBeProvided("Password must be provided");
            CannotBeBlank CANNOT_BE_BLANK = new CannotBeBlank("Password cannot be blank");
            FormatIsInvalid FORMAT_IS_INVALID = new FormatIsInvalid("Password must be at between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters long");

            static MustBeProvided mustBeProvided(String password) {
                return MUST_BE_PROVIDED;
            }

            static CannotBeBlank cannotBeBlank(String password) {
                return CANNOT_BE_BLANK;
            }

            static FormatIsInvalid formatIsInvalid(String password) {
                return FORMAT_IS_INVALID;
            }
        }
    }

    /// Business validation errors for uniqueness constraints
    sealed interface BusinessError extends Error {
        sealed interface EmailError extends BusinessError {
            record AlreadyTaken(String message) implements EmailError {}
            
            static AlreadyTaken alreadyTaken(ParsedRequest request) {
                return new AlreadyTaken("Email already taken: " + request.email() + " (case-insensitive)");
            }
        }
        
        sealed interface NameError extends BusinessError {
            record AlreadyTaken(String message) implements NameError {}
            
            static AlreadyTaken alreadyTaken(ParsedRequest request) {
                return new AlreadyTaken("Username already taken: " + request.username());
            }
        }
    }
}
