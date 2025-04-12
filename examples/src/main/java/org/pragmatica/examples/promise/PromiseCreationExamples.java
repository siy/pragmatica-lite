package org.pragmatica.examples.promise;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.io.CoreError;
import org.pragmatica.lang.utils.Causes;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

class PromiseCreationExamples {

    void promiseCreationExamples() {
        // Create an already resolved Promise with a value
        var successPromise = Promise.success("Success value");

        // Create an already failed promise
        var failedPromise = Promise.<String>failure(new CoreError.Fault("Operations failed"));

        // Alternative approach
        var anotherFailedPromise = new CoreError.Fault("Operation failed").promise();

        // Create a Promise that resolves after a delay
        var delayedPromise = Promise.<String>promise(timeSpan(2).seconds(),
                                                     promise -> promise.succeed("Delayed result"));

        // Asynchronously resolve Promise with result of synchronous operation
        var anotherPromise = Promise.promise(() -> Result.success("Synchronous result"));


        Option<String> stringOption = Option.option("Some value");
        var fromOption1 = stringOption.<Promise<String>>fold(Causes.cause("Potential null value")::promise,
                                                             Promise::success);
        var fromOption2 = Option.option("Some value")
                                .async(CoreError.emptyOption());
        var fromOption3 = Option.option("Some value").async(Causes.cause("Potential null value"));
    }

    void sideEffects() {
        var promise = Promise.<String>promise();

promise.onSuccess(System.out::println)
       .onFailure(System.err::println)
       .onSuccessRun(() -> System.out.println("Side effect on success"))
       .onFailureRun(() -> System.err.println("Side effect on failure"));
    }
}