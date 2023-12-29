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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for parameters binding.
 *
 * @author Antti Laisi
 */
@Tag("Slow")
public class ParametersBindingTest {

    @ClassRule
    public static final DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @BeforeClass
    public static void create() {
        drop();
        dbr.query("CREATE TABLE PS_TEST("
                + "LONG INT8,INT INT4,SHORT INT2, BYTE INT2,"
                + "CHAR CHAR(1), STRING VARCHAR(255), CLOB TEXT,"
                + "TIME TIME, DATE DATE, TS TIMESTAMP, TSWTZ TIMESTAMP,"
                + "BYTEA BYTEA, BOOLEAN BOOLEAN)");
    }

    @AfterClass
    public static void drop() {
        dbr.query("DROP TABLE IF EXISTS PS_TEST");
    }

    @Test
    public void shouldBindLong() {
        dbr.query("INSERT INTO PS_TEST(LONG) VALUES ($1)", List.of(Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, dbr.query("SELECT LONG FROM PS_TEST WHERE LONG = $1", List.of(Long.MIN_VALUE))
                                        .index(0).getLong(0).longValue());
    }

    @Test
    public void shouldBindInt() {
        dbr.query("INSERT INTO PS_TEST(INT) VALUES ($1)", List.of(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, dbr.query("SELECT INT FROM PS_TEST WHERE INT = $1", List.of(Integer.MAX_VALUE))
                                           .index(0).getInt(0).intValue());
    }

    @Test
    public void shouldBindShort() {
        dbr.query("INSERT INTO PS_TEST(SHORT) VALUES ($1)", List.of(Short.MIN_VALUE));
        assertEquals(Short.MIN_VALUE, dbr.query("SELECT SHORT FROM PS_TEST WHERE SHORT = $1", List.of(Short.MIN_VALUE))
                                         .index(0).getLong(0).shortValue());
    }

    @Test
    public void shouldBindByte() {
        dbr.query("INSERT INTO PS_TEST(BYTE) VALUES ($1)", List.of((byte) 0x41));
        assertEquals((byte) 0x41, dbr.query("SELECT BYTE FROM PS_TEST WHERE BYTE = $1", List.of(0x41)).index(0).getByte(0)
                                     .byteValue());
    }

    @Test
    public void shouldBindChar() {
        dbr.query("INSERT INTO PS_TEST(CHAR) VALUES ($1)", List.of('€'));
        assertEquals('€', dbr.query("SELECT CHAR FROM PS_TEST WHERE CHAR = $1", List.of('€')).index(0).getChar(0)
                             .charValue());
    }

    @Test
    public void shouldBindString() {
        dbr.query("INSERT INTO PS_TEST(STRING) VALUES ($1)", List.of("val"));
        assertEquals("val",
                dbr.query("SELECT STRING FROM PS_TEST WHERE STRING = $1", List.of("val")).index(0).getString(0));
    }

    @Test
    public void shouldBindClob() {
        var text = getClass().toString().repeat(1000);
        dbr.query("INSERT INTO PS_TEST(CLOB) VALUES ($1)", List.of(text));
        assertEquals(text, dbr.query("SELECT CLOB FROM PS_TEST WHERE CLOB = $1", List.of(text)).index(0).getString(0));
    }

    @Test
    public void shouldBindTime() {
        var time = LocalTime.parse("16:47:59.897");
        dbr.query("INSERT INTO PS_TEST(TIME) VALUES ($1)", List.of(time));
        assertEquals(time, dbr.query("SELECT TIME FROM PS_TEST WHERE TIME = $1", List.of(time)).index(0).getLocalTime(0));
    }

    @Test
    public void shouldBindInstant() {
        var inst = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        dbr.query("INSERT INTO PS_TEST(TSWTZ) VALUES ($1)", singletonList(inst));
        assertEquals(inst, dbr.query("SELECT TSWTZ FROM PS_TEST WHERE TSWTZ = $1", List.of(inst)).index(0).getInstant(0));
    }

    @Test
    public void shouldBindDate() {
        var date = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dbr.query("INSERT INTO PS_TEST(TS) VALUES ($1)", List.of(date));
        assertEquals(date, dbr.query("SELECT TS FROM PS_TEST WHERE TS = $1", List.of(date)).index(0).getLocalDateTime(0));
    }

    @Test
    public void shouldBindLocalDate() {
        var localDate = LocalDate.parse("2014-01-19");
        dbr.query("INSERT INTO PS_TEST(DATE) VALUES ($1)", List.of(localDate));
        assertEquals(localDate, dbr.query("SELECT DATE FROM PS_TEST WHERE DATE = $1", List.of(localDate)).index(0).getLocalDate(0));
    }

    @Test
    public void shouldBindBytes() {
        var b = "blob content".getBytes(StandardCharsets.UTF_8); // UTF-8 is hard coded here only because the ascii compatible data
        dbr.query("INSERT INTO PS_TEST(BYTEA) VALUES ($1)", List.of(b));
        assertArrayEquals(b, dbr.query("SELECT BYTEA FROM PS_TEST WHERE BYTEA = $1", List.of(b)).index(0).getBytes(0));
    }

    @Test
    public void shouldBindBoolean() {
        dbr.query("INSERT INTO PS_TEST(BOOLEAN) VALUES ($1)", singletonList(true));
        assertTrue((Boolean) ((PgRow) dbr.query("SELECT BOOLEAN FROM PS_TEST WHERE BOOLEAN = $1",
                List.of(true)).index(0)).get("boolean"));

    }
}
