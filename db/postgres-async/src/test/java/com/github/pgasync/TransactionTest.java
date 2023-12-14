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

import com.github.pgasync.net.Connection;
import com.github.pgasync.net.ResultSet;
import com.github.pgasync.net.SqlException;
import com.github.pgasync.net.Transaction;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.io.Timeout;
import org.pragmatica.lang.utils.Causes;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.pragmatica.lang.io.Timeout.timeout;

/**
 * Tests for BEGIN/COMMIT/ROLLBACK.
 *
 * @author Antti Laisi
 */
@Tag("Slow")
public class TransactionTest {

    @ClassRule
    public static DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @BeforeClass
    public static void create() {
        drop();
        dbr.query("CREATE TABLE TX_TEST(ID INT8 PRIMARY KEY)");
    }

    @AfterClass
    public static void drop() {
        dbr.query("DROP TABLE IF EXISTS TX_TEST");
    }

    private <T> Result<T> withTransaction(Fn1<Promise<T>, Transaction> function) {
        return dbr.pool()
                  .connection()
                  .flatMap(connection -> connection.begin()
                                                   .flatMap(function)
                                                   .onResultDo(_ -> connection.close()))
                  .await(timeout(5).seconds());
    }

    @Test
    public void shouldCommitSelectInTransaction() {
        withTransaction(transaction ->
                            transaction.completeQuery("SELECT 1")
                                       .onSuccessDo(result -> {
                                           assertEquals(1L, result.index(0).getLong(0).longValue());
                                           return transaction.commit();
                                       }));
    }

    @Test
    public void shouldCommitInsertInTransaction() throws Exception {
        withTransaction(transaction ->
                            transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(10)")
                                       .onSuccessDo(result -> {
                                           assertEquals(1, result.affectedRows());
                                           return transaction.commit();
                                       }));

        assertEquals(10L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 10")
            .unwrap().index(0).getLong(0).longValue());
    }

    @Test
    public void shouldCommitParameterizedInsertInTransaction() {
        // Ref: https://github.com/alaisi/postgres-async-driver/issues/34
        withTransaction(transaction ->
                            transaction.completeQuery("INSERT INTO TX_TEST (ID) VALUES ($1) RETURNING ID",
                                                      35)
                                       .map(rs -> rs.index(0))
                                       .map(row -> row.getLong(0))
                                       .onResultDo(_ -> transaction.commit()))
            .onSuccess(id -> assertEquals(35L, (long) id))
            .onFailureDo(Assert::fail);
    }

    @Test
    public void shouldRollbackTransaction() {
        withTransaction(transaction ->
                            transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(9)")
                                       .flatMap(result -> {
                                           assertEquals(1, result.affectedRows());
                                           return transaction.rollback();
                                       }));

        assertEquals(0L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 9").unwrap().size());
    }


    @Test(expected = SqlException.class)
    public void shouldRollbackTransactionOnBackendError() {
        // Insert duplicate key
        withTransaction(transaction ->
                            transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(11)")
                                       .flatMap(result -> {
                                           assertEquals(1, result.affectedRows());
                                           return transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(11)");
                                       }))
            .onSuccessDo(() -> Assert.fail("Should not succeed"))
            .onFailureDo(() -> assertEquals(0, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 11").unwrap().size()));
    }

    @Test
    public void shouldRollbackTransactionAfterBackendError() {
        withTransaction(transaction ->
                            transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(22)")
                                       .flatMap(result -> {
                                           assertEquals(1, result.affectedRows());
                                           return transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(22)")
                                                             .flatMap(_ -> Promise.<ResultSet>promise()
                                                                                  .failure(Causes.cause("The transaction should fail")));
                                       }));

        assertEquals(0, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 22").unwrap().size());
    }

    @Test
    public void shouldSupportNestedTransactions() {
        withTransaction(transaction ->
                            transaction.begin()
                                       .flatMap(nested ->
                                                    nested.completeQuery("INSERT INTO TX_TEST(ID) VALUES(19)")
                                                          .flatMap(result -> {
                                                              assertEquals(1, result.affectedRows());
                                                              return nested.commit();
                                                          })
                                                          .flatMap(_ -> transaction.commit())));

        assertEquals(1L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 19").unwrap().size());
    }

    @Test
    public void shouldRollbackNestedTransaction() {
        withTransaction(transaction ->
                            transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(24)")
                                       .flatMap(result -> {
                                           assertEquals(1, result.affectedRows());
                                           return transaction.begin()
                                                             .flatMap(nested ->
                                                                          nested.completeQuery(
                                                                                    "INSERT INTO TX_TEST(ID) VALUES(23)")
                                                                                .flatMap(res2 -> {
                                                                                    assertEquals(1,
                                                                                                 res2.affectedRows());
                                                                                    return nested.rollback();
                                                                                }));
                                       }));
        assertEquals(1L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 24").unwrap().size());
        assertEquals(0L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 23").unwrap().size());
    }

    @Test
    public void shouldRollbackNestedTransactionOnBackendError() {
        withTransaction(transaction ->
                            transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(25)")
                                       .flatMap(result -> {
                                           assertEquals(1, result.affectedRows());
                                           return transaction.begin()
                                                             .flatMap(nested ->
                                                                          nested.completeQuery("INSERT INTO TX_TEST(ID) VALUES(26)")
                                                                                .onSuccess(res2 -> assertEquals(1, res2.affectedRows()))
                                                                                // try insert duplicate key
                                                                                .flatMap(v -> nested.completeQuery(
                                                                                    "INSERT INTO TX_TEST(ID) VALUES(26)"))
                                                                                .onResultDo(_ -> transaction.commit()));
                                       }));
        assertEquals(1L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 25").unwrap().size());
        assertEquals(0L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 26").unwrap().size());
    }
}
