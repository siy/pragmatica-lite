package org.pragmatica.db.postgres;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pragmatica.uri.IRI;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

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
            IRI.fromString(STR."postgres://localhost:\{postgres.getMappedPort(5432)}/\{postgres.getDatabaseName()}"),
            postgres.getUsername(),
            postgres.getPassword(),
            -1,
            10,
            none(),
            none()
        ));

        dbEnv.execute(DDL."CREATE TABLE IF NOT EXISTS test (id INT, value VARCHAR(255))")
             .await().unwrap();

        var ids = List.of(1, 2, 3).iterator();
        var values = List.of("one", "two", "three").iterator();

        dbEnv.execute(QRY."""
        INSERT INTO test (id, value) VALUES
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()}),
        (\{ids.next()}, \{values.next()})
        """).await().unwrap();
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
             .onSuccess(rs -> {
                 assertEquals(1, rs.size());
                 assertEquals(3, rs.index(0).getInt("id"));
                 assertEquals("three", rs.index(0).getString("value"));
             });
    }
}