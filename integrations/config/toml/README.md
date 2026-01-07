# TOML Parser for Pragmatica Lite

Zero-dependency TOML parser with Result-based error handling and Option-based accessors.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>toml</artifactId>
    <version>0.9.10</version>
</dependency>
```

## Quick Start

```java
import org.pragmatica.config.toml.TomlParser;
import org.pragmatica.config.toml.TomlDocument;

var content = """
    title = "My Application"
    debug = false

    [server]
    host = "localhost"
    port = 8080

    [database]
    url = "jdbc:postgresql://localhost/mydb"
    pool_size = 10
    """;

TomlParser.parse(content)
    .onFailure(error -> System.err.println("Parse error: " + error.message()))
    .onSuccess(doc -> {
        // Access root-level properties (empty string section)
        String title = doc.getString("", "title").or("Untitled");
        boolean debug = doc.getBoolean("", "debug").or(false);

        // Access section properties
        String host = doc.getString("server", "host").or("127.0.0.1");
        int port = doc.getInt("server", "port").or(8080);

        System.out.println("Starting " + title + " on " + host + ":" + port);
    });
```

## API Reference

### TomlParser

Static utility for parsing TOML content.

```java
// Parse from string
Result<TomlDocument> doc = TomlParser.parse(content);

// Parse from file
Result<TomlDocument> doc = TomlParser.parseFile(Path.of("config.toml"));
```

### TomlDocument

Immutable document with typed accessors returning `Option<T>`.

#### String Values

```java
doc.getString("section", "key")           // Option<String>
doc.getString("", "root_key")             // Root-level property
```

#### Numeric Values

```java
doc.getInt("server", "port")              // Option<Integer>
doc.getLong("stats", "total_bytes")       // Option<Long>
doc.getDouble("config", "ratio")          // Option<Double>
```

#### Boolean Values

```java
doc.getBoolean("features", "enabled")     // Option<Boolean>
```

#### Array Values

```java
doc.getStringList("tags", "environments") // Option<List<String>>
```

#### Section Utilities

```java
doc.hasSection("database")                // boolean
doc.hasKey("database", "url")             // boolean
doc.sectionNames()                        // Set<String>
doc.keys("database")                      // Set<String>
doc.getSection("database")                // Map<String, String>
```

#### Immutable Updates

```java
TomlDocument updated = doc.with("server", "port", 9090);
// Original doc is unchanged
```

## Supported TOML Features

| Feature | Example | Supported |
|---------|---------|-----------|
| Sections | `[section]` | Yes |
| Nested sections | `[section.subsection]` | Yes |
| Quoted strings | `key = "value"` | Yes |
| Unquoted strings | `key = value` | Yes |
| Escape sequences | `key = "line1\nline2"` | Yes |
| Integers | `port = 8080` | Yes |
| Negative integers | `offset = -100` | Yes |
| Booleans | `enabled = true` | Yes |
| Arrays | `tags = ["a", "b"]` | Yes |
| Comments | `# comment` | Yes |
| Inline comments | `port = 8080 # default` | Yes |
| Inline tables | `{key = value}` | No |
| Multi-line strings | `"""..."""` | No |
| Floating point | `pi = 3.14` | Yes |
| Dates | `date = 2024-01-01` | No |

## Error Handling

All parsing errors are returned as typed `TomlError` causes:

```java
TomlParser.parse(content)
    .onFailure(error -> {
        switch (error) {
            case TomlError.SyntaxError e ->
                log.error("Syntax error at line {}: {}", e.line(), e.details());
            case TomlError.UnterminatedString e ->
                log.error("Unterminated string at line {}", e.line());
            case TomlError.UnterminatedArray e ->
                log.error("Unterminated array at line {}", e.line());
            case TomlError.InvalidValue e ->
                log.error("Invalid {} at line {}: {}", e.expectedType(), e.line(), e.value());
            case TomlError.FileReadFailed e ->
                log.error("Failed to read file: {}", e.message());
        }
    });
```

## Complete Example

```java
public record AppConfig(
    String title,
    ServerConfig server,
    DatabaseConfig database,
    List<String> enabledFeatures
) {
    public static Result<AppConfig> load(Path configPath) {
        return TomlParser.parseFile(configPath)
            .flatMap(AppConfig::fromDocument);
    }

    private static Result<AppConfig> fromDocument(TomlDocument doc) {
        return Result.all(
            doc.getString("", "title").toResult(Causes.cause("Missing title")),
            ServerConfig.from(doc),
            DatabaseConfig.from(doc),
            Result.success(doc.getStringList("features", "enabled").or(List.of()))
        ).map(AppConfig::new);
    }
}

public record ServerConfig(String host, int port) {
    public static Result<ServerConfig> from(TomlDocument doc) {
        return Result.all(
            doc.getString("server", "host").toResult(Causes.cause("Missing server.host")),
            doc.getInt("server", "port").toResult(Causes.cause("Missing server.port"))
        ).map(ServerConfig::new);
    }
}

public record DatabaseConfig(String url, int poolSize) {
    public static Result<DatabaseConfig> from(TomlDocument doc) {
        return Result.all(
            doc.getString("database", "url").toResult(Causes.cause("Missing database.url")),
            Result.success(doc.getInt("database", "pool_size").or(10))
        ).map(DatabaseConfig::new);
    }
}
```

## Dependencies

None - this module only depends on `pragmatica-lite-core`.
