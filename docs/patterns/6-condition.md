# Condition Pattern in PFJ

## Core Concept

The Condition pattern provides a functional approach to branching logic, leveraging Java's expression-based conditional constructs (ternary operator and switch expressions) rather than statement-based control flow. This pattern ensures that conditions return values rather than causing side effects, maintaining functional purity.

## Implementation in Pragmatica Lite Core

The Condition pattern leverages monadic types to create composable and expressive conditional flows:

```java
// Basic conditional using monadic map
Result<PaymentStatus> processPayment(Payment payment) {
    return validatePayment(payment)
        .map(validPayment -> 
            validPayment.amount() > 1000 
                ? PaymentStatus.REQUIRES_APPROVAL  // Condition as expression
                : PaymentStatus.APPROVED
        );
}

// Using flatMap for conditional paths that may fail
Result<Receipt> handleOrder(Order order) {
    return validateOrder(order)
        .flatMap(validOrder -> 
            validOrder.isPrePaid()
                ? generateReceipt(validOrder)  // This returns Result<Receipt>
                : processPayment(validOrder)
                    .flatMap(paymentConfirmation -> 
                        generateReceipt(validOrder, paymentConfirmation)
                    )
        );
}
```

## Key Characteristics

1. **Expression-Based**: Always produces a value rather than performing side effects
2. **Compositional**: Conditions easily compose with other functional patterns
3. **Type Safety**: Ensures all conditional branches return the same type
4. **Explicit Error Handling**: Makes failure cases explicit and composable

## Common Condition Patterns

### 1. Ternary Operator for Simple Conditions

```java
Result<PricingTier> determinePricingTier(Customer customer) {
    return Result.success(
        customer.isPremium() 
            ? PricingTier.PREMIUM 
            : customer.getLoyaltyYears() > 5 
                ? PricingTier.LOYAL 
                : PricingTier.STANDARD
    );
}
```

### 2. Switch Expressions for Multi-Path Conditions

```java
Result<ShippingOption> determineShippingOption(Order order) {
    return Result.success(
        switch(order.getDeliveryRegion()) {
            case DOMESTIC -> 
                order.isExpedited() 
                    ? ShippingOption.EXPRESS_DOMESTIC 
                    : ShippingOption.STANDARD_DOMESTIC;
            case CONTINENTAL -> 
                order.getTotalWeight() > 10 
                    ? ShippingOption.FREIGHT_CONTINENTAL 
                    : ShippingOption.STANDARD_CONTINENTAL;
            case INTERNATIONAL -> 
                order.hasRestricted() 
                    ? ShippingOption.SPECIAL_HANDLING_INTERNATIONAL 
                    : ShippingOption.STANDARD_INTERNATIONAL;
        }
    );
}
```

### 3. Pattern Matching for Type-Based Conditions (Java 17+)

```java
Result<TaxCalculation> calculateTax(Transaction transaction) {
    return Result.lift(() ->
        switch(transaction) {
            case ProductPurchase p -> 
                new ProductTaxCalculation(p.getAmount(), p.getCategory().getTaxRate());
            case ServicePurchase s -> 
                new ServiceTaxCalculation(s.getAmount(), s.isB2B());
            case Subscription s -> 
                new SubscriptionTaxCalculation(s.getMonthlyAmount(), s.getDurationMonths());
            default -> 
                throw new UnsupportedTransactionException("Unknown transaction type");
        }
    );
}
```

### 4. Conditional Sequencing with Monadic Types

```java
Promise<AccountStatus> checkAccountStatus(String accountId) {
    return accountRepository.findById(accountId)
        .filter(
            account -> account != null,
            () -> new AccountNotFoundException("Account not found: " + accountId)
        )
        .flatMap(account -> 
            account.isLocked() 
                ? Promise.success(AccountStatus.LOCKED)
                : account.getBalance() < 0 
                    ? Promise.success(AccountStatus.OVERDRAWN)
                    : retrieveRecentTransactions(account)
                        .map(transactions -> 
                            transactions.size() > 10 
                                ? AccountStatus.ACTIVE 
                                : AccountStatus.INACTIVE
                        )
        );
}
```

### 5. Functional Guard Clauses

