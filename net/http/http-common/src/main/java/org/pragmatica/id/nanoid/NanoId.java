package org.pragmatica.id.nanoid;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;

public sealed interface NanoId {
    static String secureNanoId() {
        return secureNanoId(DEFAULT_LEN);
    }

    static String secureNanoId(int len) {
        return customNanoId(SECURE_RANDOM, len);
    }

    static String nonSecureNanoId() {
        return nonSecureNanoId(DEFAULT_LEN);
    }

    static String nonSecureNanoId(int len) {
        return customNanoId(NON_SECURE_RANDOM, len);
    }

    static String customNanoId(Random random, int size) {
        byte[] bytes = new byte[size];
        int step = (int) Math.ceil(1.6 * MASK * size / ALPHABET.length);
        int cursor = 0;

        var randomBytes = new byte[step];

        while (true) {
            random.nextBytes(randomBytes);

            for (int i = 0; i < step; i++) {
                var index = randomBytes[i] & MASK;

                bytes[cursor++] = ALPHABET[index];

                if (cursor == size) {
                    return new String(bytes, StandardCharsets.US_ASCII);
                }
            }
        }
    }

    Random SECURE_RANDOM = new SecureRandom();
    Random NON_SECURE_RANDOM = new Random();
    byte[] ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(StandardCharsets.US_ASCII);
    int DEFAULT_LEN = 21;
    int MASK = (2 << (int) Math.floor(Math.log(ALPHABET.length - 1) / Math.log(2))) - 1;
//    int STEP = (int) Math.ceil(1.6 * MASK * SIZE / ALPHABET.length);


    record unused() implements NanoId {}
}
