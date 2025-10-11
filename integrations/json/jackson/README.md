# Jackson Integration for Pragmatica Lite

Functional wrapper around Jackson 3.0 providing Result-based JSON serialization/deserialization.

## Status

**Ready for Testing** - Core API design complete, Jackson 3.0 API migration complete.

## API Design

### Functional JsonMapper

All operations return `Result<T>` instead of throwing exceptions:

```java
var mapper = JsonMapper.create();

// Serialization
mapper.writeAsString(user)       // Result<String>
mapper.writeAsBytes(order)        // Result<byte[]>

// Deserialization
mapper.readString(json, User.class)                    // Result<User>
mapper.readBytes(bytes, new TypeReference<List<Order>>() {})  // Result<List<Order>>
```

### Builder Pattern

```java
var mapper = JsonMapper.builder()
    .withPragmaticaTypes()  // Enables Result/Option support
    .configure(builder -> builder.enable(...))
    .withModule(customModule)
    .build();
```

### Serialization Format

**Result<T>:**
```json
// Success
{"success": true, "value": "data"}

// Failure
{"success": false, "error": {"message": "...", "type": "..."}}
```

**Option<T>:**
```json
// Some
"data"

// None
null
```

## Jackson 3.0 Migration

This integration uses Jackson 3.0.0-rc9 with the following changes applied:
- Group ID: `tools.jackson.core` (was `com.fasterxml.jackson.core`)
- Package: `tools.jackson.*` (was `com.fasterxml.jackson.*`)
- Renamed classes (✓ completed):
  - `SerializerProvider` → `SerializationContext`
  - `JsonSerializer` → `ValueSerializer`
  - `JsonDeserializer` → `ValueDeserializer`
  - `ContextualSerializer` → removed (method moved to `ValueSerializer`)
  - `ContextualDeserializer` → removed (method moved to `ValueDeserializer`)
  - Exceptions changed from checked to unchecked
  - `writeFieldName` → `writeName`
  - `writeObjectFieldStart` → `writeName` + `writeStartObject`
  - `defaultSerializeValue` → `writePOJO`

## TODO

- [ ] Implement proper generic type handling
- [ ] Add comprehensive tests
- [ ] Document error handling patterns
- [ ] Add examples

## Dependencies

```xml
<dependency>
    <groupId>tools.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>3.0.0-rc9</version>
</dependency>
```
