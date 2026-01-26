# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.11.0] - 2026-01-26

### Fixed
- **Split-brain resurrection vulnerability in consensus topology:**
  - `TopologyManager.clusterSize()` now returns fixed configured value instead of dynamic topology size
  - Prevents minority partitions from achieving false quorum after removing unreachable nodes
  - Added `TopologyConfig.clusterSize` required parameter for explicit cluster size configuration
  - Added `TopologyManagementMessage.SetClusterSize` for operational cluster size changes with validation:
    - Rejects size < 3 (minimum for Byzantine fault tolerance)
    - Rejects size increase if active nodes < new quorum
    - Triggers `QuorumStateNotification.ESTABLISHED` on resurrection via size decrease
- **Sync deadlock when actual cluster smaller than configured size:**
  - `RabiaEngine` sync now uses connected peer count for quorum instead of fixed cluster size
  - Allows new nodes to join a reduced cluster (e.g., after partition healing)

### Added
- **Memoization utilities** for caching computation results:
  - `Memo<K,V>` - pure synchronous memoization with optional LRU eviction
  - `MemoResult<K,V>` - Result-returning computation caching (failures not cached)
  - `MemoPromise<K,V>` - Promise-returning computation caching with request deduplication
  - All variants support: hit/miss counters, invalidation, size limits
- **`Lazy<T>`** - deferred computation with thread-safe memoization
  - `lazy(Supplier)` / `value(T)` factory methods
  - `map()` / `flatMap()` combinators (lazily evaluated)
  - `isComputed()` for inspection without triggering evaluation
- **Property-based testing module** (`testing`):
  - `Arbitrary<T>` - composable value generators with shrinking
  - `Shrinkable<T>` - values with shrink streams for failure minimization
  - `Shrinkers` - shrinking strategies for integers, longs, strings, lists
  - `PropertyTest` - test runner with configurable tries, seed, shrinking depth
  - `PropertyResult` sealed interface (Passed/Failed) with shrunk counterexamples
  - Generators: `integers`, `longs`, `doubles`, `booleans`, `strings`, `lists`, `sets`, `oneOf`, `frequency`, `constant`
  - Combinators: `map`, `flatMap`, `filter` (returns `Result`), `tryGenerate`
  - Multi-arbitrary support: `forAll(a1, a2, ...)` up to 5 arbitraries

### Changed
- **JBCT compliance improvements:**
  - `TcpTopologyManager.tcpTopologyManager()` now returns `Result<TcpTopologyManager>` instead of throwing on invalid config
  - Added `TcpTopologyManager.TopologyError` sealed interface with `SelfNodeNotInCoreNodes` variant
  - `Arbitrary.filter()` now returns `Arbitrary<Result<T>>` instead of throwing on exhaustion
  - `Arbitrary.fromFactory()` now returns `Arbitrary<Result<T>>` instead of throwing
  - `Arbitrary.oneOf()` / `frequency()` now return `Result<Arbitrary<T>>` instead of throwing
  - `Memo.invalidate()` / `invalidateAll()` return `Unit` instead of `void`
  - `MemoResult.invalidate()` / `invalidateAll()` return `Unit` instead of `void`
  - `MemoPromise.invalidate()` / `invalidateAll()` return `Unit` instead of `void`
  - `PropertyTest` uses pattern matching instead of `fold()` for Result handling
  - Added null validation to `Lazy`, `Arbitrary`, `PropertyTest`, `Shrinkable` factory methods
- **Merged `Arbitraries` utility class into `Arbitrary` interface** - all generator factory methods now accessed via `Arbitrary.integers()`, `Arbitrary.strings()`, etc.
- Factory method naming: `CacheEntry.of()` → `cacheEntry()`, `ResultCacheEntry.of()` → `resultCacheEntry()`
- Test improvements: replaced `Thread.sleep` with condition polling, added `@Timeout` to `PromiseTest`, organized `LazyTest` with `@Nested`

### Removed
- `Arbitraries` class - merged into `Arbitrary` interface

---

## [0.10.0] - 2026-01-23

