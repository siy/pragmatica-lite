package com.realworld.business.user.registration.hashpassword.domain;

import org.pragmatica.lang.Cause;

public sealed interface Error extends Cause {

    sealed interface Base64Decoder extends Error {
        record invalidBase64EncodedString(String message) implements Base64Decoder {}

        record unknownError(String message) implements Base64Decoder {}

        static Cause map(Throwable throwable) {
            return switch (throwable) {
                default -> new unknownError(throwable.getMessage());
            };
        }
    }

    sealed interface Base64Encoder extends Error {
        record unknownError(String message) implements Base64Encoder {}

        static Cause map(Throwable throwable) {
            return switch (throwable) {
                default -> new Base64Decoder.unknownError(throwable.getMessage());
            };
        }
    }

    sealed interface HashFunction extends Error {
        record unknownError(String message) implements HashFunction {}

        static Cause map(Throwable throwable) {
            return switch (throwable) {
                default -> new HashFunction.unknownError(throwable.getMessage());
            };
        }
    }
}
