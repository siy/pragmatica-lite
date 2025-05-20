# Asynchronous Processing in Java with Promises

Traditionally asynchronous processing is considered complex and error-prone. There are several approaches to address this issue:

- The async/await language constructs focusing on making asynchronous code look like synchronous code. Unfortunately, this approach
  never achieves the goal, code remains complex and error-prone. Errors like accidental invocation of synchronous method within asynchronous context are difficult to spot while
  they easily can effectively kill scalability.
- Classic threading in various forms and shapes. Java with Executors or Virtual Threads, Go with goroutines, etc. The core idea remains the same:
  expose all internals and let users handle all this. Tools like Structured Concurrency make it somewhat bearable, but
  the approach remains complex to use and prone to various kinds of difficult to nail down and fix errors like deadlocks and alike.
- Third approach is to build composable processing pipelines with Reactive Streams. Unfortunately, design decisions
  (namely pull model and artificial "everything is a stream" mental model) resulted in convoluted API and several technical details leaking
  into user code (schedulers, subscribing, back pressure, etc.). This made Reactive Streams famous for being difficult to master and reason about,
  especially for non-trivial processing scenarios.
- Fourth approach is Promises with functional style API. Unlike mentioned above approaches, API remains straightforward and code easy to read
  and reason about. There are other advantages as well: very few technical details leaking into the user code and simple mental model.
  Unlike Reactive Streams, Promises use push processing model.

> Push vs Pull Processing Model
> These models define how processing pipeline receives messages for processing. In push model events are
> pushed into pipeline and pipeline eventually produces a result. In contrast, in pull model pipeline retrieves
> events from the external source using built-in scheduling mechanisms. As a consequence, pull model requires
> backpressure to balance external source of events and productivity of the pipeline.

## The Promise Monad