### Added
- **Super-majority fast path optimization for Rabia consensus:**
  - When `n - f` nodes vote the same value in Round 1, skip Round 2 and decide immediately
  - `TopologyManager.superMajoritySize()` returns the fast path threshold
  - `PhaseData.getSuperMajorityRound1Value()` detects super-majority agreement
  - `ConsensusMetrics.recordFastPath()` for observability
  - 52 new tests in `RabiaSuperMajorityFastPathTest` covering threshold calculations, trigger conditions, and correctness
- `TimeSpan` value object for parsing human-friendly duration strings
  - Supports units: d (days), h (hours), m (minutes), s (seconds), ms (milliseconds), us (microseconds), ns (nanoseconds)
  - Optional whitespace between components ("1d16h" and "1d 16h" both valid)
  - `TimeSpan.timeSpan(String)` factory returning `Result<TimeSpan>`
  - `TimeSpanError` sealed interface for typed parsing errors
- Comprehensive Rabia consensus test suite for `weak_mvc.ivy` specification compliance
  - 6 helper classes: `ClusterConfiguration`, `ClusterState`, `InvariantChecker`, `ProtocolAction`, `SpecClusterSimulator`, `VotingHistoryRecorder`
  - 5 spec test classes covering 50 testable invariants (237 parameterized tests across cluster sizes 3, 5, 7)
  - `RabiaSpecInvariantTest`: Conjectures 1-28 (proposal, decision, phase, vote, decision_bc, coin invariants)
  - `RabiaValueLockingTest`: Conjectures 31-46 (value locking and decision locking properties)
  - `RabiaWrapperInvariantTest`: Conjectures 47-52 (phase goodness and started predicates)
  - `RabiaQuorumIntersectionTest`: Quorum intersection axioms
  - `RabiaMultiPhaseTest`: Multi-phase scenarios including failure and coin flip cases
- `Idempotency` utility for at-most-once execution guarantees
  - TTL-based caching with automatic cleanup
  - In-flight request coalescing (concurrent calls share same Promise)
  - Failed operations not cached, allowing immediate retry
  - Thread-safe via `ConcurrentHashMap.compute()`
  - `Idempotency.create(TimeSpan)` factory returning `Result<Idempotency>`

### Changed
- Bumped jbct-maven-plugin to 0.4.9
- **Rabia consensus engine JBCT compliance overhaul:**
  - Thread safety: All state-mutating methods now route through single-executor serialization (`clusterDisconnected`, `handleNewBatch`, `submitCommands`, `synchronize`, `cleanupOldPhases`, `handleSyncRequest`)
  - Structural patterns: `handlePropose`, `handleVoteRound1`, `handleVoteRound2` refactored into focused Leaf methods
  - Extracted predicate methods: `canVoteRound1()`, `canVoteRound2()`, `canMakeDecision()`, `isPastPhase()`
  - Multi-statement lambdas extracted: `performStop()`, `broadcastLockedVote()`, `applyRestoredState()`
  - Value objects: `Batch` adds defensive copy of commands list, `Phase` validates non-negative values
- **JBCT compliance fixes across integrations module:**
  - Parse-don't-validate: `DHTConfig`, `Partition`, `NodeId`, `BatchId`, `CorrelationId`, `DomainName`, `NodeAddress` now return `Result<T>`
  - `SocketOptions` factory now returns `Result<SocketOptions>` with validation
  - `JdbcTransactional` refactored to use Promise composition instead of blocking `.await()`
  - `RabiaEngine.submitCommands()` decomposed into Leaf methods
  - `RabiaPersistence` uses `Option` instead of null for saved state
  - `MemoryStorageEngine` and `Headers` use `Option.option()` pattern
  - Security: Removed SQL from error messages in `JdbcError`, `JooqError`, `R2dbcError`
  - Async patterns: `JooqR2dbcTransactional` properly chains async operations
  - Defensive copies: `TopologyConfig`, `HttpServerConfig`, `DHTMessage` byte arrays
  - Void→Unit: `StateMachine.reset()`, `TopologyManager.start()/stop()`, `ClusterNetwork.broadcast()/send()`
  - Error naming: `ResolverErrors`→`ResolverError`, `ConsensusErrors`→`ConsensusError`
  - Null policy: Replaced null checks with `Option` patterns in 29+ files
