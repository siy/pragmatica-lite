package com.realworld.business.user.registration.hashpassword;

import com.realworld.business.user.registration.domain.HashedPassword;
import org.pragmatica.lang.Result;

public interface HashPassword {
    Result<HashedPassword> perform(String password);

    static HashPassword hashPassword(HashFunction hashFunction,
                                     SaltGenerator saltGenerator,
                                     Base64Encoder base64Encoder) {
        record hashPassword(HashFunction hashFunction, SaltGenerator saltGenerator,
                            Base64Encoder base64Encoder) implements HashPassword {
            @Override
            public Result<HashedPassword> perform(String password) {
                var salt = saltGenerator.generate();

                return hashFunction().hash(password, salt)
                                     .flatMap2(this::encode, salt);
            }

            private Result<HashedPassword> encode(byte[] hashedPassword, byte[] salt) {
                return Result.all(base64Encoder().encode(hashedPassword),
                                  base64Encoder().encode(salt))
                             .map(HashedPassword::new);
            }
        }
        return new hashPassword(hashFunction, saltGenerator, base64Encoder);
    }
}
