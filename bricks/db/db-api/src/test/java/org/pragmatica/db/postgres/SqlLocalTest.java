package org.pragmatica.db.postgres;

import org.junit.jupiter.api.*;
import org.pragmatica.uri.IRI;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.db.postgres.Sql.DDL;
import static org.pragmatica.db.postgres.Sql.QRY;
import static org.pragmatica.lang.Option.none;

// This test uses local environment with Postgres running on localhost with following configuration:
// port: 5432
// user: test
// password: test123test321
// database: test
@Disabled
class SqlLocalTest {
    static DbEnv dbEnv;

    @SuppressWarnings("deprecation")
    @BeforeAll
    static void start() {
        dbEnv = DbEnv.with(new DbEnvConfig(
            IRI.fromString("postgres://localhost:5432/test"),
            "test",
            "test123test321",
            -1,
            10,
            false,
            none(),
            none()
        ));

        DDL."""
            DROP TABLE IF EXISTS test;
            CREATE TABLE test (id INT PRIMARY KEY, value VARCHAR(255));
            """.in(dbEnv).await().unwrap();

        var ids = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).iterator();
        var values = List.of("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine").iterator();

        QRY."""
        INSERT INTO test (id, value) VALUES
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()})
        """.in(dbEnv)
           .get()
           .await()
           .unwrap();
    }

    @SuppressWarnings("deprecation")
    @AfterAll
    static void stop() {
        DDL."DROP TABLE IF EXISTS test".in(dbEnv)
                                       .await()
                                       .unwrap();
        dbEnv.close()
             .await()
             .unwrap();
    }

    @Test
    public void simpleQuery() {
        dbEnv.execute(QRY."SELECT 1")
             .await()
             .onFailure(System.out::println)
             .onFailureRun(Assertions::fail)
             .onSuccess(System.out::println);
    }

    @Test
    public void valuesAreSelected() {
        var id = 3;
        dbEnv.execute(QRY."SELECT * FROM test WHERE id = \{id}")
             .await()
             .onFailure(System.out::println)
             .onFailureRun(Assertions::fail)
             .onSuccess(System.out::println)
             .map(ResultAccessor::resultSet)
             .onSuccess(rs -> {
                 assertEquals(1, rs.size());
                 assertEquals(3, rs.index(0).getInt("id"));
                 assertEquals("three", rs.index(0).getString("value"));
             });
    }

    @Test
    void alternativeSyntaxWorksForQueries() {
        var id = 3;

        QRY."SELECT * FROM test WHERE id = \{id}"
            .in(dbEnv)
            .get()
            .await()
            .onFailure(System.out::println)
            .onFailureRun(Assertions::fail)
            .onSuccess(System.out::println)
            .map(ResultAccessor::resultSet)
            .onSuccess(rs -> {
                assertEquals(1, rs.size());
                assertEquals(3, rs.index(0).getInt("id"));
                assertEquals("three", rs.index(0).getString("value"));
            });
    }

    @Test
    void miniPerfTest() throws InterruptedException {
        int iterationsCount = 2_000_000;

        var successes = new AtomicInteger(0);
        var failures = new AtomicInteger(0);

        var latch = new CountDownLatch(iterationsCount);

        var start = System.nanoTime();
        IntStream.range(0, iterationsCount)
                 .forEach(i -> QRY."SELECT * FROM test WHERE id = \{i % 10}".in(dbEnv)
                                                                            .get()
                                                                            .onSuccessRun(successes::incrementAndGet)
                                                                            .onFailureRun(successes::incrementAndGet)
                                                                            .onResult(_ -> latch.countDown()));
        latch.await();
        var end = System.nanoTime();

        var freq = (int) (iterationsCount / ((end - start) / 1_000_000_000.0));

        System.out.println(
            "Executed "
            + iterationsCount
            + " queries in "
            + (end - start) / 1_000_000
            + " ms\nRequests per second "
            + freq
            + "\nSuccessful requests "
            + successes.get()
            + "\nFailed requests "
            + failures.get()
            + " failed\");\n");
    }
}