```java
Result<Invoice> generateInvoice(Order order, Customer customer) {
    // Early returns using monadic composition instead of guard clauses
    return Result.success(order)
        .filter(
            o -> !o.getItems().isEmpty(),
            () -> Causes.of("Cannot generate invoice for empty order")
        )
        .filter(
            o -> customer.getPaymentMethods().size() > 0,
            () -> Causes.of("Customer has no payment methods")
        )
        .filter(
            o -> !customer.hasPaymentRestrictions(),
            () -> Causes.of("Customer has payment restrictions")
        )
        .flatMap(validOrder -> 
            invoiceService.createInvoice(validOrder, customer)
        );
}
```

### 6. Complex Decision Trees with Monadic Methods

```java
Result<ShipmentStatus> processFulfillment(Order order) {
    return inventoryService.checkAvailability(order.getItems())
        .flatMap(availability -> {
            if (availability.allAvailable()) {
                return warehouseService.createShipment(order)
                    .map(shipment -> new ShipmentStatus(ShipmentState.PROCESSING, shipment));
            } else if (availability.partiallyAvailable() && order.allowsPartialShipment()) {
                return warehouseService.createPartialShipment(order, availability.getAvailableItems())
                    .flatMap(partialShipment -> 
                        backorderService.createBackorder(order, availability.getUnavailableItems())
                            .map(backorder -> new ShipmentStatus(ShipmentState.PARTIAL, partialShipment, backorder))
                    );
            } else {
                return backorderService.createBackorder(order, order.getItems())
                    .map(backorder -> new ShipmentStatus(ShipmentState.BACKORDERED, backorder));
            }
        });
}
```

### 7. Conditional Aspect Application

```java
Promise<Document> getDocument(String documentId, User user) {
    Promise<Document> baseOperation = documentRepository.findById(documentId)
        .filter(
            doc -> doc != null,
            () -> new DocumentNotFoundException("Document not found: " + documentId)
        );
    
    // Conditional aspect application
    return user.isAdmin()
        ? baseOperation  // No additional aspects for admins
        : user.hasPremiumAccess()
            ? cacheAspect("doc:" + documentId).apply(baseOperation)
            : rateLimitAspect(user.getId())
                .andThen(cacheAspect("doc:" + documentId))
                .apply(baseOperation);
}
```

## Integration with Other Patterns

The Condition pattern works seamlessly with other PFJ patterns:

1. **With Sequencer**: Conditions determine which sequence path to follow
2. **With Fan-Out-Fan-In**: Different fan-out operations based on conditions
3. **With Leaf**: Conditional error handling for external operations
4. **With Aspect**: Applying different aspects based on conditions

```java
// Condition with Sequencer
Result<OrderConfirmation> processOrder(Order order) {
    return Sequencer.start(order)
        .then(this::validateOrder)
        .then(validOrder -> 
            validOrder.isPrePaid()
                ? Sequencer.next(validOrder)  // Skip payment for pre-paid orders
                : Sequencer.next(validOrder)
                    .then(this::processPayment)
        )
        .then(this::allocateInventory)
        .then(this::generateConfirmation)
        .end();
}
```

## Why This Pattern is Powerful

1. **Expressiveness**: Conditions as expressions make intent clearer
2. **Composition**: Allows building complex decision trees functionally
3. **Error Handling**: Integrated with the Result/Promise error model
4. **Reduced Complexity**: Eliminates mutable state often needed in imperative conditionals
5. **Testability**: Pure functions with explicit inputs and outputs are easier to test

The Condition pattern transforms traditional imperative branching into a functional paradigm, ensuring that business decisions are expressed as values rather than control flow, which makes your code more predictable, testable, and composable.

User:
In examples, please, replace inner ternary operators with method calls. Using ternary operators here means that we have 2 layers of abstractions in the code. One of the core concepts of the whole approach is the single level of abstraction inside each method/function. Therefore, each function implements exactly one of the patterns, nesting several patterns inside one method/function is highly discouraged.

Assistant:
# Condition Pattern in PFJ (Revised)

## Core Concept

The Condition pattern provides a functional approach to branching logic, leveraging Java's expression-based conditional constructs (ternary operator and switch expressions) rather than statement-based control flow. This pattern ensures that conditions return values rather than causing side effects, maintaining functional purity.