- **Serializer design note:** Added documentation explaining intentional exception-based error handling for fatal serialization failures

## [0.9.11] - 2026-01-15

### Added
- HTTP routing module with type-safe parameter builders
  - 35 builder interfaces supporting path, query, and body parameter combinations
  - `QueryParameter` type with 8 factory methods (aString, aInteger, aLong, aBoolean, aDouble, aDecimal, aLocalDate, aLocalDateTime)
  - Combined path + body builders (1-4 path params + body)
  - Combined query + body builders (1-4 query params + body)
  - Combined path + query builders (up to 5 total params)
  - Combined path + query + body builders (up to 5 total params)
- `JsonCodecAdapter` wrapping `JsonMapper` for Netty ByteBuf I/O

### Changed
- `Route.java` rewritten with fluent builder API supporting parameter combinations
- `RequestContext` reduced to 5 path params, added 5 query match methods
- `ProblemDetail` now uses `Option<String>` for optional fields, removed Jackson annotations
- `JsonMapper` builder API refactored to use configurators instead of exposing Jackson types
- `JsonCodecAdapter.defaultCodec()` configures NON_EMPTY serialization

### Fixed
- http-routing module now uses existing jackson integration instead of direct dependency

## [0.9.10] - 2026-01-07

### Added
- TOML array of tables (`[[section]]`) with full spec compliance
  - Multiple table entries in arrays via repeated `[[name]]` declarations
  - Sub-tables within array elements (`[name.subtable]`)
  - Nested array of tables (`[[parent.child]]`)
  - `TomlDocument.getTableArray(name)` returns `Option<List<Map<String, Object>>>`
  - `TomlDocument.hasTableArray(name)` checks existence
  - `TomlDocument.tableArrayNames()` lists all array table names
  - `TomlError.TableTypeMismatch` for mixing `[x]` and `[[x]]` syntax
- Multi-line basic strings (`"""..."""`) with escape sequence and line-ending backslash support
- Multi-line literal strings (`'''...'''`) with no escape processing
- `TomlError.UnterminatedMultilineString` error type for unclosed multiline strings
- Single-quote literal strings (`'...'`) with no escape processing
- Quoted keys (`"key with spaces"` and `'literal key'`) support including empty keys
- Dotted keys (`server.host = "value"`) that create implicit sections
- Multi-line array support with continuation across lines and inline comment stripping
- Hyphen support in section names (`[my-section]`)
- Unicode escape sequences (`\u03B1` and `\U0001F600`) with surrogate validation
- Backspace (`\b`) and form feed (`\f`) escape sequences
- `TomlError.DuplicateKey` error for detecting duplicate keys
- `TomlError.InvalidEscapeSequence` error for invalid escape sequences (now actively enforced)
- `TomlError.DuplicateSection` error for detecting repeated section headers
- `TomlError.UnsupportedFeature` error for inline tables and date/time values
- `TomlError.InvalidSurrogate` error for invalid Unicode surrogate code points (D800-DFFF)
- `TomlError.DottedKeyConflict` error for detecting value/table conflicts in dotted keys
- Support for underscores in numbers (`1_000_000`, `1_000.5`, `0xDEAD_BEEF`)
- Support for special float values: `inf`, `+inf`, `-inf`, `nan`, `+nan`, `-nan`
- Support for hexadecimal integers (`0xDEADBEEF`)
- Support for octal integers (`0o755`)
- Support for binary integers (`0b11010110`)
- Escape sequence processing for quoted keys (`"key\nwith\nnewlines"`)

### Changed
- Pre-compile regex patterns (FLOAT_PATTERN, INTEGER_PATTERN) for better performance
- Refactor `stripInlineComment` to properly track quote state and bracket depth
- Simplify `MultilineStartResult` to use pattern matching instead of boolean accessors
- Extract shared escape processing method for consistent handling
- Encapsulate multiline parsing state into `MultilineState` regular class (for mutable StringBuilder)
- `MultilineState` changed from record to class to fix JBCT pattern violation (mutable field in record)
- `parseFile()` now uses `Result.lift` for consistent exception handling
- Quote tracking in `isArrayComplete()`, `parseArray()`, and `stripInlineComment()` now correctly distinguishes double vs single quotes

