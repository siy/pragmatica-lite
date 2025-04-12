package org.pragmatica.examples.promise;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.util.List;

class AsynchronousUserFetchingExample {
    private final UserRepository userRepository = List::of;

    record User(String id, String name) {

        boolean isActive() {
            return true;
        }
    }

    // Synchronous API
    interface UserRepository {
        List<User> findAll();
    }

    // Run synchronous API asynchronously (1)
    Promise<List<User>> fetchUsers1() {
        return Promise.promise(promise ->
                                       promise.resolve(Result.lift(Causes::fromThrowable,
                                                                   userRepository::findAll)));
    }

    // Run synchronous API asynchronously (2)
    Promise<List<User>> fetchUsers2() {
        return Promise.promise(() -> Result.lift(Causes::fromThrowable, userRepository::findAll));
    }

    // Run synchronous API asynchronously (3) (recommended approach for this type of tasks)
    Promise<List<User>> fetchUsers3() {
        return Promise.lift(Causes::fromThrowable, userRepository::findAll);
    }

    // Alternative way using Promise.async
    Promise<List<User>> fetchUsers4() {
        var promise = Promise.<List<User>>promise();

        promise.async(p -> p.resolve(Result.lift(Causes::fromThrowable, userRepository::findAll)));

        return promise;
    }
}
