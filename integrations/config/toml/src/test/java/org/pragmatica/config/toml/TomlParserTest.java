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
 */

package org.pragmatica.config.toml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TomlParserTest {

    @Nested
    class BasicParsing {

        @Test
        void parseEmptyContent() {
            TomlParser.parse("")
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(TomlDocument.EMPTY.sections(), doc.sections());
                });
        }

        @Test
        void parseNullContent() {
            TomlParser.parse(null)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(TomlDocument.EMPTY.sections(), doc.sections());
                });
        }

        @Test
        void parseCommentsOnly() {
            var content = """
                # This is a comment
                # Another comment
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertTrue(doc.keys("").isEmpty()));
        }
    }

    @Nested
    class StringValues {

        @Test
        void parseQuotedString() {
            var content = """
                name = "hello world"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.getString("", "name").isPresent());
                    assertEquals("hello world", doc.getString("", "name").unwrap());
                });
        }

        @Test
        void parseUnquotedString() {
            var content = """
                name = simple
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("simple", doc.getString("", "name").unwrap());
                });
        }

        @Test
        void parseEscapedString() {
            var content = """
                path = "C:\\\\Users\\\\test"
                quote = "He said \\"hello\\""
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("C:\\Users\\test", doc.getString("", "path").unwrap());
                    assertEquals("He said \"hello\"", doc.getString("", "quote").unwrap());
                });
        }

        @Test
        void parseStringWithNewlines() {
            var content = """
                text = "line1\\nline2"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("line1\nline2", doc.getString("", "text").unwrap());
                });
        }
    }

    @Nested
    class NumericValues {

        @Test
        void parsePositiveInteger() {
            var content = """
                port = 8080
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.getInt("", "port").isPresent());
                    assertEquals(8080, doc.getInt("", "port").unwrap());
                });
        }

        @Test
        void parseNegativeInteger() {
            var content = """
                offset = -100
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(-100, doc.getInt("", "offset").unwrap());
                });
        }

        @Test
        void parseLongValue() {
            var content = """
                bignum = 9999999999
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.getLong("", "bignum").isPresent());
                    assertEquals(9999999999L, doc.getLong("", "bignum").unwrap());
                });
        }

        @Test
        void parseFloatValue() {
            var content = """
                pi = 3.14159
                negative = -2.5
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.getDouble("", "pi").isPresent());
                    assertEquals(3.14159, doc.getDouble("", "pi").unwrap(), 0.00001);
                    assertEquals(-2.5, doc.getDouble("", "negative").unwrap(), 0.00001);
                });
        }

        @Test
        void parseScientificNotation() {
            var content = """
                large = 1.5e10
                small = 2.5e-3
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(1.5e10, doc.getDouble("", "large").unwrap(), 1e6);
                    assertEquals(2.5e-3, doc.getDouble("", "small").unwrap(), 1e-6);
                });
        }

        @Test
        void getDoubleFromInteger() {
            var content = """
                count = 42
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    // Should be able to get integer as double
                    assertEquals(42.0, doc.getDouble("", "count").unwrap(), 0.00001);
                });
        }
    }

    @Nested
    class BooleanValues {

        @Test
        void parseTrue() {
            var content = """
                enabled = true
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.getBoolean("", "enabled").isPresent());
                    assertTrue(doc.getBoolean("", "enabled").unwrap());
                });
        }

        @Test
        void parseFalse() {
            var content = """
                disabled = false
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertFalse(doc.getBoolean("", "disabled").unwrap());
                });
        }
    }

    @Nested
    class ArrayValues {

        @Test
        void parseEmptyArray() {
            var content = """
                items = []
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.getStringList("", "items").isPresent());
                    assertTrue(doc.getStringList("", "items").unwrap().isEmpty());
                });
        }

        @Test
        void parseStringArray() {
            var content = """
                tags = ["alpha", "beta", "gamma"]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var tags = doc.getStringList("", "tags").unwrap();
                    assertEquals(3, tags.size());
                    assertEquals(List.of("alpha", "beta", "gamma"), tags);
                });
        }

        @Test
        void parseIntegerArray() {
            var content = """
                ports = [80, 443, 8080]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var ports = doc.getStringList("", "ports").unwrap();
                    assertEquals(List.of("80", "443", "8080"), ports);
                });
        }

        @Test
        void parseMixedArray() {
            var content = """
                mixed = ["text", 123, true]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var mixed = doc.getStringList("", "mixed").unwrap();
                    assertEquals(List.of("text", "123", "true"), mixed);
                });
        }
    }

    @Nested
    class Sections {

        @Test
        void parseSimpleSection() {
            var content = """
                [database]
                host = "localhost"
                port = 5432
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("localhost", doc.getString("database", "host").unwrap());
                    assertEquals(5432, doc.getInt("database", "port").unwrap());
                });
        }

        @Test
        void parseNestedSection() {
            var content = """
                [server.http]
                port = 8080

                [server.https]
                port = 8443
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(8080, doc.getInt("server.http", "port").unwrap());
                    assertEquals(8443, doc.getInt("server.https", "port").unwrap());
                });
        }

        @Test
        void parseMultipleSections() {
            var content = """
                title = "My App"

                [database]
                host = "db.example.com"

                [cache]
                enabled = true
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("My App", doc.getString("", "title").unwrap());
                    assertEquals("db.example.com", doc.getString("database", "host").unwrap());
                    assertTrue(doc.getBoolean("cache", "enabled").unwrap());
                });
        }
    }

    @Nested
    class Comments {

        @Test
        void parseInlineComment() {
            var content = """
                port = 8080 # default port
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(8080, doc.getInt("", "port").unwrap());
                });
        }

        @Test
        void parseCommentBetweenProperties() {
            var content = """
                host = "localhost"
                # This is a comment
                port = 8080
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("localhost", doc.getString("", "host").unwrap());
                    assertEquals(8080, doc.getInt("", "port").unwrap());
                });
        }
    }

    @Nested
    class DocumentMethods {

        @Test
        void hasSection() {
            var content = """
                [database]
                host = "localhost"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.hasSection("database"));
                    assertFalse(doc.hasSection("cache"));
                });
        }

        @Test
        void hasKey() {
            var content = """
                [database]
                host = "localhost"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.hasKey("database", "host"));
                    assertFalse(doc.hasKey("database", "port"));
                });
        }

        @Test
        void sectionNames() {
            var content = """
                title = "test"

                [server]
                port = 8080

                [database]
                host = "localhost"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var names = doc.sectionNames();
                    assertEquals(3, names.size());
                    assertTrue(names.contains(""));
                    assertTrue(names.contains("server"));
                    assertTrue(names.contains("database"));
                });
        }

        @Test
        void keys() {
            var content = """
                [database]
                host = "localhost"
                port = 5432
                name = "mydb"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var keys = doc.keys("database");
                    assertEquals(3, keys.size());
                    assertTrue(keys.contains("host"));
                    assertTrue(keys.contains("port"));
                    assertTrue(keys.contains("name"));
                });
        }

        @Test
        void getSection() {
            var content = """
                [database]
                host = "localhost"
                port = 5432
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var section = doc.getSection("database");
                    assertEquals("localhost", section.get("host"));
                    assertEquals("5432", section.get("port"));
                });
        }

        @Test
        void withMethod() {
            var content = """
                [database]
                host = "localhost"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var updated = doc.with("database", "port", 5432);

                    // Original unchanged
                    assertFalse(doc.hasKey("database", "port"));

                    // New document has the value
                    assertEquals(5432, updated.getInt("database", "port").unwrap());
                    assertEquals("localhost", updated.getString("database", "host").unwrap());
                });
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void invalidSyntax() {
            var content = """
                this is not valid toml
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.SyntaxError.class, error);
                    assertTrue(error.message().contains("line 1"));
                });
        }

        @Test
        void unterminatedString() {
            var content = """
                name = "unterminated
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.UnterminatedString.class, error);
                });
        }

        @Test
        void unterminatedArray() {
            var content = """
                items = ["a", "b"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.UnterminatedArray.class, error);
                });
        }
    }

    @Nested
    class FileOperations {

        @TempDir
        Path tempDir;

        @Test
        void parseValidFile() throws IOException {
            var tomlFile = tempDir.resolve("config.toml");
            Files.writeString(tomlFile, """
                title = "Test App"

                [server]
                port = 8080
                """);

            TomlParser.parseFile(tomlFile)
                .onFailure(e -> fail("Should not fail: " + e.message()))
                .onSuccess(doc -> {
                    assertEquals("Test App", doc.getString("", "title").unwrap());
                    assertEquals(8080, doc.getInt("server", "port").unwrap());
                });
        }

        @Test
        void parseNonExistentFile() {
            var nonExistent = tempDir.resolve("does-not-exist.toml");

            TomlParser.parseFile(nonExistent)
                .onSuccess(_ -> fail("Should fail for non-existent file"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.FileReadFailed.class, error);
                    assertTrue(error.message().contains("does-not-exist.toml"));
                });
        }
    }

    @Nested
    class RealWorldExample {

        @Test
        void parseCompleteConfig() {
            var content = """
                # Application configuration
                title = "My Application"
                version = "1.0.0"
                debug = false

                [server]
                host = "0.0.0.0"
                port = 8080
                workers = 4

                [database]
                host = "localhost"
                port = 5432
                name = "myapp"
                pool_size = 10

                [features]
                auth = true
                caching = true
                logging = false

                [tags]
                environments = ["dev", "staging", "prod"]
                """;

            TomlParser.parse(content)
                .onFailure(e -> fail("Should not fail: " + e.message()))
                .onSuccess(doc -> {
                    // Root properties
                    assertEquals("My Application", doc.getString("", "title").unwrap());
                    assertEquals("1.0.0", doc.getString("", "version").unwrap());
                    assertFalse(doc.getBoolean("", "debug").unwrap());

                    // Server section
                    assertEquals("0.0.0.0", doc.getString("server", "host").unwrap());
                    assertEquals(8080, doc.getInt("server", "port").unwrap());
                    assertEquals(4, doc.getInt("server", "workers").unwrap());

                    // Database section
                    assertEquals("localhost", doc.getString("database", "host").unwrap());
                    assertEquals(5432, doc.getInt("database", "port").unwrap());
                    assertEquals("myapp", doc.getString("database", "name").unwrap());
                    assertEquals(10, doc.getInt("database", "pool_size").unwrap());

                    // Features section
                    assertTrue(doc.getBoolean("features", "auth").unwrap());
                    assertTrue(doc.getBoolean("features", "caching").unwrap());
                    assertFalse(doc.getBoolean("features", "logging").unwrap());

                    // Tags section
                    var envs = doc.getStringList("tags", "environments").unwrap();
                    assertEquals(List.of("dev", "staging", "prod"), envs);
                });
        }
    }
}
