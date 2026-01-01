# Serialization Module

Binary serialization integrations for Pragmatica Lite.

## Modules

- **serialization-api** - Core interfaces (`Serializer`, `Deserializer`, `ClassRegistrator`)
- **serialization-kryo** - Kryo-based implementation (fast, compact binary format)
- **serialization-fury** - Apache Fury-based implementation (extremely fast, JIT optimized)

## Usage

### Kryo Serialization

```java
import org.pragmatica.serialization.kryo.KryoSerializer;
import org.pragmatica.serialization.kryo.KryoDeserializer;

// Create serializer/deserializer
var serializer = KryoSerializer.kryoSerializer();
var deserializer = KryoDeserializer.kryoDeserializer();

// Serialize to bytes
byte[] bytes = serializer.encode(myObject);

// Deserialize from bytes
MyObject result = deserializer.decode(bytes);
```

### Fury Serialization

```java
import org.pragmatica.serialization.fury.FurySerializer;
import org.pragmatica.serialization.fury.FuryDeserializer;

// Create serializer/deserializer
var serializer = FurySerializer.furySerializer();
var deserializer = FuryDeserializer.furyDeserializer();

// Serialize to bytes
byte[] bytes = serializer.encode(myObject);

// Deserialize from bytes
MyObject result = deserializer.decode(bytes);
```

### Class Registration

For best performance, register your domain classes:

```java
import org.pragmatica.serialization.ClassRegistrator;

ClassRegistrator registrator = consumer -> {
    consumer.accept(User.class);
    consumer.accept(Order.class);
    consumer.accept(Product.class);
};

var serializer = KryoSerializer.kryoSerializer(registrator);
var deserializer = KryoDeserializer.kryoDeserializer(registrator);
```

### ByteBuf Integration

For Netty integration, use `write()` and `read()` directly with ByteBuf:

```java
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

ByteBuf buffer = Unpooled.buffer();

// Write to buffer
serializer.write(buffer, myObject);

// Read from buffer
MyObject result = deserializer.read(buffer);
```

## Choosing Between Kryo and Fury

| Aspect | Kryo | Fury |
|--------|------|------|
| Performance | Fast | Very fast (JIT optimized) |
| Compatibility | Wide ecosystem | Newer, less adoption |
| Memory | Low | Lower (native memory) |
| Class registration | Recommended | Recommended |
| Cross-language | No | Yes (multiple languages) |

Both implementations are thread-safe and use pooling for optimal performance.
