package com.github.pgasync.sasl;

import java.text.Normalizer;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * This is a refactored version of <a href="https://github.com/ogrebgr/scram-sasl">original code</a>.
 */
public class SaslPrep {

    public static class CharsClass {
        // Each character class is a set of [start, end] tuples; each tuple is represented
        // in the mapping as mapping[start] = (end - start + 1).
        // Invariants:
        // - No empty ranges.
        // - No overlapping ranges.

        private final NavigableMap<Integer, Integer> mapping = new TreeMap<>();

        private CharsClass() {
        }

        private void addRange(int start, int end) {
            if (start > end) {
                throw new IllegalStateException("An empty range [" + start + ", " + end + "] detected");
            }

            int coalescedStart = start;
            int coalescedEnd = end;
            var left = mapping.floorEntry(start);

            if (left != null) {
                int leftStart = left.getKey();
                int leftEnd = leftStart + left.getValue() - 1;

                if (start <= leftEnd) {
                    mapping.remove(leftStart);
                    coalescedStart = leftStart;
                    coalescedEnd = Math.max(end, leftEnd);
                }
            }

            var right = mapping.ceilingEntry(start);
            while (right != null) {
                int rightStart = right.getKey();
                int rightEnd = rightStart + right.getValue() - 1;

                if (end >= rightStart) {
                    mapping.remove(rightStart);
                    coalescedEnd = Math.max(end, rightEnd);
                } else {
                    break;
                }
                right = mapping.ceilingEntry(start);
            }
            mapping.put(coalescedStart, coalescedEnd - coalescedStart + 1);
        }

        public boolean isCharInClass(int c) {
            var left = mapping.floorEntry(c);
            return left != null && c < left.getKey() + left.getValue();
        }

        /**
         * Returns the first character index in s which is in {@link CharsClass}, or -1 if
         * no character is in the class.
         */
        private int indexOfIn(String s) {
            for (int i = 0; i < s.length(); ) {
                int c = Character.codePointAt(s, i);

                if (isCharInClass(c)) {
                    return i;
                }

                i += Character.charCount(c);
            }
            return -1;
        }

        /**
         * Replaces each character of {@code s} which is in the {@link CharsClass} mapFrom
         * with the string {@code mapTo}.
         */
        private String applyMapTo(String s, String mapTo) {
            var result = new StringBuilder();

            for (int i = 0; i < s.length(); ) {
                int c = Character.codePointAt(s, i);
                int charCount = Character.charCount(c);

                if (isCharInClass(c)) {
                    result.append(mapTo);
                } else {
                    result.append(s, i, i + charCount);
                }
                i += charCount;
            }

            return result.toString();
        }

        public void forEach(BiConsumer<Integer, Integer> action) {
            mapping.forEach(action);
        }

        public static CharsClass fromList(int... charList) {
            var charsClass = new CharsClass();

            for (int j : charList) {
                charsClass.addRange(j, j);
            }
            return charsClass;
        }


        public static CharsClass fromRanges(int... charMap) {
            // There must be an even number of tuples in RANGES tables.
            if ((charMap.length % 2) != 0) {
                throw new IllegalArgumentException("Invalid character list size");
            }

            var charsClass = new CharsClass();
            for (int i = 0; i < charMap.length; i += 2) {
                charsClass.addRange(charMap[i], charMap[i + 1]);
            }
            return charsClass;
        }

        private static CharsClass fromClasses(CharsClass... classes) {
            var charsClass = new CharsClass();

            for (var aCharsClass : classes) {
                for (var e : aCharsClass.mapping.entrySet()) {
                    int start = e.getKey();
                    int end = start + e.getValue() - 1;
                    charsClass.addRange(start, end);
                }
            }
            return charsClass;
        }
    }

    /**
     * B.1 Commonly mapped to nothing
     */
    static final CharsClass B1 = CharsClass.fromList(
            0x00AD, 0x034F, 0x1806, 0x180B, 0x180C, 0x180D, 0x200B, 0x200C, 0x200D, 0x2060,
            0xFE00, 0xFE01, 0xFE02, 0xFE03, 0xFE04, 0xFE05, 0xFE06, 0xFE07, 0xFE08, 0xFE09,
            0xFE0A, 0xFE0B, 0xFE0C, 0xFE0D, 0xFE0E, 0xFE0F, 0xFEFF
    );

