package org.pragmatica.examples.promise;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.Causes;

class PromiseResultAwaitAndPatternMatchingExample {
    void resultAwait() {
        Promise<Integer> promise = calculateAsync(42);

        // Wait for the promise to resolve with a timeout
        Result<Integer> result = promise.await(TimeSpan.timeSpan(5).seconds());

        // Process the result using built-in APIs (recommended approach)
        result.onSuccess(value -> System.out.println("Result: " + value))
              .onFailure(cause -> System.err.println("Operation failed: " + cause.message()));

        // Process the result using pattern matching in instanceof
        if (result instanceof Result.Success<Integer>(Integer value)) {
            System.out.println("Result: " + value);
        } else if (result instanceof Result.Failure<Integer>(Cause cause)) {
            System.err.println("Operation failed: " + cause.message());
        }

        // Using pattern matching (Java 21+)
        switch (result) {
            case Result.Success<Integer> success -> System.out.println("Result: " + success.value());
            case Result.Failure<Integer> failure ->
                    System.err.println("Operation failed: " + failure.cause().message());
        }
    }

    private Promise<Integer> calculateAsync(int i) {
        return Promise.promise(TimeSpan.timeSpan(1).seconds(), () -> Result.success(i * 2));
    }

    void toPromiseConversions() {
        // Use default cause (CoreError.emptyOption()) if Option is empty
        var fromOption1 = Option.option("Some value").async();

        // Use specific cause if Option is empty
        var fromOption2 = Option.option("Some value").async(Causes.cause("Another cause"));

        // Retrieve the Promise from provided supplier for the empty Option
        var fromOption3 = Option.option("Some other value").async(() -> Promise.promise());

        // Convert Result into resolved Promise
        var fromResult1 = Result.success("Some value").async();

    }

    void asynchronousInvocation() {
        var unitPromise = Promise.async(() -> doSomethingAsynchronously());

        // Run lambda and eventually resolve Promise with the returned Result.
        var promise1 = Promise.promise(() -> Result.success("Some value"));

        // Run lambda and do whatever necessary with the provided Promise instance
        var promise2 = Promise.promise(promise -> promise.succeed("Some value"));

        // Execute passed lambda after specified delay
        var promise3 = Promise.promise(TimeSpan.timeSpan(5).seconds(), promise -> promise.succeed(123));

        // Same, but Result returned by supplier is used to resolve the Promise
        var promise4 = Promise.promise(TimeSpan.timeSpan(5).seconds(), () -> Result.success("Some value"));
    }

    private void doSomethingAsynchronously() {

        // Use library method to convert exception into Cause instance
        var promise1 = Promise.lift(Causes::fromThrowable, () -> throwingMethodReturningValue());

        // Use library method to handle exceptions. This time no value is expected
        // and Promise<Unit> is returned.
        var promise2 = Promise.lift(Causes::fromThrowable, () -> throwingMethod());

        // Fixed cause
        var promise3 = Promise.lift(Causes.cause("Call failed"), () -> throwingMethodReturningValue());
        var promise4 = Promise.lift(Causes.cause("This one failed too"), () -> throwingMethod());
    }

    private void throwingMethod() throws Exception {
        throw new Exception("Some exception");
    }
    private Object throwingMethodReturningValue() throws Exception {
        throw new Exception("Some exception");
    }
}