So, what is `Promise<T>` in general?
> The `Promise<T>` is a representation of the computation, which eventually may succeed or fail. The
> promise has two main states - pending and resolved and, once resolved, two outcomes - success or failure.
> `Promise<T>` is one of three core [monads](https://dev.to/siy/beautiful-world-of-mondas-2cd6) which are used
> to represent [special states](https://dev.to/siy/leveraging-java-type-system-to-represent-special-states-688).
>
> The resolution may happen only once and is thread safe - many threads may try to resolve `Promise<T>`,
> but only one value will be accepted. Application of the transformations provided via `map()` and `flatMap()` methods
> (as well as few others, see below for more details) is postponed until the `Promise<T>` instance is resolved.
> From this point of view, resolution serves as a synchronization point.

As mentioned above, `Promise<T>` API has two main transformation methods, `map()` and `flatMap()`. The `map()` transforms value if `Promise<T>`
is resolved to `success`. The `map()` does not change the outcome, `success` remains `success`, `failure` remains `failure`.
The `flatMap()` may change the outcome if transformation function passed to `flatMap()` return `failure`. Just like `Optional<T>`,
transformations are applied to `Promise<T>` in the order they are written in the code. This mental model is easy to understand
and adopt, resulting in good ergonomics.

Besides transformations, there are methods to attach `side effects`, i.e. actions which are submitted to execution either, at the moment of
`Promise<T>` resolution or (if `Promise<T>` is already resolved) immediately. The execution of each side effect happens
asynchronously and independently of the other side effects or transformations. The core `side effect` method is `onResult()`,
which asynchronously executes provided `Consumer<Result<T>>` instance once `Promise<T>` is resolved. Since dealing with whole `Result<T>`
is often inconvenient and verbose, there are other helper methods: `onSuccess()`, `onFailure()`, `onResultRun()`, `onSuccessRun()` and `onFailureRun()`
which cover various use cases.

The resolution of `Promise<T>` can be awaited. This rarely necessary in the production code, but extremely useful
for testing.

So, let's take a look how functional style Promises API looks like.

> Important Coding Style Notice
> It is highly recommended to use Single Level of Abstraction principle, while writing
> code which uses `Promise<T>` and functional style code in general. Consistent application of this
> principle keeps code easy to write and reason about. Use of complex lambdas quickly results in tangled,
> hard to read and maintain code.

## Basic Examples

Create unresolved promise:
```java
var promise = Promise.<String>promise();
```

Create immediately or eventually resolved promise:
```java
// Create an already resolved Promise with a value
var successPromise = Promise.success("Success value");

// Create an already failed Promise
var failedPromise = Promise.<String>failure(new CoreError.Fault("Operations failed"));

// Alternative (recommended) approach for creating failed Promise
var anotherFailedPromise = new CoreError.Fault("Operation failed").promise();

// Create a Promise that resolves after a delay
var delayedPromise = Promise.<String>promise(timeSpan(2).seconds(),
                                             promise -> promise.succeed("Delayed result"));

// Asynchronously resolve Promise with result of synchronous operation
var anotherPromise = Promise.promise(() -> Result.success("Synchronous result"));
```
Other core monads (`Option<T>` and `Result<T>`) can be transformed into `Promise<T>`:

```java
// Use default cause (CoreError.emptyOption()) if Option is empty
var fromOption1 = Option.option("Some value").async();

// Use specific cause if Option is empty
var fromOption2 = Option.option("Some value").async(Causes.cause("Another cause"));

// Retrieve the Promise from provided supplier for the empty Option
var fromOption3 = Option.option("Some other value").async(() -> Promise.promise());

// Convert Result into resolved Promise
var fromResult1 = Result.success("Some value").async();
 
```
All such conversions produce already resolved Promise instance except the last conversion from `Option<T>`.
It will produce resolved `Promise<T>` instance for present `Option<T>`, but the state of the `Promise<T>` created by
provided supplier depends on particular supplier implementation.

Transform `Promise<T>` into `Result<T>` (see note above about waiting `Promise<T>` for resolution):
```java
var promise = ...;

// Wait indefinitely for Promise resolution
var result = promise.await();  

// Wait for resolution for 10 seconds and if Promise is still not resolved 
// return failure Result with CoreError.Timeout() as a cause.
var result = promise.await(TimeSpan.timeSpan(10).seconds());
```

Launch asynchronous operation:
```java
// General purpose asynchronous invocation method.
// Returns Promise<Unit> which is resolved when passed lambda finishes execution.
var unitPromise = Promise.async(() -> doSomethingAsynchronously());

// Run lambda and eventually resolve Promise with the returned Result.
var promise1 = Promise.promise(() -> Result.success("Some value"));

// Run lambda and do whatever necessary with the provided Promise instance
var promise2 = Promise.promise(promise -> promise.succeed("Some value"));

// Execute passed lambda after specified delay
var promise3 = Promise.promise(TimeSpan.timeSpan(5).seconds(), promise -> promise.succeed(123));

// Same, but Result returned by supplier is used to resolve the Promise
var promise4 = Promise.promise(TimeSpan.timeSpan(5).seconds(), () -> Result.success("Some value"));
```
Separate category of asynchronous invocations: ones created for interfacing with imperative code:

```java
// Use library method to convert exception into Cause instance
var promise1 = Promise.lift(Causes::fromThrowable, () -> throwingMethodReturningValue());

// Use library method to handle exceptions. This time no value is expected
// and Promise<Unit> is returned.
var promise2 = Promise.lift(Causes::fromThrowable, () -> throwingMethod());

// Fixed cause
var promise3 = Promise.lift(Causes.cause("Call failed"), () -> throwingMethodReturningValue());
var promise4 = Promise.lift(Causes.cause("This one failed too"), () -> throwingMethod());
```
These methods enable convenient asynchronous invocation of the existing code. Note that since `Promise<T>` implementation
is based on virtual threads, such calls are handled by JVM and can be efficiently scaled, especially if they perform
network I/O.

Launching independent actions upon resolution (aka `side effects`):
```java
promise.onSuccess(System.out::println) // Print value in case of success
       .onFailure(System.err::println) // Print cause of the error
       .onResult(System.out::println)  // Print result upon resolution
       .onSuccessRun(() -> System.out.println("Side effect on success"))  // Run action in case of success
       .onFailureRun(() -> System.err.println("Side effect on failure"))  // Run action in case of failure
       .onResultRun(() -> System.err.println("Side effect upon resolution")); // Run action once instance is resolved
```
The `side effects` are useful for performing operations, whose outcome is irrelevant for the processing pipeline success or failure.
Asynchronous execution of `side effects` means that they can't block or otherwise impact main processing pipeline.

## Asynchronous Patterns

Below described typical asynchronous processing patterns which can be efficiently implemented with `Promise<T>`.

### Sequencer

This is nothing else than the asynchronous equivalent of synchronous execution. Each operation starts when the previous
one is finished. The main advantage of the `Promise<T>` in this scenario is that the thread is not blocked when
operations are executed. Instead, `Promise<T>` just sits in memory until resolution at each step and immediately launches the next
operation and releases the thread. Such behavior makes asynchronous processing pipeline extremely scalable. Another
advantage - when the system reaches saturation (i.e. incoming requests coming as fast as the system is physically
capable of processing them due to CPU limits), further increase in the load causes graceful performance degradation.
Graceful performance degradation is more preferred than abrupt performance degradation observed in traditional
synchronous designs with thread pool.

Sequential processing example:
```java
// Example data records
record UserId(String id) {}

record User(UserId id, String name) {}

record Order(UserId userId, String description) {}

record Invoice(List<Order> orders) {}

// Example services
interface UserRepository {
    Promise<User> findUserById(UserId userId);
}

interface OrderRepository {
    Promise<List<Order>> findOrdersByUser(User user);
}

interface InvoiceService {
    Promise<Invoice> createInvoice(List<Order> orders);
}

interface EmailService {
    void sendInvoice(Invoice invoice);
}

interface LogService {
    void logError(String message, Cause cause);
}

// Format business logic as a sequence of operations
Promise<Invoice> processUserOrders(UserId userId) {
    return userRepository.findUserById(userId)
                         .flatMap(orderRepository::findOrdersByUser)
                         .flatMap(invoiceService::createInvoice)
                         .onSuccess(emailService::sendInvoice)
                         .onFailure(cause -> logService.logError("Invoice generation failed", cause));
}
```

### Fork-Join

The asynchronous nature of `Promise<T>` in some cases enables transformation of sequential execution into parallel one.
The main condition (which is quite frequently satisfied in practice) - independence of each operation. This is a very
natural and effortless approach for speeding-up processing, especially for I/O operations. Usually, this pattern is
called “Fan-Out-Fan-In” or “Fork-Join”. The first step is to launch several operations in parallel. Each operation is
represented by the `Promise<T>` instance. The next step is to collect and process all the results. There are several
possible use cases, each is covered by a dedicated `Promise<T>` predicate.

### The all() Predicate (Classic Join)

This one covers the most frequent case: several results, each of its own type, need to be consolidated:

```java
// Example data records
record UserId(UUID id) {}

record PostId(UUID id) {}

record UserData(UserId userId, String name, String email) {}

record Post(PostId postId, String content) {}

record Friend(UserId friendId, String name) {}

record UserProfile(UserData userData, List<Post> posts, List<Friend> friends) {}

// Example services
interface UserService {
    Promise<UserData> fetchUserData(UserId userId);
}

interface PostService {
    Promise<List<Post>> fetchUserPosts(UserId userId);
}

interface FriendService {
    Promise<List<Friend>> fetchUserFriends(UserId userId);
}

Promise<UserProfile> fetchUserProfile(UserId userId) {
    return Promise.all(userService.fetchUserData(userId),
                       postService.fetchUserPosts(userId),
                       friendService.fetchUserFriends(userId))
                  .map(UserProfile::new);
}
```

Note that the function passed as a parameter to `map()` or `flatMap()` methods of predicate output is invoked only if all
operations were successful. Any errors are automatically propagated, and the processing pipeline is short-circuited.
Function parameters have the same order and type as `Promise<T>` instances passed to the `all()` predicate, making using it
straightforward.

### The any() Predicate (Rat Race)

This predicate covers the case, when only one result is necessary from the launched several ones. Typical scenario -
get some information from different providers. The source is not relevant, so anyone who first provides a successful
result wins the race. Notice that all sources produce a result of the same type:
```java
// Example data record
record WeatherInfo(String city, String temperature) {}

// Example service interface
interface WeatherService {
    Promise<WeatherInfo> fetchWeatherInfo(String city);
}

Promise<WeatherInfo> fetchWeatherInfo(String city) {
    return Promise.any(openWeatherMapService.fetchWeatherInfo(city),
                       weatherstackService.fetchWeatherInfo(city),
                       accuWeatherService.fetchWeatherInfo(city),
                       NWService.fetchWeatherInfo(city));
}
```

Just like the `all()` predicate, `any()` handles errors transparently, returning failure only if all operations failed.

### The allOf() Predicate (Single Type Join)

This predicate covers the case when several results of the same type should be collected. Unlike `all()` and `any()`, this
predicate collects all results (successes and failures) and passes them as a single list of results:
```java
var promises = IntStream.range(0, 10)
                        .mapToObj(i -> Promise.promise(() -> Result.success(i)))
                        .toList();

Promise.allOf(promises)
       .onSuccess(results -> results.forEach(System.out::println))
       .onFailure(System.err::println);
```

Further processing of the list depends on the use case. In some case, for example, `Result.allOf()` might be helpful to
extract values into `List<T>`:
```java
// Promise<List<T>>
var list = Promise.allOf(promises)
                  .map(results -> Result.allOf(results).async()) // .async() converts Result<T> into Promise<T>
                  .onFailure(System.err::println);

```

### Error Recovery (Fallback)

Sometimes it is necessary to use an alternative source of information if the main one fails. For this purpose,
`Promise<T>` has special transformation - `recover()`:
```java
var promise = Promise.success("Success");

promise.recover(cause -> "Alternative result");
```
The case above just replaces the value producing the resolved `Promise<T>` immediately. Sometimes it is necessary
to perform other operation to obtain the replacement result:

```java
promise.orElse(performAnotherOperation());
promise.orElse(() -> performAnotherOperation());
```
Two forms of `orElse()` method are similar, except the second one will invoke the method only if `Promise<T>` failed.

### Retry and Circuit Breaker

The `Promise<T>` is accompanied by two utility classes, which implement frequently observed scenarios: retrying operations and preventing
cascade failures.

`Retry` performs the operation as many times as necessary to get a result (or fail, if all attempts failed):
```java
// Example data records
record Amount(BigDecimal value) {}

record Payment(UserId userId, Amount amount, Currency currency) {}

record PaymentConfirmation(String message) {}

// Example service interface
interface PaymentService {
    Promise<PaymentConfirmation> processPayment(Payment payment);
}

// Repeat attempts at most 5 times, retry every 2 seconds
private Retry retry = Retry.create(5, fixed(timeSpan(2).seconds()));

Promise<PaymentConfirmation> processPayment(Payment payment) {
    return retry.execute(() -> paymentService.processPayment());
}

```

`Retry` has support for several different backoff strategies - linear, exponential and fixed:
```java
// Linear
var linear = linear().initialDelay(timeSpan(50).millis())
                     .increment(timeSpan(100).millis())
                     .maxDelay(timeSpan(1).seconds());

// Exponential
var strategy2 = exponential().initialDelay(timeSpan(50).millis())
                             .maxDelay(timeSpan(1).seconds())
                             .factor(2.0)
                             .withoutJitter();
// Fixed 
var strategy3 = fixed().interval(timeSpan(50).millis());
```

`CircuitBreaker` (obviously) implements a classic pattern with the same name. The API is very similar to the `Retry`:
```java
// Configure circuit breaker
var breaker = CircuitBreaker.builder()
                            .failureThreshold(3)
                            .resetTimeout(timeSpan(100).millis())
                            .testAttempts(2)
                            .shouldTrip(cause -> cause == TEST_ERROR)
                            .withDefaultTimeSource();

// Use to protect endpoint
return circuitBreaker.execute(() -> service.processOrder(order));

```

Note that both utility classes are thread safe. There is a difference, though: `Retry` is entirely stateless, so one can
create one or few differently configured instances and use them safely through the code for different endpoints.
The `CircuitBreaker` is stateful, so, while several threads could call an external endpoint protected by the same `CircuitBreaker`,
each external endpoint must have a dedicated `CircuitBreaker` instance.

## Pragmatica Lite Core Library
The **Pragmatica Lite Core Library** contains implementation of all three core monads, as well as several utility classes.
To use it in a Maven project, one needs to include the following repository description:
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
And then add the following dependency (most recent version at the time of writing):

```xml
    <dependency>
        <groupId>org.pragmatica-lite</groupId>
        <artifactId>core</artifactId>
        <version>0.7.1</version>
    </dependency>
```

## Conclusion

Functional style `Promise<T>` is a powerful yet easy to use tool. Code written with `Promise<T>` is easy to reason about
and understand, although keeping code at a single level of abstraction is highly recommended, to preserve clarity.
Simple mental model and very few technical details leaking into the user code, making `Promise<T>` the best tool for
implementing highly scalable business logic.  
