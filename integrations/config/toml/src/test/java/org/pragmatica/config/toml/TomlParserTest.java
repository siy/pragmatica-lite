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
    class MultilineBasicStrings {

        @Test
        void parseSimpleMultilineBasicString() {
            var content = """
                text = \"\"\"
                Hello
                World
                \"\"\"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("Hello\nWorld\n", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineBasicStringTrimsLeadingNewline() {
            // Per TOML spec: newline immediately after opening """ is trimmed
            var content = "text = \"\"\"\nHello\"\"\"";

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("Hello", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineBasicStringWithEscapeSequences() {
            var content = """
                text = \"\"\"
                Tab:\\there
                Newline:\\nhere
                Quote:\\"here
                \"\"\"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("Tab:\there\nNewline:\nhere\nQuote:\"here\n", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineBasicStringWithLineEndingBackslash() {
            // Line-ending backslash trims newline and following whitespace
            var content = "text = \"\"\"\nThe quick brown \\\n    fox jumps over \\\n    the lazy dog.\"\"\"";

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("The quick brown fox jumps over the lazy dog.", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineBasicStringSameLine() {
            var content = """
                text = \"\"\"inline content\"\"\"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("inline content", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineBasicStringPreservesInternalNewlines() {
            var content = "text = \"\"\"\nLine 1\n\nLine 3\"\"\"";

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("Line 1\n\nLine 3", doc.getString("", "text").unwrap());
                });
        }
    }

    @Nested
    class MultilineLiteralStrings {

        @Test
        void parseSimpleMultilineLiteralString() {
            var content = """
                text = '''
                Hello
                World
                '''
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("Hello\nWorld\n", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineLiteralStringTrimsLeadingNewline() {
            var content = "text = '''\nHello'''";

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("Hello", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineLiteralStringNoEscapeProcessing() {
            // Literal strings don't process escape sequences
            var content = """
                text = '''
                C:\\Users\\test
                No \\n newline
                '''
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("C:\\Users\\test\nNo \\n newline\n", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineLiteralStringSameLine() {
            var content = """
                text = '''inline literal'''
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("inline literal", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void parseMultilineLiteralStringPreservesBackslashAtEndOfLine() {
            // Unlike basic strings, literal strings preserve backslash at end of line
            var content = "text = '''\nLine 1 \\\nLine 2'''";

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("Line 1 \\\nLine 2", doc.getString("", "text").unwrap());
                });
        }
    }

    @Nested
    class MultilineStringsInSections {

        @Test
        void parseMultilineStringInSection() {
            var content = """
                [config]
                description = \"\"\"
                This is a
                multi-line description
                \"\"\"
                enabled = true
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("This is a\nmulti-line description\n", doc.getString("config", "description").unwrap());
                    assertTrue(doc.getBoolean("config", "enabled").unwrap());
                });
        }

        @Test
        void parseMultipleSectionsWithMultilineStrings() {
            var content = """
                [section1]
                text = \"\"\"
                First
                \"\"\"

                [section2]
                text = '''
                Second
                '''
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("First\n", doc.getString("section1", "text").unwrap());
                    assertEquals("Second\n", doc.getString("section2", "text").unwrap());
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

        @Test
        void parseArrayWithInlineComment() {
            var content = """
                tags = ["a", "b", "c"] # list of tags
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var tags = doc.getStringList("", "tags").unwrap();
                    assertEquals(List.of("a", "b", "c"), tags);
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

        @Test
        void unterminatedMultilineString() {
            var content = """
                text = \"\"\"
                This is not closed
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.UnterminatedMultilineString.class, error);
                    assertTrue(error.message().contains("line 1"));
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
    class QuotedKeys {

        @Test
        void parseDoubleQuotedKey() {
            var content = """
                "quoted key" = "value"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("value", doc.getString("", "quoted key").unwrap());
                });
        }

        @Test
        void parseSingleQuotedKey() {
            var content = """
                'literal key' = "value"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("value", doc.getString("", "literal key").unwrap());
                });
        }

        @Test
        void parseKeyWithSpecialCharacters() {
            var content = """
                "key with spaces" = "works"
                "key-with-dashes" = "also works"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("works", doc.getString("", "key with spaces").unwrap());
                    assertEquals("also works", doc.getString("", "key-with-dashes").unwrap());
                });
        }
    }

    @Nested
    class DottedKeys {

        @Test
        void parseDottedKeyCreatesImplicitSection() {
            var content = """
                server.host = "localhost"
                server.port = 8080
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("localhost", doc.getString("server", "host").unwrap());
                    assertEquals(8080, doc.getInt("server", "port").unwrap());
                });
        }

        @Test
        void parseDottedKeyWithMultipleLevels() {
            var content = """
                database.connection.host = "localhost"
                database.connection.port = 5432
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("localhost", doc.getString("database.connection", "host").unwrap());
                    assertEquals(5432, doc.getInt("database.connection", "port").unwrap());
                });
        }

        @Test
        void parseDottedKeyInSection() {
            var content = """
                [app]
                server.host = "localhost"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("localhost", doc.getString("app.server", "host").unwrap());
                });
        }
    }

    @Nested
    class DuplicateKeyDetection {

        @Test
        void detectsDuplicateKey() {
            var content = """
                name = "first"
                name = "second"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for duplicate key"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.DuplicateKey.class, error);
                    assertTrue(error.message().contains("name"));
                });
        }

        @Test
        void detectsDuplicateKeyInSection() {
            var content = """
                [database]
                host = "localhost"
                host = "127.0.0.1"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for duplicate key"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.DuplicateKey.class, error);
                });
        }
    }

    @Nested
    class SingleQuoteLiteralStrings {

        @Test
        void parseSingleQuoteLiteralString() {
            var content = """
                path = 'C:\\Users\\test'
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    // No escape processing - backslashes preserved
                    assertEquals("C:\\Users\\test", doc.getString("", "path").unwrap());
                });
        }

        @Test
        void parseSingleQuoteWithNoEscapes() {
            var content = """
                text = 'Hello\\nWorld'
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    // Literal - no escape processing
                    assertEquals("Hello\\nWorld", doc.getString("", "text").unwrap());
                });
        }
    }

    @Nested
    class HyphenatedSections {

        @Test
        void parseHyphenatedSectionName() {
            var content = """
                [my-section]
                key = "value"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("value", doc.getString("my-section", "key").unwrap());
                });
        }

        @Test
        void parseNestedHyphenatedSection() {
            var content = """
                [my-app.sub-section]
                key = "value"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("value", doc.getString("my-app.sub-section", "key").unwrap());
                });
        }
    }

    @Nested
    class MultilineArrays {

        @Test
        void parseMultilineArray() {
            var content = """
                items = [
                    "first",
                    "second",
                    "third"
                ]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var items = doc.getStringList("", "items").unwrap();
                    assertEquals(List.of("first", "second", "third"), items);
                });
        }

        @Test
        void parseMultilineArrayWithTrailingComma() {
            var content = """
                items = [
                    "first",
                    "second",
                ]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var items = doc.getStringList("", "items").unwrap();
                    assertEquals(2, items.size());
                });
        }

        @Test
        void parseMultilineNestedArray() {
            var content = """
                matrix = [
                    [1, 2, 3],
                    [4, 5, 6]
                ]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.hasKey("", "matrix"));
                });
        }
    }

    @Nested
    class UnicodeEscapes {

        @Test
        void parseUnicode4Digit() {
            // Greek small letter alpha
            var content = "symbol = \"\\u03B1\"";

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("\u03B1", doc.getString("", "symbol").unwrap());
                });
        }

        @Test
        void parseUnicode8Digit() {
            // Emoji (grinning face)
            var content = "emoji = \"\\U0001F600\"";

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("\uD83D\uDE00", doc.getString("", "emoji").unwrap());
                });
        }

        @Test
        void parseBackspaceAndFormFeed() {
            var content = """
                bs = "before\\bafter"
                ff = "before\\fafter"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("before\bafter", doc.getString("", "bs").unwrap());
                    assertEquals("before\fafter", doc.getString("", "ff").unwrap());
                });
        }
    }

    @Nested
    class CommentStripping {

        @Test
        void preserveHashInQuotedString() {
            var content = """
                text = "value with # hash"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("value with # hash", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void preserveHashInSingleQuotedString() {
            var content = """
                text = 'value with # hash'
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals("value with # hash", doc.getString("", "text").unwrap());
                });
        }

        @Test
        void preserveHashInArrayString() {
            var content = """
                tags = ["a#b", "c#d"] # comment
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var tags = doc.getStringList("", "tags").unwrap();
                    assertEquals(List.of("a#b", "c#d"), tags);
                });
        }
    }

    @Nested
    class ArrayOfTables {

        @Test
        void parseSimpleArrayOfTables() {
            var content = """
                [[products]]
                name = "Hammer"
                price = 10

                [[products]]
                name = "Nail"
                price = 1
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.hasTableArray("products"));
                    var products = doc.getTableArray("products").unwrap();
                    assertEquals(2, products.size());

                    assertEquals("Hammer", products.get(0).get("name"));
                    assertEquals(10L, products.get(0).get("price"));

                    assertEquals("Nail", products.get(1).get("name"));
                    assertEquals(1L, products.get(1).get("price"));
                });
        }

        @Test
        void parseArrayOfTablesWithSubTable() {
            var content = """
                [[products]]
                name = "Hammer"

                [products.details]
                weight = 500
                material = "steel"

                [[products]]
                name = "Nail"

                [products.details]
                weight = 5
                material = "iron"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var products = doc.getTableArray("products").unwrap();
                    assertEquals(2, products.size());

                    // First product
                    assertEquals("Hammer", products.get(0).get("name"));
                    @SuppressWarnings("unchecked")
                    var details0 = (java.util.Map<String, Object>) products.get(0).get("details");
                    assertEquals(500L, details0.get("weight"));
                    assertEquals("steel", details0.get("material"));

                    // Second product
                    assertEquals("Nail", products.get(1).get("name"));
                    @SuppressWarnings("unchecked")
                    var details1 = (java.util.Map<String, Object>) products.get(1).get("details");
                    assertEquals(5L, details1.get("weight"));
                    assertEquals("iron", details1.get("material"));
                });
        }

        @Test
        void parseNestedArrayOfTables() {
            var content = """
                [[fruits]]
                name = "apple"

                [[fruits.varieties]]
                name = "red delicious"

                [[fruits.varieties]]
                name = "granny smith"

                [[fruits]]
                name = "banana"

                [[fruits.varieties]]
                name = "plantain"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var fruits = doc.getTableArray("fruits").unwrap();
                    assertEquals(2, fruits.size());

                    // First fruit (apple)
                    assertEquals("apple", fruits.get(0).get("name"));
                    @SuppressWarnings("unchecked")
                    var appleVarieties = (java.util.List<java.util.Map<String, Object>>)
                        doc.getTableArray("fruits.varieties").unwrap();
                    // Note: TOML spec says [[fruits.varieties]] adds to current [[fruits]]
                    // but our impl stores them flat - verify accordingly
                    assertTrue(doc.hasTableArray("fruits.varieties"));

                    // Second fruit (banana)
                    assertEquals("banana", fruits.get(1).get("name"));
                });
        }

        @Test
        void detectTableTypeMismatchSectionThenArray() {
            var content = """
                [products]
                name = "something"

                [[products]]
                name = "Hammer"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for type mismatch"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.TableTypeMismatch.class, error);
                    assertTrue(error.message().contains("products"));
                    assertTrue(error.message().contains("regular table"));
                });
        }

        @Test
        void detectTableTypeMismatchArrayThenSection() {
            var content = """
                [[products]]
                name = "Hammer"

                [products]
                name = "something"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for type mismatch"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.TableTypeMismatch.class, error);
                    assertTrue(error.message().contains("products"));
                    assertTrue(error.message().contains("array of tables"));
                });
        }

        @Test
        void accessViaGetTableArray() {
            var content = """
                [[servers]]
                name = "alpha"
                ip = "10.0.0.1"

                [[servers]]
                name = "beta"
                ip = "10.0.0.2"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.hasTableArray("servers"));
                    assertFalse(doc.hasTableArray("nonexistent"));

                    var servers = doc.getTableArray("servers");
                    assertTrue(servers.isPresent());
                    assertEquals(2, servers.unwrap().size());

                    var missing = doc.getTableArray("nonexistent");
                    assertFalse(missing.isPresent());
                });
        }

        @Test
        void tableArrayNames() {
            var content = """
                [[products]]
                name = "a"

                [[orders]]
                id = 1
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var names = doc.tableArrayNames();
                    assertEquals(2, names.size());
                    assertTrue(names.contains("products"));
                    assertTrue(names.contains("orders"));
                });
        }

        @Test
        void parseEmptyArrayOfTables() {
            var content = """
                [[empty]]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(doc.hasTableArray("empty"));
                    var empty = doc.getTableArray("empty").unwrap();
                    assertEquals(1, empty.size());
                    assertTrue(empty.get(0).isEmpty());
                });
        }

        @Test
        void parseArrayOfTablesWithArrayValue() {
            var content = """
                [[config]]
                name = "test"
                tags = ["a", "b", "c"]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var configs = doc.getTableArray("config").unwrap();
                    assertEquals(1, configs.size());
                    assertEquals("test", configs.get(0).get("name"));
                    @SuppressWarnings("unchecked")
                    var tags = (java.util.List<String>) configs.get(0).get("tags");
                    assertEquals(List.of("a", "b", "c"), tags);
                });
        }

        @Test
        void parseArrayOfTablesFollowedByRegularSection() {
            var content = """
                [[products]]
                name = "Hammer"

                [settings]
                debug = true
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    // Array tables
                    assertTrue(doc.hasTableArray("products"));
                    var products = doc.getTableArray("products").unwrap();
                    assertEquals(1, products.size());
                    assertEquals("Hammer", products.get(0).get("name"));

                    // Regular section
                    assertTrue(doc.hasSection("settings"));
                    assertTrue(doc.getBoolean("settings", "debug").unwrap());
                });
        }

        @Test
        void parseMixedContentWithArrayOfTables() {
            var content = """
                title = "Config"

                [database]
                host = "localhost"

                [[servers]]
                name = "alpha"

                [[servers]]
                name = "beta"

                [logging]
                level = "debug"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    // Root properties
                    assertEquals("Config", doc.getString("", "title").unwrap());

                    // Regular sections
                    assertEquals("localhost", doc.getString("database", "host").unwrap());
                    assertEquals("debug", doc.getString("logging", "level").unwrap());

                    // Array tables
                    var servers = doc.getTableArray("servers").unwrap();
                    assertEquals(2, servers.size());
                    assertEquals("alpha", servers.get(0).get("name"));
                    assertEquals("beta", servers.get(1).get("name"));
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

    @Nested
    class InvalidEscapeSequences {

        @Test
        void rejectsInvalidEscapeX() {
            var content = """
                text = "hello\\xworld"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for invalid escape"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.InvalidEscapeSequence.class, error);
                    assertTrue(error.message().contains("\\x"));
                });
        }

        @Test
        void rejectsInvalidEscapeQ() {
            var content = """
                text = "hello\\qworld"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for invalid escape"))
                .onFailure(error -> assertInstanceOf(TomlError.InvalidEscapeSequence.class, error));
        }

        @Test
        void rejectsInvalidEscapeZ() {
            var content = """
                text = "test\\z"
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for invalid escape"))
                .onFailure(error -> assertInstanceOf(TomlError.InvalidEscapeSequence.class, error));
        }
    }

    @Nested
    class LeadingZeros {

        @Test
        void rejectsLeadingZerosInInteger() {
            var content = """
                value = 007
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for leading zeros"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.InvalidValue.class, error);
                    assertTrue(error.message().contains("leading zeros"));
                });
        }

        @Test
        void acceptsZeroAlone() {
            var content = """
                value = 0
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail for zero"))
                .onSuccess(doc -> assertEquals(0L, doc.getLong("", "value").unwrap()));
        }

        @Test
        void acceptsNegativeZero() {
            var content = """
                value = -0
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail for -0"))
                .onSuccess(doc -> assertEquals(0L, doc.getLong("", "value").unwrap()));
        }
    }

    @Nested
    class UnderscoresInNumbers {

        @Test
        void parseIntegerWithUnderscores() {
            var content = """
                million = 1_000_000
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals(1000000L, doc.getLong("", "million").unwrap()));
        }

        @Test
        void parseFloatWithUnderscores() {
            var content = """
                value = 1_000.000_5
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals(1000.0005, doc.getDouble("", "value").unwrap(), 0.0001));
        }

        @Test
        void parseHexWithUnderscores() {
            var content = """
                value = 0xDEAD_BEEF
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals(0xDEADBEEFL, doc.getLong("", "value").unwrap()));
        }
    }

    @Nested
    class SpecialFloatValues {

        @Test
        void parsePositiveInfinity() {
            var content = """
                value = inf
                positive = +inf
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(Double.POSITIVE_INFINITY, doc.getDouble("", "value").unwrap());
                    assertEquals(Double.POSITIVE_INFINITY, doc.getDouble("", "positive").unwrap());
                });
        }

        @Test
        void parseNegativeInfinity() {
            var content = """
                value = -inf
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals(Double.NEGATIVE_INFINITY, doc.getDouble("", "value").unwrap()));
        }

        @Test
        void parseNaN() {
            var content = """
                value = nan
                positive = +nan
                negative = -nan
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertTrue(Double.isNaN(doc.getDouble("", "value").unwrap()));
                    assertTrue(Double.isNaN(doc.getDouble("", "positive").unwrap()));
                    assertTrue(Double.isNaN(doc.getDouble("", "negative").unwrap()));
                });
        }
    }

    @Nested
    class AlternativeIntegerBases {

        @Test
        void parseHexadecimal() {
            var content = """
                hex = 0xDEADBEEF
                lower = 0xdeadbeef
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    assertEquals(0xDEADBEEFL, doc.getLong("", "hex").unwrap());
                    assertEquals(0xDEADBEEFL, doc.getLong("", "lower").unwrap());
                });
        }

        @Test
        void parseOctal() {
            var content = """
                value = 0o755
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals(0755L, doc.getLong("", "value").unwrap()));
        }

        @Test
        void parseBinary() {
            var content = """
                value = 0b11010110
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals(0b11010110L, doc.getLong("", "value").unwrap()));
        }
    }

    @Nested
    class EmptyQuotedKeys {

        @Test
        void parseEmptyDoubleQuotedKey() {
            var content = """
                "" = "empty key"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals("empty key", doc.getString("", "").unwrap()));
        }

        @Test
        void parseEmptySingleQuotedKey() {
            var content = """
                '' = "empty literal key"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals("empty literal key", doc.getString("", "").unwrap()));
        }
    }

    @Nested
    class DuplicateSectionDetection {

        @Test
        void detectsDuplicateSection() {
            var content = """
                [database]
                host = "localhost"

                [database]
                port = 5432
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for duplicate section"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.DuplicateSection.class, error);
                    assertTrue(error.message().contains("database"));
                });
        }
    }

    @Nested
    class UnicodeSurrogateValidation {

        @Test
        void rejectsHighSurrogate() {
            var content = "text = \"\\uD800\"";

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for surrogate"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.InvalidSurrogate.class, error);
                    assertTrue(error.message().contains("D800"));
                });
        }

        @Test
        void rejectsLowSurrogate() {
            var content = "text = \"\\uDFFF\"";

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for surrogate"))
                .onFailure(error -> assertInstanceOf(TomlError.InvalidSurrogate.class, error));
        }
    }

    @Nested
    class UnsupportedFeatureErrors {

        @Test
        void rejectsInlineTable() {
            var content = """
                config = {key = "value"}
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for inline table"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.UnsupportedFeature.class, error);
                    assertTrue(error.message().contains("inline tables"));
                });
        }

        @Test
        void rejectsDateTime() {
            var content = """
                date = 2024-01-15
                """;

            TomlParser.parse(content)
                .onSuccess(_ -> fail("Should fail for date"))
                .onFailure(error -> {
                    assertInstanceOf(TomlError.UnsupportedFeature.class, error);
                    assertTrue(error.message().contains("dates"));
                });
        }
    }

    @Nested
    class LiteralStringArrays {

        @Test
        void parseLiteralStringsWithCommas() {
            var content = """
                paths = ['C:\\Users\\test', 'D:\\Data\\files']
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var paths = doc.getStringList("", "paths").unwrap();
                    assertEquals(2, paths.size());
                    assertEquals("C:\\Users\\test", paths.get(0));
                    assertEquals("D:\\Data\\files", paths.get(1));
                });
        }
    }

    @Nested
    class MultilineArrayComments {

        @Test
        void stripCommentsFromMultilineArray() {
            var content = """
                items = [
                    "first", # comment 1
                    "second", # comment 2
                    "third"
                ]
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> {
                    var items = doc.getStringList("", "items").unwrap();
                    assertEquals(List.of("first", "second", "third"), items);
                });
        }
    }

    @Nested
    class EscapeSequencesInKeys {

        @Test
        void parseKeyWithNewlineEscape() {
            var content = """
                "key\\nwith\\nnewlines" = "value"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals("value", doc.getString("", "key\nwith\nnewlines").unwrap()));
        }

        @Test
        void parseKeyWithTabEscape() {
            var content = """
                "key\\twith\\ttabs" = "value"
                """;

            TomlParser.parse(content)
                .onFailure(_ -> fail("Should not fail"))
                .onSuccess(doc -> assertEquals("value", doc.getString("", "key\twith\ttabs").unwrap()));
        }
    }
}