A key principle is maintaining a single level of abstraction within each method, with each function implementing exactly one pattern.

## Implementation in Pragmatica Lite Core

The Condition pattern leverages monadic types to create composable and expressive conditional flows:

```java
// Basic conditional using monadic map - single level of abstraction
Result<PaymentStatus> processPayment(Payment payment) {
    return validatePayment(payment)
        .map(validPayment -> 
            validPayment.amount() > 1000 
                ? PaymentStatus.REQUIRES_APPROVAL
                : PaymentStatus.APPROVED
        );
}

// Using flatMap for conditional paths that may fail - each branch is a function call
Result<Receipt> handleOrder(Order order) {
    return validateOrder(order)
        .flatMap(validOrder -> 
            validOrder.isPrePaid()
                ? generateReceiptForPrepaid(validOrder)
                : generateReceiptForStandardPayment(validOrder)
        );
}

// Helper methods maintain single level of abstraction
private Result<Receipt> generateReceiptForPrepaid(Order order) {
    return generateReceipt(order);
}

private Result<Receipt> generateReceiptForStandardPayment(Order order) {
    return processPayment(order)
        .flatMap(paymentConfirmation -> generateReceipt(order, paymentConfirmation));
}
```

## Key Characteristics

1. **Expression-Based**: Always produces a value rather than performing side effects
2. **Single Level of Abstraction**: Each method implements exactly one pattern
3. **Compositional**: Conditions easily compose with other functional patterns
4. **Type Safety**: Ensures all conditional branches return the same type
5. **Explicit Error Handling**: Makes failure cases explicit and composable

## Common Condition Patterns

### 1. Simple Condition with Single Level of Abstraction

```java
Result<PricingTier> determinePricingTier(Customer customer) {
    return Result.success(
        customer.isPremium() 
            ? PricingTier.PREMIUM 
            : determineTierBasedOnLoyalty(customer)
    );
}

private PricingTier determineTierBasedOnLoyalty(Customer customer) {
    return customer.getLoyaltyYears() > 5 
        ? PricingTier.LOYAL 
        : PricingTier.STANDARD;
}
```

### 2. Switch Expressions with Function Extraction

```java
Result<ShippingOption> determineShippingOption(Order order) {
    return Result.success(
        switch(order.getDeliveryRegion()) {
            case DOMESTIC -> determineDomesticShipping(order);
            case CONTINENTAL -> determineContinetalShipping(order);
            case INTERNATIONAL -> determineInternationalShipping(order);
        }
    );
}

private ShippingOption determineDomesticShipping(Order order) {
    return order.isExpedited() 
        ? ShippingOption.EXPRESS_DOMESTIC 
        : ShippingOption.STANDARD_DOMESTIC;
}

private ShippingOption determineContinetalShipping(Order order) {
    return order.getTotalWeight() > 10 
        ? ShippingOption.FREIGHT_CONTINENTAL 
        : ShippingOption.STANDARD_CONTINENTAL;
}

private ShippingOption determineInternationalShipping(Order order) {
    return order.hasRestricted() 
        ? ShippingOption.SPECIAL_HANDLING_INTERNATIONAL 
        : ShippingOption.STANDARD_INTERNATIONAL;
}
```

### 3. Pattern Matching with Extracted Methods

```java
Result<TaxCalculation> calculateTax(Transaction transaction) {
    return Result.lift(() ->
        switch(transaction) {
            case ProductPurchase p -> calculateProductTax(p);
            case ServicePurchase s -> calculateServiceTax(s);
            case Subscription s -> calculateSubscriptionTax(s);
            default -> handleUnsupportedTransaction(transaction);
        }
    );
}

private TaxCalculation calculateProductTax(ProductPurchase purchase) {
    return new ProductTaxCalculation(purchase.getAmount(), purchase.getCategory().getTaxRate());
}

private TaxCalculation calculateServiceTax(ServicePurchase purchase) {
    return new ServiceTaxCalculation(purchase.getAmount(), purchase.isB2B());
}

private TaxCalculation calculateSubscriptionTax(Subscription subscription) {
    return new SubscriptionTaxCalculation(subscription.getMonthlyAmount(), subscription.getDurationMonths());
}

private TaxCalculation handleUnsupportedTransaction(Transaction transaction) {
    throw new UnsupportedTransactionException("Unknown transaction type");
}
```

