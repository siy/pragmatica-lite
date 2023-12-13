package org.pragmatica.id.nanoid;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;

public sealed interface NanoId {
    static String secureNanoId() {
        return customNanoId(SECURE_RANDOM);
    }

    static String nonSecureNanoId() {
        return customNanoId(NON_SECURE_RANDOM);
    }

    static String customNanoId(Random random) {
        byte[] bytes = new byte[SIZE];
        int cursor = 0;

        var randomBytes = new byte[STEP];

        while (true) {
            random.nextBytes(randomBytes);

            for (int i = 0; i < STEP; i++) {
                var index = randomBytes[i] & MASK;

                if (index < ALPHABET.length) {
                    bytes[cursor++] = ALPHABET[index];
                    if (cursor == SIZE) {
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                }
            }
        }
    }

    Random SECURE_RANDOM = new SecureRandom();
    Random NON_SECURE_RANDOM = new Random();
    byte[] ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(StandardCharsets.UTF_8);
    int SIZE = 21;
    int MASK = (2 << (int) Math.floor(Math.log(ALPHABET.length - 1) / Math.log(2))) - 1;
    int STEP = (int) Math.ceil(1.6 * MASK * SIZE / ALPHABET.length);


    record unused() implements NanoId {}
}
