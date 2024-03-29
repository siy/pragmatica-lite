/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

/**
 * Conversion tests from/to SQL types.
 *
 * @author Antti Laisi
 */
@Tag("Slow")
public class TypeConverterTest {

    @ClassRule
    public static final DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @Test
    public void shouldConvertNullToString() {
        assertNull(dbr.query("select NULL").index(0).getString(0));
    }

    @Test
    public void shouldConvertUnspecifiedToString() {
        assertEquals("test", dbr.query("select 'test'").index(0).getString(0));
    }

    @Test
    public void shouldConvertTextToString() {
        assertEquals("test", dbr.query("select 'test'::TEXT").index(0).getString(0));
    }

    @Test
    public void shouldConvertVarcharToString() {
        assertEquals("test", dbr.query("select 'test'::VARCHAR").index(0).getString(0));
    }

    @Test
    public void shouldConvertCharToString() {
        assertEquals("test ", dbr.query("select 'test'::CHAR(5)").index(0).getString(0));
    }

    @Test
    public void shouldConvertSingleCharToString() {
        assertEquals("X", dbr.query("select 'X'::CHAR AS single").index(0).getString("single"));
    }

    @Test
    public void shouldConvertNullToLong() {
        assertNull(dbr.query("select NULL").index(0).getLong(0));
    }

    @Test
    public void shouldConvertInt8ToLong() {
        assertEquals(5000L, dbr.query("select 5000::INT8").index(0).getLong(0).longValue());
    }

    @Test
    public void shouldConvertInt4ToLong() {
        assertEquals(4000L, dbr.query("select 4000::INT4 AS R").index(0).getLong("r").longValue());
    }

    @Test
    public void shouldConvertInt2ToLong() {
        assertEquals(4000L, dbr.query("select 4000::INT2").index(0).getLong(0).longValue());
    }

    @Test
    public void shouldConvertInt4ToInteger() {
        assertEquals(5000, dbr.query("select 5000::INT4 AS I").index(0).getInt("i").intValue());
    }

    @Test
    public void shouldConvertInt2ToInteger() {
        assertEquals(4000, dbr.query("select 4000::INT2").index(0).getInt(0).intValue());
    }

    @Test
    public void shouldConvertInt2ToShort() {
        assertEquals(3000, dbr.query("select 3000::INT2").index(0).getShort(0).shortValue());
    }

    @Test
    public void shouldConvertInt2ToShortWithName() {
        assertEquals(128, dbr.query("select 128::INT2 AS S").index(0).getShort("s").shortValue());
    }

    @Test
    public void shouldConvertCharToByte() {
        assertEquals(65, dbr.query("select 65::INT2").index(0).getByte(0).byteValue());
    }

    @Test
    public void shouldConvertCharToByteWithName() {
        assertEquals(65, dbr.query("select 65::INT2 as C").index(0).getByte("c").byteValue());
    }

    @Test
    public void shouldConvertInt8ToBigInteger() {
        assertEquals(new BigInteger("9223372036854775807"), dbr.query("select 9223372036854775807::INT8").index(0)
                                                               .getBigInteger(0));
    }

    @Test
    public void shouldConvertInt4ToBigInteger() {
        assertEquals(new BigInteger("1000"), dbr.query("select 1000::INT4 as num").index(0).getBigInteger("num"));
    }

    @Test
    public void shouldConvertFloat8ToBigDecimal() {
        assertEquals(new BigDecimal("123.56"), dbr.query("select 123.56::FLOAT8").index(0).getBigDecimal(0));
    }

    @Test
    public void shouldConvertFloat4ToBigDecimal() {
        assertEquals(new BigDecimal("789.01"), dbr.query("select 789.01::FLOAT4 as sum").index(0).getBigDecimal("sum"));
    }

    @Test
    public void shouldConvertNumericToBigDecimal() {
        assertEquals(new BigDecimal("1223423.01"), dbr.query("select 1223423.01::NUMERIC as sum").index(0).getBigDecimal("sum"));
    }

    @Test
    public void shouldConvertFloat4ToDouble() {
        assertEquals((Double) 1223420.0, dbr.query("select 1223420.0::FLOAT4 as sum").index(0).getDouble("sum"));
    }

