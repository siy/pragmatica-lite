## URL Shortener Example

The example is inspired by series of articled published
on [Medium](https://medium.com/deno-the-complete-reference/url-shortener-service-benchmarking-quarkus-vs-rust-actix-606d46fff88b).
Note that only creation of short URLs is implemented. Nevertheless, the goal was not to build shortest/fastest URL
shortener, but to demonstrate recommended design approach for the backend applications with Pragmatica toolkit.

### Design
Application is divided into three parts: 
 - Controller and relevant objects - HTTP API layer
 - Service and relevant entities and objects - business logic layer
 - Repository - persistence layer

Note that Repository re-uses the same entity as Service.

### Running
The application requires PostgreSQL database configured as follows:

