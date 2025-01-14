package com.github.pgasync.sasl;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SaslPrepCharClassTest {

    private static String rangeToString(SaslPrep.CharsClass charsClass) {
        StringBuilder ranges = new StringBuilder();
        charsClass.forEach((start, length) -> ranges.append("[").append(start).append(",").append(start + length - 1).append("]"));
        return ranges.toString();
    }

    @Test
    public void nonOverlappingRanges() {
        assertEquals("[1,1][2,2][3,3]", rangeToString(SaslPrep.CharsClass.fromList(1, 2, 3)));
        assertEquals("[1,1][2,2][3,3]", rangeToString(SaslPrep.CharsClass.fromList(1, 3, 2)));
    }

    @Test(expected = IllegalStateException.class)
    public void emptyRanges() {
        SaslPrep.CharsClass.fromRanges(1, 0, 2, 3);
    }

    /**
     * |-----|
     * |-----|
     */
    @Test
    public void overlappingRanges1() {
        assertEquals("[1,4]", rangeToString(SaslPrep.CharsClass.fromRanges(1, 3, 2, 4)));
    }

    /**
     * |-----|
     * |-----|
     */
    @Test
    public void overlappingRanges2() {
        assertEquals("[1,4]", rangeToString(SaslPrep.CharsClass.fromRanges(2, 4, 1, 3)));
    }

    /**
     * |-------|
     * |---|
     */
    @Test
    public void overlappingRanges3() {
        assertEquals("[1,4]", rangeToString(SaslPrep.CharsClass.fromRanges(1, 4, 2, 3)));
    }

    /**
     * |---|
     * |-------|
     */
    @Test
    public void overlappingRanges4() {
        assertEquals("[1,4]", rangeToString(SaslPrep.CharsClass.fromRanges(2, 3, 1, 4)));
    }

    /**
     * |-----|
     * |-----|
     */
    @Test
    public void overlappingRanges5() {
        assertEquals("[1,4]", rangeToString(SaslPrep.CharsClass.fromRanges(1, 4, 1, 4)));
    }

    /**
     * |---|
     * |---|
     */
    @Test
    public void overlappingRanges6() {
        assertEquals("[1,4]", rangeToString(SaslPrep.CharsClass.fromRanges(1, 3, 3, 4)));
    }

    /**
     * |---|
     * |---|
     */
    @Test
    public void overlappingRanges7() {
        assertEquals("[1,4]", rangeToString(SaslPrep.CharsClass.fromRanges(3, 4, 1, 3)));
    }

    /**
     * |--| |-| |--| |-|
     * |-----------------|
     */
    @Test
    public void overlappingRanges8() {
        assertEquals("[1,8]", rangeToString(SaslPrep.CharsClass.fromRanges(1, 2, 4, 4, 5, 6, 7, 7, 1, 8)));
    }

    /**
     * |-----------------|
     * |--| |-| |--| |-|
     */
    @Test
    public void overlappingRanges9() {
        assertEquals("[1,8]", rangeToString(SaslPrep.CharsClass.fromRanges(1, 8, 1, 2, 4, 4, 5, 6, 7, 7)));
    }

}
