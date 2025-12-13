/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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
 *
 */

package org.pragmatica.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

class JdbcOperationsTest {

    private DataSource dataSource;
    private JdbcOperations jdbc;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = createH2DataSource();
        jdbc = JdbcOperations.create(dataSource);
        initSchema();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS users");
        }
    }

    @Test
    void queryOne_returnsResult() {
        insert("Alice", "alice@test.com");

        jdbc.queryOne("SELECT name FROM users WHERE email = ?",
                rs -> rs.getString("name"),
                "alice@test.com")
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(name -> assertThat(name).isEqualTo("Alice"));
    }

    @Test
    void queryOne_failsWhenNoResult() {
        jdbc.queryOne("SELECT name FROM users WHERE email = ?",
                rs -> rs.getString("name"),
                "nonexistent@test.com")
            .await()
            .onSuccess(_ -> fail("Expected failure"));
    }

    @Test
    void queryOptional_returnsSome() {
        insert("Bob", "bob@test.com");

        jdbc.queryOptional("SELECT name FROM users WHERE email = ?",
                rs -> rs.getString("name"),
                "bob@test.com")
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(opt -> {
                assertThat(opt.isPresent()).isTrue();
                assertThat(opt.or("")).isEqualTo("Bob");
            });
    }

    @Test
    void queryOptional_returnsNone() {
        jdbc.queryOptional("SELECT name FROM users WHERE email = ?",
                rs -> rs.getString("name"),
                "nonexistent@test.com")
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(opt -> assertThat(opt.isEmpty()).isTrue());
    }

    @Test
    void queryList_returnsAllRows() {
        insert("Alice", "alice@test.com");
        insert("Bob", "bob@test.com");
        insert("Charlie", "charlie@test.com");

        jdbc.queryList("SELECT name FROM users ORDER BY name",
                rs -> rs.getString("name"))
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(names -> assertThat(names).containsExactly("Alice", "Bob", "Charlie"));
    }

    @Test
    void queryList_returnsEmptyList() {
        jdbc.queryList("SELECT name FROM users", rs -> rs.getString("name"))
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(names -> assertThat(names).isEmpty());
    }

    @Test
    void update_returnsAffectedRows() {
        insert("Alice", "alice@test.com");

        jdbc.update("UPDATE users SET name = ? WHERE email = ?", "Alicia", "alice@test.com")
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(count -> assertThat(count).isEqualTo(1));
    }

    @Test
    void update_insertsNew() {
        jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "Dave", "dave@test.com")
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(count -> assertThat(count).isEqualTo(1));

        jdbc.queryOne("SELECT name FROM users WHERE email = ?",
                rs -> rs.getString("name"),
                "dave@test.com")
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(name -> assertThat(name).isEqualTo("Dave"));
    }

    @Test
    void batch_executesMultiple() {
        var params = List.of(
            new Object[]{"Alice", "alice@test.com"},
            new Object[]{"Bob", "bob@test.com"},
            new Object[]{"Charlie", "charlie@test.com"}
        );

        jdbc.batch("INSERT INTO users (name, email) VALUES (?, ?)", params)
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(counts -> {
                assertThat(counts).hasSize(3);
                assertThat(counts).containsOnly(1);
            });

        jdbc.queryList("SELECT name FROM users ORDER BY name", rs -> rs.getString("name"))
            .await()
            .onSuccess(names -> assertThat(names).containsExactly("Alice", "Bob", "Charlie"));
    }

    @Test
    void queryFailed_onInvalidSql() {
        jdbc.queryOne("INVALID SQL", rs -> rs.getString("x"))
            .await()
            .onSuccess(_ -> fail("Expected failure"))
            .onFailure(cause -> assertInstanceOf(JdbcError.QueryFailed.class, cause));
    }

    private void insert(String name, String email) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("INSERT INTO users (name, email) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initSchema() throws SQLException {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE
                )
                """);
        }
    }

    private static DataSource createH2DataSource() {
        var ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
}
