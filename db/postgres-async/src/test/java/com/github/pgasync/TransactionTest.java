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

import com.github.pgasync.async.ThrowingPromise;
import com.github.pgasync.net.SqlException;
import com.github.pgasync.net.Transaction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.pragmatica.lang.Functions.Fn1;

import static org.junit.Assert.assertEquals;

/**
 * Tests for BEGIN/COMMIT/ROLLBACK.
 *
 * @author Antti Laisi
 */
@Tag("Slow")
public class TransactionTest {

    @ClassRule
    public static final DatabaseRule dbr = DatabaseRule.defaultConfiguration();

    @BeforeClass
    public static void create() {
        drop();
        dbr.query("CREATE TABLE TX_TEST(ID INT8 PRIMARY KEY)");
    }

    @AfterClass
    public static void drop() {
        dbr.query("DROP TABLE IF EXISTS TX_TEST");
    }

    private static <T> ThrowingPromise<T> withinTransaction(Fn1<ThrowingPromise<T>, Transaction> fn) {
        return dbr.pool()
                  .getConnection()
                  .flatMap(connection ->
                               connection.begin()
                                         .flatMap(fn)
                                         .fold(result -> connection.close()
                                                                   .fold(_ -> ThrowingPromise.resolved(result))));
    }

    @Test
    public void shouldCommitSelectInTransaction() {
        withinTransaction(transaction ->
                              transaction.completeQuery("SELECT 1")
                                         .onSuccess(rs -> assertEquals(1L, rs.index(0).getLong(0).longValue()))
                                         .flatMap(_ -> transaction.commit()))
            .await();
    }

    @Test
    public void shouldCommitInsertInTransaction() {
        withinTransaction(transaction ->
                              transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(10)")
                                         .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                         .flatMap(_ -> transaction.commit()))
            .await();

        assertEquals(10L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 10").index(0).getLong(0).longValue());
    }

    @Test
    public void shouldCommitParameterizedInsertInTransaction() {
        // Ref: https://github.com/alaisi/postgres-async-driver/issues/34
        long id = withinTransaction(transaction ->
                                        transaction.completeQuery("INSERT INTO TX_TEST (ID) VALUES ($1) RETURNING ID", 35)
                                                   .map(rs -> rs.index(0).getLong(0))
                                                   .flatMap(value -> transaction.commit().map(_ -> value)))
            .await();
        assertEquals(35L, id);
    }

    @Test
    public void shouldRollbackTransaction() {
        withinTransaction(transaction ->
                              transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(9)")
                                         .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                         .flatMap(_ -> transaction.rollback()))
            .await();

        assertEquals(0L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 9").size());
    }


    @Test(expected = SqlException.class)
    public void shouldRollbackTransactionOnBackendError() throws Exception {
        try {
            withinTransaction(transaction ->
                                  transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(11)")
                                             .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                             .flatMap(_ -> transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(11)")))
                .await();
        } catch (Exception ex) {
            DatabaseRule.ifCause(ex, sqlException -> {
                assertEquals(0, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 11").size());
                throw sqlException;
            }, () -> {
                throw ex;
            });
        }
    }

    @Test
    public void shouldRollbackTransactionAfterBackendError() {
        withinTransaction(transaction ->
                              transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(22)")
                                         .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                         .flatMap(_ -> transaction
                                             .completeQuery("INSERT INTO TX_TEST(ID) VALUES(22)")
                                             .fold(_ -> transaction.completeQuery("SELECT 1"))))
            .await();
        assertEquals(0, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 22").size());
    }

    @Test
    public void shouldSupportNestedTransactions() {
        withinTransaction(transaction ->
                              transaction.begin()
                                         .flatMap(nested ->
                                                      nested
                                                          .completeQuery("INSERT INTO TX_TEST(ID) VALUES(19)")
                                                          .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                                          .flatMap(_ -> nested.commit())
                                                          .flatMap(_ -> transaction.commit())))
            .await();
        assertEquals(1L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 19").size());
    }

    @Test
    public void shouldRollbackNestedTransaction() {
        withinTransaction(transaction ->
                              transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(24)")
                                         .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                         .flatMap(_ -> transaction.begin()
                                                                  .flatMap(nested ->
                                                                               nested
                                                                                   .completeQuery("INSERT INTO TX_TEST(ID) VALUES(23)")
                                                                                   .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                                                                   .flatMap(_ -> nested.rollback()))
                                                                  .flatMap(_ -> transaction.commit())))
            .await();
        assertEquals(1L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 24").size());
        assertEquals(0L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 23").size());
    }

    @Test
    public void shouldRollbackNestedTransactionOnBackendError() {
        withinTransaction(transaction ->
                              transaction.completeQuery("INSERT INTO TX_TEST(ID) VALUES(25)")
                                         .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                         .flatMap(_ -> transaction.begin()
                                                                  .flatMap(nested ->
                                                                               nested
                                                                                   .completeQuery("INSERT INTO TX_TEST(ID) VALUES(26)")
                                                                                   .onSuccess(rs -> assertEquals(1, rs.affectedRows()))
                                                                                   .flatMap(_ -> nested.completeQuery(
                                                                                       "INSERT INTO TX_TEST(ID) VALUES(26)"))
                                                                                   .fold(_ -> transaction.commit()))))
            .await();
        assertEquals(1L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 25").size());
        assertEquals(0L, dbr.query("SELECT ID FROM TX_TEST WHERE ID = 26").size());
    }
}
