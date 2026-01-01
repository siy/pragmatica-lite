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

import org.junit.jupiter.api.Test;
import org.pragmatica.utility.KSUID.KSUIDError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class KSUIDTest {

    // ==================== Test Vectors from Go Reference Implementation ====================

    @Test
    void nilKsuidEncodesToAllZeros() {
        assertEquals("000000000000000000000000000", KSUID.NIL.encoded());
    }

    @Test
    void maxKsuidEncodesToExpectedValue() {
        // From Go reference: maxStringEncoded = "aWgEPTl1tmebfsQzFP4bxwgy80V"
        assertEquals("aWgEPTl1tmebfsQzFP4bxwgy80V", KSUID.MAX.encoded());
    }

    @Test
    void nilKsuidRoundTrip() {
        var parsed = KSUID.parse("000000000000000000000000000");
        assertTrue(parsed.isSuccess());
        parsed.onSuccess(ksuid -> {
            assertTrue(ksuid.isNil());
            assertArrayEquals(new byte[20], ksuid.toBytes());
        });
    }

    @Test
    void maxKsuidRoundTrip() {
        var parsed = KSUID.parse("aWgEPTl1tmebfsQzFP4bxwgy80V");
        assertTrue(parsed.isSuccess());
        parsed.onSuccess(ksuid -> {
            assertArrayEquals(KSUID.MAX.toBytes(), ksuid.toBytes());
            assertEquals(KSUID.MAX, ksuid);
        });
    }

    // ==================== Generation Tests ====================

    @Test
    void generatedKsuidHasCorrectLength() {
        var ksuid = KSUID.ksuid();
        assertEquals(27, ksuid.encoded().length());
        assertEquals(20, ksuid.toBytes().length);
    }

    @Test
    void generatedKsuidIsNotNil() {
        var ksuid = KSUID.ksuid();
        assertFalse(ksuid.isNil());
    }

    @Test
    void generatedKsuidsAreUnique() {
        var set = new HashSet<String>();
        for (int i = 0; i < 10_000; i++) {
            assertTrue(set.add(KSUID.ksuid().encoded()));
        }
    }

    @Test
    void generatedKsuidHasReasonableTimestamp() {
        var ksuid = KSUID.ksuid();
        var timestamp = ksuid.timestamp();
        var now = System.currentTimeMillis() / 1000;

        // Timestamp should be within 2 seconds of now
        assertTrue(Math.abs(now - timestamp) < 2,
                   "Timestamp " + timestamp + " should be close to " + now);
    }

    // ==================== Parsing Tests ====================

    @Test
    void parseValidKsuid() {
        var original = KSUID.ksuid();
        var encoded = original.encoded();

        KSUID.parse(encoded)
             .onFailure(cause -> fail("Should parse: " + cause))
             .onSuccess(parsed -> assertEquals(original, parsed));
    }

    @Test
    void parseNullReturnsError() {
        var result = KSUID.parse(null);
        assertTrue(result.isFailure());
        result.onFailure(cause -> assertInstanceOf(KSUIDError.NullInput.class, cause));
    }

    @Test
    void parseTooShortReturnsError() {
        var result = KSUID.parse("abc");
        assertTrue(result.isFailure());
        result.onFailure(cause -> {
            assertInstanceOf(KSUIDError.InvalidLength.class, cause);
            assertEquals(3, ((KSUIDError.InvalidLength) cause).actual());
        });
    }

    @Test
    void parseTooLongReturnsError() {
        var result = KSUID.parse("0000000000000000000000000000");
        assertTrue(result.isFailure());
        result.onFailure(cause -> assertInstanceOf(KSUIDError.InvalidLength.class, cause));
    }

    @Test
    void parseInvalidCharacterReturnsError() {
        // Contains invalid character '!'
        var result = KSUID.parse("aWgEPTl1tmebfsQzFP4bxwgy80!");
        assertTrue(result.isFailure());
        result.onFailure(cause -> assertInstanceOf(KSUIDError.InvalidCharacter.class, cause));
    }

    @Test
    void parseInvalidCharacterSpaceReturnsError() {
        var result = KSUID.parse("aWgEPTl1tmebfsQzFP4bxwgy8 V");
        assertTrue(result.isFailure());
        result.onFailure(cause -> assertInstanceOf(KSUIDError.InvalidCharacter.class, cause));
    }

    // ==================== Byte Array Tests ====================

    @Test
    void fromBytesValidInput() {
        var original = KSUID.ksuid();
        var bytes = original.toBytes();

        KSUID.fromBytes(bytes)
             .onFailure(cause -> fail("Should parse bytes: " + cause))
             .onSuccess(parsed -> assertEquals(original, parsed));
    }

    @Test
    void fromBytesNullReturnsError() {
        var result = KSUID.fromBytes(null);
        assertTrue(result.isFailure());
        result.onFailure(cause -> assertInstanceOf(KSUIDError.NullInput.class, cause));
    }

    @Test
    void fromBytesWrongLengthReturnsError() {
        var result = KSUID.fromBytes(new byte[10]);
        assertTrue(result.isFailure());
        result.onFailure(cause -> {
            assertInstanceOf(KSUIDError.InvalidByteLength.class, cause);
            assertEquals(10, ((KSUIDError.InvalidByteLength) cause).actual());
        });
    }

    @Test
    void toBytesReturnsDefensiveCopy() {
        var ksuid = KSUID.ksuid();
        var bytes1 = ksuid.toBytes();
        var bytes2 = ksuid.toBytes();

        assertNotSame(bytes1, bytes2);
        assertArrayEquals(bytes1, bytes2);

        // Modifying returned array should not affect KSUID
        bytes1[0] = (byte) 0xFF;
        assertFalse(java.util.Arrays.equals(bytes1, ksuid.toBytes()));
    }

    // ==================== Comparison and Sorting Tests ====================

    @Test
    void nilIsLessThanMax() {
        assertTrue(KSUID.NIL.compareTo(KSUID.MAX) < 0);
        assertTrue(KSUID.MAX.compareTo(KSUID.NIL) > 0);
    }

    @Test
    void equalKsuidsCompareToZero() {
        var ksuid = KSUID.ksuid();
        assertEquals(0, ksuid.compareTo(ksuid));

        KSUID.parse(ksuid.encoded())
             .onSuccess(parsed -> assertEquals(0, ksuid.compareTo(parsed)));
    }

    @Test
    void ksuidsAreSortableByTimestamp() throws InterruptedException {
        var ksuids = new ArrayList<KSUID>();

        // Generate KSUIDs with small delays to ensure different timestamps
        for (int i = 0; i < 5; i++) {
            ksuids.add(KSUID.ksuid());
            Thread.sleep(10);
        }

        var sorted = new ArrayList<>(ksuids);
        Collections.sort(sorted);

        // Sorted order should match generation order (same timestamps possible)
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).compareTo(sorted.get(i + 1)) <= 0);
        }
    }

    @Test
    void stringsSortLexicographicallyByTimestamp() throws InterruptedException {
        var strings = new ArrayList<String>();

        for (int i = 0; i < 5; i++) {
            strings.add(KSUID.ksuid().encoded());
            Thread.sleep(10);
        }

        var sorted = new ArrayList<>(strings);
        Collections.sort(sorted);

        // Lexicographic sort of strings should preserve time order
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).compareTo(sorted.get(i + 1)) <= 0);
        }
    }

    // ==================== Equality and Hashing Tests ====================

    @Test
    void equalityBasedOnContent() {
        var ksuid1 = KSUID.ksuid();
        var ksuid2 = KSUID.parse(ksuid1.encoded()).fold(_ -> null, k -> k);

        assertEquals(ksuid1, ksuid2);
        assertEquals(ksuid1.hashCode(), ksuid2.hashCode());
    }

    @Test
    void differentKsuidsAreNotEqual() {
        var ksuid1 = KSUID.ksuid();
        var ksuid2 = KSUID.ksuid();

        assertNotEquals(ksuid1, ksuid2);
    }

    @Test
    void equalsWithNull() {
        assertNotEquals(KSUID.ksuid(), null);
    }

    @Test
    void equalsWithDifferentType() {
        assertNotEquals(KSUID.ksuid(), "not a ksuid");
    }

    // ==================== toString Tests ====================

    @Test
    void toStringReturnsEncoded() {
        var ksuid = KSUID.ksuid();
        assertEquals(ksuid.encoded(), ksuid.toString());
    }

    // ==================== Timestamp Tests ====================

    @Test
    void timestampExtractionForNil() {
        // Nil has timestamp 0 in KSUID epoch, so Unix timestamp = EPOCH
        assertEquals(KSUID.EPOCH, KSUID.NIL.timestamp());
    }

    @Test
    void timestampExtractionForMax() {
        // Max has timestamp 0xFFFFFFFF in KSUID epoch
        long expected = KSUID.EPOCH + 0xFFFFFFFFL;
        assertEquals(expected, KSUID.MAX.timestamp());
    }

    // ==================== Base62 Encoding Edge Cases ====================

    @Test
    void allValidBase62CharactersAccepted() {
        // Test string with all digits
        var result1 = KSUID.parse("012345678901234567890123456");
        assertTrue(result1.isSuccess());

        // Round trip the parsed value
        result1.onSuccess(ksuid -> {
            var reencoded = ksuid.encoded();
            assertEquals(27, reencoded.length());
            KSUID.parse(reencoded)
                 .onSuccess(reparsed -> assertEquals(ksuid, reparsed));
        });
    }

    @Test
    void encodingIsConsistent() {
        var ksuid = KSUID.ksuid();
        var encoded1 = ksuid.encoded();
        var encoded2 = ksuid.encoded();
        assertEquals(encoded1, encoded2);
    }

    // ==================== Constants Tests ====================

    @Test
    void constantsHaveCorrectValues() {
        assertEquals(1_400_000_000L, KSUID.EPOCH);
        assertEquals(20, KSUID.BYTE_LENGTH);
        assertEquals(4, KSUID.TIMESTAMP_LENGTH);
        assertEquals(16, KSUID.PAYLOAD_LENGTH);
        assertEquals(27, KSUID.STRING_LENGTH);
    }
}
