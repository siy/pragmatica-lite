package org.pragmatica.examples.promise;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import java.util.stream.IntStream;

public class PromiseAllOfExample {
    public static void main(String[] args) {
        var promises = IntStream.range(0, 10)
                                .mapToObj(i -> Promise.promise(() -> Result.success(i)))
                                .toList();

        Promise.allOf(promises)
               .onSuccess(results -> results.forEach(System.out::println))
               .onFailure(System.err::println);

        Promise.allOf(promises)
               .map(results -> Result.allOf(results).async())
               .onFailure(System.err::println);

        var promise = Promise.success("Success");

        promise.recover(cause -> "Alternative result");

        promise.orElse(performAnotherOperation());
        promise.orElse(() -> performAnotherOperation());
    }

    private static Promise<String> performAnotherOperation() {
        return null;
    }
}
