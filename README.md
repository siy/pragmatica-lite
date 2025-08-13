# Pragmatica Lite

![License](https://img.shields.io/badge/license-Apache%202-blue.svg)

A lightweight Java framework built on modern functional programming principles for robust application development.

## Project Overview

Pragmatica Lite is a Java framework that provides robust error handling, functional programming constructs, and distributed systems capabilities. Built on Java 24, it leverages modern language features to create reliable and maintainable applications.

## Modules

The project is organized into the following modules:

### Core
Contains the essential functional programming abstractions including:
- Result types for safe error handling
- Option types for null-safety
- Promise implementations for asynchronous operations
- Functional interfaces and utilities

### Common
Provides common utilities and shared components used across the framework.

## Features
* Functional style - no NPE, no exceptions, type safety, etc.
* Option<T>/Result<T>/Promise<T> monads with consistent and compatible APIs.
* Simple and convenient to use Promise-based asynchronous API.

## Core Library
The Pragmatica Lite Core contains implementation of basic monads:
`Option<T>`, `Result<T>` and `Promise<T>`. See module [README.md](core/README.md) for more information.

### Cluster
Implementation of distributed clustering layer, based on modern
leaderless Rabia consensus algorithm. See module [README.md](cluster/README.md) for more details.

### Net-Core
Easy to use Netty-based networking layer.

### Examples
Sample applications demonstrating framework usage.

## Getting Started

As for now project packages are hosted at GitHub Packages. 
To use them use the following Maven repository:
```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/siy/pragmatica-lite</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
        <releases>
            <enabled>true</enabled>
        </releases>
    </repository>
</repositories>
```
Then add a necessary dependency:
```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>[MODULE_NAME]</artifactId>
    <version>0.7.6</version>
</dependency>
```
Where `[MODULE_NAME]` is one of the following:
 - core
 - cluster
 - net-core
 - common

## Contributing

Feel free to open pull requests with your changes. 
 
## License 

Pragmatica Lite is licensed under the Apache License, Version 2.0.
