# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.8.6] - 2025-12-25

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
