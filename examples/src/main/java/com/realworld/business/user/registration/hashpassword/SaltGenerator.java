package com.realworld.business.user.registration.hashpassword;

import java.security.SecureRandom;

public interface SaltGenerator {
    int SALT_LENGTH = 32;

    byte[] generate();

    static SaltGenerator saltGenerator() {
        record saltGenerator(SecureRandom secureRandom) implements SaltGenerator {
            @Override
            public byte[] generate() {
                byte[] salt = new byte[SALT_LENGTH];
                secureRandom.nextBytes(salt);
                return salt;
            }
        }

        return new saltGenerator(new SecureRandom());
    }
}
