# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
