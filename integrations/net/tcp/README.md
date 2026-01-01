# TCP Server Module

Convenient wrapper for Netty TCP server setup with TLS support.

## Features

- Simple Promise-based API
- TLS support (self-signed and PEM files)
- Configurable socket options
- Client connection capability (reuses server's event loop)
- Graceful shutdown with intermediate operation support

## Usage

### Basic Server

```java
import org.pragmatica.net.tcp.Server;
import io.netty.channel.ChannelHandler;
import java.util.List;

// Start a simple TCP server
Server.server("echo-server", 8080, () -> List.of(
    new EchoHandler()
)).onSuccess(server -> {
    System.out.println("Server started on port " + server.port());
});
```

### Server with TLS

```java
import org.pragmatica.net.tcp.ServerConfig;
import org.pragmatica.net.tcp.TlsConfig;

// Self-signed certificate (development)
var config = ServerConfig.serverConfig("secure-server", 8443)
    .withTls(TlsConfig.selfSigned());

// Or with PEM files (production)
var prodConfig = ServerConfig.serverConfig("secure-server", 8443)
    .withTls(TlsConfig.fromFiles(
        Path.of("/path/to/cert.pem"),
        Path.of("/path/to/key.pem")
    ));

Server.server(config, () -> List.of(new MyHandler()))
    .onSuccess(server -> System.out.println("TLS server started"));
```

### Client Connections

```java
// Connect to another server using the same event loop
server.connectTo(NodeAddress.nodeAddress("peer.example.com", 8080))
    .onSuccess(channel -> {
        channel.writeAndFlush(message);
    });
```

### Graceful Shutdown

```java
// Stop accepting new connections, finish existing work, then shutdown
server.stop(() -> {
    // Intermediate operation - finish processing queued work
    return finishPendingWork();
}).await();
```

## Configuration

### Socket Options

```java
var socketOptions = SocketOptions.socketOptions()
    .withSoBacklog(256)
    .withSoKeepalive(true);

var config = ServerConfig.serverConfig("server", 8080)
    .withSocketOptions(socketOptions);
```

### TLS Options

| Method | Description |
|--------|-------------|
| `TlsConfig.selfSigned()` | Generate self-signed certificate at startup |
| `TlsConfig.fromFiles(cert, key)` | Load from PEM files |
| `TlsConfig.fromFiles(cert, key, password)` | Load password-protected key |

## API Reference

### Server

| Method | Description |
|--------|-------------|
| `name()` | Server name for logging |
| `port()` | Bound port number |
| `connectTo(NodeAddress)` | Connect to peer, returns `Promise<Channel>` |
| `stop(Supplier<Promise<Unit>>)` | Graceful shutdown with intermediate operation |

### Factory Methods

| Method | Description |
|--------|-------------|
| `Server.server(name, port, handlers)` | Simple server with defaults |
| `Server.server(config, handlers)` | Full configuration |

## Dependencies

- Netty 4.2+
- Pragmatica Lite Core
