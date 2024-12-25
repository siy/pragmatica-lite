package org.pragmatica.id.nanoid;

import org.pragmatica.config.api.DataConversionError.InvalidInput;
import org.pragmatica.lang.Result;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;

import static org.pragmatica.lang.Option.none;

public interface NanoId extends Comparable<NanoId> {
    String value();

    @Override
    default int compareTo(NanoId other) {
        return value().compareTo(other.value());
    }

    static NanoId secureNanoId() {
        return secureNanoId(DEFAULT_LEN);
    }

    static NanoId secureNanoId(int len) {
        return customNanoId(SECURE_RANDOM, len);
    }

    static NanoId nonSecureNanoId() {
        return nonSecureNanoId(DEFAULT_LEN);
    }

    static NanoId nonSecureNanoId(int len) {
        return customNanoId(NON_SECURE_RANDOM, len);
    }

    static NanoId customNanoId(Random random, int size) {
        var bytes = new byte[size];
        var step = (int) Math.ceil(1.6 * MASK * size / ALPHABET.length);
        var cursor = 0;
        var randomBytes = new byte[step];

        while (true) {
            random.nextBytes(randomBytes);

            for (var i = 0; i < step; i++) {
                var index = randomBytes[i] & MASK;

                bytes[cursor++] = ALPHABET[index];

                if (cursor == size) {
                    record nanoId(String value) implements NanoId {}
                    return new nanoId(new String(bytes, StandardCharsets.US_ASCII));
                }
            }
        }
    }

    static Result<NanoId> parse(String input) {
        var value = input.trim();

        if (value.length() != DEFAULT_LEN) {
            return new InvalidInput("Invalid NanoId length " + value.length(), none()).result();
        }

        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);

            if (c < 45 || c > 122 || (c > 57 && c < 65) || (c > 90 && c < 95)) {
                return new InvalidInput("Invalid NanoId character " + c, none()).result();
            }
        }

        record nanoId(String value) implements NanoId {}
        return Result.success(new nanoId(value));
    }

    Random SECURE_RANDOM = new SecureRandom();
    Random NON_SECURE_RANDOM = new Random();
    byte[] ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(StandardCharsets.US_ASCII);
    int DEFAULT_LEN = 21;
    int MASK = (2 << (int) Math.floor(Math.log(ALPHABET.length - 1) / Math.log(2))) - 1;
}
