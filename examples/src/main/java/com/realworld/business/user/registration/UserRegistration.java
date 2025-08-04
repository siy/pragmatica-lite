package com.realworld.business.user.registration;

import com.realworld.business.user.registration.domain.*;
import com.realworld.business.user.registration.domain.Error.BusinessError;
import com.realworld.business.user.registration.hashpassword.PasswordHasher;
import org.pragmatica.lang.Promise;

import java.util.UUID;

/// User registration endpoint that validates and transforms registration requests
/// This endpoint performs input validation and transforms raw registration data
/// into a validated request object suitable for further business processing
public interface UserRegistration {

    /// Processes user registration from validation to user creation
    /// Validates input, checks uniqueness constraints, hashes password,
    /// creates user entity, stores it, and generates authentication token
    /// - **request**: Raw registration data from client
    /// - **Returns**: Complete registration response with user data and token
    Promise<Response> perform(Request request);

    /// Creates a UserRegistration endpoint instance with all required dependencies
    /// - **userDataStore**: Mock data store for user persistence
    /// - **passwordHasher**: Password hashing utility
    /// - **jwtTokenGenerator**: JWT token generation utility
    /// - **Returns**: Configured endpoint ready for use
    static UserRegistration userRegistration(UniquenessChecker checker,
                                             PasswordHasher passwordHasher,
                                             JwtTokenGenerator jwtTokenGenerator) {
        record UserRegistrationImpl(MockUserDataStore userDataStore, PasswordHasher passwordHasher,
                                    JwtTokenGenerator jwtTokenGenerator) implements UserRegistration {
            @Override
            public Promise<Response> perform(Request request) {
                return ParsedRequest.parse(request).async()
                                    .flatMap(this::checkUniqueness)
                                    .flatMap(this::replacePasswordWithHashed)
                                    .flatMap(this::storeUser)
                                    .flatMap(this::generateTokenAndCreateResponse);
            }

            private Promise<ParsedRequest> checkEmailIsUnique(ParsedRequest request) {
                return userDataStore.isEmailTaken(request.email())
                                    .flatMap(emailTaken -> emailTaken 
                                            ? Promise.failure(BusinessError.EmailError.alreadyTaken(request))
                                            : Promise.success(request));
            }

            private Promise<ParsedRequest> checkUsernameIsUnique(ParsedRequest request) {
                return userDataStore.isUsernameTaken(request.username())
                                    .flatMap(usernameTaken -> usernameTaken 
                                            ? Promise.failure(BusinessError.NameError.alreadyTaken(request))
                                            : Promise.success(request));
            }

            private Promise<RegisteredUser> replacePasswordWithHashed(ParsedRequest request) {
                return passwordHasher.hashPassword(request.password())
                                     .async()
                                     .map(passwordHash -> new RegisteredUser(
                                             new Profile(
                                                     new UserId(UUID.randomUUID().toString()),
                                                     Email.parse(request.email()).unwrap(),
                                                     Name.parse(request.username()).unwrap(),
                                                     new ProfileDetails(null, null)
                                             ),
                                             new com.realworld.business.user.registration.domain.Password(passwordHash),
                                             java.time.Instant.now(),
                                             java.time.Instant.now()
                                     ));
            }

            private Promise<RegisteredUser> storeUser(RegisteredUser user) {
                return userDataStore.saveUser(user);
            }

            private Promise<Response> generateTokenAndCreateResponse(RegisteredUser user) {
                return jwtTokenGenerator.generateToken(user.profile().id().id())
                                        .async()
                                        .map(token -> new Response(
                                                user.profile(),
                                                new AuthenticationToken(token)
                                        ));
            }
        }

        return new UserRegistrationImpl(userDataStore, passwordHasher, jwtTokenGenerator);
    }
}