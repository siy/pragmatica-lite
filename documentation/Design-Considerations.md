# Design Considerations

## General Considerations: Toolkit vs Framework
This document contains ideas and considerations related the design of the Pragmatica toolkit. That's not a typo, strictly speaking, Pragmatica is a toolkit, not a framework. The difference lies in the relationship between toolkit/framework and user code. Framework embeds user code in depth of its own code and calls user code to handle some event (usually a request). Toolkit, on the other hand, is just a set of APIs which are invoked from user code.

Each design has its own pro's and con's. Framework can provide more "magic", especially if "convention over configuration" approach is used. Toolkit in contrast, requires more explicit interaction between user code and toolkit APIs. With proper design, however, there is no bid difference in the amount of code required to implement the same functionality. But resulting code is better structured, transparently navigable and easier to read, maintain and reason about. What is even more important, such a code naturally supports layering and separation between business logic and toolkit. 


## Introduction

Pragmatica, consists of several components with consistent and compatible APIs. Components could be split into three categories: top level functionality, shared components and core.

Top level components are minimal set of building blocks necessary for virtually any backend application:
- HTTP Server,
- HTTP Client,
- Database Client.

Group of shared components contains functionality which is used by all top level components. Of course, these components available fir use by application code as well. Shared components are:
- Configuration - provides access to configuration data in generalized and streamlined way.
- Domain Name Resolver - asynchronous built-in DNS resolver with TTL-based caching.
- JSON serialization/deserialization with pluggable implementation.

At the bottom of this hierarchy there is a core library. It contains basic functional building blocks used by all other components. Since it is a foundation of the whole toolkit, let's start with it. 

### Core 

This component perhaps the main distinction of the Pragmatica from other similar toolkits and frameworks because it uses functional style API. This defines API style of all other components,
The considerations behind this choice are described in the [Pragmatic Functional Java](Pragmatic-Functional-Java.md) document.

### Configuration
This component provides common API to access configuration data in format-agnostic way.  

### Domain Name Resolver