### 4. Conditional Sequencing with Extracted Functions

```java
Promise<AccountStatus> checkAccountStatus(String accountId) {
    return accountRepository.findById(accountId)
        .filter(
            account -> account != null,
            () -> new AccountNotFoundException("Account not found: " + accountId)
        )
        .flatMap(account -> 
            account.isLocked() 
                ? Promise.success(AccountStatus.LOCKED)
                : determineAccountStatusByState(account)
        );
}

private Promise<AccountStatus> determineAccountStatusByState(Account account) {
    return account.getBalance() < 0 
        ? Promise.success(AccountStatus.OVERDRAWN)
        : determineAccountStatusByActivity(account);
}

private Promise<AccountStatus> determineAccountStatusByActivity(Account account) {
    return retrieveRecentTransactions(account)
        .map(this::categorizeAccountByTransactionVolume);
}

private AccountStatus categorizeAccountByTransactionVolume(List<Transaction> transactions) {
    return transactions.size() > 10 
        ? AccountStatus.ACTIVE 
        : AccountStatus.INACTIVE;
}
```

### 5. Complex Decision Tree with Single Level of Abstraction

```java
Result<ShipmentStatus> processFulfillment(Order order) {
    return inventoryService.checkAvailability(order.getItems())
        .flatMap(availability -> {
            if (availability.allAvailable()) {
                return createFullShipment(order);
            } else if (availability.partiallyAvailable() && order.allowsPartialShipment()) {
                return createPartialShipmentWithBackorder(order, availability);
            } else {
                return createFullBackorder(order);
            }
        });
}

private Result<ShipmentStatus> createFullShipment(Order order) {
    return warehouseService.createShipment(order)
        .map(shipment -> new ShipmentStatus(ShipmentState.PROCESSING, shipment));
}

private Result<ShipmentStatus> createPartialShipmentWithBackorder(Order order, Availability availability) {
    return warehouseService.createPartialShipment(order, availability.getAvailableItems())
        .flatMap(partialShipment -> 
            backorderService.createBackorder(order, availability.getUnavailableItems())
                .map(backorder -> new ShipmentStatus(ShipmentState.PARTIAL, partialShipment, backorder))
        );
}

private Result<ShipmentStatus> createFullBackorder(Order order) {
    return backorderService.createBackorder(order, order.getItems())
        .map(backorder -> new ShipmentStatus(ShipmentState.BACKORDERED, backorder));
}
```

### 6. Conditional Aspect Application with Extracted Functions

```java
Promise<Document> getDocument(String documentId, User user) {
    Promise<Document> baseOperation = fetchDocumentById(documentId);
    
    return user.isAdmin()
        ? baseOperation  // No additional aspects for admins
        : applyUserSpecificAspects(baseOperation, user);
}

private Promise<Document> fetchDocumentById(String documentId) {
    return documentRepository.findById(documentId)
        .filter(
            doc -> doc != null,
            () -> new DocumentNotFoundException("Document not found: " + documentId)
        );
}

private Promise<Document> applyUserSpecificAspects(Promise<Document> operation, User user) {
    return user.hasPremiumAccess()
        ? applyCacheAspect(operation, user)
        : applyRateLimitAndCacheAspects(operation, user);
}

private Promise<Document> applyCacheAspect(Promise<Document> operation, User user) {
    return cacheAspect("doc:" + user.getId()).apply(operation);
}

private Promise<Document> applyRateLimitAndCacheAspects(Promise<Document> operation, User user) {
    return rateLimitAspect(user.getId())
        .andThen(cacheAspect("doc:" + user.getId()))
        .apply(operation);
}
```

### 7. Conditional With Result Transformation

```java
Result<UserResponse> processUserRequest(UserRequest request) {
    return validateRequest(request)
        .flatMap(validRequest -> 
            validRequest.isAdminRequest() 
                ? processAdminRequest(validRequest)
                : processStandardRequest(validRequest)
        );
}

private Result<UserResponse> processAdminRequest(UserRequest request) {
    return adminService.executePrivilegedOperation(request)
        .map(result -> UserResponse.withAdminPrivileges(result));
}

private Result<UserResponse> processStandardRequest(UserRequest request) {
    return userService.executeStandardOperation(request)
        .map(result -> UserResponse.standard(result));
}
```

