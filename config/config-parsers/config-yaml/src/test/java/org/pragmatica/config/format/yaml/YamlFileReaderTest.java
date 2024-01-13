package org.pragmatica.config.format.yaml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class YamlFileReaderTest {
    @SuppressWarnings("deprecation")
    @Test
    public void readSpaces() {
        var map = YamlFileReader.readFile("src/test/resources/application.yaml").unwrap();

        System.out.println(map);

        assertEquals(map.get("include.profiles"), Result.success("local,alpha"));
        assertEquals(map.get("cors.allow_origin"), Result.success("*"));
        assertEquals(map.get("cors.enable"), Result.success("true"));
        assertEquals(map.get("server.port"), Result.success("3005"));
        assertEquals(map.get("server.host"), Result.success("0.0.0.0"));
        assertEquals(map.get("server.admin.username"), Result.success("admin"));
        assertEquals(map.get("server.admin.password"), Result.success("123456"));
        assertEquals(map.get("hello"), Result.success("world"));
        assertEquals(map.get("foo"), Result.success("bar"));

        map = YamlFileReader.readFile("src/test/resources/hello_yaml").unwrap();

        assertEquals(map.get("hello"), Result.success("world"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void readTabs() {
        var map = YamlFileReader.readFile("src/test/resources/application2_yaml.txt").unwrap();

        System.out.println(map);

        assertEquals(map.get("cors.allow_origin"), Result.success("*"));
        assertEquals(map.get("cors.enable"), Result.success("true"));
        assertEquals(map.get("server.port"), Result.success("3005"));
        assertEquals(map.get("server.host"), Result.success("0.0.0.0"));
        assertEquals(map.get("server.admin.User-Name1"), Result.success("admin"));
        assertEquals(map.get("server.admin.password2"), Result.success("123456"));
        assertEquals(map.get("hello"), Result.success("world"));
        assertEquals(map.get("foo"), Result.success("bar"));
    }

    @Test
    public void testNoContainsSeparateChar() {
        YamlFileReader.readFile("src/test/resources/invalid_yaml1.txt")
                      .onSuccessRun(Assertions::fail)
                      .onFailure(System.out::println)
                      .onFailure(cause -> assertInstanceOf(YamlParseError.InvalidSyntaxError.class, cause));
    }

    @Test
    public void testEmptyKey() {
        YamlFileReader.readFile("src/test/resources/invalid_yaml2.txt")
                      .onSuccessRun(Assertions::fail)
                      .onFailure(System.out::println)
                      .onFailure(cause -> assertInstanceOf(YamlParseError.InvalidSyntaxError.class, cause));
    }

    @Test
    public void testDashKey() {
        YamlFileReader.readFile("src/test/resources/invalid_yaml4.txt")
                      .onSuccessRun(Assertions::fail)
                      .onFailure(System.out::println)
                      .onFailure(cause -> assertInstanceOf(YamlParseError.InvalidSyntaxError.class, cause));
    }

    @Test
    public void testInvalidKey() {
        YamlFileReader.readFile("src/test/resources/invalid_yaml5.txt")
                      .onSuccessRun(Assertions::fail)
                      .onFailure(System.out::println)
                      .onFailure(cause -> assertInstanceOf(YamlParseError.InvalidSyntaxError.class, cause));
    }

    @Test
    public void readInvalidYamlFileLastPropertyKeyIsNull() {
        YamlFileReader.readFile("src/test/resources/v112_application_invalid.yaml")
                      .onSuccessRun(Assertions::fail)
                      .onFailure(System.out::println)
                      .onFailure(cause -> assertInstanceOf(YamlParseError.InvalidSyntaxError.class, cause));
    }

    @Test
    public void readInvalidYamlFileLastSpaceCountEqualsLastSpaceCount() {
        YamlFileReader.readFile("src/test/resources/v112_application_invalid2.yaml")
                      .onSuccessRun(Assertions::fail)
                      .onFailure(System.out::println)
                      .onFailure(cause -> assertInstanceOf(YamlParseError.InvalidSyntaxError.class, cause));
    }
}