    /**
     * C.1.1 ASCII space characters
     */
    @SuppressWarnings("unused")
    static final CharsClass C11 = CharsClass.fromList(0x0020);

    /**
     * C.1.2 Non-ASCII space characters
     */
    static final CharsClass C12 = CharsClass.fromList(
            0x00A0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007,
            0x2008, 0x2009, 0x200A, 0x200B, 0x202F, 0x205F, 0x3000
    );

    /**
     * C.2.1 ASCII control characters
     */
    static final CharsClass C21 = CharsClass.fromList(
            0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007, 0x0008, 0x0009,
            0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F, 0x0010, 0x0011, 0x0012, 0x0013,
            0x0014, 0x0015, 0x0016, 0x0017, 0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D,
            0x001E, 0x001F, 0x007F
    );

    /**
     * C.2.2 Non-ASCII control characters
     */
    static final CharsClass C22 = CharsClass.fromList(
            0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087, 0x0088, 0x0089,
            0x008A, 0x008B, 0x008C, 0x008D, 0x008E, 0x008F, 0x0090, 0x0091, 0x0092, 0x0093,
            0x0094, 0x0095, 0x0096, 0x0097, 0x0098, 0x0099, 0x009A, 0x009B, 0x009C, 0x009D,
            0x009E, 0x009F, 0x06DD, 0x070F, 0x180E, 0x200C, 0x200D, 0x2028, 0x2029, 0x2060,
            0x2061, 0x2062, 0x2063, 0x206A, 0x206B, 0x206C, 0x206D, 0x206E, 0x206F, 0xFEFF,
            0xFFF9, 0xFFFA, 0xFFFB, 0xFFFC,
            0x1D173, 0x1D174, 0x1D175, 0x1D176, 0x1D177, 0x1D178, 0x1D179, 0x1D17A
    );

    /**
     * C.3 Private use
     */
    static final CharsClass C3 = CharsClass.fromRanges(
            0xE000, 0xF8FF, 0xF0000, 0xFFFFD, 0x100000, 0x10FFFD
    );

    /**
     * C.4 Non-character code points
     */
    static final CharsClass C4 = CharsClass.fromRanges(
            0xFDD0, 0xFDEF, 0xFFFE, 0xFFFF, 0x1FFFE, 0x1FFFF, 0x2FFFE, 0x2FFFF,
            0x3FFFE, 0x3FFFF, 0x4FFFE, 0x4FFFF, 0x5FFFE, 0x5FFFF, 0x6FFFE, 0x6FFFF,
            0x7FFFE, 0x7FFFF, 0x8FFFE, 0x8FFFF, 0x9FFFE, 0x9FFFF, 0xAFFFE, 0xAFFFF,
            0xBFFFE, 0xBFFFF, 0xCFFFE, 0xCFFFF, 0xDFFFE, 0xDFFFF, 0xEFFFE, 0xEFFFF,
            0xFFFFE, 0xFFFFF, 0x10FFFE, 0x10FFFF
    );

    /**
     * C.5 Surrogate codes
     */
    static final CharsClass C5 = CharsClass.fromRanges(
            0xD800, 0xDFFF
    );

    /**
     * C.6 Inappropriate for plain text
     */
    static final CharsClass C6 = CharsClass.fromList(
            0xFFF9, 0xFFFA, 0xFFFB, 0xFFFC, 0xFFFD
    );

    /**
     * C.7 Inappropriate for canonical representation
     */
    static final CharsClass C7 = CharsClass.fromList(
            0x2FF0, 0x2FF1, 0x2FF2, 0x2FF3, 0x2FF4, 0x2FF5, 0x2FF6, 0x2FF7, 0x2FF8, 0x2FF9,
            0x2FFA, 0x2FFB
    );

    /**
     * C.8 Change display properties or are deprecated
     */
    static final CharsClass C8 = CharsClass.fromList(
            0x0340, 0x0341, 0x200E, 0x200F, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E, 0x206A,
            0x206B, 0x206C, 0x206D, 0x206E, 0x206F
    );

