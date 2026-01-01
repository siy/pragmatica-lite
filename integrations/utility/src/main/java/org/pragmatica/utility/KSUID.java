/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.pragmatica.utility;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * K-Sortable Unique Identifier (KSUID) implementation.
 * <p>
 * A KSUID is a 20-byte identifier consisting of:
 * <ul>
 *   <li>4 bytes: 32-bit unsigned timestamp (seconds since KSUID epoch)</li>
 *   <li>16 bytes: 128-bit cryptographically random payload</li>
 * </ul>
 * <p>
 * The text representation is 27 characters using base62 encoding
 * (0-9, A-Z, a-z), which preserves lexicographic ordering by timestamp.
 * <p>
 * TODO: Consider enhanced timestamp resolution for high-frequency event ordering.
 * Current 1-second resolution may be insufficient for events happening in rapid succession.
 * Proposed enhancement: 32-bit seconds + 8-bit sub-second fraction (~3.9ms resolution),
 * stealing 8 bits from random payload (reducing from 128 to 120 bits).
 * Structure would become: [4 bytes seconds][1 byte fraction][15 bytes random].
 * This would deviate from standard KSUID spec but maintain same size and encoding.
 * Cryptographic impact: minimal (120 bits still provides 2^60 birthday bound).
 *
 * @see <a href="https://github.com/segmentio/ksuid">KSUID Specification</a>
 */
public final class KSUID implements Comparable<KSUID> {
    /// KSUID epoch: May 13, 2014 18:53:20 UTC (Unix timestamp 1400000000)
    public static final long EPOCH = 1_400_000_000L;

    /// Binary length in bytes
    public static final int BYTE_LENGTH = 20;

    /// Timestamp length in bytes
    public static final int TIMESTAMP_LENGTH = 4;

    /// Payload length in bytes
    public static final int PAYLOAD_LENGTH = 16;

    /// String-encoded length
    public static final int STRING_LENGTH = 27;

    /// Base62 encoding alphabet (lexicographically ordered)
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    /// Reverse lookup table for base62 decoding (ASCII -> value, -1 for invalid)
    private static final byte[] DECODE = new byte[128];

