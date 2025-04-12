package org.pragmatica.examples.promise;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.io.TimeSpan;

import java.util.List;

class AsynchronousUserDataProcessingExample {
    record UserData(String userId, String name, String email) {}

    void retrieveAndPrintUserSummaries() {
        //List<String>
        var userIds = List.of("user1", "user2", "user3", "user4", "user5");

        // Process each user ID in parallel
        List<Promise<UserData>> promises = userIds.stream()
                                                  .map(this::processUserAsync)
                                                  .toList();

        // Wait for all to complete
        Promise.allOf(promises)
               .flatMap(users -> Result.allOf(users).async())
               .onSuccess(users -> users.forEach(this::displayUserSummary))
               .onFailure(System.err::println);
    }

    Promise<UserData> processUserAsync(String userId) {
        // Simulate processing
        return Promise.promise(TimeSpan.timeSpan(100).millis(),
                               promise -> promise.succeed(new UserData(userId,
                                                                       "User " + userId,
                                                                       "user" + userId + "@example.com")));
    }

    void displayUserSummary(UserData userData) {
        System.out.println("User ID: " + userData.userId());
        System.out.println("Name: " + userData.name());
        System.out.println("Email: " + userData.email());
    }
}