    /**
     * C.9 Tagging characters (tuples)
     */
    static final CharsClass C9 = CharsClass.fromRanges(
            0xE0001, 0xE0001, 0xE0020, 0xE007F
    );

    /**
     * rfc4013 2.3. Prohibited Output
     */
    static final CharsClass saslProhibited = CharsClass.fromClasses(C12, C21, C22, C3, C4, C5, C6, C7, C8, C9);

    /**
     * D.1 Characters with bidirectional property "R" or "AL"
     */
    static final CharsClass D1 = CharsClass.fromRanges(
            0x05BE, 0x05BE, 0x05C0, 0x05C0, 0x05C3, 0x05C3, 0x05D0, 0x05EA, 0x05F0, 0x05F4,
            0x061B, 0x061B, 0x061F, 0x061F, 0x0621, 0x063A, 0x0640, 0x064A, 0x066D, 0x066F,
            0x0671, 0x06D5, 0x06DD, 0x06DD, 0x06E5, 0x06E6, 0x06FA, 0x06FE, 0x0700, 0x070D,
            0x0710, 0x0710, 0x0712, 0x072C, 0x0780, 0x07A5, 0x07B1, 0x07B1, 0x200F, 0x200F,
            0xFB1D, 0xFB1D, 0xFB1F, 0xFB28, 0xFB2A, 0xFB36, 0xFB38, 0xFB3C, 0xFB3E, 0xFB3E,
            0xFB40, 0xFB41, 0xFB43, 0xFB44, 0xFB46, 0xFBB1, 0xFBD3, 0xFD3D, 0xFD50, 0xFD8F,
            0xFD92, 0xFDC7, 0xFDF0, 0xFDFC, 0xFE70, 0xFE74, 0xFE76, 0xFEFC
    );

