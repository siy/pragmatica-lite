# TCP Server Module

Convenient wrapper for Netty TCP server setup with comprehensive TLS support for server-side, client-side, and mutual TLS configurations.

## Features

- Simple Promise-based API
- Unified TLS configuration supporting server, client, and mutual TLS
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
    .withTls(TlsConfig.selfSignedServer());

// Or with PEM files (production)
var prodConfig = ServerConfig.serverConfig("secure-server", 8443)
    .withTls(TlsConfig.server(
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

## TLS Configuration

### Overview

The TCP module provides a unified TLS configuration API supporting:
- **Server TLS** - Secure incoming connections
- **Client TLS** - Secure outgoing connections
- **Mutual TLS (mTLS)** - Both sides authenticate

### Configuration Types

#### Identity (Your Certificate)

```java
// Self-signed (development only)
TlsConfig.Identity.SelfSigned()

// From PEM files
TlsConfig.Identity.FromFiles(
    Path.of("cert.pem"),
    Path.of("key.pem"),
    Option.empty()  // or Option.some("password")
)
```

#### Trust (Who You Trust)

```java
// System CA certificates (default for clients)
TlsConfig.Trust.SystemDefault()

// Custom CA (for self-signed servers or internal CAs)
TlsConfig.Trust.FromCaFile(Path.of("ca.pem"))

// Trust everything (DEVELOPMENT ONLY - logs warning)
TlsConfig.Trust.InsecureTrustAll()
```

### Server Configuration

#### Basic Server TLS

```java
// Accept TLS connections, don't require client certificates
var tls = TlsConfig.server(
    Path.of("/etc/certs/server.crt"),
    Path.of("/etc/certs/server.key")
);
```

#### Server with Client Authentication

```java
// Require clients to present certificates (mTLS server-side)
var tls = TlsConfig.serverWithClientAuth(
    Path.of("/etc/certs/server.crt"),
    Path.of("/etc/certs/server.key"),
    Path.of("/etc/certs/client-ca.crt")  // CA that signed client certs
);
```

### Client Configuration

#### Connect to Public HTTPS Server

```java
// Uses system CA certificates
var clientTls = TlsConfig.client();

var config = ServerConfig.serverConfig("MyService", 8080)
    .withClientTls(clientTls);
```

#### Connect to Self-Signed Server

```java
// Trust specific CA
var clientTls = TlsConfig.clientWithCa(
    Path.of("/etc/certs/server-ca.crt")
);
```

#### Development Client (Trust All)

```java
// WARNING: Only for development!
var clientTls = TlsConfig.insecureClient();
```

### Mutual TLS (mTLS)

For service-to-service communication where both sides authenticate:

```java
// Same config works for both server and client contexts
var mtls = TlsConfig.mutual(
    Path.of("/etc/certs/node.crt"),      // This node's certificate
    Path.of("/etc/certs/node.key"),       // This node's private key
    Path.of("/etc/certs/cluster-ca.crt")  // CA that signed all node certs
);

// Server accepts mTLS connections AND connects to peers with mTLS
var config = ServerConfig.serverConfig("ClusterNode", 9000)
    .withTls(mtls)
    .withClientTls(mtls);
```

#### Development mTLS

```java
// Auto-generated certs, trusts all (development only)
var devMtls = TlsConfig.selfSignedMutual();
```

### Consensus Cluster mTLS

For secure consensus cluster communication:

```java
var mtls = TlsConfig.mutual(
    Path.of("/etc/certs/node.crt"),
    Path.of("/etc/certs/node.key"),
    Path.of("/etc/certs/cluster-ca.crt")
);

var topology = TopologyConfig.topologyConfig(
    nodeId,
    Duration.ofSeconds(5),   // reconciliation
    Duration.ofSeconds(1),   // ping
    Duration.ofSeconds(5),   // hello timeout
    coreNodes,
    Option.some(mtls)        // Enable mTLS
);

var network = new NettyClusterNetwork(topology, serializer, deserializer, router);
network.start();
```

### Error Handling

All TLS operations return `Result<T>`:

```java
TlsContextFactory.createServer(config)
    .onSuccess(ctx -> log.info("TLS context created"))
    .onFailure(error -> {
        switch (error) {
            case TlsError.CertificateLoadFailed e ->
                log.error("Cannot load cert: {}", e.path());
            case TlsError.PrivateKeyLoadFailed e ->
                log.error("Cannot load key: {}", e.path());
            case TlsError.TrustStoreLoadFailed e ->
                log.error("Cannot load CA: {}", e.path());
            case TlsError.ContextBuildFailed e ->
                log.error("SSL context build failed: {}", e.cause());
            case TlsError.WrongMode e ->
                log.error("Wrong TLS mode: {}", e.details());
            default -> log.error("TLS error: {}", error.message());
        }
    });
```

## Socket Options

```java
var socketOptions = SocketOptions.socketOptions()
    .withSoBacklog(256)
    .withSoKeepalive(true)
    .withTcpNoDelay(true);

var config = ServerConfig.serverConfig("server", 8080)
    .withSocketOptions(socketOptions);
```

## TLS API Reference

### Factory Methods

| Method | Description |
|--------|-------------|
| `TlsConfig.selfSignedServer()` | Self-signed server certificate |
| `TlsConfig.server(cert, key)` | Server from PEM files |
| `TlsConfig.server(cert, key, password)` | Server with encrypted key |
| `TlsConfig.serverWithClientAuth(cert, key, ca)` | Server requiring client certs |
| `TlsConfig.client()` | Client using system CAs |
| `TlsConfig.clientWithCa(ca)` | Client with custom CA |
| `TlsConfig.insecureClient()` | Client trusting all (dev only) |
| `TlsConfig.mutual(cert, key, ca)` | Mutual TLS configuration |
| `TlsConfig.selfSignedMutual()` | Dev mTLS configuration |

### Certificate Generation

```bash
# Generate CA
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days 365 -key ca.key -out ca.crt -subj "/CN=Dev CA"

# Generate server cert
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr -subj "/CN=localhost"
openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt

# Generate client cert (for mTLS)
openssl genrsa -out client.key 2048
openssl req -new -key client.key -out client.csr -subj "/CN=client"
openssl x509 -req -days 365 -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt
```

## Best Practices

1. **Never use InsecureTrustAll in production** - It disables certificate verification
2. **Rotate certificates regularly** - Use short-lived certs (90 days max)
3. **Separate CA for clusters** - Don't reuse public CA for internal mTLS
4. **Store keys securely** - Use file permissions (600) or secrets managers
5. **Use PEM format** - More portable than JKS, easier to debug

## Dependencies

- Netty 4.2+
- Pragmatica Lite Core
