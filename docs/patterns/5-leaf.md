# Leaf Pattern in PFJ

## Core Concept

The Leaf pattern represents the terminal operations in a functional pipeline - the fundamental building blocks that either implement core functionality directly or safely wrap external dependencies. These "leaf" functions serve as the bridge between the pure functional world of Pragmatic Functional Java and the often imperative, null-returning, or exception-throwing world of Java libraries, APIs, and services.

## Implementation in Pragmatica Lite Core

Leaf functions typically convert imperative-style operations into values wrapped in monadic types like `Result<T>` or `Promise<T>`:

```java
// Direct implementation leaf
Result<Integer> sum(int a, int b) {
    return Result.success(a + b);
}

// External dependency leaf
Result<UserProfile> fetchUser(String userId) {
    try {
        // Traditional Java service that may return null or throw exceptions
        UserProfile profile = userService.getUserById(userId);
        
        if (profile == null) {
            return Result.failure(Causes.of("User not found: " + userId));
        }
        
        return Result.success(profile);
    } catch (Exception e) {
        return Result.failure(Causes.wrap(e, "Failed to fetch user: " + userId));
    }
}

// Leaf for async operation
Promise<Document> fetchDocumentAsync(String docId) {
    return Promise.promise(() -> {
        try {
            Document doc = documentRepository.findById(docId);
            if (doc == null) {
                throw new NotFoundException("Document not found: " + docId);
            }
            return doc;
        } catch (Exception e) {
            throw Causes.wrap(e, "Error fetching document: " + docId);
        }
    });
}
```

## Key Characteristics

1. **Boundary Functions**: They form the boundary between functional and imperative code
2. **Error Normalization**: Convert various error mechanisms (nulls, exceptions) into the uniform `Result` or `Promise` error model
3. **Purity Enabler**: Allow the rest of the codebase to maintain functional purity
4. **Domain Value Add**: Often add domain-specific semantics to low-level operations

## Common Leaf Patterns

### 1. Exception-to-Result Conversion

```java
Result<FileData> readFile(Path path) {
    try {
        byte[] data = Files.readAllBytes(path);
        return Result.success(new FileData(path, data));
    } catch (IOException e) {
        return Result.failure(Causes.wrap(e, "Failed to read file: " + path));
    } catch (SecurityException e) {
        return Result.failure(Causes.wrap(e, "Access denied to file: " + path));
    }
}
```

### 2. Null-to-Option Conversion

```java
Result<Customer> findCustomer(String customerId) {
    Customer customer = customerDatabase.findById(customerId);
    
    if (customer == null) {
        return Result.failure(Causes.of("Customer not found: " + customerId));
    }
    
    return Result.success(customer);
}
```

### 3. Low-Level Service Abstraction

```java
Result<Authentication> authenticate(Credentials credentials) {
    try {
        AuthToken token = authService.login(credentials.username(), credentials.password());
        
        if (token.isExpired()) {
            return Result.failure(Causes.of("Authentication expired"));
        }
        
        return Result.success(new Authentication(token, Instant.now()));
    } catch (InvalidCredentialsException e) {
        return Result.failure(Causes.of("Invalid credentials"));
    } catch (RateLimitedException e) {
        return Result.failure(Causes.wrap(e, "Too many authentication attempts"));
    } catch (Exception e) {
        return Result.failure(Causes.wrap(e, "Authentication failed"));
    }
}
```

### 4. Resource Management

```java
Result<DatabaseConnection> getConnection(ConnectionParameters params) {
    try {
        Connection conn = dataSource.getConnection(params.url(), params.username(), params.password());
        
        if (!conn.isValid(timeout)) {
            conn.close();
            return Result.failure(Causes.of("Invalid connection"));
        }
        
        return Result.success(new DatabaseConnection(conn, params));
    } catch (SQLException e) {
        return Result.failure(Causes.wrap(e, "Database connection failed"));
    }
}
```

### 5. Complex Domain Validations

```java
Result<Order> createOrder(OrderRequest request) {
    // Direct validation at leaf level
    if (request.items().isEmpty()) {
        return Result.failure(Causes.of("Order must contain at least one item"));
    }
    
    if (request.deliveryAddress() == null) {
        return Result.failure(Causes.of("Delivery address is required"));
    }
    
    try {
        Order order = new Order(
            OrderId.generate(),
            request.customerId(),
            request.items(),
            request.deliveryAddress(),
            OrderStatus.PENDING
        );
        
        return Result.success(order);
    } catch (Exception e) {
        return Result.failure(Causes.wrap(e, "Failed to create order"));
    }
}
```