    /**
     * D.2 Characters with bidirectional property "L"
     */
    static final CharsClass D2 = CharsClass.fromRanges(
            0x0041, 0x005A, 0x0061, 0x007A, 0x00AA, 0x00AA, 0x00B5, 0x00B5, 0x00BA, 0x00BA, 0x00C0, 0x00D6,
            0x00D8, 0x00F6, 0x00F8, 0x0220, 0x0222, 0x0233, 0x0250, 0x02AD, 0x02B0, 0x02B8, 0x02BB, 0x02C1,
            0x02D0, 0x02D1, 0x02E0, 0x02E4, 0x02EE, 0x02EE, 0x037A, 0x037A, 0x0386, 0x0386, 0x0388, 0x038A,
            0x038C, 0x038C, 0x038E, 0x03A1, 0x03A3, 0x03CE, 0x03D0, 0x03F5, 0x0400, 0x0482, 0x048A, 0x04CE,
            0x04D0, 0x04F5, 0x04F8, 0x04F9, 0x0500, 0x050F, 0x0531, 0x0556, 0x0559, 0x055F, 0x0561, 0x0587,
            0x0589, 0x0589, 0x0903, 0x0903, 0x0905, 0x0939, 0x093D, 0x0940, 0x0949, 0x094C, 0x0950, 0x0950,
            0x0958, 0x0961, 0x0964, 0x0970, 0x0982, 0x0983, 0x0985, 0x098C, 0x098F, 0x0990, 0x0993, 0x09A8,
            0x09AA, 0x09B0, 0x09B2, 0x09B2, 0x09B6, 0x09B9, 0x09BE, 0x09C0, 0x09C7, 0x09C8, 0x09CB, 0x09CC,
            0x09D7, 0x09D7, 0x09DC, 0x09DD, 0x09DF, 0x09E1, 0x09E6, 0x09F1, 0x09F4, 0x09FA, 0x0A05, 0x0A0A,
            0x0A0F, 0x0A10, 0x0A13, 0x0A28, 0x0A2A, 0x0A30, 0x0A32, 0x0A33, 0x0A35, 0x0A36, 0x0A38, 0x0A39,
            0x0A3E, 0x0A40, 0x0A59, 0x0A5C, 0x0A5E, 0x0A5E, 0x0A66, 0x0A6F, 0x0A72, 0x0A74, 0x0A83, 0x0A83,
            0x0A85, 0x0A8B, 0x0A8D, 0x0A8D, 0x0A8F, 0x0A91, 0x0A93, 0x0AA8, 0x0AAA, 0x0AB0, 0x0AB2, 0x0AB3,
            0x0AB5, 0x0AB9, 0x0ABD, 0x0AC0, 0x0AC9, 0x0AC9, 0x0ACB, 0x0ACC, 0x0AD0, 0x0AD0, 0x0AE0, 0x0AE0,
            0x0AE6, 0x0AEF, 0x0B02, 0x0B03, 0x0B05, 0x0B0C, 0x0B0F, 0x0B10, 0x0B13, 0x0B28, 0x0B2A, 0x0B30,
            0x0B32, 0x0B33, 0x0B36, 0x0B39, 0x0B3D, 0x0B3E, 0x0B40, 0x0B40, 0x0B47, 0x0B48, 0x0B4B, 0x0B4C,
            0x0B57, 0x0B57, 0x0B5C, 0x0B5D, 0x0B5F, 0x0B61, 0x0B66, 0x0B70, 0x0B83, 0x0B83, 0x0B85, 0x0B8A,
            0x0B8E, 0x0B90, 0x0B92, 0x0B95, 0x0B99, 0x0B9A, 0x0B9C, 0x0B9C, 0x0B9E, 0x0B9F, 0x0BA3, 0x0BA4,
            0x0BA8, 0x0BAA, 0x0BAE, 0x0BB5, 0x0BB7, 0x0BB9, 0x0BBE, 0x0BBF, 0x0BC1, 0x0BC2, 0x0BC6, 0x0BC8,
            0x0BCA, 0x0BCC, 0x0BD7, 0x0BD7, 0x0BE7, 0x0BF2, 0x0C01, 0x0C03, 0x0C05, 0x0C0C, 0x0C0E, 0x0C10,
            0x0C12, 0x0C28, 0x0C2A, 0x0C33, 0x0C35, 0x0C39, 0x0C41, 0x0C44, 0x0C60, 0x0C61, 0x0C66, 0x0C6F,
            0x0C82, 0x0C83, 0x0C85, 0x0C8C, 0x0C8E, 0x0C90, 0x0C92, 0x0CA8, 0x0CAA, 0x0CB3, 0x0CB5, 0x0CB9,
            0x0CBE, 0x0CBE, 0x0CC0, 0x0CC4, 0x0CC7, 0x0CC8, 0x0CCA, 0x0CCB, 0x0CD5, 0x0CD6, 0x0CDE, 0x0CDE,
            0x0CE0, 0x0CE1, 0x0CE6, 0x0CEF, 0x0D02, 0x0D03, 0x0D05, 0x0D0C, 0x0D0E, 0x0D10, 0x0D12, 0x0D28,
            0x0D2A, 0x0D39, 0x0D3E, 0x0D40, 0x0D46, 0x0D48, 0x0D4A, 0x0D4C, 0x0D57, 0x0D57, 0x0D60, 0x0D61,
            0x0D66, 0x0D6F, 0x0D82, 0x0D83, 0x0D85, 0x0D96, 0x0D9A, 0x0DB1, 0x0DB3, 0x0DBB, 0x0DBD, 0x0DBD,
            0x0DC0, 0x0DC6, 0x0DCF, 0x0DD1, 0x0DD8, 0x0DDF, 0x0DF2, 0x0DF4, 0x0E01, 0x0E30, 0x0E32, 0x0E33,
            0x0E40, 0x0E46, 0x0E4F, 0x0E5B, 0x0E81, 0x0E82, 0x0E84, 0x0E84, 0x0E87, 0x0E88, 0x0E8A, 0x0E8A,
            0x0E8D, 0x0E8D, 0x0E94, 0x0E97, 0x0E99, 0x0E9F, 0x0EA1, 0x0EA3, 0x0EA5, 0x0EA5, 0x0EA7, 0x0EA7,
            0x0EAA, 0x0EAB, 0x0EAD, 0x0EB0, 0x0EB2, 0x0EB3, 0x0EBD, 0x0EBD, 0x0EC0, 0x0EC4, 0x0EC6, 0x0EC6,
            0x0ED0, 0x0ED9, 0x0EDC, 0x0EDD, 0x0F00, 0x0F17, 0x0F1A, 0x0F34, 0x0F36, 0x0F36, 0x0F38, 0x0F38,
            0x0F3E, 0x0F47, 0x0F49, 0x0F6A, 0x0F7F, 0x0F7F, 0x0F85, 0x0F85, 0x0F88, 0x0F8B, 0x0FBE, 0x0FC5,
            0x0FC7, 0x0FCC, 0x0FCF, 0x0FCF, 0x1000, 0x1021, 0x1023, 0x1027, 0x1029, 0x102A, 0x102C, 0x102C,
            0x1031, 0x1031, 0x1038, 0x1038, 0x1040, 0x1057, 0x10A0, 0x10C5, 0x10D0, 0x10F8, 0x10FB, 0x10FB,
            0x1100, 0x1159, 0x115F, 0x11A2, 0x11A8, 0x11F9, 0x1200, 0x1206, 0x1208, 0x1246, 0x1248, 0x1248,
            0x124A, 0x124D, 0x1250, 0x1256, 0x1258, 0x1258, 0x125A, 0x125D, 0x1260, 0x1286, 0x1288, 0x1288,
            0x128A, 0x128D, 0x1290, 0x12AE, 0x12B0, 0x12B0, 0x12B2, 0x12B5, 0x12B8, 0x12BE, 0x12C0, 0x12C0,
            0x12C2, 0x12C5, 0x12C8, 0x12CE, 0x12D0, 0x12D6, 0x12D8, 0x12EE, 0x12F0, 0x130E, 0x1310, 0x1310,
            0x1312, 0x1315, 0x1318, 0x131E, 0x1320, 0x1346, 0x1348, 0x135A, 0x1361, 0x137C, 0x13A0, 0x13F4,
            0x1401, 0x1676, 0x1681, 0x169A, 0x16A0, 0x16F0, 0x1700, 0x170C, 0x170E, 0x1711, 0x1720, 0x1731,
            0x1735, 0x1736, 0x1740, 0x1751, 0x1760, 0x176C, 0x176E, 0x1770, 0x1780, 0x17B6, 0x17BE, 0x17C5,
            0x17C7, 0x17C8, 0x17D4, 0x17DA, 0x17DC, 0x17DC, 0x17E0, 0x17E9, 0x1810, 0x1819, 0x1820, 0x1877,
            0x1880, 0x18A8, 0x1E00, 0x1E9B, 0x1EA0, 0x1EF9, 0x1F00, 0x1F15, 0x1F18, 0x1F1D, 0x1F20, 0x1F45,
            0x1F48, 0x1F4D, 0x1F50, 0x1F57, 0x1F59, 0x1F59, 0x1F5B, 0x1F5B, 0x1F5D, 0x1F5D, 0x1F5F, 0x1F7D,
            0x1F80, 0x1FB4, 0x1FB6, 0x1FBC, 0x1FBE, 0x1FBE, 0x1FC2, 0x1FC4, 0x1FC6, 0x1FCC, 0x1FD0, 0x1FD3,
            0x1FD6, 0x1FDB, 0x1FE0, 0x1FEC, 0x1FF2, 0x1FF4, 0x1FF6, 0x1FFC, 0x200E, 0x200E, 0x2071, 0x2071,
            0x207F, 0x207F, 0x2102, 0x2102, 0x2107, 0x2107, 0x210A, 0x2113, 0x2115, 0x2115, 0x2119, 0x211D,
            0x2124, 0x2124, 0x2126, 0x2126, 0x2128, 0x2128, 0x212A, 0x212D, 0x212F, 0x2131, 0x2133, 0x2139,
            0x213D, 0x213F, 0x2145, 0x2149, 0x2160, 0x2183, 0x2336, 0x237A, 0x2395, 0x2395, 0x249C, 0x24E9,
            0x3005, 0x3007, 0x3021, 0x3029, 0x3031, 0x3035, 0x3038, 0x303C, 0x3041, 0x3096, 0x309D, 0x309F,
            0x30A1, 0x30FA, 0x30FC, 0x30FF, 0x3105, 0x312C, 0x3131, 0x318E, 0x3190, 0x31B7, 0x31F0, 0x321C,
            0x3220, 0x3243, 0x3260, 0x327B, 0x327F, 0x32B0, 0x32C0, 0x32CB, 0x32D0, 0x32FE, 0x3300, 0x3376,
            0x337B, 0x33DD, 0x33E0, 0x33FE, 0x3400, 0x4DB5, 0x4E00, 0x9FA5, 0xA000, 0xA48C, 0xAC00, 0xD7A3,
            0xD800, 0xFA2D, 0xFA30, 0xFA6A, 0xFB00, 0xFB06, 0xFB13, 0xFB17, 0xFF21, 0xFF3A, 0xFF41, 0xFF5A,
            0xFF66, 0xFFBE, 0xFFC2, 0xFFC7, 0xFFCA, 0xFFCF, 0xFFD2, 0xFFD7, 0xFFDA, 0xFFDC,
            0x10300, 0x1031E, 0x10320, 0x10323, 0x10330, 0x1034A, 0x10400, 0x10425, 0x10428, 0x1044D,
            0x1D000, 0x1D0F5, 0x1D100, 0x1D126, 0x1D12A, 0x1D166, 0x1D16A, 0x1D172, 0x1D183, 0x1D184,
            0x1D18C, 0x1D1A9, 0x1D1AE, 0x1D1DD, 0x1D400, 0x1D454, 0x1D456, 0x1D49C, 0x1D49E, 0x1D49F,
            0x1D4A2, 0x1D4A2, 0x1D4A5, 0x1D4A6, 0x1D4A9, 0x1D4AC, 0x1D4AE, 0x1D4B9, 0x1D4BB, 0x1D4BB,
            0x1D4BD, 0x1D4C0, 0x1D4C2, 0x1D4C3, 0x1D4C5, 0x1D505, 0x1D507, 0x1D50A, 0x1D50D, 0x1D514,
            0x1D516, 0x1D51C, 0x1D51E, 0x1D539, 0x1D53B, 0x1D53E, 0x1D540, 0x1D544, 0x1D546, 0x1D546,
            0x1D54A, 0x1D550, 0x1D552, 0x1D6A3, 0x1D6A8, 0x1D7C9, 0x20000, 0x2A6D6, 0x2F800, 0x2FA1D,
            0xF0000, 0xFFFFD, 0x100000, 0x10FFFD
    );

    private SaslPrep() {
    }

    public static String asQueryString(String value) {
        // Map
        var mapped1 = B1.applyMapTo(value, "");
        var mapped2 = C12.applyMapTo(mapped1, " ");
        // Normalize
        var normalized = Normalizer.normalize(mapped2, Normalizer.Form.NFKC);
        // Prohibit
        int idx = saslProhibited.indexOfIn(normalized);
        if (idx != -1) {
            throw new IllegalStateException("Prohibited character detected while SASLPrep");
        }
        // Check bidirectional strings
        verifyRTL(normalized);
        return normalized;
    }

    /**
     * Performs RTL verification according to rfc3454 section 6.  On failure,
     * throws a {@link IllegalStateException}.
     */
    protected static void verifyRTL(String s) {
        int containsRAL = D1.indexOfIn(s);
        if (containsRAL != -1) {
            // 2) If a string contains any RandALCat character, the string MUST NOT
            // contain any LCat character.
            int containsL = D2.indexOfIn(s);
            if (containsL != -1) {
                throw new IllegalStateException("Both RAL and L characters detected while SASLprep");
            }
            // 3) If a string contains any RandALCat character, a RandALCat
            // character MUST be the first character of the string
            if (containsRAL != 0) {
                throw new IllegalStateException("RAL without prefix detected while SASLprep");
            }
            // ... and a RandALCat character MUST be the last character of the string.
            if (!D1.isCharInClass(s.charAt(s.length() - 1))) {
                throw new IllegalStateException("RAL without suffix detected while SASLprep");
            }
        }
    }
}
