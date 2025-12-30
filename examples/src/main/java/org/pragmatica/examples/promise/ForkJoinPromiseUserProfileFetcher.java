package org.pragmatica.examples.promise;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.utils.Causes;

import java.util.List;
import java.util.UUID;

class ForkJoinPromiseUserProfileFetcher {
    private final UserService userService = _ -> Causes.cause("Not implemented")
                                                       .promise();
    private final PostService postService = _ -> Causes.cause("Not implemented")
                                                       .promise();
    private final FriendService friendService = _ -> Causes.cause("Not implemented")
                                                           .promise();

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
}