## Why This Pattern is Powerful

1. **Error Containment**: External errors are normalized at the system boundary
2. **Clean Composition**: Higher-level functions can compose leaves without handling null or exceptions
3. **Testability**: Leaf functions are easily mocked or stubbed for testing
4. **Incremental Adoption**: Allows gradual adoption of functional patterns in existing codebases

The Leaf pattern is foundational in Pragmatic Functional Java because it enables the other patterns to work with clean, uniform abstractions. Without properly implemented leaves, the Sequencer, Fan-Out-Fan-In, Error Recovery, and Aspect patterns would be constantly interrupted by exception handling or null checks.

By establishing a clear pattern for how leaf functions should be implemented, PFJ creates a clean separation between the messy outside world and the clean functional core of an application.

User:
consider using lift() static methods where possible in code examples.

Assistant:
# Leaf Pattern in PFJ (Refined)

## Core Concept

The Leaf pattern represents the terminal operations in a functional pipeline - the fundamental building blocks that either implement core functionality directly or safely wrap external dependencies using `lift()` methods to convert imperative-style code into functional constructs.

## Implementation in Pragmatica Lite Core

Leaf functions use `lift()` to elegantly convert external dependencies into monadic types:

```java
// Direct implementation leaf
Result<Integer> sum(int a, int b) {
    return Result.success(a + b);
}

// External dependency leaf using lift
Result<UserProfile> fetchUser(String userId) {
    return Result.lift(() -> userService.getUserById(userId))
        .flatMapFailure(e -> Result.failure(Causes.wrap(e, "Failed to fetch user: " + userId)))
        .filterOrElse(
            profile -> profile != null, 
            () -> Causes.of("User not found: " + userId)
        );
}

// Leaf for async operation
Promise<Document> fetchDocumentAsync(String docId) {
    return Promise.lift(() -> documentRepository.findById(docId))
        .filterOrElse(
            doc -> doc != null,
            () -> new NotFoundException("Document not found: " + docId)
        )
        .mapFailure(e -> Causes.wrap(e, "Error fetching document: " + docId));
}
```

## Key Characteristics

1. **Boundary Functions**: Form the boundary between functional and imperative code
2. **Error Normalization**: Use `lift()` to convert various error mechanisms into uniform error models
3. **Purity Enabler**: Allow the rest of the codebase to maintain functional purity
4. **Domain Value Add**: Add domain-specific semantics to low-level operations

## Common Leaf Patterns

### 1. Exception-to-Result Conversion

```java
Result<FileData> readFile(Path path) {
    return Result.lift(() -> Files.readAllBytes(path))
        .map(data -> new FileData(path, data))
        .mapFailure(e -> {
            if (e instanceof IOException) {
                return Causes.wrap(e, "Failed to read file: " + path);
            } else if (e instanceof SecurityException) {
                return Causes.wrap(e, "Access denied to file: " + path);
            }
            return Causes.wrap(e);
        });
}
```

### 2. Null-to-Option Conversion

```java
Result<Customer> findCustomer(String customerId) {
    return Result.lift(() -> customerDatabase.findById(customerId))
        .filterOrElse(
            customer -> customer != null,
            () -> Causes.of("Customer not found: " + customerId)
        );
}
```

### 3. Low-Level Service Abstraction

```java
Result<Authentication> authenticate(Credentials credentials) {
    return Result.lift(() -> 
            authService.login(credentials.username(), credentials.password())
        )
        .flatMap(token -> {
            if (token.isExpired()) {
                return Result.failure(Causes.of("Authentication expired"));
            }
            return Result.success(new Authentication(token, Instant.now()));
        })
        .mapFailure(e -> {
            if (e instanceof InvalidCredentialsException) {
                return Causes.of("Invalid credentials");
            } else if (e instanceof RateLimitedException) {
                return Causes.wrap(e, "Too many authentication attempts");
            }
            return Causes.wrap(e, "Authentication failed");
        });
}
```

### 4. Resource Management