## Integration with Other Patterns

The Condition pattern works seamlessly with other PFJ patterns, maintaining single level of abstraction:

```java
// Condition with Sequencer - single level of abstraction
Result<OrderConfirmation> processOrder(Order order) {
    return Sequencer.start(order)
        .then(this::validateOrder)
        .then(validOrder -> 
            validOrder.isPrePaid()
                ? processPrePaidOrder(validOrder)
                : processStandardOrder(validOrder)
        )
        .end();
}

private Sequencer<Order, Order> processPrePaidOrder(Order order) {
    return Sequencer.next(order)
        .then(this::allocateInventory)
        .then(this::generateConfirmation);
}

private Sequencer<Order, Order> processStandardOrder(Order order) {
    return Sequencer.next(order)
        .then(this::processPayment)
        .then(this::allocateInventory)
        .then(this::generateConfirmation);
}
```

## Why This Pattern is Powerful

1. **Clean Abstraction Levels**: Each method maintains a single level of abstraction
2. **Improved Readability**: Breaking complex conditions into named functions clarifies intent
3. **Better Testability**: Smaller functions with clear responsibilities are easier to test
4. **Reduced Complexity**: Function extraction prevents deep nesting of conditions
5. **Enhanced Maintainability**: Changes to business logic are isolated to specific functions

The Condition pattern in PFJ emphasizes maintaining a single level of abstraction in each method, promoting code organization where complex decision trees are broken down into well-named helper methods. This approach significantly improves readability and maintainability while preserving the functional composition benefits.

User:
check example for 5: flatMap parameter is second level of abstraction too. Single level of abstraction is applied everywhere in function implementation.

Assistant:
You're absolutely right. Let me correct example #5 to ensure we maintain a strict single level of abstraction throughout all examples, including the `flatMap` implementation.

# Revised Example 5: Complex Decision Tree with Single Level of Abstraction

```java
Result<ShipmentStatus> processFulfillment(Order order) {
    return inventoryService.checkAvailability(order.getItems())
        .flatMap(availability -> decideFulfillmentStrategy(order, availability));
}

private Result<ShipmentStatus> decideFulfillmentStrategy(Order order, Availability availability) {
    if (availability.allAvailable()) {
        return createFullShipment(order);
    } else if (availability.partiallyAvailable() && order.allowsPartialShipment()) {
        return createPartialShipmentWithBackorder(order, availability);
    } else {
        return createFullBackorder(order);
    }
}

private Result<ShipmentStatus> createFullShipment(Order order) {
    return warehouseService.createShipment(order)
        .map(shipment -> new ShipmentStatus(ShipmentState.PROCESSING, shipment));
}

private Result<ShipmentStatus> createPartialShipmentWithBackorder(Order order, Availability availability) {
    return warehouseService.createPartialShipment(order, availability.getAvailableItems())
        .flatMap(partialShipment -> createBackorderForPartialShipment(order, availability, partialShipment));
}

private Result<ShipmentStatus> createBackorderForPartialShipment(
        Order order, 
        Availability availability, 
        Shipment partialShipment) {
    return backorderService.createBackorder(order, availability.getUnavailableItems())
        .map(backorder -> new ShipmentStatus(ShipmentState.PARTIAL, partialShipment, backorder));
}

private Result<ShipmentStatus> createFullBackorder(Order order) {
    return backorderService.createBackorder(order, order.getItems())
        .map(backorder -> new ShipmentStatus(ShipmentState.BACKORDERED, backorder));
}
```

Thank you for pointing this out. This revision now properly maintains a single level of abstraction everywhere, including inside the `flatMap` operation. Each method has exactly one responsibility and implements a single pattern, making the code more maintainable and easier to reason about.

The key change was extracting the condition logic from inside the `flatMap` parameter into its own dedicated method `decideFulfillmentStrategy`, and further extracting the nested transformation in the partial shipment case to its own method `createBackorderForPartialShipment`. This way, each method follows the single level of abstraction principle.