### Fixed
- Comment stripping now correctly preserves `#` inside quoted strings
- Section names with hyphens now parse correctly
- Invalid escape sequences (`\x`, `\q`, `\z`) now produce `TomlError.InvalidEscapeSequence` instead of being silently accepted
- Leading zeros in integers (`007`) now correctly rejected with descriptive error
- Empty quoted keys (`"" = "value"`) now accepted as per TOML spec
- Duplicate section definitions now detected and reported with `TomlError.DuplicateSection`
- Unicode surrogate code points (D800-DFFF) now rejected in escape sequences
- Inline tables and dates now produce explicit "unsupported feature" errors instead of silent failures
- Escape sequences only applied to double-quoted strings, not single-quoted literals
- `putValue()` potential NPE fixed by using `computeIfAbsent` pattern
- Typo fixed: `tablArrays` renamed to `tableArrays` in `TomlDocument`
- `TomlDocument` now makes defensive copies in canonical constructor for true immutability

## [0.9.9] - 2026-01-05

### Added
- `Server.bossGroup()` and `Server.workerGroup()` accessors for EventLoopGroup metrics collection
- `ClusterNetwork.server()` accessor to expose underlying Server instance for metrics

### Changed
- Convert all documentation comments to JEP 467 Markdown format (`///`)

## [0.9.8] - 2026-01-05

### Fixed
- Simplify peer connection null check in `NettyClusterNetwork` to prevent NPE when connecting before server is ready

## [0.9.7] - 2026-01-04

### Added
- Unified TLS configuration API with server, client, and mutual TLS support
  - `TlsConfig.Identity` sealed interface with `SelfSigned` and `FromFiles` variants
  - `TlsConfig.Trust` sealed interface with `SystemDefault`, `FromCaFile`, `InsecureTrustAll` variants
  - `TlsConfig.Server` for server-side TLS with optional client authentication
  - `TlsConfig.Client` for client-side TLS with optional identity
  - `TlsConfig.Mutual` for bidirectional mTLS authentication
- New TLS factory methods:
  - `TlsConfig.selfSignedServer()` - self-signed server certificate
  - `TlsConfig.server(cert, key)` - server from PEM files
  - `TlsConfig.serverWithClientAuth(cert, key, ca)` - server requiring client certs
  - `TlsConfig.client()` - client using system CAs
  - `TlsConfig.clientWithCa(ca)` - client with custom CA
  - `TlsConfig.insecureClient()` - development client trusting all certs
  - `TlsConfig.mutual(cert, key, ca)` - mutual TLS configuration
  - `TlsConfig.selfSignedMutual()` - development mTLS configuration
- `ServerConfig.withClientTls(TlsConfig)` - configure TLS for outgoing connections
- `TlsContextFactory.createServer(TlsConfig)` - create server-side SSL context
- `TlsContextFactory.createClient(TlsConfig)` - create client-side SSL context
- New `TlsError` variants: `PrivateKeyLoadFailed`, `TrustStoreLoadFailed`, `ContextBuildFailed`, `WrongMode`
- Client TLS support in `Server.connectTo()` for secure outgoing connections
- mTLS support in `NettyClusterNetwork` for secure cluster communication
- `NettyClusterNetwork` constructor with `additionalHandlers` parameter for custom pipeline handlers (e.g., metrics)

### Changed
- TCP README updated with comprehensive TLS documentation and examples

### Deprecated
- `TlsConfig.selfSigned()` - use `selfSignedServer()` instead
- `TlsConfig.fromFiles()` - use `server()` instead
- `TlsContextFactory.create()` - use `createServer()` or `createClient()` instead

## [0.9.6] - 2026-01-04

### Added
- `MessageRouter.DelegateRouter` - solves circular dependency when classes need MessageRouter during construction