    @Test
    public void shouldConvertNumericToDouble() {
        assertEquals((Double) 1223423.01, dbr.query("select 1223423.01::NUMERIC as sum").index(0).getDouble("sum"));
    }

    @Test
    public void shouldConvertDateToDate() {
        assertEquals(LocalDate.parse("2014-01-31"),
                     dbr.query("select '2014-01-31'::DATE").index(0).getLocalDate(0));
    }

    @Test
    public void shouldConvertDateToDateWithName() {
        assertEquals(LocalDate.parse("2014-02-21"), dbr.query("select '2014-02-21'::DATE as D").index(0)
                                                       .getLocalDate("d"));
    }

    @Test
    public void shouldConvertTimeToTime() {
        assertEquals(LocalTime.parse("10:15:31.123"), dbr.query("select '10:15:31.123'::TIME").index(0)
                                                         .getLocalTime(0));
    }

    @Test
    public void shouldConvertZonedTimeToTime() {
        assertEquals(OffsetTime.parse("23:59:59.999Z").toLocalTime(), dbr.query("select '23:59:59.999Z'::TIMETZ as zoned")
                                                                         .index(0).getLocalTime("zoned"));
    }

    @Test
    public void shouldConvertTimestampToTimestamp() {
        assertEquals(LocalDateTime.parse("2014-02-21T23:59:59.999").toInstant(ZoneOffset.UTC),
                     dbr.query("select '2014-02-21 23:59:59.999'::TIMESTAMP as ts").index(0).getInstant("ts"));
    }

    @Test
    public void shouldConvertTimestampWithShortMillisToTimestamp() {
        assertEquals(LocalDateTime.parse("2014-02-21T23:59:59.990").toInstant(ZoneOffset.UTC),
                     dbr.query("select '2014-02-21 23:59:59.99'::TIMESTAMP as ts").index(0).getInstant("ts"));
    }

    @Test
    public void shouldConvertTimestampWithNoMillisToTimestamp() {
        assertEquals(LocalDateTime.parse("2014-02-21T23:59:59").toInstant(ZoneOffset.UTC),
                     dbr.query("select '2014-02-21 23:59:59'::TIMESTAMP as ts").index(0).getInstant("ts"));
    }

    @Test
    public void shouldConvertZonedTimestampToTimestamp() {
        assertEquals(Instant.from(ZonedDateTime.parse("2014-02-21T23:59:59.999Z")),
                     dbr.query("select '2014-02-21 23:59:59.999Z'::TIMESTAMPTZ as ts").index(0).getInstant("ts"));
    }

    @Test
    public void shouldConvertZonedTimestampWithNanosToTimestamp() {
        assertEquals(Instant.parse("2014-02-21T23:59:59.000999Z"),
                     dbr.query("select '2014-02-21 23:59:59.000999+00'::TIMESTAMPTZ as ts").index(0).getInstant("ts"));
    }

    @Test
    public void shouldConvertByteAToBytes() {
        assertArrayEquals(new byte[]{0x41, 0x41}, dbr.query("select '\\x4141'::BYTEA").index(0).getBytes(0));
    }

    @Test
    public void shouldConvertByteAToBytesWithName() {
        assertArrayEquals(new byte[]{0x41, 0x41}, dbr.query("select $1::BYTEA as bytes", List.of("AA")).index(0)
                                                     .getBytes("bytes"));
    }

    @Test
    public void shouldConvertBoolean() {
        assertTrue(dbr.query("select $1::BOOL as b", List.of(true)).index(0).getBoolean("b"));
        assertFalse(dbr.query("select $1::BOOL as b", List.of(false)).index(0).getBoolean(0));
        assertNull(dbr.query("select $1::BOOL as b", Collections.singletonList(null)).index(0).getBoolean("b"));
        assertArrayEquals(new Boolean[]{true, false}, dbr.query("select '{true, false}'::BOOL[]").index(0).getArray(0, Boolean[].class));
    }

    @Test
    public void shouldConvertUUID() {
        UUID uuid = UUID.randomUUID();
        PgRow row = (PgRow) dbr.query("select $1::UUID as uuid", singletonList(uuid)).index(0);
        assertEquals(uuid, row.get("uuid"));
    }
}
