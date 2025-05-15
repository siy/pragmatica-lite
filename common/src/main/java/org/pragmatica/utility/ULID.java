/*
 * MIT License
 *
 * Copyright (c) 2016 Azamshul Azizy
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.pragmatica.utility;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * ULID string generator and parser class, using Crockford Base32 encoding. Only upper case letters are used for generation. Parsing allows upper and
 * lower case letters, and i and l will be treated as 1 and o will be treated as 0.
 * <br>
 * This version is significantly reworked implementation created by Azamshul Azizy.
 *
 * @author azam
 * @see <a href="http://www.crockford.com/wrmg/base32.html">Base32 Encoding</a>
 * @see <a href="https://github.com/alizain/ulid">ULID</a>
 * @see <a href="https://github.com/azam/ulidj">ULIDJ</a>
 * @since 0.0.1
 */
@SuppressWarnings("unused")
public final class ULID implements Comparable<ULID> {
    public static final int ULID_LENGTH = 26;

    private static final SecureRandom SECURE = new SecureRandom();
    private static final long OFFSET = System.currentTimeMillis() * 1000_000;

    // Base32 characters mapping
    private static final char[] C = new char[]{
        0X30, 0X31, 0X32, 0X33, 0X34, 0X35, 0X36, 0X37,
        0X38, 0X39, 0X41, 0X42, 0X43, 0X44, 0X45, 0X46,
        0X47, 0X48, 0X4A, 0X4B, 0X4D, 0X4E, 0X50, 0X51,
        0X52, 0X53, 0X54, 0X56, 0X57, 0X58, 0X59, 0X5A};

    private static final byte[] V = new byte[]{
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0X00, (byte) 0X01, (byte) 0X02, (byte) 0X03,
        (byte) 0X04, (byte) 0X05, (byte) 0X06, (byte) 0X07,
        (byte) 0X08, (byte) 0X09, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0X0A, (byte) 0X0B, (byte) 0X0C,
        (byte) 0X0D, (byte) 0X0E, (byte) 0X0F, (byte) 0X10,
        (byte) 0X11, (byte) 0XFF, (byte) 0X12, (byte) 0X13,
        (byte) 0XFF, (byte) 0X14, (byte) 0X15, (byte) 0XFF,
        (byte) 0X16, (byte) 0X17, (byte) 0X18, (byte) 0X19,
        (byte) 0X1A, (byte) 0XFF, (byte) 0X1B, (byte) 0X1C,
        (byte) 0X1D, (byte) 0X1E, (byte) 0X1F, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0X0A, (byte) 0X0B, (byte) 0X0C,
        (byte) 0X0D, (byte) 0X0E, (byte) 0X0F, (byte) 0X10,
        (byte) 0X11, (byte) 0XFF, (byte) 0X12, (byte) 0X13,
        (byte) 0XFF, (byte) 0X14, (byte) 0X15, (byte) 0XFF,
        (byte) 0X16, (byte) 0X17, (byte) 0X18, (byte) 0X19,
        (byte) 0X1A, (byte) 0XFF, (byte) 0X1B, (byte) 0X1C,
        (byte) 0X1D, (byte) 0X1E, (byte) 0X1F, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF,
        (byte) 0XFF, (byte) 0XFF, (byte) 0XFF, (byte) 0XFF
    };

    private final String representation;
    private final long timestamp;
    private final byte[] entropy;

    private ULID(final String representation, final long timestamp, final byte[] entropy) {
        this.representation = representation;
        this.timestamp = timestamp;
        this.entropy = entropy;
    }

    /**
     * Generate random ULID string using {@link SecureRandom} instance.
     *
     * @return ULID string
     */
    public static ULID randomULID() {
        final var entropy = new byte[10];
        SECURE.nextBytes(entropy);

        return generate(System.nanoTime() + OFFSET, entropy);
    }

    private static ULID generate(final long timestamp, final byte[] entropy) {
        final var chars = new char[ULID_LENGTH];

        // time
        chars[0] = C[(byte) (timestamp >>> 45) & 0x1F];
        chars[1] = C[(byte) (timestamp >>> 40) & 0x1F];
        chars[2] = C[(byte) (timestamp >>> 35) & 0x1F];
        chars[3] = C[(byte) (timestamp >>> 30) & 0x1F];
        chars[4] = C[(byte) (timestamp >>> 25) & 0x1F];
        chars[5] = C[(byte) (timestamp >>> 20) & 0x1F];
        chars[6] = C[(byte) (timestamp >>> 15) & 0x1F];
        chars[7] = C[(byte) (timestamp >>> 10) & 0x1F];
        chars[8] = C[(byte) (timestamp >>> 5) & 0x1F];
        chars[9] = C[((byte) (timestamp)) & 0x1F];

        // entropy
        chars[10] = C[(byte) ((entropy[0] & 0xFF) >>> 3)];
        chars[11] = C[(byte) ((entropy[0] << 2 | ((entropy[1] & 0xFF) >>> 6)) & 0x1F)];
        chars[12] = C[(byte) ((entropy[1] & 0xFF) >>> 1 & 0x1F)];
        chars[13] = C[(byte) ((entropy[1] << 4 | ((entropy[2] & 0xFF) >>> 4)) & 0x1F)];
        chars[14] = C[(byte) ((entropy[2] << 5 | ((entropy[3] & 0xFF) >>> 7)) & 0x1F)];
        chars[15] = C[(byte) ((entropy[3] & 0xFF) >>> 2 & 0x1F)];
        chars[16] = C[(byte) ((entropy[3] << 3 | ((entropy[4] & 0xFF) >>> 5)) & 0x1F)];
        chars[17] = C[(byte) (entropy[4] & 0x1F)];
        chars[18] = C[(byte) ((entropy[5] & 0xFF) >>> 3)];
        chars[19] = C[(byte) ((entropy[5] << 2 | ((entropy[6] & 0xFF) >>> 6)) & 0x1F)];
        chars[20] = C[(byte) ((entropy[6] & 0xFF) >>> 1 & 0x1F)];
        chars[21] = C[(byte) ((entropy[6] << 4 | ((entropy[7] & 0xFF) >>> 4)) & 0x1F)];
        chars[22] = C[(byte) ((entropy[7] << 5 | ((entropy[8] & 0xFF) >>> 7)) & 0x1F)];
        chars[23] = C[(byte) ((entropy[8] & 0xFF) >>> 2 & 0x1F)];
        chars[24] = C[(byte) ((entropy[8] << 3 | ((entropy[9] & 0xFF) >>> 5)) & 0x1F)];
        chars[25] = C[(byte) (entropy[9] & 0x1F)];

        return new ULID(new String(chars), timestamp, entropy);
    }

    /**
     * Extract and return the timestamp part from ULID.
     *
     * @return timestamp value
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Extract and return the entropy part from ULID.
     *
     * @return Entropy bytes
     */
    public byte[] entropy() {
        return Arrays.copyOf(entropy, entropy.length);
    }

    /**
     * Return encoded representation of ULID.
     *
     * @return string representation
     */
    public String encoded() {
        return representation;
    }

    @Override
    public int compareTo(final ULID o) {
        return representation.compareTo(o.representation);
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof ULID other && representation.equals(other.representation);
    }

    @Override
    public int hashCode() {
        return representation.hashCode();
    }

    @Override
    public String toString() {
        return "ULID(" + representation + ")";
    }
}
