package com.github.pgasync.util;

final public class HexConverter {
    private static final char[] HEX_DIGITS = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private HexConverter() {}

    public static byte[] parseHexBinary(String s) {
        var chars = s.toCharArray();

        if (chars.length % 2 != 0) {
            throw new IllegalArgumentException(STR."Input string length must be even <\{s}> = \{s.length()}");
        }

        var out = new byte[chars.length / 2];

        for (int i = 0; i < chars.length; i += 2) {
            out[i / 2] = (byte) ((hexToBin(chars[i]) << 4) + hexToBin(chars[i + 1]));
        }

        return out;
    }

    public static String printHexBinary(byte[] data) {
        var r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(HEX_DIGITS[(b >> 4) & 0xF]);
            r.append(HEX_DIGITS[(b & 0xF)]);
        }
        return r.toString();
    }

    private static int hexToBin(char ch) {
        return switch (ch) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> ch - '0';
            case 'A', 'B', 'C', 'D', 'E', 'F' -> ch - 'A' + 10;
            case 'a', 'b', 'c', 'd', 'e', 'f' -> ch - 'a' + 10;

            default -> throw new IllegalArgumentException(STR."illegal hexadecimal character '\{ch}'");
        };
    }
}