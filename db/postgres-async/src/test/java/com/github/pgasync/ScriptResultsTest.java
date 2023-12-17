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

import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.SqlException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for results of script execution.
 *
 * @author Marat Gainullin
 */
@Tag("Slow")
public class ScriptResultsTest {

    @ClassRule
    public static DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @BeforeClass
    public static void create() {
        drop();
        dbr.query("CREATE TABLE SCRIPT_TEST(ID INT8)");
    }

    @AfterClass
    public static void drop() {
        dbr.query("DROP TABLE IF EXISTS SCRIPT_TEST");
    }

    @Test
    public void shouldReturnMultipleResultSets() {
        var results = new ArrayList<>(dbr.script(
            "INSERT INTO SCRIPT_TEST (ID) VALUES (1),(2);" +
            "SELECT SCRIPT_TEST.ID first_id FROM SCRIPT_TEST WHERE ID <= 2 ORDER BY ID;" +
            "INSERT INTO SCRIPT_TEST (ID) VALUES (3),(4),(5);" +
            "SELECT SCRIPT_TEST.ID second_id FROM SCRIPT_TEST WHERE ID > 2 ORDER BY ID;"
        ));
        assertEquals(4, results.size());

        var firstInsertResult = results.get(0);
        assertEquals(2, firstInsertResult.affectedRows());
        assertEquals(0, firstInsertResult.size());
        assertTrue(firstInsertResult.orderedColumns().isEmpty());
        assertTrue(firstInsertResult.columnsByName().isEmpty());

        var firstSelectResult = results.get(1);
        assertEquals(0, firstSelectResult.affectedRows());
        assertEquals(2, firstSelectResult.size());
        assertEquals(1, firstSelectResult.index(0).getLong("first_id").intValue());
        assertEquals(2, firstSelectResult.index(1).getLong("first_id").intValue());
        assertEquals(1, firstSelectResult.orderedColumns().size());
        assertEquals("first_id", firstSelectResult.orderedColumns().get(0).name());
        assertEquals(1, firstSelectResult.columnsByName().size());
        assertTrue(firstSelectResult.columnsByName().containsKey("first_id"));

        var secondInsertResult = results.get(2);
        assertEquals(3, secondInsertResult.affectedRows());
        assertEquals(0, secondInsertResult.size());
        assertTrue(secondInsertResult.orderedColumns().isEmpty());
        assertTrue(secondInsertResult.columnsByName().isEmpty());

        var secondSelectResult = results.get(3);
        assertEquals(0, secondSelectResult.affectedRows());
        assertEquals(3, secondSelectResult.size());
        assertEquals(3, secondSelectResult.index(0).getLong("second_id").intValue());
        assertEquals(4, secondSelectResult.index(1).getLong("second_id").intValue());
        assertEquals(5, secondSelectResult.index(2).getLong("second_id").intValue());
        assertEquals(1, secondSelectResult.orderedColumns().size());
        assertEquals("second_id", secondSelectResult.orderedColumns().get(0).name());
        assertEquals(1, secondSelectResult.columnsByName().size());
        assertTrue(secondSelectResult.columnsByName().containsKey("second_id"));
    }

    @Test(expected = SqlException.class)
    public void shouldInvokeErrorHandlerOnError() throws Exception {
        try {
            dbr.script("SELECT * FROM not_there");
        } catch (Exception ex) {
            DatabaseRule.ifCause(ex, sqlException -> {
                throw sqlException;
            }, () -> {
                throw ex;
            });
        }
    }
}