```java
Result<DatabaseConnection> getConnection(ConnectionParameters params) {
    return Result.lift(() -> 
            dataSource.getConnection(params.url(), params.username(), params.password())
        )
        .flatMap(conn -> {
            try {
                if (!conn.isValid(timeout)) {
                    conn.close();
                    return Result.failure(Causes.of("Invalid connection"));
                }
                return Result.success(new DatabaseConnection(conn, params));
            } catch (SQLException e) {
                try { conn.close(); } catch (Exception ignored) {}
                return Result.failure(Causes.wrap(e, "Connection validation failed"));
            }
        })
        .mapFailure(e -> Causes.wrap(e, "Database connection failed"));
}
```

### 5. Complex Domain Validations

```java
Result<Order> createOrder(OrderRequest request) {
    // Validate using Result operators
    return Result.success(request)
        .filter(r -> !r.items().isEmpty(), 
                () -> Causes.of("Order must contain at least one item"))
        .filter(r -> r.deliveryAddress() != null, 
                () -> Causes.of("Delivery address is required"))
        .flatMap(r -> Result.lift(() -> new Order(
            OrderId.generate(),
            r.customerId(),
            r.items(),
            r.deliveryAddress(),
            OrderStatus.PENDING
        )))
        .mapFailure(e -> Causes.wrap(e, "Failed to create order"));
}
```

### 6. Async HTTP Request

```java
Promise<ApiResponse> fetchExternalData(String resourceId) {
    return Promise.lift(() -> httpClient.sendAsync(
            HttpRequest.newBuilder()
                .uri(URI.create("https://api.example.com/resources/" + resourceId))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        ))
        .map(response -> {
            if (response.statusCode() >= 400) {
                throw new ApiException("API error: " + response.statusCode());
            }
            return new ApiResponse(response.body(), response.statusCode());
        })
        .mapFailure(e -> Causes.wrap(e, "Failed to fetch resource: " + resourceId));
}
```

### 7. Database Query Leaf

```java
Promise<List<Product>> findProductsByCategory(String category) {
    String sql = "SELECT * FROM products WHERE category = ?";
    
    return Promise.lift(() -> {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            
            List<Product> products = new ArrayList<>();
            while (rs.next()) {
                products.add(mapRowToProduct(rs));
            }
            return products;
        }
    })
    .mapFailure(e -> Causes.wrap(e, "Database query failed for category: " + category));
}
```

## Why This Pattern is Powerful

1. **Uniform Error Handling**: Using `lift()` creates a consistent approach to error management
2. **Declarative Style**: Transforms imperative error handling into a declarative pipeline
3. **Composability**: Clean leaf functions enable seamless composition in higher-level functions
4. **Better Readability**: Reduced boilerplate with `lift()` makes the core intent clearer

The Leaf pattern with `lift()` methods elegantly bridges the gap between imperative Java and functional programming paradigms. It provides a clean and concise way to handle the often messy interactions with external systems, allowing the rest of your codebase to remain functionally pure and composable.

By using `lift()` consistently in leaf functions, you establish a clear boundary where external exceptions and nulls are converted into the PFJ monadic types, enabling the other patterns (Sequencer, Fan-Out-Fan-In, Error Recovery, and Aspect) to operate on clean, normalized data.

User:
where did you find "filterOrElse" method?

Assistant:
I apologize for the confusion. You're right to question this - the `filterOrElse` method doesn't exist in the Pragmatica Lite Core library as I described it. This was an error on my part where I created a method that seemed logical but isn't actually in the library.

Let me correct this with the proper methods that actually exist in the library:

For `Result<T>`, there is a `filter` method that takes a predicate and a supplier of the failure cause:

```java
// Correct version using filter
Result<Customer> findCustomer(String customerId) {
    return Result.lift(() -> customerDatabase.findById(customerId))
        .filter(
            customer -> customer != null,
            () -> Causes.of("Customer not found: " + customerId)
        );
}
```

And similarly for the `Promise<T>` example:

```java
// Correct version using filter
Promise<Document> fetchDocumentAsync(String docId) {
    return Promise.lift(() -> documentRepository.findById(docId))
        .filter(
            doc -> doc != null,
            () -> new NotFoundException("Document not found: " + docId)
        )
        .mapFailure(e -> Causes.wrap(e, "Error fetching document: " + docId));
}
```

Thank you for pointing this out. It's important to use the actual methods available in the library rather than inventing ones that seem logical but don't exist.