    static {
        Arrays.fill(DECODE, (byte) - 1);
        for (int i = 0; i < ALPHABET.length; i++ ) {
            DECODE[ALPHABET[i]] = (byte) i;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    /// Nil KSUID (all zeros) - represents invalid/empty state
    public static final KSUID NIL = new KSUID(new byte[BYTE_LENGTH]);

    /// Maximum possible KSUID (all 0xFF bytes)
    public static final KSUID MAX = new KSUID(maxBytes());

    private final byte[] data;

    private KSUID(byte[] data) {
        this.data = data;
    }

    /// Generate a new random KSUID using the current timestamp.
    public static KSUID ksuid() {
        var data = new byte[BYTE_LENGTH];
        var timestamp = (int)(System.currentTimeMillis() / 1000 - EPOCH);
        // Timestamp: big-endian 32-bit unsigned
        data[0] = (byte)(timestamp>>> 24);
        data[1] = (byte)(timestamp>>> 16);
        data[2] = (byte)(timestamp>>> 8);
        data[3] = (byte) timestamp;
        // Payload: 128-bit random
        var payload = new byte[PAYLOAD_LENGTH];
        RANDOM.nextBytes(payload);
        System.arraycopy(payload, 0, data, TIMESTAMP_LENGTH, PAYLOAD_LENGTH);
        return new KSUID(data);
    }

    /// Parse a KSUID from its 27-character base62 string representation.
    public static Result<KSUID> parse(String input) {
        if (input == null) {
            return KSUIDError.NULL_INPUT.result();
        }
        if (input.length() != STRING_LENGTH) {
            return KSUIDError.invalidLength(input.length())
                             .result();
        }
        var decoded = decodeBase62(input);
        if (decoded == null) {
            return KSUIDError.INVALID_CHARACTER.result();
        }
        return Result.success(new KSUID(decoded));
    }

    /// Create a KSUID from its 20-byte binary representation.
    public static Result<KSUID> fromBytes(byte[] bytes) {
        if (bytes == null) {
            return KSUIDError.NULL_INPUT.result();
        }
        if (bytes.length != BYTE_LENGTH) {
            return KSUIDError.invalidByteLength(bytes.length)
                             .result();
        }
        var copy = new byte[BYTE_LENGTH];
        System.arraycopy(bytes, 0, copy, 0, BYTE_LENGTH);
        return Result.success(new KSUID(copy));
    }

    /// Get the 27-character base62 encoded string representation.
    public String encoded() {
        return encodeBase62(data);
    }

    /// Get the raw 20-byte binary representation (defensive copy).
    public byte[] toBytes() {
        var copy = new byte[BYTE_LENGTH];
        System.arraycopy(data, 0, copy, 0, BYTE_LENGTH);
        return copy;
    }

    /// Get the Unix timestamp (seconds since Unix epoch, not KSUID epoch).
    public long timestamp() {
        long ts = ((data[0] & 0xFFL)<< 24) | ((data[1] & 0xFFL)<< 16) | ((data[2] & 0xFFL)<< 8) | (data[3] & 0xFFL);
        return ts + EPOCH;
    }

    /// Check if this is the nil (all-zero) KSUID.
    public boolean isNil() {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(KSUID other) {
        return Arrays.compareUnsigned(data, other.data);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KSUID other && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return encoded();
    }

    // ==================== Base62 Encoding ====================
    /// Encode 20 bytes to 27-character base62 string.
    /// Uses an optimized algorithm processing 32-bit words.
    private static String encodeBase62(byte[] src) {
        var dst = new char[STRING_LENGTH];
        Arrays.fill(dst, '0');
        // Convert bytes to 5 x 32-bit words (big-endian)
        var parts = new long[5];
        for (int i = 0; i < 5; i++ ) {
            int offset = i * 4;
            parts[i] = ((src[offset] & 0xFFL)<< 24) | ((src[offset + 1] & 0xFFL)<< 16) | ((src[offset + 2] & 0xFFL)<< 8) | (src[offset + 3] & 0xFFL);
        }
        // Convert to base62, right-to-left
        int pos = STRING_LENGTH - 1;
        while (pos >= 0) {
            long carry = 0;
            boolean allZero = true;
            for (int i = 0; i < 5; i++ ) {
                long value = parts[i] + (carry<< 32);
                parts[i] = value / 62;
                carry = value % 62;
                if (parts[i] != 0) {
                    allZero = false;
                }
            }
            dst[pos-- ] = ALPHABET[(int) carry];
            if (allZero) {
                break;
            }
        }
        return new String(dst);
    }

    /// Decode 27-character base62 string to 20 bytes.
    /// Returns null if any character is invalid.
    private static byte[] decodeBase62(String src) {
        var parts = new long[5];
        // Convert from base62
        for (int i = 0; i < STRING_LENGTH; i++ ) {
            char c = src.charAt(i);
            if (c >= 128) {
                return null;
            }
            int value = DECODE[c];
            if (value < 0) {
                return null;
            }
            // Multiply by 62 and add digit
            long carry = value;
            for (int j = 4; j >= 0; j-- ) {
                long product = parts[j] * 62 + carry;
                parts[j] = product & 0xFFFFFFFFL;
                carry = product>>> 32;
            }
        }
        // Convert words to bytes
        var dst = new byte[BYTE_LENGTH];
        for (int i = 0; i < 5; i++ ) {
            int offset = i * 4;
            long value = parts[i];
            dst[offset] = (byte)(value>>> 24);
            dst[offset + 1] = (byte)(value>>> 16);
            dst[offset + 2] = (byte)(value>>> 8);
            dst[offset + 3] = (byte) value;
        }
        return dst;
    }

    private static byte[] maxBytes() {
        var bytes = new byte[BYTE_LENGTH];
        Arrays.fill(bytes, (byte) 0xFF);
        return bytes;
    }

    /// Error causes for KSUID operations.
    public sealed interface KSUIDError extends Cause {
        KSUIDError NULL_INPUT = new NullInput();
        KSUIDError INVALID_CHARACTER = new InvalidCharacter();

        static KSUIDError invalidLength(int actual) {
            return new InvalidLength(actual);
        }

        static KSUIDError invalidByteLength(int actual) {
            return new InvalidByteLength(actual);
        }

        record NullInput() implements KSUIDError {
            @Override
            public String message() {
                return "Input is null";
            }
        }

        record InvalidLength(int actual) implements KSUIDError {
            @Override
            public String message() {
                return "Invalid string length: expected " + STRING_LENGTH + ", got " + actual;
            }
        }

        record InvalidByteLength(int actual) implements KSUIDError {
            @Override
            public String message() {
                return "Invalid byte array length: expected " + BYTE_LENGTH + ", got " + actual;
            }
        }

        record InvalidCharacter() implements KSUIDError {
            @Override
            public String message() {
                return "Invalid base62 character in input";
            }
        }
    }
}