### Changed
- Remove unnecessary executor from `NettyClusterNetwork.broadcast()` - Netty's `writeAndFlush` is already non-blocking

## [0.9.5] - 2026-01-03

### Added
- Consensus observability and leader query methods
  - `ClusterNetwork.connectedNodeCount()` - real-time connected peer count
  - `LeaderManager.leader()` - query current leader node
  - `LeaderManager.isLeader()` - check if current node is leader

### Fixed
- Leader election consistency - topology now includes self node for deterministic leader selection across all cluster nodes

## [0.9.4] - 2026-01-02

### Added
- TCP Server optimizations for low-latency networking
  - `TCP_NODELAY` socket option enabled by default (disables Nagle's algorithm)
  - `PooledByteBufAllocator` for efficient buffer management
- Consensus Netty network layer
  - `NettyClusterNetwork` - TCP-based cluster networking with Netty
  - Hello protocol handshake for reliable connection identification
  - Configurable hello timeout (default 5 seconds)
  - Channel-to-NodeId tracking (eliminates address-based lookup issues)

## [0.9.3] - 2026-01-01

### Added
- Consensus module (`integrations/consensus`)
  - Rabia consensus protocol implementation
  - Crash-fault-tolerant (CFT) consensus algorithm
  - Batch-based command processing
  - Automatic state synchronization
  - TCP network performance benchmark (~1400 decisions/sec on localhost)

## [0.9.2] - 2025-12-31

### Added
- `KSUID` - K-Sortable Unique Identifier implementation (replaces ULID)
  - 20-byte identifiers: 4-byte timestamp + 16-byte random payload
  - 27-character base62 string representation (lexicographically sortable)
  - `KSUID.ksuid()` - generate new random KSUID
  - `KSUID.parse(String)` - parse from string with Result error handling
  - `KSUID.fromBytes(byte[])` - create from binary with Result error handling
  - `timestamp()` - extract Unix timestamp
  - `encoded()` / `toBytes()` - serialize to string/bytes
  - `KSUIDError` sealed interface for typed parsing errors
- JOOQ Integration (`integrations/db/jooq`)
  - `JooqOperations` - Promise-based JOOQ operations with JDBC
    - `fetchOne()` - fetch single record
    - `fetchOptional()` - fetch optional record
    - `fetch()` - fetch all records
    - `execute()` - execute INSERT/UPDATE/DELETE
  - `JooqTransactional` - transaction management with auto commit/rollback
  - `JooqError` - sealed interface for typed error handling

### Changed
- `IdGenerator.generate()` now uses KSUID instead of ULID

### Removed
- `ULID` class - replaced by KSUID

## [0.9.1] - 2025-12-30

### Changed

- Bumped jbct-maven-plugin to 0.4.2
- Fixed deprecation warnings:
  - `URL(String)` constructor replaced with URI-based parsing in `Network.parseURL()`
  - Jackson `getText()` replaced with `getString()` in `ResultDeserializer`
  - `Result.unwrap()` replaced with `onSuccess()` callbacks in `TomlParser`
- Added `jakarta.xml.bind-api:4.0.4` dependency to jooq-r2dbc module
- Updated dependencies:
  - Netty 4.2.2.Final → 4.2.9.Final
  - Mockito 5.20.0 → 5.21.0
  - Jackson 3.0.0 → 3.0.3 (jackson-annotations 2.20)
  - Micrometer 1.14.2 → 1.16.1
  - jOOQ 3.19.15 → 3.20.10
  - HikariCP 6.2.1 → 7.0.2
  - Hibernate 6.6.4.Final → 7.0.0.Final
  - H2 2.3.232 → 2.4.240

## [0.9.0] - 2025-12-26

### Added

#### TOML Parser Integration
- New module `integrations/config/toml` for zero-dependency TOML parsing
- `TomlParser` - parses TOML content with Result-based error handling
  - Supports sections, properties, quoted/unquoted strings, booleans, integers, arrays, comments
  - `parse(String)` - parse TOML content from string
  - `parseFile(Path)` - parse TOML content from file
- `TomlDocument` - immutable document with typed accessors
  - `getString(section, key)` returns `Option<String>`
  - `getInt(section, key)` returns `Option<Integer>`
  - `getLong(section, key)` returns `Option<Long>`
  - `getBoolean(section, key)` returns `Option<Boolean>`
  - `getStringList(section, key)` returns `Option<List<String>>`
  - `with(section, key, value)` - immutable update
- `TomlError` - sealed interface with typed error causes (SyntaxError, InvalidValue, UnterminatedString, UnterminatedArray, FileReadFailed)

### Changed
- Integration README files updated to use JBCT-compliant factory method names

### Removed

## [0.8.6] - 2025-12-25 (Christmas 2025 Release 🎄)

### Added

#### Result Lazy Sequential Evaluation (Issue #42)
- `Result.sequence(Supplier...)` - lazy/sequential variant of `all()` with short-circuit behavior
  - Evaluates suppliers sequentially, stopping on first failure
  - Returns Mapper1-Mapper15 for type-safe tuple transformation
  - Suppliers are only invoked when terminal operation (map/flatMap) is called
  - Perfect for dependent operations that should fail fast

#### Instance all() Methods for Result, Promise and Option
- Extended existing `Result.all(Fn1...)` from Mapper9 to Mapper15
- `Promise.all(Fn1...)` - instance method for for-comprehension style composition
  - Chains 1-15 dependent operations with access to source Promise value
  - Returns Mapper1-Mapper15 for type-safe tuple transformation
- `Option.all(Fn1...)` - instance method for for-comprehension style composition
  - Chains 1-15 dependent operations with access to source Option value
  - Returns Mapper1-Mapper15 for type-safe tuple transformation
  - Functions not invoked when source Option is empty
- Added `Option.Mapper10`-`Option.Mapper15` interfaces
- Extended `Option.all(Option...)` static methods from 9 to 15 parameters

#### Verify Optional Validation
- `Verify.ensureOption()` methods - validate optional values, succeeding with `Option.none()` if absent
  - Unary predicate: `ensureOption(Option<T>, Predicate<T>)` and variants with Cause/CauseProvider
  - Binary predicate: `ensureOption(Option<T>, Fn2, P1)` and variants with Cause/CauseProvider
  - Ternary predicate: `ensureOption(Option<T>, Fn3, P1, P2)` and variants with Cause/CauseProvider

#### Value Extraction (Issue #40)
- `Result.getOrThrow(String)` - extract value or throw `IllegalStateException` with context message
- `Result.getOrThrow(Fn1, String)` - extract value or throw custom exception
- `Option.getOrThrow(String)` - extract value or throw `IllegalStateException` with context message
- `Option.getOrThrow(Fn1, String)` - extract value or throw custom exception
- Existing `unwrap()` and `expect()` now delegate to `getOrThrow()` in both types

### Removed
- `CircuitBreaker.TimeSource` inner interface - use `org.pragmatica.lang.utils.TimeSource` instead
- `Causes.forValue(String)` - use `forOneValue(String)` instead
- `JsonMapper.readString(String, TypeReference)` - use `readString(String, TypeToken)` instead
- `JsonMapper.readBytes(byte[], TypeReference)` - use `readBytes(byte[], TypeToken)` instead

### Deprecated
- `Verify.ensure(Cause, T, Predicate)` - use `ensure(T, Predicate, Cause)` instead
- `Verify.ensure(Fn1<Cause,T>, T, Predicate)` - use `ensure(T, Predicate, Fn1)` instead
- `Verify.ensure(Cause, T, Fn2, P1)` - use `ensure(T, Fn2, P1, Cause)` instead
- `Verify.ensure(Fn1<Cause,T>, T, Fn2, P1)` - use `ensure(T, Fn2, P1, Fn1)` instead
- `Verify.ensure(Cause, T, Fn3, P1, P2)` - use `ensure(T, Fn3, P1, P2, Cause)` instead
- `Verify.ensure(Fn1<Cause,T>, T, Fn3, P1, P2)` - use `ensure(T, Fn3, P1, P2, Fn1)` instead

## [0.8.5] - 2025-12-16

### Added

#### Core Utilities
- `RateLimiter` - Token Bucket rate-limiting utility
  - Staged fluent builder with `rate()`, `period()`, `burst()` configuration
  - `execute()` - immediate rejection when rate limited
  - `RateLimiterError.LimitExceeded` with `retryAfter` information
  - Thread-safe implementation suitable for concurrent access
- `TimeSource` - shared time abstraction for testability (extracted from CircuitBreaker)

#### Result Aliases (Issue #39, #43)
- `Result.onErr(Consumer)` - alias for `onFailure()`
- `Result.onOk(Consumer)` - alias for `onSuccess()`
- `Result.run(failureConsumer, successConsumer)` - alias for `apply()`
- `Result.tryOf(ThrowingFn0)` - alias for `lift()` with supplier-first signature
- `Result.tryOf(ThrowingFn0, Cause)` - alias for `lift()` with supplier first, cause at end
- `Result.tryOf(ThrowingFn0, Fn1)` - alias for `lift()` with supplier first, exception mapper at end

#### Instance all() Methods (Issue #44)
- `Result.all(Fn1...)` - instance method for for-comprehension style composition
  - Chains 1-9 dependent operations with access to source Result value
  - Returns Mapper1-Mapper9 for type-safe tuple transformation
  - Enables cleaner code without nested flatMaps

#### Verify Aliases (Issue #41)
- `Verify.ensure(T, Predicate, Cause)` - alias with cause at end
- `Verify.ensure(T, Predicate, Fn1<Cause, T>)` - alias with cause provider at end
- `Verify.ensure(T, Fn2, P1, Cause)` - binary predicate alias with cause at end
- `Verify.ensure(T, Fn2, P1, Fn1<Cause, T>)` - binary predicate alias with cause provider at end
- `Verify.ensure(T, Fn3, P1, P2, Cause)` - ternary predicate alias with cause at end
- `Verify.ensure(T, Fn3, P1, P2, Fn1<Cause, T>)` - ternary predicate alias with cause provider at end

### Removed
- `Verify.ensureFn()` methods - removed to simplify API surface (use `ensure()` aliases instead)

## [0.8.4] - 2025-12-13

### Added

#### Core Extensions
- Extended all()/FnXX/Tuple support from 9 to 15 parameters:
  - `Fn10`-`Fn15` functional interfaces
  - `ThrowingFn10`-`ThrowingFn15` throwing variants
  - `Tuple10`-`Tuple15` tuple types with records and factories
  - `Result.all()` overloads for 10-15 parameters with `Mapper10`-`Mapper15`
  - `Promise.all()` overloads for 10-15 parameters with `Mapper10`-`Mapper15`
  - Corresponding `unitFn` overloads for 10-15 parameters

#### HTTP Client Integration
- New module `integrations/net/http-client` for Promise-based HTTP operations
- `HttpOperations` interface abstracting over HTTP client implementations
- `JdkHttpOperations` implementation wrapping JDK HttpClient
- `HttpResult<T>` typed response wrapper with status, headers, and body
- `HttpError` sealed interface with typed error causes:
  - ConnectionFailed, Timeout, RequestFailed, InvalidResponse, HttpFailure
- Zero external dependencies (JDK 11+ only)

#### JDBC Integration
- New module `integrations/db/jdbc` for Promise-based JDBC operations
- `JdbcOperations` interface for common database operations (queryOne, queryOptional, queryList, update, batch)
- `JdbcTransactional` transaction aspect with automatic commit/rollback
- `JdbcError` sealed interface with typed error causes:
  - ConnectionFailed, QueryFailed, ConstraintViolation, Timeout, TransactionRollback, TransactionRequired, DatabaseFailure
- Optional HikariCP integration for connection pooling

#### R2DBC Integration
- New module `integrations/db/r2dbc` for Promise-based reactive database access
- `ReactiveOperations` bridge for converting Reactive Streams Publisher to Promise
- `R2dbcOperations` interface for reactive database operations (queryOne, queryOptional, queryList, update)
- `R2dbcError` sealed interface with typed error causes:
  - ConnectionFailed, QueryFailed, ConstraintViolation, Timeout, NoResult, MultipleResults, DatabaseFailure

#### JOOQ R2DBC Integration
- New module `integrations/db/jooq-r2dbc` for Promise-based JOOQ with R2DBC
- `JooqR2dbcOperations` interface for type-safe JOOQ queries (fetchOne, fetchOptional, fetch, execute)
- `JooqR2dbcTransactional` transaction aspect for reactive transaction management
- Builds on top of R2DBC module with JOOQ 3.19+ support

## [0.8.3] - 2025-10-15

### Added

#### Verify Extensions
- `Verify.ensure(Cause, T, Predicate<T>)` - validation with fixed error cause
- `Verify.ensure(Cause, T, Fn2<...>, P1)` - binary validation with fixed error cause
- `Verify.ensure(Cause, T, Fn3<...>, P1, P2)` - ternary validation with fixed error cause
- `Verify.ensureFn(Cause, Predicate<T>)` - reusable validation function with fixed error cause
- `Verify.ensureFn(Cause, Fn2<...>, P1)` - reusable binary validation function with fixed error cause
- `Verify.ensureFn(Cause, Fn3<...>, P1, P2)` - reusable ternary validation function with fixed error cause
- Comprehensive JavaDoc for all `ensureFn` method variants
- Test coverage for fixed Cause `ensureFn` variants (4 new tests)

### Changed
- `JsonMapper.builder()` → `JsonMapper.jsonMapper()` for JBCT naming consistency
- `JsonMapper.create()` → `JsonMapper.defaultJsonMapper()` for JBCT naming consistency
- `Verify.ensure()` now uses `Causes.forOneValue()` instead of deprecated `Causes.forValue()`

## [0.8.2] - 2025-10-11

### Added

#### Jackson Integration
- Complete Jackson 3.0 API migration (tools.jackson.* package)
- JsonMapper API for Result/Option/Promise serialization with typed error mapping
- TypeToken support as alternative to TypeReference
- Comprehensive test suite (24 tests) for serialization/deserialization
- JBCT-compliant factory method naming (deserializedCause)

#### JPA Integration
- JpaError sealed interface with typed error causes:
  - EntityNotFound, OptimisticLock, PessimisticLock, ConstraintViolation
  - TransactionRequired, EntityExists, QueryTimeout, DatabaseFailure
- JpaOperations functional wrapper for EntityManager with Promise-based operations
- Transactional aspect decorator for transaction management with rollback handling
- Comprehensive test suite (35 tests) using stub pattern for Java 25 sealed interfaces
- Example UserRepository demonstrating adapter leaf pattern

#### Micrometer Integration
- Metrics wrappers for Promise, Result, and Option
- Timer and counter support with tag customization
- Histogram support for execution times

#### Documentation
- Updated CLAUDE.md with testing guidelines (assertInstanceOf, stub pattern)
- Documented unused parameter naming convention (underscore `_`)
- Updated README.md with integration examples and Maven Central instructions

### Changed
- Upgraded Jackson to 3.0.0 GA (with jackson-annotations 3.0-rc5)

### Deprecated
- TypeReference methods in JsonMapper (use TypeToken instead)

### Fixed
- Jackson StreamWriteException mapping to SERIALIZATION_FAILED error
- ResultDeserializer field-order dependency in JSON parsing
- JPA error handling for entity operations

## [0.8.1] - 2025-10-08

### Added
- Parse wrappers for Network APIs (URL, URI, UUID, InetAddress)
- Parse wrappers for Text APIs (Enum, Pattern, Base64)
- Parse wrappers for I18n APIs (Currency, Locale, Charset, ZoneId)
- Standardized documentation format to @param/@return tags

### Changed
- Documentation format across all parse package files

## [0.8.0] - 2025-10-01

Initial release with core functionality:
- Result<T> monad for safe error handling
- Option<T> monad for null safety
- Promise<T> monad for asynchronous operations
- Cause interface for typed errors
- Tuple types (Tuple2-Tuple9)
- Verify utilities for validation
- CircuitBreaker and Retry patterns
- Java 25 support with sealed interfaces
