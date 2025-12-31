# Asynchronous DNS Resolver

Fully asynchronous DNS resolver with TTL-based caching using Netty UDP.

## Features

- **Fully async**: Returns `Promise<DomainAddress>` - no blocking
- **TTL caching**: Automatic cache eviction based on DNS TTL
- **Multi-server failover**: Queries multiple DNS servers, returns first success
- **Event loop sharing**: Can use own event loop or share with existing server
- **Zero background threads**: Cache eviction uses promise mechanics

## Quick Start

```java
import org.pragmatica.net.dns.DomainNameResolver;
import java.net.InetAddress;
import java.util.List;

// Create resolver with public DNS servers
var resolver = DomainNameResolver.domainNameResolver(List.of(
    InetAddress.getByName("8.8.8.8"),      // Google DNS
    InetAddress.getByName("1.1.1.1")       // Cloudflare DNS
));

// Resolve domain
resolver.resolve("example.com")
        .onSuccess(address -> {
            System.out.println("IP: " + address.ip());
            System.out.println("TTL: " + address.ttl());
        })
        .onFailure(cause -> {
            System.err.println("Resolution failed: " + cause.message());
        });

// Don't forget to close when done
resolver.close();
```

## API

### DomainNameResolver

Main entry point for DNS resolution.

```java
public interface DomainNameResolver extends AsyncCloseable {
    // Resolve domain, using cache if available
    Promise<DomainAddress> resolve(String name);
    Promise<DomainAddress> resolve(DomainName name);

    // Get cached result without triggering resolution
    Promise<DomainAddress> resolveCached(String name);
    Promise<DomainAddress> resolveCached(DomainName name);

    // Factory methods
    static DomainNameResolver domainNameResolver(List<InetAddress> servers);
    static DomainNameResolver domainNameResolver(List<InetAddress> servers, EventLoopGroup eventLoop);
}
```

### Factory Methods

**Own event loop** (auto-cleanup on close):
```java
var resolver = DomainNameResolver.domainNameResolver(List.of(
    InetAddress.getByName("8.8.8.8")
));
// Event loop created internally, shut down when resolver.close() called
```

**Shared event loop** (no cleanup, caller manages lifecycle):
```java
var sharedEventLoop = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
var resolver = DomainNameResolver.domainNameResolver(servers, sharedEventLoop);
// Event loop NOT shut down when resolver.close() called
// Useful when sharing with other Netty components
```

### DomainAddress

Resolved address with metadata.

```java
public interface DomainAddress {
    DomainName name();    // Original domain name
    InetAddress ip();     // Resolved IP address
    Duration ttl();       // Time-to-live from DNS response
}
```

### Error Handling

All errors are typed via `ResolverErrors`:

```java
public sealed interface ResolverErrors extends Cause {
    record InvalidIpAddress(String message) implements ResolverErrors {}
    record ServerError(String message) implements ResolverErrors {}
    record RequestTimeout(String message) implements ResolverErrors {}
    record UnknownError(String message) implements ResolverErrors {}
    record UnknownDomain(String message) implements ResolverErrors {}
}
```

## Caching Behavior

The resolver implements intelligent caching:

1. **Successful resolutions** are cached until TTL expires
2. **Failed resolutions** are NOT cached (allows retry)
3. **TTL eviction** uses `promise.async(ttl, ...)` - no background threads
4. **localhost** is pre-cached with infinite TTL

```java
// First call: DNS query sent
resolver.resolve("example.com").await();

// Second call: returns cached result (no network)
resolver.resolve("example.com").await();

// After TTL expires: DNS query sent again
```

## Multi-Server Failover

When multiple DNS servers are configured:

```java
var resolver = DomainNameResolver.domainNameResolver(List.of(
    InetAddress.getByName("8.8.8.8"),      // Primary
    InetAddress.getByName("8.8.4.4"),      // Secondary
    InetAddress.getByName("1.1.1.1")       // Tertiary
));
```

- Queries are sent to ALL servers in parallel
- First successful response wins (`Promise.any()`)
- Response with shortest TTL is preferred (faster DNS change propagation)
- If all fail, returns `UnknownDomain` error

## Timeout Handling

Each DNS query has a 10-second timeout. If no response is received:

```java
resolver.resolve("slow-dns.example.com")
        .onFailure(cause -> {
            // cause instanceof ResolverErrors.RequestTimeout
        });
```

## Thread Safety

- `DomainNameResolver` is fully thread-safe
- Cache operations use `ConcurrentHashMap`
- Safe for concurrent resolution from multiple threads

## Dependencies

- Netty (`netty-codec-dns`, `netty-transport`)
- Pragmatica Lite Core (`Promise`, `Result`, `Cause`, `TimeSpan`)
- SLF4J for logging
