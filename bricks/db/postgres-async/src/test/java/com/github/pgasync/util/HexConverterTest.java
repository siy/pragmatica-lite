package com.github.pgasync.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class HexConverterTest {
    @Test
    public void correctHexadecimalStringIsParsedSuccessfully() {
        assertArrayEquals(new byte[]{0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF}, HexConverter.parseHexBinary("0123456789ABCDEF"));
        assertArrayEquals(new byte[]{0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF}, HexConverter.parseHexBinary("0123456789abcdef"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectHexadecimalCharacterThrowsException() {
        HexConverter.parseHexBinary("G");
        HexConverter.parseHexBinary("h");
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectLenghtThrowsException() {
        HexConverter.parseHexBinary("012");
    }
}