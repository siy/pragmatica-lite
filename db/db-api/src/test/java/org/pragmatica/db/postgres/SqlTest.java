package org.pragmatica.db.postgres;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pragmatica.uri.IRI;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.db.postgres.Sql.DDL;
import static org.pragmatica.db.postgres.Sql.QRY;
import static org.pragmatica.lang.Option.none;

class SqlTest {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:15-alpine"
    );

    static DbEnv dbEnv;

    @SuppressWarnings("deprecation")
    @BeforeAll
    static void start() {
        postgres.start();
        dbEnv = DbEnv.with(new DbEnvConfig(
            IRI.fromString("postgres://localhost:" + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName()),
            postgres.getUsername(),
            postgres.getPassword(),
            -1,
            10,
            false,
            none(),
            none()
        ));

        DDL."CREATE TABLE IF NOT EXISTS test (id INT PRIMARY KEY, value VARCHAR(255) NOT NULL)"
            .in(dbEnv)
            .await()
            .unwrap();

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

    @AfterAll
    static void stop() {
        postgres.stop();
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
    void recordInstancesCanBeRetrieved() {
        QRY."SELECT * FROM test"
            .in(dbEnv)
            .get()
            .await()
            .onFailure(System.out::println)
            .onFailureRun(Assertions::fail)
            .onSuccess(ra -> assertEquals(10, ra.size()))
            .flatMap(ra -> ra.as(TestRecord.template()))
            .onFailureRun(Assertions::fail)
            .map(stream -> stream.peek(System.out::println))
            .onSuccess(stream -> {
                // Do not use count() collector here, it will optimize out the peek() call above
                var count = new AtomicInteger(0);
                stream.forEach(_ -> count.incrementAndGet());
                assertEquals(10, count.get());
            });
    }

    @Test
    void recordsCanBeInserted() {
        var newInstance = TestRecordTemplate.builder()
                                            .id(15)
                                            .value("fifteen");

        var columns = TestRecord.template().fieldNames();
        var values = TestRecord.template().fieldValues(newInstance);

        QRY."INSERT INTO test (\{columns}) VALUES (\{values})"
            .in(dbEnv)
            .get()
            .await()
            .onFailure(System.out::println)
            .onFailureRun(Assertions::fail);

        QRY."SELECT * FROM test WHERE id = \{newInstance.id()}"
            .in(dbEnv)
            .get()
            .await()
            .onFailureRun(Assertions::fail)
            .flatMap(ra -> ra.as(TestRecord.template())
                             .map(Stream::toList))
            .onSuccess(list -> assertEquals(1, list.size()))
            .onSuccess(list -> assertEquals(newInstance, list.getFirst()))
            .onSuccess(list -> list.forEach(System.out::println));
    }

    @Test
    void miniPerfTest() throws InterruptedException {
        int iterationsCount = 100_000;

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

        System.out.println("Executed "
                           + iterationsCount
                           + " queries in "
                           + (end - start)
                             / 1_000_000
                           + " ms ("
                           + freq
                           + " RPS), "
                           + successes.get()
                           + " succeeded, "
                           + failures.get()
                           + " failed");
    }
}