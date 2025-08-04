package com.realworld.business.user.registration.hashpassword;

import com.realworld.business.user.registration.hashpassword.domain.Error;
import org.pragmatica.lang.Result;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public interface HashFunction {
    String ALGORITHM = "PBKDF2WithHmacSHA256";
    int ITERATION_COUNT = 330_000;
    int KEY_LENGTH = 256;

    Result<byte[]> hash(String password, byte[] salt);

    static HashFunction hashFunction() {
        return hashFunction(ALGORITHM, ITERATION_COUNT, KEY_LENGTH);
    }

    static HashFunction hashFunction(String algorithm, int iterationCount, int keyLength) {
        record hasher(String algorithm, int iterationCount, int keyLength) implements HashFunction {

            @Override
            public Result<byte[]> hash(String password, byte[] salt) {
                return Result.lift(Error.HashFunction::map, () -> {
                    var spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount(), keyLength());

                    return SecretKeyFactory.getInstance(algorithm())
                                           .generateSecret(spec)
                                           .getEncoded();
                });
            }
        }

        return new hasher(algorithm, iterationCount, keyLength);
    }
}
