# Pragmatic Functional Java (PFJ) Coding Style Library: Pragmatica Lite Core

The **Pragmatica Lite Core** library provides three main monads (`Option<T>`, `Result<T>` and `Promise<T>`) and a few utility classes to make common use cases more convenient.

## Motivation

Main considerations for the appearance of the PFJ as an approach are described in this [article](https://medium.com/codex/we-should-write-java-code-differently-c32212152af1). The PFJ style in more detail is described [here](https://medium.com/codex/introduction-to-pragmatic-functional-java-md-fba6bdaae6a8).

## Features

The library provides three main monads:
    - `Option<T>` - a monad that represents an optional (i.e. potentially missing) value. _(synchronous)_
    - `Result<T>` - a monad that represents a computation that may fail. _(synchronous)_
    - `Promise<T>` - a monad that represents a computation that may fail. _(asynchronous)_

All monads are designed to be compatible with each other and provide a consistent API. Methods for transformation of one monad to another are also provided.

### Option&lt;T>

The `Option<T>` monad represents an optional value. It should be used for all public APIs to represent a potentially missing value. This applies to all cases when such values may appear - method return value, method parameters and class/record fields.
The `Option<T>` is designed to support pattern matching. There are two implementations of `Option<T>`: `Some<T>` and `None<T>`. For memory optimization, the `None<T>` implementation is a singleton.

Basic usage looks like this:

```java
var name = Option.option(methodThatMayReturnNull())
                 .map(String::toUpperCase);
```
Now `name` contains potentially missing value. If value is present, then it is converted to upper case. At some point in the code usually it is necessary to make a decision, what to do with value stored in the `Option<T>`. There are several ways to do this:

#### Using `.or` method(s)

There are two `.or` methods which can be used to provide a default value in case when value is missing.

First one usually is convenient when default value is a constant or it's computation is not expensive:
```java
var name = Option.option(methodThatMayReturnNull())
                 .map(String::toUpperCase)
                 .or("default name");
```
The second one is more flexible and can be used when default value is expensive to compute:
```java
var name = Option.option(methodThatMayReturnNull())
                 .map(String::toUpperCase)
                 .or(() -> expensiveComputation());
```

#### Using pattern matching

As mentioned above, the `Option<T>` monad can be either `Some<T>` or `None<T>`:

```java
var name = Option.option(methodThatMayReturnNull());

var uppercaseName = switch(name) {
    case Some<String> some -> some.value().toUpperCase();
    case None<String> _ -> "default name";
};
```

### Result&lt;T>

This monad is a main tool for handling errors in the code. It should be used for all methods that may fail. The `Result<T>` contains either, the the value obtained from the computation, or an error. The error must be an instance implementing `Cause` interface. This interface in some sense serves purpose similar to `Throwable` class and is a root for all possible errors in the application. For convenience, there is `Causes` utility interface, which provides convenience methods for creating `Cause` instances in simple cases. For more complex cases, it is recommended to create custom implementations of `Cause` interface (see [corresponding section](#application-specific-errors)). 

Basic usage looks like this:

```java
Result<String> method(String value) {
    if (value.isEmpty()) {
        return Result.failure(AppErrors.VALUE_IS_EMPTY);
    }
    return Result.success(value);
}
```
The `Result<T>` monad is designed to support pattern matching. There are two implementations of `Result<T>`: `Success<T>` and `Failure<T>`.

#### Transformation Pipeline
One of the most frequent use cases for `Result<T>` is to chain several computations, where some of them may fail. The `Result<T>` monad provides a convenient way to do this:

```java
Result<SomeType5> method(SomeType1 inputParameter) {
    return method1(inputParameter)
        .flatMap(value1 -> fallibleComputation1(value1))
        .map(value2 -> computation2(value2))
        .flatMap(value3 -> fallibleComputation2(value3));
}
```
It is highly recommended to extract computations into dedicated methods and use method references:

```java
Result<SomeType5> method(SomeType1 inputParameter) {
    return method1(inputParameter)
        .flatMap(this::fallibleComputation1)
        .map(this::computation2)
        .flatMap(this::fallibleComputation2);
}

Result<SomeType3> fallibleComputation1(SomeType2 value) {
    //...
}

SomeType4 computation2(SomeType3 value) {
    //...
}

Result<SomeType5> fallibleComputation2(SomeType4 value) {
    //...
}
```

#### Application-specific errors
There are two main approaches for application-specific errors. The choice between them depends mostly on particular use case: if errors are fixed and don't require passing additional information with them, then errors could be organized as Java enums:

```java
public enum ServiceHealthError implements Cause {
    INVALID_HEARTBEAT_RESPONSE("Invalid heartbeat response"),
    TIMEOUT("Timeout"),
    PERMANENTLY_DOWN("Permanently down");
    
    private final String message;
    
    ServiceHealthError(String message) {
        this.message = message;
    }
  
    public String message() {
        return message;
    }
} 
```
But in most cases the error must contain additional information. Sealed classes and records enable compact implementation of such errors:

```java
public sealed interface CoreError extends Cause {
    record Cancelled(String message) implements CoreError {}
    record Timeout(String message) implements CoreError {}
    record Fault(String message) implements CoreError {}
}
```
#### Extended Tracing
Sometimes, especially if same errors could be produced via different execution paths, it might be useful to provide additional information about the execution path. For this purpose `Result<T>` monad provides `trace()` method, which provides information about source code location when error passes through it. The `trace()` method is designed to be used in the following way:

```java
Result<SomeType5> method(SomeType1 inputParameter) {
    return method1(inputParameter)
        .flatMap(this::fallibleComputation1)
        .map(this::computation2)
        .flatMap(this::fallibleComputation2)
        .trace();
}
```
The `trace()` method can be placed anywhere in the chain of transformations and as many times as necessary. It is triggered only if error passes through it, otherwise it has no effect and does not cause any overhead.

### Promise&lt;§T>
The `Promise<T>` monad represents a computation that may fail and the result of computation is eventually available. The `Promise<T>` is very similar to `Result<T>` and underlying mental model is designed to be very similar too: all `map()` and `flatMap()` transformations are applied in order they are written in code. This makes code easy to reason about despite the asynchronous nature of computations. Nevertheless, there are cases, when strict sequential processing is not necessary. For this purpose `Promise<T>` contains a set of methods, which are designed to perform actions in parallel with main processing pipeline. 

Basic usage looks like shown below. All actions are performed sequentially as they are written in code:

```java
Promise<SomeType5> method(SomeType1 inputParameter) {
    return method1(inputParameter)
        .flatMap(this::fallibleComputation1)
        .map(this::computation2)
        .flatMap(this::fallibleComputation2);
}
```

It is also possible to run actions in parallel:

```java
Promise<SomeType5> method(SomeType1 inputParameter) {
    return method1(inputParameter)
        .flatMap(this::fallibleComputation1)
        .map(this::computation2)
        .flatMap(this::fallibleComputation2)
        .onResultAsync(this::independentAction1)
        .onResultRunAsync(this::independentAction2);
}

void independentAction1(Result<SomeOtherType> result) {
    // Perform some independent action
}

void independentAction2() {
// Perform another independent action
}
```
Although independent actions run in parallel, the moment when they are started is tied to execution of previous dependent action or the moment of resolution of the Promise, if there are no previous dependent actions.

## Predicates

One of the distinctive features of the **Pragmatica Lite Core** library are type safe `all()` and `any()` predicates. 
For each monad there are implementations of `all()` and `any()` predicates, which provide a convenient tool for handling 
several monads at once. The API is carefully designed to be as convenient as possible, while still being type-safe and 
consistent. The underlying mental model for the `all()` predicate is following: all passed monads are checked 
for presence/success/awaited availability and then unwrapped values are passed to the provided function as parameters.

The `all()` predicate returns monad-specific version of the `Mapper` interface, which provides `map()` and `flatMap()` methods
similar to corresponding monads' methods.

Basic usage looks like this:

```java
Option<SomeType> method(SomeType1 parameter1, SomeType2 parameter2, SomeType3 parameter3) {
    var option1 = object1.method(parameter1, parameter2);
    var option2 = object2.method(parameter1, parameter2, parameter3);
    var option3 = object3.method(parameter1);

    return Option.all(option1, option2, option3)
          .map((value1, value2, value3) -> method(value1, value2, value3));
}
```
By using method references and excluding intermediate variables, same code can be written as follows: 

```java
Option<SomeType> method(SomeType1 parameter1, SomeType2 parameter2, SomeType3 parameter3) {
    return Option.all(object1.method(parameter1, parameter2),
                      object2.method(parameter1, parameter2, parameter3),
                      object3.method(parameter1))
                 .map(this::methodWithThreeParameters);
}

SomeType methodWithThreeParameters(SomeType1 value1, SomeType2 value2, SomeType3 value3) {
    //...
}
```

The output of the `map()` method is a new monad instance with the result of application of transformation 
to all three values. The `flatMap()` method behaves identically except transformation function returns a value wrapped 
into monad.

The `all()` predicate enables expression in code that computation of intermediate values are independent 
of each other. Traditional approach with sequential computation of each value does not provide such capability. 

The `Promise<T>` version of both predicates (`all()` and `any()`) enables effortless parallelization 
using `fan out -> fan in` pattern with convenient API and mental model:

```java
Promise<SomeType> method(SomeType1 parameter1, SomeType2 parameter2, SomeType3 parameter3) {
    return Promise.all(service1.method(parameter1, parameter2),
                       service2.method(parameter1, parameter2, parameter3),
                       service3.method(parameter1))
                  .map(this::methodWithThreeParameters);
}
```
Note that invoked methods return a `Promise<T>` which can still be awaiting completion of the underlying action. 
The `map()` method also returns not yet resolved promise. In other words, all processing is performed in 
non-blocking manner. When all values will be available and all involved promises will be resolved to 
successful result, the transformation function passed to `map()` will be executed.

